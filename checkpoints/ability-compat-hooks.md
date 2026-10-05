# Ability Compatibility Hooks — implementation checkpoint

Branch: `armory/cp5-compat-hooks`. Accepted base: `7b11423a1000fd2a57477a34fbb0c6554f5b03f5`.

Status: **CP5 COMPATIBILITY HOOKS — RUNTIME PASS** (user-confirmed 2026-10-05). Static/build/publication/external consumer compile gates previously passed; final closeout results are recorded below. Armory was not modified or resumed; Codex did not launch Minecraft.

## Source closure and implementation

Eight retained identities/classes were reconciled against frozen Armory `72a3ec94642390a4586b28374f61254223921471`, Paladins `2807417a1dd9a65204c002ded487da0e6ae467a1`, Rogues `89ba33ad29adc42d7306660f5b74f28bd17b8ffa`, Spell Engine `d057c13b434cadc8eeaea6df8deab6992f8bda7e`, and actual MartialSpells executors. No unresolved source contradiction; the Penance PARTIAL translation is explicitly user-approved. See `MARTIALSPELLS_ABILITY_COMPAT_API.md` for every registry ID, implementation class, setter/getter, arithmetic, timing and persistence detail.

- Circle Healing: one server-cast coefficient adjustment, `0.40 * (1 + 0.10)`, for self/all eligible allies; no global Holy/healing change.
- Penance: existing Apothic impact-time chance +0.05 through strict ordinal-0 Sponge redirect and exact source/target/owner scoped context. Apothic crit damage, target RNG, particles and overcrit remain unchanged. No second roll, attribute mutation or launch crit snapshot; absorption code/NBT unchanged.
- Divine Protection/Judgement/Charge/Shadow Step: Iron's Pre cooldown event, LOW priority, subtract 100/60/40/60 ticks from the ordinary reduced cooldown; zero floor. This target adaptation preserves Iron's CDR then subtracts fixed seconds, unlike upstream's pre-haste deduction.
- Shout: +4.0 means `1 + 4.0`, exactly 5x chip damage (0.05 -> 0.25 coefficient). Status health gate/amplifier/duration/range/cooldown unchanged.
- Bear Trap: base power/control limit still captured and saved at cast. Query once at trigger, matching source impact-time modifiers: damage `captured P * 1.10`; control cap `captured cap + 2 * P * 0.10`. Upstream's unfiltered bonus affects both DAMAGE and STATUS_EFFECT control eligibility. No bonus written into saved values or applied twice.

`AbilityCompatibility` has eight volatile replacement providers with zero defaults and only frozen supported values; null/invalid/client use fails explicitly. External mods own qualification. No Armory knowledge, equipment detector, Wizards/Spell Engine dependency, or client-only gameplay imports. RangerCompatibility and Ranger mechanics/presentation remain unchanged.

Penance scope nests per thread and matches exact DamageSource, target and owner identities. Try-with-resources restores/removes the scope on exceptions; the original hurt timer is also restored in finally. Unrelated nested hits cannot qualify by caster/school/timing alone.

## Validation evidence

Initial offline clean build: PASS, 21 existing deprecation warnings, no Gradle test sources. New compatibility audit: PASS; AR2/AR3/AR4/AR5/AR6: PASS. The audit verifies pinned Apothic jar SHA-1 `1dc0744d17f0861fe6b067798d39abce9d8e0e86`, chance/damage read order and generated SRG refmap; standalone fixtures verify provider defaults/supported/rejected values and nested identity/exception cleanup. Fixtures do not prove Minecraft Mixin transformation.

Initial Maven Local publication and isolated external consumer compile: PASS. The consumer registers all eight ability provider types plus both Ranger types and resolves the published dev artifact without target-project source/classes/private APIs. Minecraft/Forge/external library types are compile-only prerequisites for the public Player signature.

Self-review found the existing dev publication lacked its refmap. The devJar task now includes the generated refmap, leaving all dependency sources/versions unchanged. Final clean offline build: PASS (21 existing deprecation warnings). Republication: PASS. Compatibility audit and AR6 rerun: PASS; AR2-AR5 earlier checks remain PASS with their code unchanged. External consumer recompile: PASS. Main/dev/sources published jars match current build outputs byte-for-byte; both binary jars contain the new refmap. Final git diff --check: PASS; 7 modified and 9 new files, all unstaged. HEAD remains the accepted base. Coordinates remain `com.w0of26:martial_spells:1.20.1-0.1.0`.

