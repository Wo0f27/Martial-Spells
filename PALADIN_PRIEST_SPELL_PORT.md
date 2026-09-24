# Paladin / Priest spell port — CP11 contract

## Frozen sources

Paladins source contract:
- repository: `ZsoltMolnarrr/Paladins`
- branch lineage: `1.20.1-modern`
- frozen commit: `2807417a1dd9a65204c002ded487da0e6ae467a1`
- private-use source/assets only; upstream is All Rights Reserved

Iron's Spells target contract:
- Minecraft 1.20.1
- Forge 47.4.10
- Iron's Spells 'n Spellbooks `v1.20.1-3.16.3`
- Java 17
- Parchment `2023.09.03-1.20.1`

The Paladins equipment repository remains equipment-only. All spell implementation belongs in Martial Spells.

## Frozen spell catalogue

The upstream frozen source contains 15 spell JSONs. Fourteen are player-facing; one is an internal Lightwell helper.

### Holy Wand source binding
- `heal` — **Heal**, tier 0, Priest Holy, 16-block aimed friendly heal with self fallback.
- Migration decision: **do not register `martial_spells:heal`**. Iron's already supplies the basic Holy-healing role; the eventual Holy Wand integration will use native `irons_spellbooks:blessing_of_life` as the closest targeted-heal replacement instead of duplicating Heal under a second namespace.

### Holy Staff source binding
- `holy_shock` — **Holy Shock**, tier 1, Priest Holy, 16-block aimed dual-purpose heal-or-damage spell.

### Paladin Libram
- `flash_heal` — **Flash Heal**, tier 2, Protection.
- `blessed_strikes` — **Blessed Strikes**, tier 2, Retribution.
- `divine_protection` — **Divine Protection**, tier 3, Protection.
- `judgement` — **Judgement**, tier 3, Retribution.
- `battle_banner` — **Battle Banner**, tier 4, Protection.
- `immolation` — **Immolation**, tier 4, Retribution.

### Priest Holy Book
- `holy_beam` — **Holy Light**, tier 2, Priest Holy.
- `circle_of_healing` — **Circle of Healing**, tier 3, Priest Holy.
- `barrier` — **Barrier**, tier 4, Priest Discipline.
- `lightwell` — **Lightwell**, tier 4, Priest Holy.
- `levitate` — **Levitate**, tier 2, Priest Discipline.
- `penance` — **Penance**, tier 3, Priest Discipline.

### Internal helper
- `lightwell_orb` — **Holy Mote**, tier 0. Not player-obtainable; fired by Lightwell.

## Spell Engine -> Iron's translation rules

### School and damage ownership
- Player-facing Paladin/Priest magic registers as **Iron's Holy school**.
- Iron's `SchoolRegistry.HOLY_RESOURCE` is the school authority.
- Existing Paladins equipment Holy Spell Power therefore scales the port naturally.
- Source-authored hybrid mechanics are preserved explicitly in spell logic rather than by inventing another school:
  - Blessed Strikes keeps its Holy/melee hybrid role.
  - Judgement keeps its melee-dominant + Holy contribution and undead bonus.
- Spell Engine / Spell Power do not become dependencies of Martial Spells.

### Tier -> rarity
Spell Engine tiers are progression tiers, not spell levels. Initial fidelity mapping:

| Source tier | Iron's minimum rarity |
| --- | --- |
| 0 | Common |
| 1 | Uncommon |
| 2 | Rare |
| 3 | Epic |
| 4 | Legendary |

Each source spell initially has **max level 1**. Source coefficients, durations, ranges, cooldowns and Holy Spell Power scaling are the fidelity baseline. Multi-level redesign is out of scope for the port checkpoints and can be a later balancing pass.

### Cast time and cooldown
- Source seconds -> Iron's ticks at 20 ticks/second.
- Fixed source cooldown duration is preserved in seconds.
- Source proportional cooldown on channel spells must scale with actual channel completion rather than silently becoming a full fixed cooldown.
- Iron's normal cast-time/cooldown-reduction attributes remain authoritative unless the source explicitly disables haste/cooldown scaling (Lightwell does).
- Cancellation and partial-channel behavior must be tested explicitly.

