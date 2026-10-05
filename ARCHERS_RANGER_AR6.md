# Archers Ranger AR6 — final integration and consumer handoff

Status: **AR6 RUNTIME PASS** — user-confirmed full runtime validation.
The complete intended six-spell Archers Ranger migration is runtime-validated.
Accepted base: `7e3653d60a5ff56c1da6f4aab1cafd4bf1c1fc41`, branch
`archers/ranger-port`. AR1–AR5 are frozen and runtime-accepted. Source references
remain Archers `ee8fb4081422ba0ab8ced57fe773a1c048d7bf74` and Spell Engine
`d057c13b434cadc8eeaea6df8deab6992f8bda7e`. No additional ability or balance
change is introduced. The accepted mixed implementation commit `f1f9ad5` and
all 106 intentionally tracked private Paladin/Priest assets remain intact.

## Final player-facing catalogue

All IDs have namespace `martial_spells`, school Martial, and Ranger technique
classification. Zero mana is the Martial technique policy, not source mana
parity. Six migrated spells and two independent local spells occur once each in
the spell registry and Ranger tag. Cooldowns below are base ticks.

| ID | Max level | Mana | Cast type/time | Base cooldown | Cooldown policy | Delivery | Power source | Persisted state | Runtime acceptance |
| --- | ---: | ---: | --- | ---: | --- | --- | --- | --- | --- |
| `power_shot` | 1 | 0 | INSTANT / 0 | 160 | ordinary Iron's CDR | stash → physical arrow → mark | no direct spell damage; incoming damage +10/20/30% | effect duration/charges, arrow transfers/hits, target mark | AR2 + AR4 presentation |
| `entangling_roots` | 1 | 0 | INSTANT / 0 | 360 | ordinary Iron's CDR | stationary cloud, pulses 15..150 | current Attack Damage per pulse, health gate | cloud owner/age, ordinary effect NBT | AR2 |
| `barrage` | 1 | 0 | LONG / 10 ticks | 200 | ordinary Iron's CDR | critical ammo arrows at 0/+2/+4 | arrow enchantment base × RWA ratio × .75; Apothic once at join | vanilla arrows + Ranger metadata; transient four-tick jobs | AR3 |
| `rain_of_arrows` | 1 | 0 | LONG / 10 ticks | 300 | ordinary Iron's CDR | 21 FALL meteors at 0/5/…/100 | .4 × release-time nominal P | SavedData sequence/anchor/P, meteor owner/follow/distance/P | AR4 |
| `magic_arrow` | 1 | 0 | held LONG / reference round(20/haste) | 160 | ordinary Iron's CDR | non-arrow homing/piercing projectile | 1.2 × release-time P × r | owner/follow/P/r/range/distance/pierce/hit UUIDs | AR4 |
| `spirit_wolf` | 1 | 0 | INSTANT / 0 | 900 | fixed; only 0/-100 neutral adjustment | summons at 0/+5; lifecycle 20/600/20 | creation-time nominal P snapshot | owner/age/P/health/target/animation; pending second insertion | AR5 |
| `entangling_arrow` (local) | 5 | 0 | LONG / 20 ticks | 300 | ordinary CDR with 140-tick minimum | existing custom physical arrow / hard RootEntity | existing RWA + Martial Power / level formula | existing arrow/root duration | AR2–AR5 regression |
| `caltrops` (local) | 5 | 0 | INSTANT / 0 | 300 | existing ordinary CDR | thrown bundle → field | existing level/Martial damage | bundle/field owner, life and effects | AR2–AR5 regression |

## Shared-helper audit

- `NominalPhysicalRangedPower`: W × effective Apothic Arrow Damage, main hand
  first, bow/crossbow fallback 6/9, unarmed fallback 6. Rain and Magic capture
  once at release; Wolf captures once during preparation. Saved descriptors
  intentionally duplicate the snapshot into each created entity, without
  re-reading gear. Roots instead reads melee power on each pulse.
- `ArchersPhysicalArrow`: actual ammo type, bow customization/enchantments,
  crossbow Piercing, one RWA ratio and one .75 multiplier. Apothic owns its
  normal join scaling. No nominal P, Martial Power or manual Apothic multiplier.
