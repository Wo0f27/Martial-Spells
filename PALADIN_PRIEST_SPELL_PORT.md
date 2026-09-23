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
- Heal
- Flash Heal
- Holy Shock
- Circle of Healing

P1 target normalization:
- all four spells use a shared Iron's base spell power reference of **5.0**,
- frozen source coefficients remain exact multipliers over that reference,
- unmodified target baselines are therefore Heal 2.5, Flash Heal 6.0, Holy Shock 2.0 heal / 4.0 damage, Circle of Healing 2.0 per target,
- Iron's generic Spell Power and Holy Spell Power then scale those values normally,
- P1 mana values are target-side balance values: Heal 15, Holy Shock 20, Flash Heal 30, Circle of Healing 40,
- those mana values are not claimed as upstream Spell Engine fidelity.

### P2 — Retribution and protection core
- Blessed Strikes
- Divine Protection
- Judgement
- Immolation

### P3 — Priest channel and control
- Holy Light (`holy_beam`)
- Levitate
- Penance

### P4 — constructs and summons
- Barrier
- Battle Banner
- Lightwell
- Holy Mote (`lightwell_orb`) internal helper

### P5 — bindings, presentation and final integration
- Paladin/Priest grouping
- source-equivalent Holy Wand -> Heal binding
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
