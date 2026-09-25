$ErrorActionPreference = "Stop"

# Re-assert the finalized P1 resource set first. This also removes the
# intentionally dropped martial_spells:heal icon from older working trees.
& "$PSScriptRoot\sync-paladin-p1-assets.ps1"

$SourceCommit = "2807417a1dd9a65204c002ded487da0e6ae467a1"
$SourceRoot = "https://raw.githubusercontent.com/ZsoltMolnarrr/Paladins/$SourceCommit/common/src/main/resources/assets/paladins"
$TargetRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot ".."))

function Copy-PaladinAsset {
    param(
        [Parameter(Mandatory=$true)][string]$SourceRelative,
        [Parameter(Mandatory=$true)][string]$TargetRelative
    )

    $source = "$SourceRoot/$SourceRelative"
    $target = Join-Path $TargetRoot $TargetRelative
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $target) | Out-Null
    Write-Host "sync $TargetRelative"
    Invoke-WebRequest -Uri $source -OutFile $target -UseBasicParsing -Headers @{ "User-Agent" = "Martial-Spells-Paladin-P2-sync" }
}

foreach ($spell in @(
    "blessed_strikes",
    "divine_protection",
    "judgement",
    "immolation"
)) {
    Copy-PaladinAsset "textures/spell/$spell.png" "src/main/resources/assets/martial_spells/textures/gui/spell_icons/$spell.png"
}

Copy-PaladinAsset "textures/mob_effect/blessed_strikes.png" "src/main/resources/assets/martial_spells/textures/mob_effect/blessed_strikes.png"

# Blessed Strikes is intentionally mechanics-only. Remove presentation assets
# left by older P2 sync revisions so stale local resources do not linger.
foreach ($obsolete in @(
    "src/main/resources/assets/martial_spells/textures/misc/paladin_item_glow.png",
    "src/main/resources/assets/martial_spells/sounds/blessed_strike_start.ogg",
    "src/main/resources/assets/martial_spells/sounds/blessed_strike_casting.ogg",
    "src/main/resources/assets/martial_spells/sounds/blessed_strike_release.ogg"
)) {
    Remove-Item -Force -ErrorAction SilentlyContinue (Join-Path $TargetRoot $obsolete)
}

Copy-PaladinAsset "textures/spell_effect/divine_protection.png" "src/main/resources/assets/martial_spells/textures/spell_effect/divine_protection.png"
Copy-PaladinAsset "textures/spell_effect/divine_protection_glow.png" "src/main/resources/assets/martial_spells/textures/spell_effect/divine_protection_glow.png"

foreach ($sound in @(
    "divine_protection_release.ogg",
    "divine_protection_impact.ogg",
    "judgement_impact.ogg",
    "immolation_release.ogg"
)) {
    Copy-PaladinAsset "sounds/$sound" "src/main/resources/assets/martial_spells/sounds/$sound"
}

Write-Host ""
Write-Host "Paladin/Priest P2 source asset sync complete."
Write-Host "Pinned Paladins commit: $SourceCommit"
Write-Host "Next: python .\tools\audit-paladin-p2.py"