- `RangerArrowMetadata`: one namespaced ForgeData compound with distinct
  eligibility/evaluated, transfer/hit, Barrage, iframe and extra-pierce fields.
  Power Shot and Barrage identities coexist; no new state key is added in AR6.
- `RangerTickScheduler` is ACTIVE for Barrage's short transient callbacks.
  Rain and Wolf use separate per-world SavedData descriptors, not parallel
  duplicate scheduling. Source owner/caster checks remain ability-specific.
- `RangerSourceVfx` is SHARED, despite its client-named package. It contains
  only common/server Minecraft types and particle geometry; it does not load
  renderers, Minecraft client, particle providers or sound-loop clients.
- Cast dispatch explicitly distinguishes external `irons_spellbooks:magic_arrow`
  from local Magic, Rain, Barrage and Entangling Arrow. Power Shot has no
  invented activation clip. Source Player Animator clips remain Rain/Magic-only.
- Wolf fixed cooldown replaces Iron's post-reduction value only for Wolf.
  All other cooldown policies and accepted packet protocol remain unchanged.

## Dead-code/resource decisions

| Candidate | Classification | Decision |
| --- | --- | --- |
| old `BarrageArrow`, renderer, trail particle/JSON/eight sprites, permanent iframe impact event | OBSOLETE-SAFE-TO-REMOVE, already removed in AR3 | verified absent; no further deletion |
| old five-level Barrage table and four-tick sequential scheduler | HISTORICAL-DOC-ONLY | absent from executables; preserve history |
| Barrage spell-data level normalization mixin | ACTIVE | retains safe old level-2–5 loading |
| Power Shot iframe/mark wrapper, tracked mark visual field and renderer | ACTIVE / SHARED with Barrage impact | retain; no temporary diagnostic geometry/logs remain |
| Ranger particle/model/source asset translations | ACTIVE | registered/referenced; retain accepted source geometry |
| `SpiritWolfFixedCooldown.ticks` arithmetic and provider | SHARED / public legacy helper | retain compatibility, expose stable API facade |
| import/model translation scripts and prior audits | HISTORICAL-DOC-ONLY tooling | retain provenance/reproducibility |
| local Caltrops renderer comment calling its model a placeholder | ACTIVE local technique, outside six-source migration | retain accepted independent presentation |
| Netted/Prone and other non-Ranger packets | SHARED with Warrior/control systems | retain; no Ranger packet deletion |
| unlisted legacy client MagicArrow/optional weapon mixin classes | UNCERTAIN / external spell compatibility | not in current common mixin list; retain without changing accepted animation behavior |

No additional code/resources were deleted. Search results alone are insufficient
to establish reflective/data/registry unreachability. No unrelated cleanup.

## Public hooks and consumer readiness

`api.RangerCompatibility` is the only class required by the consumer handoff:

- `setBarrageExtraPenetrableTargetsProvider(ToIntFunction<Player>)`: neutral
  zero, per-valid-ammo-launch query; 0..127 extra targets additive to baseline
  pierce with a widened/saturated sum. Output +1 adds one distinct target,
  except at byte saturation. Ordinary arrows/Entangling Arrow never query it.
- `setSpiritWolfCooldownAdjustmentProvider(ToIntFunction<Player>)`: facade
  over the accepted provider, only 0/-100, resulting 900/800 at the existing
  LOWEST cooldown Pre listener. No unrestricted cooldown control or ordinary CDR.
- Provider replacement (last setter wins), server-thread queries, common-setup
  registration and reset-by-zero are documented explicitly. No equipment IDs,
  set logic, dependency or optional direct classes are imported into MartialSpells.

Complete invocation contract and consumer examples live in
`MARTIALSPELLS_ARMORY_RANGER_API.md`. Equipment-side validation belongs to CP5.

Existing Maven publication `mavenJava` coordinates are
`com.w0of26:martial_spells:1.20.1-0.1.0`. Main remains the reobfuscated Forge jar;
AR6 adds mapped `dev` and main `sources` classifiers. Resources are in main/dev,
the public class is in all three. Publication does not leak dev-only fixtures
through transitive dependencies. External ForgeGradle consumers use `mavenLocal`
and `fg.deobf` on main; the isolated Java fixture uses `:dev` with the same
mapped Minecraft/Forge dependency classpath, never project class output.

