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
- instant self battle trance using a 10 second `STASH_EFFECT`
- the initial stash is amplifier 0 and therefore immediately grants +10% base Attack Damage
- each `MELEE_IMPACT` adds one amplifier, so amp 1 is +20%, through amp 9 at +100%
- the Attack Damage operation is `MULTIPLY_BASE`, exactly 0.1 per amplifier level
- source amplifier cap is 9
- impact stacking has `refresh_duration = false`; the original ten-second countdown never resets on hits
- `consume = 0`; successful hits do not consume the trance
- initial stash particles are hidden; impact-updated stacks show the normal effect particles
- source release sound is `rogues:slice_and_dice`
- source release VFX are 20 Spell Engine `magic_spark` particles in a centered radius-1 circle; Martial Spells translates this dependency-owned effect to a 20-point vanilla crit ring
- source release animation is Spell Engine's `dual_handed_weapon_charge`; the Forge port does not add Spell Engine solely for that generic pose
- 15 second base cooldown; normal Iron's Cooldown Reduction may reduce it
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
- exact upstream icon and release/impact sounds are retained
- frozen release presentation used three `smoke_medium` batches plus `lightning_arc_a` and `lightning_arc_b`; those particle assets belong to Spell Engine and are not copied into Martial Spells
- Martial Spells instead registers original `martial_spells:shock_powder_smoke` and `martial_spells:shock_powder_arc` sprite particles to reproduce the same powder-cloud/electrical silhouette without a Spell Engine dependency

### Shadowstep
- source tier 3; `rogue_subtlety`
- instant cast; requires a harmful aimed target within 15 blocks
- frozen Spell Engine `BEHIND_TARGET` default is **1.0 block**; the older 1.5-block note was incorrect
- destination is `target.position + target look vector * -1.0`, followed by a ground search up to 1.5 blocks downward
- the Forge ground search starts one block above the requested destination, matching the source helper's boundary-safe ground search and preventing the caster from being placed inside the supporting block
- R3 does **not** reject blocked/world-border/build-height destinations; an earlier safety gate made valid casts trigger inconsistently as the target moved or rotated, so validation is target-only
- source release sound is exactly `shadow_step_depart`; the repository's unused `shadow_step_arrive` sound is not imported
- source teleport VFX are 20 vanilla `cloud` particles on departure and 10 vanilla `poof` particles on arrival
- applies the beneficial Shadowstep marker for 1.5 seconds / 30 ticks
- while marked, hostile `TargetGoal` follow distance against the caster becomes 5 blocks
- 12 second base cooldown; normal Iron's Cooldown Reduction may reduce it
- source exhaustion 0.4 (omitted by port contract)
- source `spell_engine:one_handed_area_release` presentation is translated without adding Spell Engine as a dependency

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
- **R2 — Shock Powder:** PASS — user-confirmed shared stun, exact source range/control cap/cooldown, frozen icon/sounds, and Martial-owned custom smoke/arc VFX.
- **R3 — Shadowstep:** PASS — user-confirmed required 15-block harmful aim, corrected source-shaped 1.0-block behind-target teleport/ground placement, 30-tick anti-tracking marker, exact departure audio/icon/effect icon and vanilla cloud/poof VFX.
- **R4 — Slice & Dice:** VALIDATING — ten-second amp-0 start, exact `MULTIPLY_BASE` Attack Damage stacking on successful player melee damage, amp-9 cap, non-refreshing duration, exact icon/effect icon/sound, dependency-free release VFX translation.
- **R5 — Vanish:** stealth, target suppression, visual state, and all source break conditions.
- **R6 — Mutilate:** dual-held-weapon damage and source cone/melee delivery behavior.
- **R7 — Bear Trap:** three-placement server-owned trap entities, one-shot trigger, root and lifecycle.
- **R8 — Fidelity/final audit:** remaining assets/descriptions, dedicated-server validation, no Spell Engine/Spell Power leaks.

## R4 validation

After pulling `feature/rogue-spells-port`:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\sync-rogue-r2-assets.ps1
powershell -ExecutionPolicy Bypass -File .\tools\sync-rogue-r3-assets.ps1
powershell -ExecutionPolicy Bypass -File .\tools\sync-rogue-r4-assets.ps1
python .\tools\audit-rogue-r4.py
.\gradlew clean build
.\gradlew runClient
```

Runtime checks:

- Casting Slice & Dice immediately applies its ten-second beneficial effect at amplifier 0, which corresponds to +10% base Attack Damage.
- Each successful `minecraft:player_attack` melee damage event increments the amplifier exactly once, through amplifier 9 / +100% base Attack Damage.
- The hit that earns a stack uses the pre-hit amplifier; the increased amplifier applies to subsequent hits because stacking occurs at Forge `LivingDamageEvent` after damage modifiers have already been resolved.
- The effect's remaining duration must continue counting down instead of returning to ten seconds after a hit.
- At amplifier 9, additional melee hits must not refresh or alter the effect.
- Projectiles, spell damage, Shock Powder, Caltrops, environmental damage, misses, canceled damage, and zero-damage hits must not add stacks.
- Test normal vanilla melee and Better Combat basic combo hits; each actual landed player melee impact should add one stack.
- The cast uses the frozen Slice & Dice sound and source icon/effect icon. Release presentation is a 20-point circular crit-particle approximation of Spell Engine's dependency-owned `magic_spark` ring.
- Base cooldown is 15 seconds and remains eligible for normal Iron's Cooldown Reduction.

Do not advance to R5 until the user explicitly reports R4 PASS.
