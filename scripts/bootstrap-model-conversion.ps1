param()

$ErrorActionPreference = 'Stop'
$version = '0.12.23'
$filename = "uv-$version-py3-none-win_amd64.whl"
$expectedSha256 = 'fb8a4117a5224d73abe2204a1e744ae34b544f1b9a0b761576851deaa7215c12'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$destination = Join-Path $root 'build/model-conversion/bootstrap'
New-Item -ItemType Directory -Force -Path $destination | Out-Null
$wheelPath = Join-Path $destination $filename
$partialPath = Join-Path $destination "$filename.part"
$executablePath = Join-Path $destination 'uv.exe'
$executablePartialPath = Join-Path $destination 'uv.exe.part'

try {
    $release = Invoke-RestMethod -Uri "https://pypi.org/pypi/uv/$version/json"
    $asset = @($release.urls | Where-Object { $_.filename -eq $filename })
    if ($asset.Count -ne 1 -or $asset[0].digests.sha256.ToLowerInvariant() -ne $expectedSha256) {
        throw 'PyPI release metadata did not match the pinned uv wheel and SHA-256.'
    }
    $downloadUri = [Uri]$asset[0].url
    if ($downloadUri.Scheme -ne 'https' -or $downloadUri.Host -ne 'files.pythonhosted.org') {
        throw 'PyPI returned an unexpected wheel URL.'
    }

    Invoke-WebRequest -Uri $downloadUri -OutFile $partialPath
    $downloadedSha256 = (Get-FileHash -LiteralPath $partialPath -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($downloadedSha256 -ne $expectedSha256) {
        throw "uv wheel SHA-256 mismatch: $downloadedSha256"
    }
    Move-Item -LiteralPath $partialPath -Destination $wheelPath -Force

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [System.IO.Compression.ZipFile]::OpenRead($wheelPath)
    try {
        $entry = $archive.GetEntry('uv-0.12.23.data/scripts/uv.exe')
        if (-not $entry) { throw 'Pinned uv wheel does not contain its expected executable.' }
        $input = $entry.Open()
        try {
            $output = [System.IO.File]::Create($executablePartialPath)
            try { $input.CopyTo($output) } finally { $output.Dispose() }
        } finally { $input.Dispose() }
    } finally { $archive.Dispose() }

    Move-Item -LiteralPath $executablePartialPath -Destination $executablePath -Force
    $reportedVersion = (& $executablePath --version).Trim()
    if ($LASTEXITCODE -ne 0 -or $reportedVersion -ne "uv $version (46b84fd0b 2026-10-03 x86_64-pc-windows-msvc)") {
        throw "Unexpected extracted uv version: $reportedVersion"
    }
    Write-Output "Pinned uv ready: $reportedVersion; wheel SHA-256 $expectedSha256"
} finally {
    Remove-Item -LiteralPath $partialPath, $executablePartialPath -Force -ErrorAction SilentlyContinue
}
