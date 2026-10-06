"""Compare generated OpenNSFW2 candidates on deterministic synthetic tensors."""
from __future__ import annotations

import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import sys


ROOT = Path(__file__).resolve().parents[1]
WEIGHTS = ROOT / "build/model-conversion/inputs/open_nsfw_weights.h5"
SOURCE_MANIFEST = ROOT / "docs/model-conversion/SOURCE_MANIFEST.json"
SELECTED_MODEL = ROOT / "safety-opennsfw2/src/main/assets/model/sakos_nudity_model.tflite"
SELECTED_MODEL_SHA = "bea35dc93c86f074ae9a047638773aff9eb84c05e6ead8d785af5c8ddde05518"
TOLERANCES = {"float32": 1e-4, "dynamic-range": 1e-2}
THRESHOLDS = (0.45, 0.75, 0.85)


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def generated_inputs(np):
    height = width = 224
    axis = np.linspace(0.0, 255.0, width, dtype=np.float32)
    x = np.tile(axis[None, :], (height, 1))
    y = np.tile(axis[:, None], (1, width))
    checker = ((np.indices((height, width)).sum(axis=0) % 2) * 255).astype(np.float32)
    rng = np.random.default_rng(20261005)
    raw_patterns = {
        "black": np.zeros((height, width, 3), dtype=np.float32),
        "white": np.full((height, width, 3), 255.0, dtype=np.float32),
        "solid-color": np.broadcast_to(np.array([12.0, 128.0, 243.0], dtype=np.float32), (height, width, 3)).copy(),
        "horizontal-channel-gradient": np.stack((x, 255.0 - x, x / 2.0), axis=-1),
        "vertical-channel-gradient": np.stack((y, y / 2.0, 255.0 - y), axis=-1),
        "channel-ramp": np.broadcast_to(np.array([0.0, 127.0, 255.0], dtype=np.float32), (height, width, 3)).copy(),
        "checkerboard": np.stack((checker, 255.0 - checker, checker), axis=-1),
        "seeded-noise": rng.integers(0, 256, size=(height, width, 3), dtype=np.uint8).astype(np.float32),
    }
    mean_bgr = np.array([104.0, 117.0, 123.0], dtype=np.float32)
    return {name: np.expand_dims((pixels - mean_bgr).astype(np.float32), 0)
            for name, pixels in raw_patterns.items()}


def keras_prediction(model, input_tensor, np):
    result = np.asarray(model(input_tensor, training=False).numpy(), dtype=np.float32)
    return result.reshape(-1, 2)[0]


def tflite_prediction(tf, model_path: Path, input_tensor, np):
    interpreter = tf.lite.Interpreter(model_path=str(model_path), num_threads=1)
    interpreter.allocate_tensors()
    inputs = interpreter.get_input_details()
    outputs = interpreter.get_output_details()
    if len(inputs) != 1 or len(outputs) != 1:
        raise ValueError(f"Expected one input and output in {model_path}.")
    detail = inputs[0]
    if tuple(detail["shape"].tolist()) != (1, 224, 224, 3) or detail["dtype"].__name__ != "float32":
        raise ValueError(f"Unexpected input contract in {model_path}.")
    interpreter.set_tensor(detail["index"], input_tensor)
    interpreter.invoke()
    return np.asarray(interpreter.get_tensor(outputs[0]["index"]), dtype=np.float32).reshape(-1, 2)[0]


def validate_probabilities(values, label: str, np) -> None:
    if values.shape != (2,) or not np.isfinite(values).all():
        raise ValueError(f"{label} emitted non-finite or malformed probabilities: {values!r}.")
    if np.any(values < -1e-6) or np.any(values > 1.000001):
        raise ValueError(f"{label} emitted out-of-range probabilities: {values!r}.")
    if abs(float(values.sum()) - 1.0) > 1e-4:
        raise ValueError(f"{label} probabilities do not sum to one: {values!r}.")


