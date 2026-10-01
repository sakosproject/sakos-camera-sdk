[CmdletBinding()]
param([string]$Serial)
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot
. (Join-Path $PSScriptRoot 'local-toolchain.ps1')
New-Item -ItemType Directory -Force build-logs | Out-Null
@{ source_commit = (git rev-parse HEAD).Trim(); tracked_worktree_dirty = [bool](git status --porcelain --untracked-files=no) } |
    ConvertTo-Json | Set-Content -LiteralPath 'build-logs/candidate-source-state.json' -Encoding utf8
if ($Serial) {
    $adb = Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe'
    if ($Serial -notmatch '^127\.0\.0\.1:\d+$|^emulator-\d+$') { throw 'Explicit isolated emulator serial required.' }
    if ((& $adb -s $Serial shell getprop ro.kernel.qemu).Trim() -ne '1') { throw 'Emulator-only verification required.' }
}
foreach ($log in @('candidate-emulator.log', 'candidate-consumer-runtime.log')) {
    $file = Join-Path $repoRoot "build-logs/$log"
    if (Test-Path $file) { Remove-Item -LiteralPath $file }
}
Invoke-LocalGradle -Log 'build-logs/candidate-build.log' -Arguments @('clean', 'testDebugUnitTest', 'assembleDebug', 'assembleDebugAndroidTest', 'lint')
& (Join-Path $PSScriptRoot 'verify-local-consumer.ps1')
if ($Serial) {
    & (Join-Path $PSScriptRoot 'run-synthetic-instrumentation.ps1') -Serial $Serial *> 'build-logs/candidate-emulator.log'
    & (Join-Path $PSScriptRoot 'run-synthetic-instrumentation.ps1') -Serial $Serial -ConsumerOnly *> 'build-logs/candidate-consumer-runtime.log'
}
$aapt = Join-Path $env:ANDROID_HOME 'build-tools/36.0.0/aapt.exe'
if (!(Test-Path $aapt)) { throw 'Installed build-tools 36.0.0 required for APK permission inspection.' }
foreach ($apk in @('sample-app/build/outputs/apk/debug/sample-app-debug.apk',
        'integration-tests/consumer/app/build/outputs/apk/localRuntime/app-localRuntime.apk')) {
    $permissions = & $aapt dump permissions $apk
    if ($LASTEXITCODE -ne 0 -or ($permissions -join "`n") -match 'android.permission.(INTERNET|READ_MEDIA|READ_EXTERNAL_STORAGE|WRITE_EXTERNAL_STORAGE|RECORD_AUDIO)') {
        throw 'Candidate permission inspection failed.'
    }
}
$auditFile = Join-Path ([IO.Path]::GetTempPath()) 'sakos-private-text-audit.json'
$auditSummary = & python (Join-Path $PSScriptRoot 'audit-local-text.py') --private-output $auditFile
if ($LASTEXITCODE -ne 0) { throw 'Private text audit failed; findings are redacted in the private temporary report.' }
$inspectionArguments = @((Join-Path $PSScriptRoot 'inspect-local-candidate.py'))
if ($Serial) { $inspectionArguments += '--emulator-tested' }
& python @inspectionArguments
if ($LASTEXITCODE -ne 0) { throw 'Candidate artifact inspection failed.' }
$summary = $auditSummary | ConvertFrom-Json
@{ text_files = $summary.current_text_files; secret_candidates = $summary.secret_candidate_count;
    media_or_signing_names = $summary.media_or_signing_name_count; scope = 'Current tracked and prospective source text; heuristic audit, not clearance.' } |
    ConvertTo-Json | Set-Content -LiteralPath 'build/private-candidate/current-text-audit.json' -Encoding utf8
& git diff --check
if ($LASTEXITCODE -ne 0) { throw 'Diff whitespace check failed.' }
Write-Output 'Private local candidate verification: OK. Manifest/checksums are in build/private-candidate.'