## Persistence audit

| State | Storage/key boundary | Missing/legacy values and reload behavior |
| --- | --- | --- |
| Power Shot / Barrage arrows | ForgeData `martial_spells:ranger_arrow`: schema, eligibleShot, shotEvaluated, transfers, transferredHits, barrage, iframeBypass, extraPierceTargets | absent compound gives neutral false/0; old Barrage levels normalize through accepted mixin; no new keys |
| stash / Hunter's Mark / Roots effect | vanilla MobEffectInstance NBT | vanilla duration/amplifier persistence |
| Roots cloud | entity `Owner`, `AgeTicks` | absent owner causes no pulse effect; age clamped to 0..160; same phase after reload |
| Rain pending | world `martial_spells_rain_of_arrows_pending`, Jobs/Caster/Target/XYZ/Power/NextIndex/Deadline | index validated; no launch-zero restart; overdue catch-up unchanged |
| Rain meteor | entity Owner/Followed/SampledPower/DistanceTraveled | absent UUID resolves null; traveled threshold retained; no new migration |
| Magic projectile | entity OwnerId/Followed/SampledPower/ChargeRatio/MaxRange/DistanceTraveled/PierceRemaining/SuccessfulHits plus inherited motion | absent owner discards; successful UUID set and remaining flight persist; no cost replay |
| Wolf | entity SpiritOwner/SpiritAge/SpiritPower/SpiritTarget/SpiritAttackStart plus vanilla attributes/health | same-world UUID; age/snapshot restored without gear resampling or full heal |
| second Wolf | world `martial_spells_spirit_wolf_pending`, Pending/Entity/Owner/Power/XYZ/Yaw/Deadline | prepared UUID duplicate guard, one consumed descriptor; no first-wolf respawn |

Equal generic key names in separate entity types/world files do not collide.
No authoritative global reloadable arrow map exists. Vanilla piercing hit IDs
remain runtime-only, an already documented AR3/source limitation. Rain/Magic's
1200-tick emergency guard uses vanilla `tickCount`, which is not explicitly
written by these entities; the guard age can restart on reload, while saved
distance/range/hit state continues. This is an existing AR4 safeguard limitation,
not a new AR6 change; no claim of persisted emergency age is made here.

## Client / dedicated-server boundary

Main construction registers common effects/entities/particles/sounds/network.
Ranger registries contain no renderer/provider imports. `MartialClientEvents`
is `Dist.CLIENT`; render/animation/sound subscribers are client-scoped. The
active common mixin list contains no client mixin classes (client array empty);
unlisted legacy client mixin files do not enter that bootstrap. Common Ranger entities
branch on client/server before particle-only work and use standard entity spawn
packets/tracked serializers. The shared VFX helper itself has no client-only
Minecraft dependencies. The public API imports only Player, Java functional
types and the common fixed helper. No new packet, serializer or classloading
path reaches a client implementation from the server. This is static evidence;
no dedicated server or Minecraft instance was launched.

## Historical-document consistency

The frozen AR0/AR1 sections describe their then-current bases, not final code.
AR3's explicit correction supersedes same-tick Barrage with 0/+2/+4 critical
shots. AR4 supersedes earlier Power Shot particle approximations and confirms no
authored activation animation, plus final Rain/Magic presentation differences.
AR5 supersedes a strict-under-four follow reading with <=4 and records snapshot
reload plus 20/600/20 phases. AR6 supersedes AR1's rejection of pierce overflow
with safe saturation and supplies the public facade. Earlier private-untracked
statements are historical; the user-authorized mixed commit makes those 106
assets intentionally tracked. Earlier contracts are retained without rewriting.
Audits now check the final accepted catalogue and private inventory, and accept
only the precise default-neutral Barrage API insertion among frozen spell code.

## Validation results

