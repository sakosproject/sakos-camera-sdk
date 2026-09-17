# Sanitized parity comparator

`compare_results.py` compares precomputed JSONL reference and SDK records. It
does not load images, video, models, corpus manifests, or network resources.
Keep raw input and report files outside the repository.

Each JSONL object needs `id` and `decision` (`Allow`, `Block`, `Review`, or
`Failure`). Optional fields are `label` (`Allow` or `Block`), `score`,
`latency_ms`, `selected_views`, `timestamps_ms`, and `rationale_categories`.
The two inputs must have exactly the same opaque IDs.

```powershell
python tools/benchmark/compare_results.py --self-test
python tools/benchmark/compare_results.py `
  --reference D:\authorized-corpus\reference-results.jsonl `
  --sdk D:\authorized-corpus\sdk-results.jsonl `
  --output D:\authorized-corpus\sanitized-parity-report.json
python -m unittest tools/benchmark/test_compare_results.py
```

Copy only reviewed aggregate results into `docs/validation/PARITY_REPORT.md`.
Missing labels, scores, timestamps, views, rationale, runtime identity, or
latency must remain unavailable rather than being reported as zero.
