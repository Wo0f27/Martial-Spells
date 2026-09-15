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
    "sounds/shadow_step_depart.ogg" `
    "src/main/resources/assets/martial_spells/sounds/shadow_step_depart.ogg" `
    "372197145aaaed43ef2e55902451f4355286b85e"

Sync-ExactAsset `
    "textures/spell/shadow_step.png" `
    "src/main/resources/assets/martial_spells/textures/gui/spell_icons/shadow_step.png" `
    "4b5137f52cec53ada756488776e0371b50a95df6"

Sync-ExactAsset `
    "textures/mob_effect/shadow_step.png" `
    "src/main/resources/assets/martial_spells/textures/mob_effect/shadow_step.png" `
    "4b5137f52cec53ada756488776e0371b50a95df6"

Write-Host "R3 Shadowstep frozen assets synced." -ForegroundColor Green