| Gate | Result |
| --- | --- |
| AR2 audit | PASS; final catalogue / accepted tracked asset scope |
| AR3 audit | PASS; exact hook insertion allowed, publication-only Gradle changes |
| AR4 audit | PASS; source assets, frozen mechanics/presentation |
| AR5 audit | PASS; frozen Wolf and private assets |
| AR6 audit | PASS; frozen accepted main files except exact neutral-hook edits |
| `gradlew clean build --offline` | PASS, 57 seconds, 14 executed tasks; 21 existing deprecation warnings |
| `publishMavenJavaPublicationToMavenLocal writeRangerConsumerClasspath --offline` | PASS, 36 seconds; explicit `C:\Users\Ian\.m2\repository` |
| `python tools/probe-archers-ranger-api.py` | PASS; isolated Maven Local `:dev` dependency, two Gradle tasks, 16 seconds; main/dev/sources contain API |
| `git diff --check` | PASS |

Gradle task names were verified with `tasks --all --offline` before publication.
The API fixture lives only in ignored `build/ar6-consumer`; the probe tool is
retained for repeatable external compilation. The AR3 Git text reader now uses
UTF-8 explicitly to avoid falsely treating unchanged non-ASCII Gradle comments
as dependency changes on Windows. Earlier untracked-asset/future-ID assertions
were superseded by the user-accepted final catalogue and tracked inventory,
with frozen gameplay checks retained.

The AR6 checkpoint includes the new API, one Barrage call, saturated pierce sum, a
neutral helper comment, local publication support, docs and audit/probe tooling.
All accepted presentation/resources and other main sources remain unchanged.
The 106 private assets remain tracked and unchanged. The assistant did not run
Minecraft. The user authorized the AR6 closeout commit; no push or CP5 implementation.

**Armory CP5 readiness:** public API and local dependency consumption are
ready, with final Ranger runtime regression and AR6 acceptance complete.
Equipment-side bonuses are not implemented or validated here.

## Focused final runtime gate — completed

1. Power Shot: activate, fire a marked arrow, verify stash spending/mark/reticle.
2. Roots: cast near a valid target, verify cloud pulse, slow/jump and visible roots.
3. Barrage: three ammo, ten-tick draw, three critical shots at 0/+2/+4, ordinary
   crossbow baseline Piercing preserved with no installed provider bonus.
4. Rain: one basic 21-meteor sequence, moving target and accepted decal/sound.
5. Magic: subthreshold fizzle, valid held charge/release and normal model/travel.
6. Wolf: two at 0/+5, normal AI/presentation, fixed 900 cooldown with ordinary CDR.
7. Entangling Arrow: existing shot/hard root; Caltrops: existing field.
8. Recheck one Barrage + Power Shot interaction. No new metadata or timing change.

Equipment-specific +1 pierce and -100 cooldown tests remain CP5 responsibilities;
no fabricated equipment/set detector is installed here. A small API arithmetic
probe and byte-saturation check cover the new neutral hook statically. The user
confirmed AR6 FULL PASS and authorized the checkpoint commit.

## User-confirmed final runtime acceptance

- Power Shot activation, marked hit, Hunter's Mark reticle and stash consumption
  PASS; Barrage spends one charge per spawned arrow without duplicate/lost transfer.
- Entangling Roots cast, control mechanics and presentation PASS.
- Barrage normal three-shot 0/+2/+4 sequence and baseline crossbow Piercing PASS;
  the neutral external provider contributes zero, with no unintended extra target.
- Rain's full basic 21-meteor sequence, scheduler and presentation regression PASS.
- Magic Arrow subthreshold fizzle, held charge, valid release and presentation PASS.
- Spirit Wolf t=0/+5 pair, AI/follow/combat and fixed 900-tick cooldown PASS;
  ordinary Iron's cooldown reduction remains bypassed.
- Independent Entangling Arrow and Caltrops PASS.
- All six migrated IDs are present and complete; no Archers Ranger spell is missing.
- The public neutral API is accepted: future +1 Barrage target (subject to byte
  clamp), and future -100 Wolf ticks (900 to 800), with no equipment knowledge
  in MartialSpells. Equipment integration remains the upcoming CP5 work.

Final closeout rerun: AR2, AR3, AR4, AR5 and AR6 audits PASS;
`gradlew clean build --offline` PASS (55 seconds, 14 executed tasks, existing
deprecation warnings); `git diff --check` PASS. The prior Maven Local publication
remains valid: its published sources match every current main Java source,
including the public API, so no source/API change requires republication.
Only the explicit 14 AR6 files belong to this checkpoint. The 106 private assets
remain tracked and unchanged. No Armory dependency, push or CP5 work was added.
