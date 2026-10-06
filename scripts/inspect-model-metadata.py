"""Print an auditable FlatBuffer/tensor/operator summary for TFLite models."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import sys


ROOT = Path(__file__).resolve().parents[1]


def inspect(tf, schema, path: Path) -> dict:
    payload = path.read_bytes()
    if len(payload) < 8 or payload[4:8] != b"TFL3":
        raise ValueError(f"Not a TFLite FlatBuffer: {path}.")
    model = schema.Model.GetRootAsModel(payload, 0)
    interpreter = tf.lite.Interpreter(model_content=payload, num_threads=1)
    interpreter.allocate_tensors()
    inputs = interpreter.get_input_details()
    outputs = interpreter.get_output_details()
    operator_names = {value: name for name, value in vars(schema.BuiltinOperator).items()
                      if isinstance(value, int)}
    tensor_names = {value: name for name, value in vars(schema.TensorType).items()
                    if isinstance(value, int)}
    operators = []
    for index in range(model.OperatorCodesLength()):
        entry = model.OperatorCodes(index)
        code = max(entry.BuiltinCode(), entry.DeprecatedBuiltinCode())
        custom = entry.CustomCode()
        custom_name = custom.decode("utf-8", errors="replace") if custom else None
        operators.append({"name": operator_names.get(code, f"BUILTIN_{code}"),
                          "version": entry.Version(), "custom_code": custom_name})
    subgraph = model.Subgraphs(0)
    constant_types = {}
    for index in range(subgraph.TensorsLength()):
        tensor = subgraph.Tensors(index)
        if model.Buffers(tensor.Buffer()).DataLength() > 0:
            name = tensor_names.get(tensor.Type(), f"TENSOR_TYPE_{tensor.Type()}")
            constant_types[name] = constant_types.get(name, 0) + 1
    metadata = []
    for index in range(model.MetadataLength()):
        entry = model.Metadata(index)
        name = entry.Name().decode("utf-8", errors="replace") if entry.Name() else ""
        buffer = model.Buffers(entry.Buffer())
        data = bytes(buffer.DataAsNumpy()) if buffer.DataLength() else b""
        try:
            readable = data.decode("utf-8").strip("\x00")
        except UnicodeDecodeError:
            readable = None
        metadata.append({"name": name, "bytes": len(data),
                         "sha256": hashlib.sha256(data).hexdigest(),
                         "utf8": readable if readable and all(char.isprintable() for char in readable) else None})
    if len(inputs) != 1 or len(outputs) != 1:
        raise ValueError(f"Expected one input/output tensor: {path}.")
    return {
        "path": path.relative_to(ROOT).as_posix() if path.is_relative_to(ROOT) else path.name,
        "bytes": len(payload),
        "sha256": hashlib.sha256(payload).hexdigest(),
        "flatbuffer_version": model.Version(),
        "inputs": [{"name": value["name"], "shape": value["shape"].tolist(),
                    "dtype": value["dtype"].__name__} for value in inputs],
        "outputs": [{"name": value["name"], "shape": value["shape"].tolist(),
                     "dtype": value["dtype"].__name__} for value in outputs],
        "operators": operators,
        "constant_tensor_types": dict(sorted(constant_types.items())),
        "metadata": metadata,
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("models", nargs="+", type=Path, help="TFLite model paths.")
    parser.add_argument("--output", type=Path, help="Optional JSON output under build/model-conversion/.")
    args = parser.parse_args()
    os.environ["TF_CPP_MIN_LOG_LEVEL"] = "3"
    import tensorflow as tf
    from tensorflow.lite.python import schema_py_generated as schema
    tf.get_logger().setLevel("ERROR")
    results = []
    for model in args.models:
        path = model if model.is_absolute() else ROOT / model
        results.append(inspect(tf, schema, path.resolve()))
    output = json.dumps({"schema": 1, "models": results}, indent=2) + "\n"
    if args.output:
        target = args.output if args.output.is_absolute() else ROOT / args.output
        target = target.resolve()
        if not target.is_relative_to((ROOT / "build/model-conversion").resolve()):
            raise SystemExit("Inspection output must remain under ignored build/model-conversion/.")
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(output, encoding="utf-8")
    print(output, end="")


if __name__ == "__main__":
    main()
