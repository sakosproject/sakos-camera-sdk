import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
import compare_results as benchmark


class CompareResultsTest(unittest.TestCase):
    def test_reports_decision_and_label_mismatches(self):
        reference = {
            "a": benchmark.parse_record({"id": "a", "decision": "Allow", "label": "Allow", "score": 0.1}, "test"),
            "b": benchmark.parse_record({"id": "b", "decision": "Block", "label": "Block", "score": 0.8}, "test"),
        }
        sdk = {
            "a": benchmark.parse_record({"id": "a", "decision": "Block", "score": 0.2, "latency_ms": 4}, "test"),
            "b": benchmark.parse_record({"id": "b", "decision": "Allow", "score": 0.7, "latency_ms": 9}, "test"),
        }

        report = benchmark.compare(reference, sdk)

        self.assertEqual(["a", "b"], report["decision_mismatch_ids"])
        self.assertEqual(1, report["labels"]["false_accepts"])
        self.assertEqual(1, report["labels"]["false_rejects"])
        self.assertEqual(9.0, report["sdk_latency_ms"]["p95"])

    def test_rejects_misaligned_record_ids(self):
        reference = {"a": benchmark.parse_record({"id": "a", "decision": "Allow"}, "test")}
        sdk = {"b": benchmark.parse_record({"id": "b", "decision": "Allow"}, "test")}

        with self.assertRaisesRegex(ValueError, "record IDs differ"):
            benchmark.compare(reference, sdk)

    def test_missing_labels_remain_unavailable(self):
        record = benchmark.parse_record({"id": "a", "decision": "Allow"}, "test")

        report = benchmark.compare({"a": record}, {"a": record})

        self.assertEqual(0, report["labels"]["count"])
        self.assertIsNone(report["labels"]["false_accepts"])


if __name__ == "__main__":
    unittest.main()