def compare_one(tf, keras_model, inputs, candidate: Path, variant: dict, np) -> dict:
    model_path = candidate / variant["file"]
    if not model_path.is_file() or sha256_file(model_path) != variant["sha256"]:
        raise ValueError(f"Candidate file does not match its conversion manifest: {model_path}.")
    optimization = variant["optimization"]
    reference_results = {}
    candidate_results = {}
    selected_results = {}
    for name, tensor in inputs.items():
        reference = keras_prediction(keras_model, tensor, np)
        converted = tflite_prediction(tf, model_path, tensor, np)
        selected = tflite_prediction(tf, SELECTED_MODEL, tensor, np)
        validate_probabilities(reference, f"Keras/{name}", np)
        validate_probabilities(converted, f"{optimization}/{name}", np)
        validate_probabilities(selected, f"selected SDK TFLite/{name}", np)
        reference_results[name] = reference
        candidate_results[name] = converted
        selected_results[name] = selected

    errors = np.stack([np.abs(candidate_results[name] - reference_results[name]) for name in inputs])
    selected_errors = np.stack([np.abs(selected_results[name] - reference_results[name]) for name in inputs])
    selected_deltas = np.stack([np.abs(candidate_results[name] - selected_results[name]) for name in inputs])
    max_error = float(errors.max())
    mean_error = float(errors.mean())
    selected_max_error = float(selected_deltas.max())
    tolerance = TOLERANCES[optimization]
    boundary_effects = {}
    for threshold in THRESHOLDS:
        differences = [name for name in inputs
                       if (candidate_results[name][1] >= threshold) != (reference_results[name][1] >= threshold)]
        boundary_effects[f"{threshold:.2f}"] = differences
    selected_boundary_effects = {}
    for threshold in THRESHOLDS:
        selected_boundary_effects[f"{threshold:.2f}"] = [
            name for name in inputs
            if (selected_results[name][1] >= threshold) != (reference_results[name][1] >= threshold)
        ]
    selected_model_boundary_effects = {}
    for threshold in THRESHOLDS:
        selected_model_boundary_effects[f"{threshold:.2f}"] = [
            name for name in inputs
            if (candidate_results[name][1] >= threshold) != (selected_results[name][1] >= threshold)
        ]
    return {
        "candidate": candidate.name,
        "optimization": optimization,
        "sha256": variant["sha256"],
        "tolerance_max_absolute_error": tolerance,
        "candidate_max_absolute_error_vs_keras": max_error,
        "candidate_mean_absolute_error_vs_keras": mean_error,
        "candidate_error_by_pattern_vs_keras": {
            name: float(np.abs(candidate_results[name] - reference_results[name]).max()) for name in inputs
        },
        "numerical_tolerance_passed": max_error <= tolerance,
        "candidate_boundary_effects_vs_keras": boundary_effects,
        "selected_model_max_absolute_error_vs_keras_informational_only": float(selected_errors.max()),
        "selected_model_boundary_effects_vs_keras_informational_only": selected_boundary_effects,
        "candidate_max_absolute_error_vs_selected_model": selected_max_error,
        "candidate_boundary_effects_vs_selected_model": selected_model_boundary_effects,
        "probabilities_by_pattern": {
            name: {"keras": reference_results[name].tolist(),
                   "candidate": candidate_results[name].tolist(),
                   "selected_sdk_tflite": selected_results[name].tolist()}
            for name in inputs
        },
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("candidate_dirs", nargs="+", type=Path,
                        help="One or more converter output directories, relative to repo root or absolute.")
    parser.add_argument("--report", type=Path, default=Path("build/model-conversion/comparison.json"))
    args = parser.parse_args()

    source = json.loads(SOURCE_MANIFEST.read_text(encoding="utf-8"))["source"]
    if not WEIGHTS.is_file() or sha256_file(WEIGHTS).lower() != source.get("sha256", "").lower():
        raise SystemExit("The HDF5 source weights are absent or do not match SOURCE_MANIFEST.json.")
    if not SELECTED_MODEL.is_file() or sha256_file(SELECTED_MODEL) != SELECTED_MODEL_SHA:
        raise SystemExit("The selected SDK model differs from its recorded SHA-256.")

    os.environ["KERAS_BACKEND"] = "tensorflow"
    os.environ["TF_ENABLE_ONEDNN_OPTS"] = "0"
    os.environ["TF_CPP_MIN_LOG_LEVEL"] = "3"
    os.environ["CUDA_VISIBLE_DEVICES"] = "-1"
    import tensorflow as tf
    import opennsfw2
    import numpy as np

    if tf.__version__ != "2.20.0" or opennsfw2.__version__ != "0.15.2" or sys.version_info[:3] != (3, 12, 15):
        raise SystemExit("Run this comparison from the pinned Python/TensorFlow/OpenNSFW2 environment.")
    tf.get_logger().setLevel("ERROR")
    tf.config.threading.set_inter_op_parallelism_threads(1)
    tf.config.threading.set_intra_op_parallelism_threads(1)
    tf.config.set_visible_devices([], "GPU")
    keras_model = opennsfw2.make_open_nsfw_model(weights_path=str(WEIGHTS))
    inputs = generated_inputs(np)

    comparisons = []
    variants_by_run = {}
    for candidate in args.candidate_dirs:
        candidate_dir = candidate if candidate.is_absolute() else ROOT / candidate
        candidate_dir = candidate_dir.resolve()
        conversion_path = candidate_dir / "CONVERSION_MANIFEST.json"
        if not conversion_path.is_file():
            raise SystemExit(f"Missing conversion manifest: {conversion_path}.")
        conversion = json.loads(conversion_path.read_text(encoding="utf-8"))
        if conversion["source"]["sha256"] != source["sha256"]:
            raise SystemExit(f"Candidate source digest differs from the current pin: {candidate_dir}.")
        variants = conversion["candidates"]
        if {variant["optimization"] for variant in variants} != set(TOLERANCES):
            raise SystemExit(f"Candidate directory must contain float32 and dynamic-range outputs: {candidate_dir}.")
        variants_by_run[candidate_dir.name] = {variant["optimization"]: variant["sha256"] for variant in variants}
        for variant in variants:
            comparisons.append(compare_one(tf, keras_model, inputs, candidate_dir, variant, np))

    reproducibility = {}
    for optimization in TOLERANCES:
        observed = [run[optimization] for run in variants_by_run.values()]
        reproducibility[optimization] = {"run_count": len(observed), "sha256_by_run": observed,
                                         "identical_bytes": len(set(observed)) == 1}
    report = {
        "schema": 1,
        "created_utc": datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z"),
        "input_scope": "Eight generated tensor patterns only; no media corpus or captured images.",
        "weights_sha256": source["sha256"],
        "selected_model_sha256": SELECTED_MODEL_SHA,
        "input_contract": {"shape": [1, 224, 224, 3], "dtype": "float32",
                            "channels": "BGR", "mean_subtraction": [104, 117, 123]},
        "patterns": list(inputs),
        "threshold_boundary_checks": list(THRESHOLDS),
        "tolerances": TOLERANCES,
        "candidate_comparisons": comparisons,
        "numerical_tolerance_gate_passed": all(item["numerical_tolerance_passed"] for item in comparisons),
        "reproducibility": reproducibility,
        "interpretation": "Synthetic numerical comparison checks source-model tolerance and current selected-model differences only; it does not establish real-world efficacy.",
    }
    report_path = args.report if args.report.is_absolute() else ROOT / args.report
    report_path = report_path.resolve()
    ignored_root = (ROOT / "build/model-conversion").resolve()
    if not report_path.is_relative_to(ignored_root):
        raise SystemExit("Report must remain under ignored build/model-conversion/.")
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"report": report_path.relative_to(ROOT).as_posix(),
                      "patterns": len(inputs), "candidate_comparisons": len(comparisons),
                      "reproducibility": reproducibility,
                      "numerical_tolerance_gate_passed": report["numerical_tolerance_gate_passed"],
                      "comparison": "passed" if report["numerical_tolerance_gate_passed"] else "failed"}, indent=2))
    if not report["numerical_tolerance_gate_passed"]:
        raise SystemExit("One or more candidates exceeded the predeclared Keras parity tolerance.")


if __name__ == "__main__":
    main()
