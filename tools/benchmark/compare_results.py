#!/usr/bin/env python3
"""Compare sanitized, precomputed source and SDK evaluation records.

The tool intentionally never reads media, model files, corpus manifests, or network resources.
"""

from __future__ import annotations

import argparse
import json
import math
from pathlib import Path
from statistics import median
from typing import Any

DECISIONS = {"Allow", "Block", "Review", "Failure"}
LABELS = {"Allow", "Block"}


def parse_record(value: dict[str, Any], origin: str) -> dict[str, Any]:
    capture_id = value.get("id")
    decision = value.get("decision")
    if not isinstance(capture_id, str) or not capture_id.strip():
        raise ValueError(f"{origin}: every record needs a non-empty string id")
    if decision not in DECISIONS:
        raise ValueError(f"{origin}:{capture_id}: decision must be one of {sorted(DECISIONS)}")
    label = value.get("label")
    if label is not None and label not in LABELS:
        raise ValueError(f"{origin}:{capture_id}: label must be Allow, Block, or omitted")
    score = value.get("score")
    if score is not None and (not isinstance(score, (int, float)) or not math.isfinite(score)):
        raise ValueError(f"{origin}:{capture_id}: score must be a finite number or omitted")
    latency = value.get("latency_ms")
    if latency is not None and (not isinstance(latency, (int, float)) or not math.isfinite(latency) or latency < 0):
        raise ValueError(f"{origin}:{capture_id}: latency_ms must be a non-negative finite number or omitted")
    normalized = dict(value)
    normalized["id"] = capture_id
    normalized["score"] = float(score) if score is not None else None
    normalized["latency_ms"] = float(latency) if latency is not None else None
    for key in ("selected_views", "timestamps_ms", "rationale_categories"):
        items = normalized.get(key, [])
        if not isinstance(items, list):
            raise ValueError(f"{origin}:{capture_id}: {key} must be an array when provided")
        normalized[key] = items
    return normalized


def load_jsonl(path: Path) -> dict[str, dict[str, Any]]:
    records: dict[str, dict[str, Any]] = {}
    for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
        if not line.strip():
            continue
        try:
            raw = json.loads(line)
        except json.JSONDecodeError as error:
            raise ValueError(f"{path}:{number}: invalid JSON: {error.msg}") from error
        if not isinstance(raw, dict):
            raise ValueError(f"{path}:{number}: record must be a JSON object")
        record = parse_record(raw, f"{path}:{number}")
        if record["id"] in records:
            raise ValueError(f"{path}:{number}: duplicate id {record['id']!r}")
        records[record["id"]] = record
    if not records:
        raise ValueError(f"{path}: no JSONL records found")
    return records


def percentile(values: list[float], fraction: float) -> float | None:
    if not values:
        return None
    sorted_values = sorted(values)
    index = math.ceil(fraction * len(sorted_values)) - 1
    return sorted_values[max(0, index)]


def compare(reference: dict[str, dict[str, Any]], sdk: dict[str, dict[str, Any]]) -> dict[str, Any]:
    if set(reference) != set(sdk):
        missing_from_sdk = sorted(set(reference) - set(sdk))
        missing_from_reference = sorted(set(sdk) - set(reference))
        raise ValueError(
            "record IDs differ; "
            f"missing_from_sdk={missing_from_sdk}, missing_from_reference={missing_from_reference}"
        )
    decision_mismatches: list[str] = []
    view_mismatches: list[str] = []
    timestamp_mismatches: list[str] = []
    rationale_mismatches: list[str] = []
    score_deltas: list[float] = []
    sdk_latencies: list[float] = []
    labeled = false_accepts = false_rejects = unresolved = 0
    for capture_id in sorted(reference):
        expected = reference[capture_id]
        actual = sdk[capture_id]
        if expected["decision"] != actual["decision"]:
            decision_mismatches.append(capture_id)
        if expected["selected_views"] != actual["selected_views"]:
            view_mismatches.append(capture_id)
        if expected["timestamps_ms"] != actual["timestamps_ms"]:
            timestamp_mismatches.append(capture_id)
        if expected["rationale_categories"] != actual["rationale_categories"]:
            rationale_mismatches.append(capture_id)
        if expected["score"] is not None and actual["score"] is not None:
            score_deltas.append(abs(expected["score"] - actual["score"]))
        if actual["latency_ms"] is not None:
            sdk_latencies.append(actual["latency_ms"])
        if expected.get("label") is not None:
            labeled += 1
            if expected["label"] == "Block" and actual["decision"] == "Allow":
                false_accepts += 1
            if expected["label"] == "Allow" and actual["decision"] == "Block":
                false_rejects += 1
            if actual["decision"] in {"Review", "Failure"}:
                unresolved += 1
    return {
        "record_count": len(reference),
        "decision_mismatch_ids": decision_mismatches,
        "decision_match_count": len(reference) - len(decision_mismatches),
        "selected_view_mismatch_ids": view_mismatches,
        "timestamp_mismatch_ids": timestamp_mismatches,
        "rationale_mismatch_ids": rationale_mismatches,
        "score_delta": {
            "count": len(score_deltas),
            "max": max(score_deltas) if score_deltas else None,
            "median": median(score_deltas) if score_deltas else None,
        },
        "labels": {
            "count": labeled,
            "false_accepts": false_accepts if labeled else None,
            "false_rejects": false_rejects if labeled else None,
            "unresolved_or_failure": unresolved if labeled else None,
        },
        "sdk_latency_ms": {
            "count": len(sdk_latencies),
            "p50": percentile(sdk_latencies, 0.50),
            "p95": percentile(sdk_latencies, 0.95),
        },
    }


def self_test() -> None:
    reference = {
        "opaque-a": parse_record({"id": "opaque-a", "decision": "Allow", "label": "Allow", "score": 0.1}, "self-test"),
        "opaque-b": parse_record({"id": "opaque-b", "decision": "Block", "label": "Block", "score": 0.9}, "self-test"),
    }
    sdk = {
        "opaque-a": parse_record({"id": "opaque-a", "decision": "Block", "score": 0.2, "latency_ms": 5}, "self-test"),
        "opaque-b": parse_record({"id": "opaque-b", "decision": "Allow", "score": 0.8, "latency_ms": 9}, "self-test"),
    }
    report = compare(reference, sdk)
    assert report["decision_match_count"] == 0
    assert report["labels"]["false_accepts"] == 1
    assert report["labels"]["false_rejects"] == 1
    assert report["score_delta"]["max"] == 0.1


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--reference", type=Path, help="sanitized source/reference JSONL")
    parser.add_argument("--sdk", type=Path, help="sanitized SDK JSONL")
    parser.add_argument("--output", type=Path, help="sanitized JSON report path")
    parser.add_argument("--self-test", action="store_true", help="validate synthetic comparison accounting")
    args = parser.parse_args()
    if args.self_test:
        self_test()
        print("self-test: OK")
        return
    if not (args.reference and args.sdk and args.output):
        parser.error("--reference, --sdk, and --output are required unless --self-test is used")
    report = compare(load_jsonl(args.reference), load_jsonl(args.sdk))
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(f"comparison report: {args.output}")


if __name__ == "__main__":
    main()
