# Warrior Techniques Port — Forge 1.20.1

## Source freeze

Primary behavior source: `ZsoltMolnarrr/Rogues`, frozen modern commit
`89ba33ad29adc42d7306660f5b74f28bd17b8ffa`.

Martial Spells integration base: accepted Rogue-complete `main` at
`3c264221bf68e5a1b369f29639dc3d78651cc5c8`.

Working branch: `feature/warrior-spells-port`.

W0 archaeology was explicitly approved by the user before implementation began.

The Rogues equipment port remains independent. Martial Spells owns these class techniques and does not require the Rogues equipment mod merely because the designs originated there.

## Frozen Warrior inventory

The frozen `Book.WARRIOR` contains exactly six class techniques:

1. `throw` — Shattering Throw
2. `throw_net` — Throw Net
3. `charge` — Charge
4. `shout` — Demoralizing Shout
5. `last_stand` — Last Stand
6. `mortal_strike` — Mortal Strike

No additional Warrior spell is in scope for this port phase.

## Ownership and IDs

The ported techniques use the Martial school and the `martial_spells` namespace. Each implementation will implement `MartialTechnique` with `MartialTechniqueClass.WARRIOR` and belong to the `martial_spells:warrior_techniques` spell tag.

Planned IDs:

- `martial_spells:shattering_throw`
- `martial_spells:throw_net`
- `martial_spells:charge`
- `martial_spells:demoralizing_shout`
- `martial_spells:last_stand`
- `martial_spells:mortal_strike`

Do not import Spell Engine or Spell Power as dependencies. Iron's Spells 'n Spellbooks remains the casting framework.

## Port resource contract

As with the accepted Rogue phase:

- Martial school;
- zero mana unless a later design checkpoint explicitly changes it;
- no legacy Spell Engine exhaustion cost;
- source base cooldown preserved;
- normal Iron's Cooldown Reduction remains available where the source cooldown was haste-affected;
- source targeting, control, damage, timing, stacking, and movement behavior preserved;
- one spell level during the fidelity port; level scaling is a separate future design pass;
- no invented weapon-family requirement where frozen Rogues declared none.

## Shared source adapters

### Single-hand physical-melee power

Frozen Spell Engine `spell_power:physical_melee` POWER is the caster's current vanilla Attack Damage value. W1 provides `PhysicalMeleePower` as the target-native adapter.

It is intentionally distinct from `DualMeleePower`, which exists for Rogue Mutilate and includes off-hand contribution. None of the six Warrior techniques uses the dual-melee school.

It is also intentionally distinct from `MartialPowerHelper.calculateTechniqueDamage`, because the frozen Warrior spells did not multiply their Spell Engine physical-melee power by Martial Spell Power.

Common source control gates use:

`healthBase + powerMultiplier * physicalMeleePower`

W1 exposes that calculation without adding any gameplay hook.

### Charge infrastructure

W1 does not introduce a generic charged-technique runtime yet. The first charged vertical slice will establish only the behavior genuinely shared by Throw Net and Shattering Throw. This follows the project rule against over-generalizing before concrete mechanics exist.

## Frozen behavior summary

### Shattering Throw
- source tier 2; `warrior_fury`; Physical Melee
- charged cast: 0.5 sec / 10 ticks
- default Spell Engine minimum release ratio 0.2; linear curve; proportional innate output
- range 12 + up to 12 charge bonus = 24 blocks at full charge
- projectile visually uses the held item; the real held item is not removed
- velocity 0.8; homing angle 2; one bounce; spin -36 degrees/tick
- full-charge damage coefficient 1.0 x physical-melee power
- Shatter lasts 8 sec and applies -30% base Armor using MULTIPLY_BASE
- Shatter application health limit: 100 + 2 x physical-melee power
- 8 sec cooldown

### Throw Net
- source tier 2; `warrior_protection`; Physical Melee
- charged cast: 0.45 sec / 9 ticks
- minimum release ratio 0.2; linear curve; `output_scaling = 0.5`
- range 10 + up to 12 = 22 blocks at full charge; the stale 16 -> 28 source comment is not authoritative
- projectile velocity 1.0; homing angle 1; custom net model; spin 12 degrees/tick
- full-charge damage coefficient 0.1 x physical-melee power with 0.1 knockback
- Netted lasts 3 sec; movement/jump root, not stun; attacks/item use/casting remain allowed
- control health limit: 100 + 2 x physical-melee power
- 12 sec cooldown

