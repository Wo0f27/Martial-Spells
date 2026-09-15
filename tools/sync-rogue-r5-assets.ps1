$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
$UpstreamCommit = "89ba33ad29adc42d7306660f5b74f28bd17b8ffa"
$RawBase = "https://raw.githubusercontent.com/ZsoltMolnarrr/Rogues/$UpstreamCommit/common/src/main/resources/assets/rogues"

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

function Sync-ExactAsset(
    [string]$SourceRelative,
    [string]$TargetRelative,
    [string]$ExpectedBlobSha
) {
    $target = Join-Path $Root $TargetRelative
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $target) | Out-Null
    $uri = "$RawBase/$SourceRelative"

    Write-Host "Fetching $uri"
    Invoke-WebRequest -UseBasicParsing -Uri $uri -OutFile $target

    $actual = Get-GitBlobSha1 $target
    if ($actual -ne $ExpectedBlobSha) {
        Remove-Item -Force $target -ErrorAction SilentlyContinue
        throw "Asset hash mismatch for $SourceRelative. Expected Git blob $ExpectedBlobSha, got $actual"
    }

    Write-Host "Verified $TargetRelative ($actual)" -ForegroundColor Green
}

Sync-ExactAsset `
    "sounds/vanish_combined.ogg" `
    "src/main/resources/assets/martial_spells/sounds/vanish_combined.ogg" `
    "ba68a4b7f561ce6842ffb1730f5c6fb9bee60c39"

Sync-ExactAsset `
    "sounds/stealth_leave.ogg" `
    "src/main/resources/assets/martial_spells/sounds/stealth_leave.ogg" `
    "43d306ab3344aa9c2fa6636ba527aaeb53967384"

Sync-ExactAsset `
    "textures/spell/vanish.png" `
    "src/main/resources/assets/martial_spells/textures/gui/spell_icons/vanish.png" `
    "7ee700360ad64723bb32660c6cee5f01fdd73ad7"

Sync-ExactAsset `
    "textures/mob_effect/stealth.png" `
    "src/main/resources/assets/martial_spells/textures/mob_effect/stealth.png" `
    "7ee700360ad64723bb32660c6cee5f01fdd73ad7"

Write-Host "R5 Vanish frozen assets synced." -ForegroundColor Green
