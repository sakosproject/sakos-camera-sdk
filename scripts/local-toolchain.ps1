$ErrorActionPreference = 'Stop'
if (!$env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME = Join-Path ([Environment]::GetFolderPath('UserProfile')) '.gradle' }
$distribution = Get-ChildItem (Join-Path $env:GRADLE_USER_HOME 'wrapper\dists\gradle-8.13-bin') -Filter gradle.bat -Recurse -File |
    Where-Object FullName -Like '*\gradle-8.13\bin\gradle.bat' | Select-Object -First 1
if (!$distribution) { throw 'The installed Gradle 8.13 distribution is required; this verifier never downloads a toolchain.' }
$script:LocalGradle = $distribution.FullName
function Invoke-LocalGradle {
    param([string[]]$Arguments, [string]$Log)
    & $script:LocalGradle --offline --no-daemon --console plain @Arguments *> $Log
    $code = $LASTEXITCODE
    if ($code -ne 0) { Get-Content $Log -Tail 35 | Write-Output; throw "Gradle check failed ($code); see the local log." }
    Write-Output "Gradle check passed: $($Arguments -join ' ')"
}
