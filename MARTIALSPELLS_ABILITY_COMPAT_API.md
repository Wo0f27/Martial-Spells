# MartialSpells Ability Compatibility API

Status: implemented; full user-confirmed external-consumer runtime PASS (2026-10-05). Base `7b11423a1000fd2a57477a34fbb0c6554f5b03f5`, branch `armory/cp5-compat-hooks`.

MartialSpells has no Armory dependency. External mods own equipment/set qualification and install pure callbacks once during common setup (`enqueueWork`). All callbacks receive `net.minecraft.world.entity.player.Player`; setters replace rather than accumulate. Getters are server-only. Unsupported values (including NaN/infinity) throw; null providers are rejected. Nonplayer casters keep their existing behavior. Providers are volatile, evaluated on the gameplay server thread, never serialized. `RangerCompatibility` is unchanged and separate.

Public class: `com.w0of26.martialspells.api.AbilityCompatibility`.

Every setter below also exposes `get<Concept>Adjustment(Player)`; cooldown getters return int, other getters double.

| Spell ID (`martial_spells:`) | Class | Setter | Callback | Default / accepted bonus | Arithmetic and evaluation |
| --- | --- | --- | --- | --- | --- |
| circle_of_healing | PaladinCircleOfHealingSpell | setCircleOfHealingPowerAdjustmentProvider | ToDoubleFunction<Player> | 0.0 / 0.10 | On server cast, coefficient `0.40 * (1 + bonus)`; Holy power remains unchanged. Every eligible ally receives that coefficient once. No snapshot survives the instantaneous burst. |
| penance | PaladinPenanceSpell / PenanceProjectile | setPenanceCriticalChanceAdjustmentProvider | ToDoubleFunction<Player> | 0.0 / 0.05 | At the existing Apothic damage-impact roll, current crit chance + bonus. No 100% cap; crit damage, RNG and particles remain Apothic-owned. No crit snapshot is persisted. |
| divine_protection | PaladinDivineProtectionSpell | setDivineProtectionCooldownAdjustmentProvider | ToIntFunction<Player> | 0 / -100 | Iron's effective cooldown + bonus, floored at zero, during Pre cooldown event after ordinary CDR. Default 600 -> 500 ticks. |
| judgement | PaladinJudgementSpell | setJudgementCooldownAdjustmentProvider | ToIntFunction<Player> | 0 / -60 | Same cooldown pipeline; default 300 -> 240 ticks. Meteor/AOE/stun unchanged. |
| charge | ChargeSpell | setChargeCooldownAdjustmentProvider | ToIntFunction<Player> | 0 / -40 | Same pipeline; default 240 -> 200 ticks. Movement/buff unchanged. |
| demoralizing_shout | DemoralizingShoutSpell | setShoutDamageMultiplierAdjustmentProvider | ToDoubleFunction<Player> | 0.0 / 4.0 | Server cast: damage `melee power * 0.05 * (1 + bonus)`, hence 0.25 coefficient with bonus. Debuff health gate, amplifier, duration, radius and cooldown untouched. |
| shadow_step | ShadowstepSpell | setShadowStepCooldownAdjustmentProvider | ToIntFunction<Player> | 0 / -60 | Same cooldown pipeline; default 240 -> 180 ticks. Teleport/stealth unchanged. |
| bear_trap | BearTrapSpell / BearTrapEntity | setBearTrapPowerAdjustmentProvider | ToDoubleFunction<Player> | 0.0 / 0.10 | Base dual-melee power/control limit still captured at cast and saved. Query once at each trap's trigger: damage `captured power * (1 + bonus)`; control cap `captured cap + 2 * captured power * bonus`. No adjustment is written back, so reload/reequip cannot accumulate it. |

## Complete public method list

All setters return void and accept the function type below. All getters accept Player.

