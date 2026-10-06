"""Convert the pinned OpenNSFW2 HDF5 weights into isolated TFLite candidates."""
from __future__ import annotations

import argparse
from collections import Counter
from datetime import datetime, timezone
import hashlib
import importlib.metadata
import json
import os
from pathlib import Path
import platform
import tempfile
import sys
import io
import contextlib


ROOT = Path(__file__).resolve().parents[1]
SOURCE_MANIFEST = ROOT / "docs/model-conversion/SOURCE_MANIFEST.json"
LOCK_FILE = ROOT / "tools/model-conversion/uv.lock"
WEIGHTS = ROOT / "build/model-conversion/inputs/open_nsfw_weights.h5"
BASELINE_MODEL = ROOT / "safety-opennsfw2/src/main/assets/model/sakos_nudity_model.tflite"
MODEL_INPUT = (1, 224, 224, 3)
MODEL_OUTPUT = (1, 2)


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def package_versions() -> dict[str, str]:
    names = ["tensorflow", "opennsfw2", "keras", "numpy", "h5py", "flatbuffers"]
    return {name: importlib.metadata.version(name) for name in names}


def write_atomic(path: Path, contents: bytes) -> None:
    fd, temp_name = tempfile.mkstemp(prefix=path.name + ".", suffix=".tmp", dir=path.parent)
    try:
        with os.fdopen(fd, "wb") as stream:
            stream.write(contents)
            stream.flush()
            os.fsync(stream.fileno())
        os.replace(temp_name, path)
    finally:
        if os.path.exists(temp_name):
            os.unlink(temp_name)


def flatbuffer_summary(model_bytes: bytes) -> dict:
    from tensorflow.lite.python import schema_py_generated as schema
    import numpy as np

    model = schema.Model.GetRootAsModel(model_bytes, 0)
    interpreter = None
    import tensorflow as tf

    interpreter = tf.lite.Interpreter(model_content=model_bytes, num_threads=1)
    interpreter.allocate_tensors()
    inputs = interpreter.get_input_details()
    outputs = interpreter.get_output_details()
    if len(inputs) != 1 or len(outputs) != 1:
        raise ValueError("Candidate must have exactly one input and one output tensor.")
    input_detail, output_detail = inputs[0], outputs[0]
    if tuple(int(value) for value in input_detail["shape"]) != MODEL_INPUT:
        raise ValueError(f"Candidate input shape changed: {input_detail['shape']}.")
    if tuple(int(value) for value in output_detail["shape"]) != MODEL_OUTPUT:
        raise ValueError(f"Candidate output shape changed: {output_detail['shape']}.")
    if np.dtype(input_detail["dtype"]).name != "float32" or np.dtype(output_detail["dtype"]).name != "float32":
        raise ValueError("Candidate external tensors must remain float32.")

    operator_names = {value: name for name, value in vars(schema.BuiltinOperator).items()
                      if isinstance(value, int)}
    tensor_names = {value: name for name, value in vars(schema.TensorType).items()
                    if isinstance(value, int)}
    operators = []
    for index in range(model.OperatorCodesLength()):
        code = model.OperatorCodes(index)
        builtin = max(code.BuiltinCode(), code.DeprecatedBuiltinCode())
        custom = code.CustomCode()
        custom_name = custom.decode("utf-8", errors="replace") if custom else None
        if custom_name or operator_names.get(builtin, "").startswith("FLEX"):
            raise ValueError(f"Candidate contains a custom/Select-TF op: {custom_name or builtin}.")
        operators.append({"builtin": operator_names.get(builtin, f"BUILTIN_{builtin}"),
                          "version": code.Version(), "custom_code": custom_name})

    subgraph = model.Subgraphs(0)
    constant_types: Counter[str] = Counter()
    for index in range(subgraph.TensorsLength()):
        tensor = subgraph.Tensors(index)
        buffer = model.Buffers(tensor.Buffer())
        if buffer.DataLength() > 0:
            constant_types[tensor_names.get(tensor.Type(), f"TENSOR_TYPE_{tensor.Type()}")] += 1

    metadata = []
    for index in range(model.MetadataLength()):
        entry = model.Metadata(index)
        name = entry.Name().decode("utf-8", errors="replace") if entry.Name() else ""
        buffer = model.Buffers(entry.Buffer())
        raw = bytes(buffer.DataAsNumpy()) if buffer.DataLength() else b""
        try:
            decoded = raw.decode("utf-8").strip("\x00")
        except UnicodeDecodeError:
            decoded = None
        metadata.append({"name": name, "bytes": len(raw), "sha256": sha256_bytes(raw),
                         "utf8": decoded if decoded and all(ch.isprintable() for ch in decoded) else None})

    return {
        "flatbuffer_version": model.Version(),
        "inputs": [{"name": detail["name"], "shape": detail["shape"].tolist(),
                    "dtype": np.dtype(detail["dtype"]).name} for detail in inputs],
        "outputs": [{"name": detail["name"], "shape": detail["shape"].tolist(),
                     "dtype": np.dtype(detail["dtype"]).name} for detail in outputs],
        "operators": operators,
        "constant_tensor_types": dict(sorted(constant_types.items())),
        "metadata": metadata,
    }