The historical AR6 whole-source freeze now permits only exact reviewed neutral-hook insertions and named new files, retaining unrelated-source and 106 private-asset protections. The new audit compares all affected old gameplay back to the accepted base after reversing only those insertions. Existing optional Archers/Paladins equipment fixture paths have no jars on this machine; Gradle skips them. No versions or fixtures were substituted.

## Completed user runtime matrix

User command from MartialSpells: `.\gradlew runClient`. No bonus is enabled automatically. A separate test consumer must register public providers and switch each zero -> supported bonus -> zero; the compile probe is not a loadable mod. These are MartialSpells hook tests, not Armory CP5 runtime validation.

Hold all other gear/attributes/targets constant. Baseline and restored-zero results must agree; repeated registration must replace, not stack.

| Ability | Supported provider | Test / exact expected change |
| --- | --- | --- |
| Circle of Healing | +0.10 | Injure caster and nearby friendly ally; avoid heal caps. Each receives 1.10x baseline healing once. Default reference output 2.0 -> 2.2 health before target heal modifiers. Other heals unchanged. |
| Penance | +0.05 | Current chance 0.05 -> 0.10; 1.00 -> 1.05 remains valid. Inspect exact callback/chance through a test consumer; do not infer the increment from a tiny random sample. Damage/particles/overcrit remain Apothic-owned; shields unchanged. |
| Divine Protection | -100 ticks | Neutral CDR: 30 -> 25 seconds. Nonzero CDR: reduced baseline minus 5 seconds, zero floor. Protected hit count unchanged. |
| Judgement | -60 ticks | Neutral CDR: 15 -> 12 seconds. With CDR: reduced baseline minus 3 seconds, zero floor. Meteor/AOE/stun unchanged. |
| Charge | -40 ticks | Neutral CDR: 12 -> 10 seconds. With CDR: reduced baseline minus 2 seconds, zero floor. Movement/buff unchanged. |
| Shout | +4.0 | Stable attack power/defenses, Apothic chance zero: chip damage 5x, coefficient 0.05 -> 0.25. Compare targets below/above the unchanged debuff cap; status duration/amplifier/range/eligibility must match baseline. |
| Shadow Step | -60 ticks | Neutral CDR: 12 -> 9 seconds. With CDR: reduced baseline minus 3 seconds, zero floor. Teleport/stealth unchanged. |
| Bear Trap | +0.10 | Stable dual-melee P, chance zero: triggered damage 1.10x; control cap 100 + 2P -> 100 + 2.2P; root duration remains 3 seconds. Change provider after placement to verify trigger-time qualification. Reload must not stack the modifier. |

### Penance-focused regression

1. First run with no provider: normal bolt damage and ordinary Apothic crit particles must remain.
2. Record original attributes. For a guaranteed first-crit setup use `/attribute @s attributeslib:crit_chance base set 1` and `/attribute @s attributeslib:crit_damage base set 1.5`; inspect effective values because equipment can still modify bases.
3. Switch provider 0/+0.05/0; verify exact effective chance via controlled consumer inspection. At 1.0/+0.05 Apothic must retain native overcrit behavior. A few damage samples cannot prove a 5% frequency difference.
4. Another MartialSpells damage spell, melee and ordinary projectile must never inherit Penance's +0.05. Their existing Apothic handling remains active.
5. Under identical Holy power, critical versus noncritical damaging bolts must leave shield stacks/amplifier/absorption output identical.
6. Exercise nested damage where available: different sources/targets must not qualify. Later unrelated damage must stay neutral after the scope closes.
7. Relog/reload, switch providers and restore recorded test attributes. No player attribute/NBT mutation or provider accumulation is expected.

PASS requires correct isolated adjustments, zero defaults, no duplication/stale context, unchanged unrelated abilities, unchanged Shout status and Penance shields, and successful client/dedicated-server Mixin application. The user confirmed full runtime PASS, including actual Apothic transformation, through the separate loadable external probe. This records MartialSpells compatibility readiness, not Armory implementation/runtime PASS.

