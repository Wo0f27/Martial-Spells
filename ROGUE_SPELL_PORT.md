# Rogue Techniques Port — Forge 1.20.1

## Source freeze

Primary behavior source: `ZsoltMolnarrr/Rogues`, frozen modern commit
`89ba33ad29adc42d7306660f5b74f28bd17b8ffa`.

Martial Spells integration base: `main` at
`b6c370bdddba056642461ecf842d8c1006a1449c`.

The Rogues equipment port remains independent. Martial Spells owns the ported class techniques and does not require the Rogues equipment mod merely because these techniques originated there.

## Frozen Rogue inventory

The frozen modern `Book.ROGUE` contains exactly six class techniques:

1. `slice_and_dice`
2. `shock_powder`
3. `shadow_step`
4. `vanish`
5. `bear_trap`
6. `mutilate`

Warrior techniques are a separate later phase.

## Ownership and IDs

The ported techniques use the Martial school and the `martial_spells` namespace. They implement `MartialTechnique` with `MartialTechniqueClass.ROGUE` and belong to the `martial_spells:rogue_techniques` spell tag.

Planned IDs:

- `martial_spells:slice_and_dice`
- `martial_spells:shock_powder`
- `martial_spells:shadow_step`
- `martial_spells:vanish`
- `martial_spells:bear_trap`
- `martial_spells:mutilate`

Do not import Spell Engine or Spell Power as dependencies. Iron's Spells 'n Spellbooks remains the casting framework.

## Port resource contract

The frozen Spell Engine versions charged small vanilla exhaustion/hunger costs. Martial Spells deliberately omits those legacy costs and keeps its established Iron's technique model instead:

- Martial school;
- zero mana for the Rogue techniques unless a later design checkpoint explicitly changes that;
- no legacy Spell Engine exhaustion cost;
- source base cooldown preserved;
- normal Iron's Cooldown Reduction remains available where the source cooldown was haste-affected;
- source targeting/control/damage behavior preserved;
- single spell level during the fidelity port; level scaling is a separate future design pass, not invented during migration.

## Frozen source behavior

### Slice & Dice
- source tier 2; `rogue_blade`
- 10 second hard-duration battle trance
- each melee impact adds another +10% base attack-damage stack
- source cap is amplifier 9 and the duration does not refresh on stacking
- 15 second cooldown
- source exhaustion 0.2 (omitted by port contract)

### Shock Powder
- source tier 2; `rogue_subtlety`
- instant 5-block centered area; vertical range multiplier 0.5
- 2 second true stun
- source control apply limit: health base 50 + 2x physical-melee spell power
- frozen physical-melee power is current vanilla Attack Damage, so the Forge translation is `50 + 2x Attack Damage`
- 16 second base cooldown; normal Iron's Cooldown Reduction may reduce it
- source exhaustion 0.3 (omitted by port contract)
- Martial Spells reuses its shared `StunService` rather than creating a second stun implementation
- exact upstream icon and release/impact sounds are retained; Spell Engine-owned smoke/lightning particles are translated to vanilla smoke/cloud/electric particles

### Shadowstep
- source tier 3; `rogue_subtlety`
- required aimed target within 15 blocks
- teleport 1.5 blocks behind the target
- applies a 1.5 second untraceable period used by source mob-targeting behavior
- 12 second cooldown
- source exhaustion 0.4 (omitted by port contract)
- Forge port must use safe destination/collision validation rather than force a teleport into blocked space

### Vanish
- source tier 4; `rogue_subtlety`
- 8 second stealth
- source stealth reduces movement speed by 50%
- stealth breaks on attacking, taking a hit, using an item, or casting another spell
- stealth affects enemy targeting in addition to visual presentation
- 30 second cooldown
- source exhaustion 0.4 (omitted by port contract)

### Bear Trap
- source tier 3; `rogue_blade`
- places three one-shot traps 2 blocks from the caster at yaw offsets 0/120/240 degrees, delayed 0/3/6 ticks
- each trap lasts 20 seconds, radius 0.6, impact cap 1
- springing deals damage with zero knockback and roots for 3 seconds
- root prevents movement and jumping while still allowing attacks, item use, and casting
- source control apply limit: health base 100 + 2x physical-melee spell power
- 15 second cooldown
- source exhaustion 0.3 (omitted by port contract)

### Mutilate
- source tier 4; `rogue_blade`
- physical dual-melee technique
- strikes using both held weapons' damage
- forward melee hitbox: width 0.5, height 0.2, arc 160 degrees
- source attack delay 0.5 and does not allow additional hits on the same target
- 12 second cooldown
- source exhaustion 0.4 (omitted by port contract)

## Checkpoint plan

- **R0 — Archaeology/contract:** DONE — exact six-technique inventory and frozen behavior.
- **R1 — Rogue architecture:** DONE — `ROGUE` technique class + spell tag; no gameplay.
- **R2 — Shock Powder:** VALIDATING — shared stun, exact source range/control cap/cooldown, frozen icon/sounds, translated dependency-free VFX.
- **R3 — Shadowstep:** targeting, safe behind-target teleport, brief untraceable state.
- **R4 — Slice & Dice:** fixed-duration melee-hit stacking and exact attack-damage operation.
- **R5 — Vanish:** stealth, target suppression, visual state, and all source break conditions.
- **R6 — Mutilate:** dual-held-weapon damage and source cone/melee delivery behavior.
- **R7 — Bear Trap:** three-placement server-owned trap entities, one-shot trigger, root and lifecycle.
- **R8 — Fidelity/final audit:** remaining assets/descriptions, dedicated-server validation, no Spell Engine/Spell Power leaks.

## R2 validation

After pulling `feature/rogue-spells-port`:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\sync-rogue-r2-assets.ps1
python .\tools\audit-rogue-r2.py
.\gradlew clean build
.\gradlew runClient
```

Do not advance to R3 until the user explicitly reports R2 PASS.