def convert_one(tf, keras_model, output: Path, optimization: str) -> dict:
    converter = tf.lite.TFLiteConverter.from_keras_model(keras_model)
    converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS]
    converter.allow_custom_ops = False
    if optimization == "dynamic-range":
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
    elif optimization == "float32":
        converter.optimizations = []
    else:
        raise ValueError(f"Unexpected optimization: {optimization}")
    with contextlib.redirect_stdout(io.StringIO()), contextlib.redirect_stderr(io.StringIO()):
        converted = converter.convert()
    details = flatbuffer_summary(converted)
    write_atomic(output, converted)
    return {"file": output.name, "optimization": optimization,
            "bytes": len(converted), "sha256": sha256_bytes(converted), **details}


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output-dir", type=Path, required=True,
                        help="New, ignored output directory under build/model-conversion/.")
    args = parser.parse_args()
    output_dir = args.output_dir if args.output_dir.is_absolute() else ROOT / args.output_dir
    output_dir = output_dir.resolve()
    ignored_root = (ROOT / "build/model-conversion").resolve()
    if not output_dir.is_relative_to(ignored_root):
        raise SystemExit("Output must remain under ignored build/model-conversion/.")
    if output_dir.exists() and any(output_dir.iterdir()):
        raise SystemExit(f"Refusing to overwrite nonempty output directory: {output_dir}.")
    if not SOURCE_MANIFEST.is_file() or not LOCK_FILE.is_file() or not WEIGHTS.is_file():
        raise SystemExit("Missing source manifest, uv lock, or downloaded model weights.")

    manifest = json.loads(SOURCE_MANIFEST.read_text(encoding="utf-8"))
    source = manifest["source"]
    input_digest = sha256_file(WEIGHTS)
    if not source.get("sha256") or input_digest.lower() != source["sha256"].lower():
        raise SystemExit("Downloaded weights do not match the pinned source manifest.")
    if WEIGHTS.stat().st_size != int(source["reported_bytes"]):
        raise SystemExit("Downloaded weights do not match the published release size.")

    os.environ["KERAS_BACKEND"] = "tensorflow"
    os.environ["TF_ENABLE_ONEDNN_OPTS"] = "0"
    os.environ["TF_CPP_MIN_LOG_LEVEL"] = "3"
    os.environ["CUDA_VISIBLE_DEVICES"] = "-1"
    import tensorflow as tf
    import opennsfw2
    tf.get_logger().setLevel("ERROR")

    if platform.python_version() != "3.12.15":
        raise SystemExit(f"Expected Python 3.12.15, got {platform.python_version()}.")
    if tf.__version__ != "2.20.0" or importlib.metadata.version("opennsfw2") != "0.15.2":
        raise SystemExit("Conversion package versions differ from the locked selection.")
    if opennsfw2.__version__ != "0.15.2":
        raise SystemExit(f"Unexpected imported OpenNSFW2 version: {opennsfw2.__version__}.")
    tf.config.threading.set_inter_op_parallelism_threads(1)
    tf.config.threading.set_intra_op_parallelism_threads(1)
    try:
        tf.config.set_visible_devices([], "GPU")
    except RuntimeError as error:
        raise SystemExit(f"TensorFlow initialized devices before CPU-only setup: {error}") from error

    source_model = opennsfw2.make_open_nsfw_model(weights_path=str(WEIGHTS))
    if tuple(source_model.input_shape[1:]) != MODEL_INPUT[1:] or tuple(source_model.output_shape) != (None, 2):
        raise SystemExit(f"Unexpected OpenNSFW2 source model interface: {source_model.input_shape} -> {source_model.output_shape}.")
    fixed_input = tf.keras.Input(batch_size=1, shape=MODEL_INPUT[1:], dtype=tf.float32, name="input")
    fixed_output = source_model(fixed_input, training=False)
    keras_model = tf.keras.Model(fixed_input, fixed_output, name="opennsfw2_resnet50_v1_batch1")

    output_dir.mkdir(parents=True, exist_ok=True)
    lock_bytes = LOCK_FILE.read_bytes()
    versions = package_versions()
    created_utc = datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")
    candidates = [
        convert_one(tf, keras_model, output_dir / "float32.tflite", "float32"),
        convert_one(tf, keras_model, output_dir / "dynamic-range.tflite", "dynamic-range"),
    ]
    conversion = {
        "schema": 1,
        "status": "candidate only; not selected for the Android SDK",
        "created_utc": created_utc,
        "source": {"asset_name": source["asset_name"], "url": source["asset_url"],
                   "sha256": input_digest, "bytes": WEIGHTS.stat().st_size,
                   "retrieved_utc": source["retrieved_utc"]},
        "source_model": {"package": "opennsfw2", "version": opennsfw2.__version__,
                         "model_id": "opennsfw2_resnet50_v1", "weights_path_explicit": True},
        "environment": {"python": platform.python_version(), "os": platform.system(),
                        "architecture": platform.machine(), "packages": versions,
                        "uv_lock_sha256": sha256_bytes(lock_bytes),
                        "cpu_only": True, "tensorflow_inter_op_threads": 1,
                        "tensorflow_intra_op_threads": 1,
                        "TF_ENABLE_ONEDNN_OPTS": "0"},
        "converter": {"api": "tf.lite.TFLiteConverter.from_keras_model",
                      "tensorflow": tf.__version__, "input_batch": 1,
                      "supported_ops": ["TFLITE_BUILTINS"], "allow_custom_ops": False,
                      "float32": {"optimizations": []},
                      "dynamic_range": {"optimizations": ["Optimize.DEFAULT"]}},
        "external_contract": {"input_shape": list(MODEL_INPUT), "input_dtype": "float32",
                              "output_shape": list(MODEL_OUTPUT), "output_dtype": "float32",
                              "preprocessing": "outside model: BGR mean subtraction [104,117,123]"},
        "converter_script_sha256": sha256_file(Path(__file__)),
        "candidates": candidates,
    }
    write_atomic(output_dir / "CONVERSION_MANIFEST.json",
                 (json.dumps(conversion, indent=2, ensure_ascii=False) + "\n").encode("utf-8"))
    print(json.dumps({"candidate_dir": output_dir.relative_to(ROOT).as_posix(),
                      "weights_sha256": input_digest,
                      "candidates": [{"optimization": item["optimization"],
                                      "bytes": item["bytes"], "sha256": item["sha256"]}
                                     for item in candidates]}, indent=2))


if __name__ == "__main__":
    main()
