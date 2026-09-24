$ErrorActionPreference = "Stop"

# Re-assert the finalized P2 resource set first.
& "$PSScriptRoot\sync-paladin-p2-assets.ps1"

$SourceCommit = "2807417a1dd9a65204c002ded487da0e6ae467a1"
$SourceRoot = "https://raw.githubusercontent.com/ZsoltMolnarrr/Paladins/$SourceCommit/common/src/main/resources/assets/paladins"
$SpellEngineCommit = "fea2dc16c1f77d2e583149354b67227d589c9a4d"
$SpellEngineRoot = "https://raw.githubusercontent.com/ZsoltMolnarrr/SpellEngine/$SpellEngineCommit/common/src/main/resources/assets/spell_engine"
$TargetRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot ".."))

function Copy-SpellEngineAsset {
    param(
        [Parameter(Mandatory=$true)][string]$SourceRelative,
        [Parameter(Mandatory=$true)][string]$TargetRelative
    )

    $source = "$SpellEngineRoot/$SourceRelative"
    $target = Join-Path $TargetRoot $TargetRelative
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $target) | Out-Null
    Write-Host "sync GPL Spell Engine VFX $TargetRelative"
    Invoke-WebRequest -Uri $source -OutFile $target -UseBasicParsing -Headers @{ "User-Agent" = "Martial-Spells-Paladin-P3-VFX-sync" }
}

function Copy-PaladinAsset {
    param(
        [Parameter(Mandatory=$true)][string]$SourceRelative,
        [Parameter(Mandatory=$true)][string]$TargetRelative
    )

    $source = "$SourceRoot/$SourceRelative"
    $target = Join-Path $TargetRoot $TargetRelative
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $target) | Out-Null
    Write-Host "sync $TargetRelative"
    Invoke-WebRequest -Uri $source -OutFile $target -UseBasicParsing -Headers @{ "User-Agent" = "Martial-Spells-Paladin-P3-sync" }
}

foreach ($spell in @(
    "holy_beam",
    "levitate",
    "penance"
)) {
    Copy-PaladinAsset "textures/spell/$spell.png" "src/main/resources/assets/martial_spells/textures/gui/spell_icons/$spell.png"
}

foreach ($effect in @(
    "levitate",
    "priest_absorption"
)) {
    Copy-PaladinAsset "textures/mob_effect/$effect.png" "src/main/resources/assets/martial_spells/textures/mob_effect/$effect.png"
}

Copy-PaladinAsset "textures/spell_projectile/judgement.png" "src/main/resources/assets/martial_spells/textures/spell_projectile/judgement.png"
Copy-PaladinAsset "textures/spell_projectile/lightwell_orb.png" "src/main/resources/assets/martial_spells/textures/spell_projectile/lightwell_orb.png"

# Exact Paladins P1-P3 visuals use this pinned subset of Spell Engine's
# GPLv3 particle artwork. Martial Spells owns the particle implementations.
Copy-SpellEngineAsset "textures/particle/magic/holy.png" "src/main/resources/assets/martial_spells/textures/particle/paladin_source/magic/holy.png"
Copy-SpellEngineAsset "textures/particle/magic/heal.png" "src/main/resources/assets/martial_spells/textures/particle/paladin_source/magic/heal.png"

foreach ($frame in 0..7) {
    Copy-SpellEngineAsset "textures/particle/magic/vertical_stripe_$frame.png" "src/main/resources/assets/martial_spells/textures/particle/paladin_source/magic/vertical_stripe_$frame.png"
}

foreach ($frame in 0..12) {
    Copy-SpellEngineAsset "textures/particle/zone/effect_553_$frame.png" "src/main/resources/assets/martial_spells/textures/particle/paladin_source/zone/effect_553_$frame.png"
}

foreach ($frame in 0..14) {
    Copy-SpellEngineAsset "textures/particle/zone/effect_637_$frame.png" "src/main/resources/assets/martial_spells/textures/particle/paladin_source/zone/effect_637_$frame.png"
}

foreach ($frame in 0..16) {
    Copy-SpellEngineAsset "textures/particle/zone/effect_676_$frame.png" "src/main/resources/assets/martial_spells/textures/particle/paladin_source/zone/effect_676_$frame.png"
}

foreach ($sound in @(
    "holy_beam_start_casting.ogg",
    "holy_beam_casting.ogg",
    "holy_beam_release.ogg",
    "holy_beam_heal.ogg",
    "holy_beam_damage.ogg",
    "holy_ward_impact.ogg",
    "penance_impact.ogg",
    "penance_release_1.ogg",
    "penance_release_2.ogg",
    "penance_release_3.ogg"
)) {
    Copy-PaladinAsset "sounds/$sound" "src/main/resources/assets/martial_spells/sounds/$sound"
}

Write-Host ""
Write-Host "Paladin/Priest P3 source asset sync complete."
Write-Host "Pinned Paladins commit: $SourceCommit"
Write-Host "Pinned Spell Engine VFX commit: $SpellEngineCommit"
Write-Host "Next: python .\tools\audit-paladin-p3.py"