## Frozen compatibility decisions

- `AbilityCompatibility` is the public neutral API for the eight non-Ranger hooks; `RangerCompatibility` remains the separate public Ranger API, unchanged.
- MartialSpells contains no Armory dependency or set/equipment knowledge. External consumers dynamically evaluate qualification against the supplied player.
- Penance preserves target-native Apothic crit semantics: only +0.05 chance is added for the exact scoped damage event; no second roll or crit multiplier exists. Apothic crit damage, particles and overcrit remain authoritative. Shields/absorption are independent.
- Demoralizing Shout +4.0 means authored damage coefficient `0.05 * (1 + 4.0) = 0.25`, a 5x damaging path; the debuff is excluded.
- Bear Trap evaluates adjustment once at the verified trigger-time stage over captured relevant power and source-authored control-cap scaling.
- Divine Protection/Judgement/Charge/Shadow Step fixed-tick adjustments apply after ordinary Iron's CDR, with a zero floor. Spirit Wolf remains the fixed-cooldown exception: 900/800 ticks, bypassing ordinary CDR.

## User-confirmed runtime acceptance — 2026-10-05

All ten public hooks passed through the external consumer runtime probe, including neutral -> enabled -> neutral restoration:

| Hook | Confirmed runtime result |
| --- | --- |
| Circle of Healing | Baseline -> healing x1.10 -> baseline; unrelated healing unchanged. |
| Penance | Native Apothic crits and +0.05 provider path; no duplicate roll/multiplier; crit damage, particles and overcrit preserved; shields/absorption unchanged; unrelated spells/melee/projectiles isolated; OFF restores baseline. |
| Divine Protection | 600 -> 500 -> 600 ticks; ordinary CDR ordering preserved; protected-hit behavior unchanged. |
| Judgement | 300 -> 240 -> 300 ticks; meteor/AOE/stun unchanged. |
| Charge | 240 -> 200 -> 240 ticks; movement/buff unchanged. |
| Demoralizing Shout | Baseline -> 5x authored damaging coefficient -> baseline; debuff unchanged. |
| Shadow Step | 240 -> 180 -> 240 ticks; teleport/stealth unchanged. |
| Bear Trap | Baseline -> relevant power x1.10 -> baseline; applied once at trigger; source-authored control-cap scaling; root duration unchanged. |
| Barrage | Baseline -> exactly +1 penetrable target -> baseline; existing crossbow Piercing preserved. |
| Spirit Wolf | 900 -> 800 -> 900 ticks; ordinary CDR remains bypassed. |

Neutral regression PASS: Power Shot, Entangling Roots, Rain of Arrows, Magic Arrow, Entangling Arrow and Caltrops. Probe session-state reset PASS.

The installed `run/mods/mscompat-runtime-probe-0.0.0-dev.jar` was removed exclusively after testing; other run/mods files were unchanged. The external project at `C:\Modding\MartialSpells-AbilityCompat-RuntimeProbe` remains available. No probe code is part of MartialSpells production or this checkpoint commit.

## Final closeout validation

Compatibility audit PASS; AR2 PASS; AR3 PASS; AR4 PASS; AR5 PASS; AR6 PASS (including all 106 tracked private assets unchanged). `gradlew clean build --offline`: BUILD PASS in this closeout session (21 existing deprecation warnings; no test sources). Production Java/resources remained byte-identical throughout closeout.

Maven Local consistency PASS for `com.w0of26:martial_spells:1.20.1-0.1.0`: rebuilt production jar matches published contents except the generated manifest build timestamp; mapped compiled classes/resources and published Java sources match. No republication was performed. RangerCompatibility remains byte-identical to the accepted pre-checkpoint base.

The compatibility audit now permits its four explicitly audited additions after Git staging/commit while still comparing all modified historical gameplay/resources against the accepted base. No production behavior changed during closeout. Python-generated cache files were removed; they are not checkpoint files. Final `git diff --check`: PASS.

Checkpoint disposition: ready for the authorized compatibility commit and branch-only push. JUS-132 remains In Progress; Armory implementation is not resumed by this closeout.
