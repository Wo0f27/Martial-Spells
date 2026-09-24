$ErrorActionPreference = "Stop"

# Re-assert the finalized P2 resource set first.
& "$PSScriptRoot\sync-paladin-p2-assets.ps1"

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
Write-Host "Pinned upstream commit: $SourceCommit"
Write-Host "Next: python .\tools\audit-paladin-p3.py"
