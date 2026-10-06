# Reproducible OpenNSFW2 model conversion

This isolated tool environment creates and compares local TFLite candidates from
the pinned OpenNSFW2 v0.1.0 HDF5 weights. It never changes the SDK model asset.
The toolchain is separate from the Android build and uses Python 3.12,
TensorFlow 2.20.0, and OpenNSFW2 0.15.2 as recorded in `pyproject.toml` and
`uv.lock`.

The weight file is fetched only by `scripts/fetch-model-weights.py` after the
source manifest has an established SHA-256. It is kept under ignored
`build/model-conversion/inputs/`. Do not use the package's implicit download
path or a preexisting model cache.

The committed source manifest already has the measured digest. If starting
from a deliberately unpinned manifest, review the release URL, then run the
fetch command once with `--establish-pin`; it records the digest and retrieval
time and conversion refuses to proceed until that pin exists.

From the repository root:

```powershell
$uvBootstrap = '.\scripts\bootstrap-model-conversion.ps1'
& $uvBootstrap
$uv = '.\build\model-conversion\bootstrap\uv.exe'
& $uv python install 3.12.15 --install-dir '.\build\model-conversion\python'
& $uv sync --project '.\tools\model-conversion' --python 3.12.15 --locked
& $uv run --project '.\tools\model-conversion' --locked python `
  '.\scripts\fetch-model-weights.py'
& $uv run --project '.\tools\model-conversion' --locked python `
  '.\scripts\convert-opennsfw2-model.py' --output-dir '.\build\model-conversion\run-1'
& $uv run --project '.\tools\model-conversion' --locked python `
  '.\scripts\convert-opennsfw2-model.py' --output-dir '.\build\model-conversion\run-2'
& $uv run --project '.\tools\model-conversion' --locked python `
  '.\scripts\compare-model-conversions.py' '.\build\model-conversion\run-1' '.\build\model-conversion\run-2'
& $uv run --project '.\tools\model-conversion' --locked python `
  '.\scripts\inspect-model-metadata.py' `
  '.\build\model-conversion\run-1\float32.tflite' `
  '.\build\model-conversion\run-1\dynamic-range.tflite' `
  '.\safety-opennsfw2\src\main\assets\model\sakos_nudity_model.tflite'
```

Conversion writes only ignored candidate and report files under
`build/model-conversion/`. Repeating it uses the same pinned source bytes and
locked wheels. Review numerical results, the exact Android LiteRT runtime gate,
and attribution/legal scope before selecting a candidate. The current SDK model
remains available as rollback until an explicitly selected replacement passes
the repository acceptance checks.

The committed comparison record documents the current dynamic-range candidate's
Keras tolerance failure. The comparison command returns nonzero for that failure;
keep the candidate unselected unless a separately reviewed conversion resolves
it without silently changing the declared tolerance.
