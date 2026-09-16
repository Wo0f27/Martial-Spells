$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$roguesCommit = '89ba33ad29adc42d7306660f5b74f28bd17b8ffa'
$spellEngineCommit = '76cd9e128468ebe005463c729ec73eff7de5fb68'
$roguesBase = "https://raw.githubusercontent.com/ZsoltMolnarrr/Rogues/$roguesCommit/common/src/main/resources/assets/rogues"
$spellEngineBase = "https://raw.githubusercontent.com/ZsoltMolnarrr/SpellEngine/$spellEngineCommit/common/src/main/resources/assets/spell_engine"

$assets = @(
    @{ Url = "$roguesBase/textures/spell/bear_trap.png"; Path = 'src/main/resources/assets/martial_spells/textures/spell/bear_trap.png'; Hash = '89ed5f2f123da657d5e5161932b5f6b20deb274e' },
    @{ Url = "$roguesBase/textures/entity/bear_trap.png"; Path = 'src/main/resources/assets/martial_spells/textures/entity/bear_trap.png'; Hash = '0501909a687c11bf3d92c879eb8d0640c27b0442' },
    @{ Url = "$roguesBase/sounds/bear_trap_release.ogg"; Path = 'src/main/resources/assets/martial_spells/sounds/bear_trap_release.ogg'; Hash = '457dcedd900201c006c9f196f08ddd291e51e3fb' },
    @{ Url = "$roguesBase/sounds/bear_trap_impact.ogg"; Path = 'src/main/resources/assets/martial_spells/sounds/bear_trap_impact.ogg'; Hash = '6f08efe0749ddd00b58cbadfc445f65874704fd4' },
    @{ Url = "$roguesBase/sounds/bear_trap_spawn_1.ogg"; Path = 'src/main/resources/assets/martial_spells/sounds/bear_trap_spawn_1.ogg'; Hash = '6746e562f2cb0e127d5cc9ba0a8ba174694e5b88' },
    @{ Url = "$roguesBase/sounds/bear_trap_spawn_2.ogg"; Path = 'src/main/resources/assets/martial_spells/sounds/bear_trap_spawn_2.ogg'; Hash = '6034dcd722a5eee618a3a81a473e86ae74a43b1e' },
    @{ Url = "$roguesBase/sounds/bear_trap_spawn_3.ogg"; Path = 'src/main/resources/assets/martial_spells/sounds/bear_trap_spawn_3.ogg'; Hash = '3949832905d2bac472dedafcdb3b38213b82d0b1' },
    @{ Url = "$roguesBase/sounds/bear_trap_despawn_1.ogg"; Path = 'src/main/resources/assets/martial_spells/sounds/bear_trap_despawn_1.ogg'; Hash = '09a0d06a2727d8144561ba4506769a84aea545a9' },
    @{ Url = "$roguesBase/sounds/bear_trap_despawn_2.ogg"; Path = 'src/main/resources/assets/martial_spells/sounds/bear_trap_despawn_2.ogg'; Hash = '25dc3f8e60ecd59061213839ed83ad1da3f455f5' },
    @{ Url = "$roguesBase/sounds/bear_trap_despawn_3.ogg"; Path = 'src/main/resources/assets/martial_spells/sounds/bear_trap_despawn_3.ogg'; Hash = '76a0bedd2623557b586f0cb917ad8e26b5c56651' }
)

foreach ($asset in $assets) {
    $target = Join-Path $root $asset.Path
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $target) | Out-Null
    Write-Host "Fetching $($asset.Path)"
    Invoke-WebRequest -UseBasicParsing -Uri $asset.Url -OutFile $target
    $actual = (git -C $root hash-object -- $target).Trim()
    if ($actual -ne $asset.Hash) {
        throw "Hash mismatch for $($asset.Path): expected $($asset.Hash), got $actual"
    }
}

$animationTarget = Join-Path $root 'src/main/resources/assets/martial_spells/player_animation/dual_handed_ground_release.json'
New-Item -ItemType Directory -Force -Path (Split-Path -Parent $animationTarget) | Out-Null
Write-Host 'Fetching exact Spell Engine dual_handed_ground_release animation'
Invoke-WebRequest -UseBasicParsing -Uri "$spellEngineBase/player_animations/dual_handed_ground_release.json" -OutFile $animationTarget
$animation = Get-Content -Raw -Encoding UTF8 $animationTarget | ConvertFrom-Json
if ($animation.name -ne 'dual_handed_ground_release') {
    throw "Unexpected PlayerAnimator registry name: $($animation.name)"
}

Write-Host 'R7 Bear Trap asset sync complete.'
