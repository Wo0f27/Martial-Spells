$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
$TargetRelative = "src/main/resources/assets/martial_spells/sounds/charge_activate.ogg"
$ExpectedBlobSha = "77ec5f2f81300f268e686c9fe2c1800127f2936f"
$Target = Join-Path $Root $TargetRelative

function Get-GitBlobSha1([string]$Path) {
    $bytes = [System.IO.File]::ReadAllBytes($Path)
    $prefix = [System.Text.Encoding]::ASCII.GetBytes("blob $($bytes.Length)`0")
    $payload = New-Object byte[] ($prefix.Length + $bytes.Length)
    [System.Buffer]::BlockCopy($prefix, 0, $payload, 0, $prefix.Length)
    [System.Buffer]::BlockCopy($bytes, 0, $payload, $prefix.Length, $bytes.Length)
    $sha1 = [System.Security.Cryptography.SHA1]::Create()
    try {
        return ([System.BitConverter]::ToString($sha1.ComputeHash($payload))).Replace("-", "").ToLowerInvariant()
    }
    finally {
        $sha1.Dispose()
    }
}

if (-not (Test-Path -LiteralPath $Target -PathType Leaf)) {
    throw "Bundled W2 Charge sound is missing: $TargetRelative. Run git pull --ff-only origin feature/warrior-spells-port."
}

$actual = Get-GitBlobSha1 $Target
if ($actual -ne $ExpectedBlobSha) {
    throw "Charge sound hash mismatch. Expected Git blob $ExpectedBlobSha, got $actual. Restore the tracked file with git restore -- $TargetRelative."
}

Write-Host "Verified bundled W2 Charge sound: $TargetRelative ($actual)" -ForegroundColor Green
