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
- source Bleed amplifier: 1 + floor(0.25 x physical-melee power), evaluated while Mortal Strike's temporary +50% Attack Damage modifier is still active
- exact Spell Engine 1.10.5.034 Bleed: lethal magic damage every 25 ticks; per effective stack 0.5 damage while stationary, scaling linearly to 2.0 damage at 2 blocks/sec horizontal speed
- 15 sec cooldown

## Fidelity risks locked by W0

- Throw Net full-charge range is 22, not the stale 28-block comment.
- Last Stand grants Max Health + Knockback Resistance, not damage reduction.
- Last Stand heal-per-channel-tick is part of the mechanic and must not be omitted.
- Netted is a root, not a stun.
- Shattering Throw renders the held weapon virtually and does not remove it from inventory.
- No frozen Warrior technique declares an axe/sword/heavy-weapon requirement.
- Projectile defaults inherited from exact Spell Engine 1.10.5.034 must be resolved before charged-projectile fidelity is locked.
- Mortal Strike Bleed was resolved against exact Spell Engine 1.10.5.034 commit `270a6d61b00f241c1c8adb87573ad6de2d547f66` before W6 implementation.
- Last Stand proportional cooldown was resolved against Spell Engine 1.10.5.034: early release applies the same fraction of the full effective cooldown as cast progress.

## Checkpoint plan

- **W0 — Archaeology/contract:** PASS — exact six-technique inventory and frozen behavior approved.
- **W1 — Shared Warrior architecture:** PASS — Warrior technique class/tag + single-hand physical-melee adapter; no Warrior gameplay.
- **W2 — Charge:** PASS — user validated the 10-second balance override and final no-overhead-sign presentation.
- **W3 — Demoralizing Shout:** PASS — user validated effect application, stacking behavior, and the source health gate.
- **W4 — Throw Net:** PASS — user validated the charged projectile, Netted root, textured projectile model, and synchronized physical net VFX.
- **W5 — Shattering Throw:** PASS — user validated charged release, held-item projectile, one block bounce, damage/knockback, Shattered Armor, sounds, and blood VFX. The comparatively noticeable drips on an Iron Golem were accepted as non-blocking/source-consistent presentation.
- **W6 — Mortal Strike:** PASS — user validated runtime behavior; exact upstream 1.10.5.034 windup/slash animation assets and playback mapping were re-verified before freeze.
- **W7 — Last Stand:** IMPLEMENTED / VALIDATING — exact five-pulse channel schedule, stacking defensive effect, sequential max-health healing, proportional early-release cooldown, and source VFX/audio.
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

W5 is frozen as **PASS** after the user validated the full runtime gate. The heavier-looking continuous blood drips observed on an Iron Golem are accepted as non-blocking presentation from the source-style center-origin particle emitter on a large target.

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


## W6 acceptance criteria

W6 is frozen as **PASS** after the user validated runtime behavior and the animation sequence was re-verified against Rogues `89ba33ad...` plus exact Spell Engine `1.10.5.034` commit `270a6d61...`.

- source tier 4 maps to Iron's `EPIC`, one spell level, Martial/Warrior, zero mana.
- standard 10-tick / 0.5-second cast; releasing early cancels rather than partially firing.
- exact Spell Engine `two_handed_slash_vertical_windup` is used during the cast and exact `two_handed_slash_vertical_slash` on release.
- exact frozen `mortal_strike_swing.ogg` starts the cast; `mortal_strike_whoosh.ogg` begins the melee delivery; `mortal_strike_impact.ogg` plays on up to three selected hit targets.
- source `range_mechanic: MELEE` resolves to fixed 3-block vanilla melee range on 1.20.1.
- hitbox factors are length 1.0, width 0.5, height 1.5 with a 120-degree arc.
- source melee `delay = 0.3` is a ratio of the current vanilla attack cycle, not seconds: contact delay is `round((20 / AttackSpeed) * 0.3)`, minimum 1 tick.
- the hit routes through normal `ServerPlayer#attack` so normal weapon/enchantment/critical/fire-aspect behavior remains available.
- source `damage_bonus = 0.5` is translated as a temporary +50% `MULTIPLY_TOTAL` Attack Damage modifier around the vanilla weapon attacks.
- the attack-strength ticker is forced fully charged for each selected contact and restored afterwards.
- target invulnerability time is temporarily zeroed for the skill hit and restored afterwards, matching Spell Engine's melee delivery.
- Bleed lasts 120 ticks / 6 seconds, uses SET/default refresh semantics, and has zero-based amplifier `1 + floor(0.25 * impactPhysicalMeleePower)`.
- `impactPhysicalMeleePower` is intentionally read while Mortal Strike's temporary +50% Attack Damage modifier is active, matching Spell Engine 1.10.5.034 execution order.
- local Bleed is harmful color `0xB30000`, lethal, and ticks every 25 ticks using vanilla magic damage.
- each effective Bleed stack deals 0.5 damage per tick at zero horizontal movement, linearly scaling to 2.0 damage at >=2 blocks/sec horizontal speed.
- active Bleed reproduces Spell Engine's visual spawner: every 5 entity ticks, `amplifier + 1` local dripping-blood particles at 0.1-0.3 speed.
- Mortal Strike's status-impact VFX emits 40 dripping-blood particles at 0.2-0.4 speed.
- exact Rogues Mortal Strike icon and all three sounds are committed directly.
- exact Spell Engine 1.10.5.034 Bleed icon and both vertical-slash animation JSONs are committed directly.
- base cooldown is 15 seconds with normal Iron's Cooldown Reduction.
- W0-W5 remain frozen and W7+ gameplay remains absent.