### Resource cost
- The source `runes:healing_stone` reagent is **not** reintroduced.
- Source exhaustion/item costs are recorded as source behavior but are not compatibility dependencies.
- Iron's mana is the casting resource. Per-spell mana values are a target-system balance parameter, not a source-fidelity invariant, and will be documented per implementation checkpoint.

### Targeting and relations
- Helpful spells must not heal hostile targets merely because they are LivingEntity instances.
- Dual-purpose spells must choose helpful vs harmful behavior from relation/target validity.
- Source self-fallback behavior is preserved where authored.
- All healing, damage, effects, summons and ownership decisions are server-authoritative.

### Assets and namespace
- Privately reused Paladins spell assets are copied into the `martial_spells` namespace.
- Do not add spell resources under `assets/paladins`; the equipment mod owns that namespace in the assembled pack.
- Iron's own assets are never copied into this repository.
- Spell icons follow Martial Spells' existing `textures/gui/spell_icons/<id>.png` convention.
- Paladins-only sounds/models/textures needed for fidelity are copied source-exact, then referenced under Martial Spells resource IDs.

## Mechanic checkpoint split

### P0 — archaeology and translation contract
Inventory and freeze the exact source/target contract. No gameplay implementation.

### P1 — Holy healing core
- Flash Heal
- Holy Shock
- Circle of Healing
- Upstream Heal is intentionally not ported because Iron's already covers the basic targeted/self-healing role.

P1 target normalization:
- the three custom spells use a shared Iron's base spell power reference of **5**,
- frozen source coefficients remain exact multipliers over that reference,
- unmodified target baselines are Flash Heal 6.0, Holy Shock 2.0 heal / 4.0 damage, Circle of Healing 2.0 per target,
- Iron's generic Spell Power and Holy Spell Power then scale those values normally,
- P1 mana values are target-side balance values: Holy Shock 20, Flash Heal 30, Circle of Healing 40,
- those mana values are not claimed as upstream Spell Engine fidelity.

### P2 — Retribution and protection core
- Blessed Strikes
- Divine Protection
- Judgement
- Immolation

P2 translation:
- Blessed Strikes keeps the frozen 2.5-second channel and five 0.5-second seal releases. Amplifier + 1 is the seal count, capped at 6 like the source; one successful melee swing consumes one seal on the next tick so same-tick Better Combat cleaves all receive the bonus impact.
- Blessed Strikes and Immolation preserve the frozen weighted-average power blend exactly: 75% Holy reference power / 25% current Attack Damage.
- Judgement preserves the inverse blend: 75% current Attack Damage / 25% Holy reference power.
- Divine Protection reproduces Spell Engine Protection semantics: effect amplifier is floor(0.5 x effective Holy multiplier), capped at 2, so amplifier + 1 gives 1-3 fully negated incoming attacks during the 8-second window.
- Judgement preserves the required 16-block hostile target, 0.5-second cast, 12-block launch height, 1.2-block/tick meteor descent, 6-block squared AOE falloff, +50% power against undead, and 3-second stun gate of 50 + 2 x Attack Damage target max health.
- Immolation preserves the 5-block / 0.5 vertical-range area, mixed helpful/harmful intent, 1.2 damage coefficient, 0.5 heal coefficient, 4-second burn, +50% undead power, and source guaranteed undead critical at the default source 1.5x critical multiplier.
- Iron's 1.20.1 exposes no spell-critical-chance or spell-critical-damage attributes. Therefore generic source random crit blending cannot be translated one-for-one; hybrid power weights are exact, while Immolation's explicit guaranteed undead critical is preserved at the frozen source default critical multiplier.
- Target-side mana: Blessed Strikes consumes 5 mana at channel initiation and each of its five seal ticks (30 for a full channel); Divine Protection 40; Judgement 45; Immolation 60. These are balancing values, not upstream reagent fidelity.
- Exact Judgement projectile model/rendering remains P5 presentation work; P2 preserves server-authoritative meteor timing and impact behavior.

### P3 — Priest channel and control
- Holy Light (`holy_beam`)
- Levitate
- Penance

P3 translation:
- Holy Light preserves Spell Engine BEAM semantics: every valid living entity
  intersecting the current 32-block beam before the first blocking collider is
  affected on each source-timed pulse; it is not reduced to a first-hit raycast.
