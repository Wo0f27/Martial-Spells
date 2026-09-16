$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
$UpstreamCommit = "89ba33ad29adc42d7306660f5b74f28bd17b8ffa"
$RawBase = "https://raw.githubusercontent.com/ZsoltMolnarrr/Rogues/$UpstreamCommit/common/src/main/resources/assets/rogues"
$SpellEngineCommit = "76cd9e128468ebe005463c729ec73eff7de5fb68"
$SpellEngineRawBase = "https://raw.githubusercontent.com/ZsoltMolnarrr/SpellEngine/$SpellEngineCommit/common/src/main/resources/assets/spell_engine"

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
    [string]$SourceBase,
    [string]$SourceRelative,
    [string]$TargetRelative,
    [string]$ExpectedBlobSha
) {
    $target = Join-Path $Root $TargetRelative
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $target) | Out-Null
    $uri = "$SourceBase/$SourceRelative"

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
    $RawBase `
    "sounds/mutilate_impact.ogg" `
    "src/main/resources/assets/martial_spells/sounds/mutilate_impact.ogg" `
    "e365f284a43fd0ae447b09652f4144f8e0c2c09b"

Sync-ExactAsset `
    $RawBase `
    "textures/spell/mutilate.png" `
    "src/main/resources/assets/martial_spells/textures/gui/spell_icons/mutilate.png" `
    "fb98fa0cacdbb47dd5b3bf2c162583d419a34c62"

# Mutilate's authored melee pose belongs to Spell Engine rather than Rogues.
# Copy the exact frozen PlayerAnimator JSON into Martial Spells' namespace so
# the technique retains its original visual identity without a runtime
# Spell Engine dependency.
Sync-ExactAsset `
    $SpellEngineRawBase `
    "player_animations/weapon_dual_slash_cross.json" `
    "src/main/resources/assets/martial_spells/player_animation/mutilate_dual_slash_cross.json" `
    "02c168e0fd4d1ea6b39f2843dd05365aedf112d0"

Write-Host "R6 Mutilate frozen assets and animation synced." -ForegroundColor Green
