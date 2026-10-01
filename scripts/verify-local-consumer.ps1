[CmdletBinding()]
param([string]$Serial)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot
. (Join-Path $PSScriptRoot 'local-toolchain.ps1')
New-Item -ItemType Directory -Force build-logs | Out-Null

Invoke-LocalGradle -Log 'build-logs/local-maven.log' -Arguments @(
    ':safety-core:publishReleasePublicationToSakosLocalRepository',
    ':safety-opennsfw2:publishReleasePublicationToSakosLocalRepository',
    ':capture-camerax:publishReleasePublicationToSakosLocalRepository',
    ':capture-video:publishReleasePublicationToSakosLocalRepository')

Invoke-LocalGradle -Log 'build-logs/local-consumer.log' -Arguments @('-p', 'integration-tests/consumer',
    'clean', ':app:assembleRelease', ':app:assembleLocalRuntime', ':app:assembleLocalRuntimeAndroidTest', ':app:lintLocalRuntime')
if ($Serial) { & (Join-Path $PSScriptRoot 'run-synthetic-instrumentation.ps1') -Serial $Serial -ConsumerOnly }

Write-Output 'Local Maven consumer verification: OK'