### Charge
- source tier 3; `warrior_fury`; Physical Melee
- frozen source duration: 2 sec / 40 ticks
- W2 user-approved balance override: 10 sec / 200 ticks
- +50% base Movement Speed, MULTIPLY_BASE
- +50% base Knockback Resistance, MULTIPLY_BASE
- SET semantics; no stacking
- 12 sec cooldown
- the separate upstream Improved Charge skill-tree behavior is out of scope

### Demoralizing Shout
- source tier 3; `warrior_protection`; Physical Melee
- instant hostile area, 12-block range with vertical range multiplier 0.5
- Demoralized lasts 8 sec and uses ADD semantics
- each stack is -20% base Attack Damage, MULTIPLY_BASE; amplifier cap 5
- application health limit: 50 + 2 x physical-melee power
- direct damage coefficient 0.05 x physical-melee power; zero knockback
- 12 sec cooldown

### Last Stand
- source tier 4; `warrior_protection`; HEALTH school
- channel 2.5 sec / 50 ticks with 5 deliveries
- caster movement speed is zero while channeling
- each delivery adds one Last Stand stack and heals 0.2 x HEALTH power
- HEALTH power is current Max Health
- each effective stack grants +20% base Max Health and +20% base Knockback Resistance
- full channel reaches five effective stacks / +100% of both attributes
- effect duration 10 sec
- maximum cooldown 60 sec and source marks it proportional for early release
- stale source text mentioning damage reduction is superseded by the frozen effect config, which uses knockback resistance

### Mortal Strike
- source tier 4; `warrior_fury`; Physical Melee
- 0.5 sec / 10-tick windup
- one delayed vertical melee delivery; `delay = 0.3` is a melee timing ratio, not 0.3 sec
- +50% weapon damage
- hitbox width factor 0.5, height factor 1.5, arc 120 degrees
- strike routes through normal weapon attack behavior
- applies `spell_engine:bleed` for 6 sec
- source Bleed amplifier: 1 + floor(0.25 x physical-melee power)
- exact Spell Engine 1.10.5.034 Bleed tick constants must be resolved before the Mortal Strike checkpoint rather than inferred from a different revision
- 15 sec cooldown

## Fidelity risks locked by W0

- Throw Net full-charge range is 22, not the stale 28-block comment.
- Last Stand grants Max Health + Knockback Resistance, not damage reduction.
- Last Stand heal-per-channel-tick is part of the mechanic and must not be omitted.
- Netted is a root, not a stun.
- Shattering Throw renders the held weapon virtually and does not remove it from inventory.
- No frozen Warrior technique declares an axe/sword/heavy-weapon requirement.
- Projectile defaults inherited from exact Spell Engine 1.10.5.034 must be resolved before charged-projectile fidelity is locked.
- Mortal Strike Bleed must be checked against exact Spell Engine 1.10.5.034 before implementation.
- Last Stand proportional cooldown must be verified against exact Spell Engine execution before its checkpoint.

## Checkpoint plan

- **W0 — Archaeology/contract:** PASS — exact six-technique inventory and frozen behavior approved.
- **W1 — Shared Warrior architecture:** PASS — Warrior technique class/tag + single-hand physical-melee adapter; no Warrior gameplay.
- **W2 — Charge:** PASS — user validated the 10-second balance override and final no-overhead-sign presentation.
- **W3 — Demoralizing Shout:** PASS — user validated effect application, stacking behavior, and the source health gate.
- **W4 — Throw Net:** PASS — user validated the charged projectile, Netted root, textured projectile model, and synchronized physical net VFX.
- **W5 — Shattering Throw:** IMPLEMENTED / VALIDATING — charged held-item projectile, one block bounce, damage + Shattered Armor vertical slice.
- **W6 — Mortal Strike:** locked until explicit W5 PASS.
- **W7 — Last Stand:** locked until explicit W6 PASS.
- **W8 — Fidelity/final audit:** locked until explicit W7 PASS.

## W1 acceptance criteria

W1 has been explicitly accepted by the user. Its frozen gate was:

- `MartialTechniqueClass.WARRIOR` exists.
- `MartialTechniqueTags.WARRIOR_TECHNIQUES` resolves `martial_spells:warrior_techniques`.
- an empty `warrior_techniques.json` tag exists at W1, because W1 registers no Warrior spell.
- `PhysicalMeleePower` reads only vanilla Attack Damage and safely returns zero if the attribute is absent.
- no Warrior spell is registered and no Warrior runtime mechanic activates in W1.
- no Spell Engine or Spell Power dependency is introduced.
- accepted Rogue behavior remains unchanged.
- clean build passes.
- `runClient` boots successfully.

## W2 acceptance criteria

W2 is frozen as **PASS** after the user validated the final Charge behavior and instructed the project to proceed to W3.

- Charge is classified as `MartialTechniqueClass.WARRIOR` and is the only value in the Warrior technique tag.
- source tier 3 is mapped to Iron's `RARE` with one spell level and zero mana.
- Charge is instant and self-targeted.
- Charge deliberately lasts 200 ticks / 10 seconds after the user's W2 balance override; the frozen source duration was 40 ticks / 2 seconds.
- Movement Speed receives +0.5 `MULTIPLY_BASE`.
- Knockback Resistance receives +0.5 `MULTIPLY_BASE`.
- reapplication uses amplifier 0 / SET-style refresh semantics rather than stacking.
- base cooldown is exactly 12 seconds and uses normal Iron's Cooldown Reduction behavior.
- the upstream Charge icon/effect icon is retained exactly.
- `charge_activate.ogg` is committed directly from the frozen source bytes and verified against Git blob `77ec5f2f81300f268e686c9fe2c1800127f2936f`.
- Spell Engine's release presentation is translated without introducing Spell Engine as a dependency: floating Rage-tinted stripe particles, decelerating sparks, and a 50-particle smoke ring replace the earlier vanilla CRIT/POOF approximation. The source speed-sign popup is intentionally omitted by user preference.
- Improved Charge skill-tree behavior is not imported.
- W3+ Warrior spell classes remain absent.
- clean build passes.
- `runClient` boots successfully and runtime attribute behavior matches the frozen values.

`tools/sync-warrior-w2-assets.ps1` is an offline verifier for the bundled frozen sound. W2 is frozen; W3 must not alter its accepted behavior.


## W3 acceptance criteria

W3 is frozen as **PASS** after the user validated Demoralizing Shout and confirmed the Iron Golem case was the intended health-gate behavior.

- Warrior/Martial, one level, tier-3/RARE, zero mana, instant self-centered hostile area.
- 12-block range with 0.5 vertical-range multiplier and obstacle-aware line of sight.
- Demoralized lasts 160 ticks / 8 seconds.
- Demoralized applies -0.2 base Attack Damage per effect level using `MULTIPLY_BASE`.
- Source ADD semantics are preserved exactly: first application becomes amplifier 0, each later application adds 1, and source `amplifier_cap = 5` means a maximum Minecraft amplifier of 5 (Demoralized VI / six effective levels).
- The debuff applies only when target max health is <= `50 + 2 * PhysicalMeleePower`.
- The separate 0.05 x Physical Melee damage impact still applies to valid area targets even when they exceed the debuff health gate.
- Direct damage uses an armor-respecting Martial damage type and restores pre-impact velocity so the source's zero-knockback contract is preserved.
- Frozen `shout_release.ogg`, `demoralize_impact.ogg`, spell icon, and effect icon are committed directly.
- Release/impact VFX are translated locally with Rage-tinted smoke; no Spell Engine runtime dependency is introduced.
- Base cooldown is 12 seconds and normal Iron's Cooldown Reduction remains available.
- W3 is frozen; W4 may build on shared charged-technique infrastructure without changing Demoralizing Shout.


## W4 acceptance criteria

W4 is frozen as **PASS** after the user validated gameplay and the final synchronized physical net VFX.