| Setter | Getter | Callback / result |
| --- | --- | --- |
| setCircleOfHealingPowerAdjustmentProvider | getCircleOfHealingPowerAdjustment | `ToDoubleFunction<Player>` / `double` |
| setPenanceCriticalChanceAdjustmentProvider | getPenanceCriticalChanceAdjustment | `ToDoubleFunction<Player>` / `double` |
| setDivineProtectionCooldownAdjustmentProvider | getDivineProtectionCooldownAdjustment | `ToIntFunction<Player>` / `int` |
| setJudgementCooldownAdjustmentProvider | getJudgementCooldownAdjustment | `ToIntFunction<Player>` / `int` |
| setChargeCooldownAdjustmentProvider | getChargeCooldownAdjustment | `ToIntFunction<Player>` / `int` |
| setShoutDamageMultiplierAdjustmentProvider | getShoutDamageMultiplierAdjustment | `ToDoubleFunction<Player>` / `double` |
| setShadowStepCooldownAdjustmentProvider | getShadowStepCooldownAdjustment | `ToIntFunction<Player>` / `int` |
| setBearTrapPowerAdjustmentProvider | getBearTrapPowerAdjustment | `ToDoubleFunction<Player>` / `double` |

## Source closure and accepted translations

Frozen Armory `72a3ec94642390a4586b28374f61254223921471`, `ArmorySpells.java`: Circle/Bear Trap +0.1 power; Penance +0.05 crit chance; cooldown reductions 5/3/2/3 seconds; Shout +4.0 restricted to DAMAGE. Frozen Rogues `89ba33ad29adc42d7306660f5b74f28bd17b8ffa` Bear Trap JSON has DAMAGE and STATUS_EFFECT impacts; the unfiltered power modifier also affects `100 + 2 * power` control eligibility. Spell Engine `d057c13b434cadc8eeaea6df8deab6992f8bda7e` resolves modifiers at impact over captured power. Therefore Bear Trap's provider belongs at trigger, including its control cap, not twice at cast and impact.

Frozen Paladins `2807417a1dd9a65204c002ded487da0e6ae467a1` supplies the retained Holy identities. There is no substitution with Iron's Healing Circle, Fortify or Divine Smite. All eight accepted target classes/registry bindings were inspected before editing; none already supplied these neutral hooks.

Iron's `1.20.1-3.16.3` MagicManager calculates ordinary cooldown reduction before posting SpellCooldownAddedEvent.Pre, then stores/synchronizes the event result. The new LOW-priority handler subtracts fixed ticks only for the four registry objects. Unlike upstream's pre-haste deduction, this target adaptation preserves the existing reduced cooldown then subtracts the specified seconds (zero floor). Subsequent already-added cooldowns remain stored normally; equipment changes affect future evaluations, not ongoing cooldowns.

## Penance: approved PARTIAL crit translation

Pinned external target: Apothic Attributes `1.20.1-1.3.7`, AttributeEvents.apothCriticalStrike(LivingHurtEvent), HIGH priority. Penance's Iron's source identifies its caster; SpellDamageEvent and resistance run before hurt and Apothic. Its first LivingEntity.getAttributeValue call reads CRIT_CHANCE; its second reads CRIT_DAMAGE.

A strict ordinal-0 Sponge redirect changes only the first read. It checks the attribute identity and requires the exact active DamageSource, exact target, expected owner and Player attacker. The synchronous ThreadLocal scope closes in try-with-resources, restoring outer scopes/removing the root even on exceptions. Different nested sources/targets do not qualify. No player AttributeModifier, second RNG roll, pre-multiplied projectile damage, crit-damage provider, or launch-time crit snapshot exists. Zero returns the original chance. Apothic overcrit, target RNG and particles remain authoritative; 1.00 + 0.05 remains 1.05. Shield calculations/NBT remain unchanged.

The source used one Spell Power crit; the approved target uses Apothic's existing repeated-roll system and impact-time attributes. This deliberate difference is not an unresolved contradiction. No new crit behavior is installed for other abilities; their preexisting Apothic processing is preserved.

## Validation and runtime acceptance

Run `python tools/audit-armory-cp5-compat-hooks.py`, AR2-AR6 audits, `gradlew clean build --offline`, then `gradlew publishToMavenLocal writeRangerConsumerClasspath --offline` and `python tools/probe-ability-api-consumer.py`. Coordinates stay `com.w0of26:martial_spells:1.20.1-0.1.0`. All ten public hooks and neutral regression passed user runtime testing through the external consumer probe; acceptance is recorded in `checkpoints/ability-compat-hooks.md`. Static checks remain regression gates; publication is needed only when production code changes, not for documentation closeout.

A consuming test mod must register providers; this checkpoint contains no equipment detector or automatically enabled armor bonus. See `checkpoints/ability-compat-hooks.md` for the runtime matrix.
