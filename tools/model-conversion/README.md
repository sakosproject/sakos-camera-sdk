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
locked wheels. Review source tolerance, differences from the currently selected
SDK model, and the license record before replacing the bundled bytes. The current
selected model is the reproduced float32 export. Its preflight size, digest, and
configuration identity are enforced by the repository gate.

The dynamic-range alternative is not selected because it exceeded the declared
Keras tolerance. The comparison command returns nonzero when any generated
variant fails its tolerance; this is expected for that rejected alternative and
must not be hidden by widening the threshold. The owner requested no device test
for the selected model; use the repository's non-device verification command.