- Warrior/Martial, one level, source tier-2 mapped to Iron's `UNCOMMON`, zero mana.
- charged cast uses the frozen 9-tick / 0.45-second source duration.
- releases below source minimum ratio 0.2 fizzle through Iron's normal cancellation path and do not trigger W4 cooldown/delivery.
- valid partial releases preserve the source linear charge curve and `output_scaling = 0.5`: output multiplier is `0.5 + 0.5 * chargeRatio`.
- projectile range is `10 + 12 * chargeRatio`: 12.4 blocks at minimum valid release and 22 blocks at full charge.
- projectile velocity is 1.0 with no gravity/drag, source age cap 1200 ticks, and 1 degree/tick homing toward the sticky target captured at cast start.
- projectile uses the frozen custom net model/texture and rotates 12 degrees per tick.
- travel sound plays every 8 ticks.
- full-charge direct damage is `0.1 * PhysicalMeleePower`; charge output scales damage and source 0.1 knockback.
- damage remains independent of the Netted control gate.
- Netted lasts 60 ticks / 3 seconds and applies only when target max health is <= `100 + 2 * PhysicalMeleePower`.
- Netted is ROOT semantics, not stun: movement and jumping are blocked, attacks/item use/spell casting remain allowed, and knockback is immune while the effect is active.
- frozen spell/effect icon, projectile texture, `net_casting.ogg`, `throw.ogg`, `net_travel.ogg`, and `net_impact.ogg` are committed directly from the source bytes.
- the source Blockbench projectile model is retained with only the asset namespace translated from `rogues:` to `martial_spells:`.
- a narrow Iron's LONG-release bridge is introduced only for `ReleaseChargedTechnique` implementations; ordinary Iron's spells retain their native cancellation behavior.
- base cooldown is 12 seconds and normal Iron's Cooldown Reduction remains available.
- W2 Charge and W3 Demoralizing Shout remain frozen.
- W5+ Warrior gameplay classes remain absent.


## W5 acceptance criteria

W5 is restricted to `martial_spells:shattering_throw` and remains **VALIDATING** until the user explicitly passes it.

- Warrior/Martial, one level, source tier-2 mapped to Iron's `UNCOMMON`, zero mana.
- charged cast uses the frozen 10-tick / 0.5-second source duration.
- minimum release ratio is the Spell Engine default 0.2; releases below 20% fizzle through Iron's normal cancellation path with no projectile/cooldown.
- source omits `output_scaling`, whose exact 1.20.1-modern default is 1.0, so innate output is fully proportional to linear charge ratio.
- charge-scaled range is `12 + 12 * chargeRatio`: 14.4 blocks at minimum valid release and 24 blocks at full charge.
- full-charge direct damage is `1.0 * PhysicalMeleePower`; at partial charge damage scales linearly with charge ratio.
- source Damage default knockback is 1.0, translated through Spell Engine's 0.4 vanilla base: `0.4 * chargeRatio`.
- source Damage default `bypass_iframes = true` is preserved for the direct hit.
- projectile velocity is 0.8 with no gravity/drag and source age cap 1200 ticks.
- AIM is not sticky: the homing target is resolved at release, not frozen when charging begins.
- homing turn cap is 2 degrees/tick.
- projectile has exactly one block bounce. The bounce reflects velocity across the hit-face normal, preserves speed, consumes the remaining travel in that collision tick, and then the next block impact terminates it.
- the projectile visually renders a captured registry-ID copy of the caster's main-hand item. The real held stack is never removed or moved; NBT/enchant glint are not serialized because upstream also resolves the captured item type's default stack.
- held-item visual uses Spell Engine's `ALONG_MOTION` orientation, `FIXED` display transform, -36 degrees/tick Z spin, and the source near-camera guard.
- release and 8-tick projectile travel cadence both use the frozen `throw.ogg`; direct impact uses frozen `throw_impact.ogg`.
- Shattered Armor lasts 160 ticks / 8 seconds with amplifier 0 / SET semantics and refreshes duration on reapplication.
- Shattered Armor applies -30% base Armor using `MULTIPLY_BASE`.
- Shattered Armor applies only when target max health is <= `100 + 2 * PhysicalMeleePower`; direct damage/knockback and throw-impact sound are independent of that gate.
- successful Shatter application emits the frozen 10-particle dark-red dripping-blood impact batch; while Shattered Armor remains active it continues to drip one particle per tick at amplifier 0.
- local dripping-blood translation uses the source `minecraft:drip_hang` sprite, #590000 tint, 0.11 +/-33% scale, DRIFT motion, gravity 0.8, collision, and 20-tick lifetime.
- frozen spell/effect icon and `throw_impact.ogg` are committed directly; W4's already-frozen `throw.ogg` is reused byte-for-byte.
- base cooldown is 8 seconds and normal Iron's Cooldown Reduction remains available.
- W0-W4 remain frozen.
- W6+ Warrior gameplay classes remain absent.
