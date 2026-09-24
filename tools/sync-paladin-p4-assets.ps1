$ErrorActionPreference = "Stop"

# Re-assert the finalized P3 resource set first.
& "$PSScriptRoot\sync-paladin-p3-assets.ps1"

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
    Invoke-WebRequest -Uri $source -OutFile $target -UseBasicParsing -Headers @{ "User-Agent" = "Martial-Spells-Paladin-VFX-sync" }
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
    Invoke-WebRequest -Uri $source -OutFile $target -UseBasicParsing -Headers @{ "User-Agent" = "Martial-Spells-Paladin-P4-sync" }
}

foreach ($spell in @(
    "barrier",
    "battle_banner",
    "lightwell"
)) {
    Copy-PaladinAsset "textures/spell/$spell.png" "src/main/resources/assets/martial_spells/textures/gui/spell_icons/$spell.png"
}

Copy-PaladinAsset "textures/mob_effect/battle_banner.png" "src/main/resources/assets/martial_spells/textures/mob_effect/battle_banner.png"

Copy-PaladinAsset "textures/item/barrier.png" "src/main/resources/assets/martial_spells/textures/item/barrier.png"
Copy-PaladinAsset "textures/entity/battle_banner.png" "src/main/resources/assets/martial_spells/textures/entity/battle_banner.png"
Copy-PaladinAsset "textures/entity/lightwell_base.png" "src/main/resources/assets/martial_spells/textures/entity/lightwell_base.png"
Copy-PaladinAsset "textures/entity/lightwell_glow.png" "src/main/resources/assets/martial_spells/textures/entity/lightwell_glow.png"

# P3 already owns this source texture for Penance; re-assert it here because
# Holy Mote now shares the same frozen Lightwell-orb model.
Copy-PaladinAsset "textures/spell_projectile/lightwell_orb.png" "src/main/resources/assets/martial_spells/textures/spell_projectile/lightwell_orb.png"

foreach ($sound in @(
    "holy_barrier_activate.ogg",
    "holy_barrier_idle.ogg",
    "holy_barrier_impact.ogg",
    "holy_barrier_deactivate.ogg",
    "battle_banner_release.ogg",
    "battle_banner_presence.ogg",
    "lightwell_spawn.ogg",
    "lightwell_ambient.ogg",
    "lightwell_despawn.ogg"
)) {
    Copy-PaladinAsset "sounds/$sound" "src/main/resources/assets/martial_spells/sounds/$sound"
}

# Exact Paladins visuals reference a small subset of Spell Engine's GPLv3
# particle artwork. These files are intentionally kept under paladin_source/
# and pinned separately from the Paladins-owned assets above.
Copy-SpellEngineAsset "textures/particle/magic/holy.png" "src/main/resources/assets/martial_spells/textures/particle/paladin_source/magic/holy.png"
Copy-SpellEngineAsset "textures/particle/magic/heal.png" "src/main/resources/assets/martial_spells/textures/particle/paladin_source/magic/heal.png"

foreach ($frame in 0..7) {
    Copy-SpellEngineAsset "textures/particle/magic/vertical_stripe_$frame.png" "src/main/resources/assets/martial_spells/textures/particle/paladin_source/magic/vertical_stripe_$frame.png"
}

foreach ($frame in 0..14) {
    Copy-SpellEngineAsset "textures/particle/zone/effect_637_$frame.png" "src/main/resources/assets/martial_spells/textures/particle/paladin_source/zone/effect_637_$frame.png"
}

foreach ($frame in 0..16) {
    Copy-SpellEngineAsset "textures/particle/zone/effect_676_$frame.png" "src/main/resources/assets/martial_spells/textures/particle/paladin_source/zone/effect_676_$frame.png"
}

Write-Host ""
Write-Host "Paladin/Priest P4 source asset sync complete."
Write-Host "Pinned Paladins commit: $SourceCommit"
Write-Host "Pinned Spell Engine VFX commit: $SpellEngineCommit"
Write-Host "Next: python .\tools\audit-paladin-p4.py"