- Channel deliveries use Spell Engine's equal-interval midpoint schedule and
  authored channel-value multipliers: Holy Light 25 releases over 5 seconds
  (0.2x output per release), Levitate 4 releases over 1.5 seconds, and Penance
  3 releases over 1.5 seconds (0.5x damage/knockback per bolt).
- Levitate preserves reset-velocity +0.15Y releases, horizontal root during the
  channel, 5-second Levitate refresh, and the executable 1.20.1 Slow Falling
  carrier logic: Slow Falling is refreshed only below 20 ticks remaining and is
  then set to current Levitate duration + 60 ticks. Because Levitate itself is
  refreshed by each channel release, a full channel can leave less than three
  seconds of Slow Falling after Levitate ends; the executable behavior wins
  over the broader source comment.
- Penance preserves its sticky 20-block hostile target, Spell Engine shoulder
  launch point, initial caster-look launch direction, 0.8 projectile speed,
  16-degree/tick homing, 20-block projectile travel cap, 8-block no-falloff
  ally absorption pulse, and 6-second absorption duration.
- Penance follows the executable status-effect integer arithmetic: per bolt
  stacks are `1 + floor(0.1 * Holy power)`; amplifier cap is
  `2 + floor(0.3 * Holy power)`. The upstream "exactly one full volley"
  comment is not used as a replacement for the executable truncation order.
- P3 target-side mana values are Holy Light 40, Levitate 25, Penance 45.
  These are balancing values, not upstream reagent fidelity.
- Exact Holy Light beam rendering and Penance's orbiting Lightwell-orb model
  remain P5 presentation work; P3 must still avoid missing-model geometry.

### P4 — constructs and summons
- Barrier
- Battle Banner
- Lightwell
- Holy Mote (`lightwell_orb`) internal helper

### P5 — bindings, presentation and final integration
- Paladin/Priest grouping
- source-equivalent Holy Wand -> native Iron's Blessing of Life binding
- source-equivalent Holy Staff -> Holy Shock binding
- spell icons/lang/SFX/models
- equipment soft integration
- full client/server/package regression

## Source behavioral baselines

| Spell | Tier | Range | Cast | Cooldown | Core behavior |
| --- | ---: | ---: | --- | ---: | --- |
| Heal | 0 | 16 | 1.0s | 4s | aimed friendly heal; self fallback |
| Holy Shock | 1 | 16 | 1.5s | 3s | friendly heal or enemy Holy damage |
| Flash Heal | 2 | 16 | 0.5s | 6s | stronger aimed friendly heal; self fallback |
| Blessed Strikes | 2 | 0 | 2.5s channel / 5 releases | 12s | stack up to five weapon blessings; melee hit spends one |
| Holy Light | 2 | 32 | 5s channel / 25 ticks | proportional 10s | beam heals allies, damages enemies |
| Levitate | 2 | 0 | 1.5s channel / 4 ticks | proportional 24s | repeated upward push + 5s float/soft-fall |
| Divine Protection | 3 | 0 | instant | 30s | protects from a source-scaled number of incoming attacks |
| Judgement | 3 | 16 | 0.5s | 15s | meteor, 6-block AOE, hybrid damage, undead bonus, stun |
| Circle of Healing | 3 | 8 | 0.5s | 10s | heals caster + friendly targets in area |
| Penance | 3 | 20 | 1.5s channel / 3 bolts | proportional 12s | homing damage bolts; absorption pulse around hit target |
| Barrier | 4 | 4 | 0.5s | 40s | 10s circular protection entity |
| Battle Banner | 4 | 0 | instant | 45s | 10s allied attack-speed/knockback-resistance aura |
| Immolation | 4 | 5 | instant | 12s | Holy damage + fire to enemies; heal self/allies |
| Lightwell | 4 | 0 | instant | 45s, source haste-disabled | 12s stationary healer summon |
| Holy Mote | helper | 12 | instant | 1.5s | Lightwell-only homing heal projectile |

## Ownership boundary

Do not move equipment code into Martial Spells. Do not add Paladins worldgen, Monk profession/workstation content, Spell Engine, Spell Power, or Runes as dependencies.

CP11 is complete only when all P1-P5 checkpoints receive local build/runtime validation and developer-confirmed PASS.