## W7 acceptance criteria

W7 is restricted to `martial_spells:last_stand` and its local presentation/runtime support and remains **VALIDATING** until explicit user PASS.

- source tier 4 maps to Iron's `EPIC`, one level, Martial/Warrior, zero mana.
- source HEALTH-school mechanics are translated directly from current Max Health; no Spell Engine or Spell Power dependency is introduced.
- exact authored channel duration is 50 ticks / 2.5 sec with five deliveries.
- exact Spell Engine 1.10.5.034 channel schedule is ticks `5, 15, 25, 35, 45`, followed by release at tick 50.
- Iron's native CONTINUOUS scheduler is intentionally not used because its 1.20.1 cadence does not match the frozen source; Iron's LONG cast state/UI is used as transport while W7 schedules the five source channel impacts from `onServerCastTick`.
- `getEffectiveCastTime` is fixed at 50 ticks because the frozen HEALTH school has no haste source for this spell.
- holding beyond 2.5 sec cannot stretch the channel; W7 settles a still-active cast automatically at source tick 50.
- caster movement speed is zero during channeling via a temporary -100% MULTIPLY_TOTAL Movement Speed modifier, removed on completion/cancel/cleanup. Incoming knockback is not forcibly erased.
- each scheduled pulse uses source ADD semantics: no current effect -> amplifier 0; later pulses increment by one; cap amplifier 4; duration refreshes to 200 ticks / 10 sec.
- full channel therefore reaches Last Stand V / five effective stacks.
- executable frozen effect config grants, per effective stack, +20% base Max Health and +20% base Knockback Resistance using MULTIPLY_BASE.
- the stale upstream comment mentioning -10% damage taken per stack is not implemented because no such modifier exists in the frozen executable config or its later correction history.
- source impact order is preserved: apply/refresh the stack first, then heal.
- source heal coefficient 0.2 combined with the 0.5-second channel output multiplier yields 10% of **current post-stack Max Health** per pulse. Healing therefore grows as Max Health stacks rise.
- early release preserves all stacks/healing already earned.
- early release cooldown is proportional to continuous cast progress: `effective full Iron cooldown * elapsed/50`, preserving Iron's normal Cooldown Reduction. Full channel keeps the normal effective 60-second base cooldown.
- exact `one_handed_ground_charge` cast animation and `one_handed_shout_release` release animation are committed from Spell Engine 1.10.5.034.
- exact `last_stand_start.ogg`, `last_stand_casting.ogg`, and `last_stand_release.ogg` are committed from frozen Rogues.
- early release explicitly syncs the release animation because Iron's suppresses finish animations for cancelled LONG casts; full completion uses Iron's normal finish-animation path.
- cast presentation reproduces the source every cast tick: 8 inward PHYSICAL_BLUE magic sparks and 6 50%-alpha PHYSICAL_BLUE smoke particles.
- active Last Stand emits the source `area_effect_700` ground aura once every 20 ticks at 1.5 scale, PHYSICAL_BLUE with 50% alpha, horizontally attached to the affected entity.
- effect application suppresses ordinary vanilla potion swirl particles, matching source `show_particles=false`.
- W0-W6 gameplay remains frozen.
