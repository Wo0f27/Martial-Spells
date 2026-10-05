# Archers Ranger AR5 — Spirit Wolf Pack

Status: **AR5 RUNTIME PASS** — all requested runtime tests are user-confirmed PASS.

Accepted implementation commit: `f1f9ad5306d31ec988ede1bb7457f87043495d1b`
(`WIP: preserve Archers Ranger AR5 working state`). The user explicitly accepts
this existing mixed commit, including the previously untracked Paladin/Priest
private assets. Those assets are intentionally tracked from this point onward.
The commit is retained without amendment or history rewriting.

## Source closure

Frozen executable references:

- Archers `ee8fb4081422ba0ab8ced57fe773a1c048d7bf74` (3.1.4+1.20.1-forge)
- Spell Engine `d057c13b434cadc8eeaea6df8deab6992f8bda7e` (1.10.5+1.20.1-forge)
- MartialSpells accepted AR5 base `92e9f34a6af8bd2eaa673ff98a3e41b7a3e4f494`

Inspected source paths include `ArcherSpells.java`, `ArcherSummons.java`,
`SpiritWolfEntity.java`, `ArcherEntities.java`, `DirewolfEntityModel.java`,
`DirewolfEntityAnimations.java`, `DirewolfEntityRenderer.java`,
`ArcherRenderLayers.java`, Spell Engine `SpellImpacts.java`, `SummonedEntity.java`,
`EntityPlacements.java`, `WorldScheduler.java`, and the summon goal classes
`FollowSummonerGoal`, `TeleportToSummonerGoal`, `PhaseBlockGoal`,
`DynamicMeleeAttackGoal`, `DefendOwnerGoal`, `ClosestAttackerRevengeGoal`,
`MirrorOwnerAttackGoal` and `DetectionRangeTargetGoal`.

No material source contradiction was found. The exact follow comparison is
`distance_squared > stop_distance²`, so it stops at 4 blocks or closer. Start
is strictly greater than 10 blocks and teleport is strictly greater than 32.

## Summon and lifecycle translation

`martial_spells:spirit_wolf` is Ranger/Martial, level 1, zero mana and instant.
The source cooldown is 45 seconds with haste disabled. The target uses the
neutral fixed cooldown helper: 900 ticks normally, with an unwired adjustment
seam that can supply only -100 ticks for a future Armory integration. Ordinary
Iron's cooldown reduction is bypassed.

The source prepares two wolves at release. Wolf one is inserted at delay 0, one
block in front; wolf two is inserted at delay 5, one block to the caster's
right. Placement uses caster yaw, ground snapping and the source's upward
two-block terrain recovery. The target stores the second insertion in a tiny
`SavedData` descriptor. This intentionally improves the source's in-memory-only
reload behavior without restarting or duplicating the first wolf.

Lifecycle ages are spawn 0–19 (20 ticks), active 20–619 (600 ticks), despawn
620–639 (20 ticks), then removal at age 640. Phase is synchronized and saved.
Spawn/despawn phase blocks movement, look and jump; active phase enables combat
and follow goals. There is no duplicate-summon cleanup.

Owner UUID is synchronized and saved. Resolution is a player lookup in the
current world only. Logout, death, missing owner and cross-dimension absence do
not immediately discard or rebind a wolf; owner-dependent goals simply wait
while lifecycle aging continues.

## Attributes and scaling

The source base and additive owner ranged-power scaling are:

| Attribute | Base | Additive term | Target final form |
| --- | ---: | ---: | --- |
| Max health | 20 | `+P` | `20 + P` |
| Movement speed | 0.35 | none | 0.35 |
| Attack damage | 3 | `+0.3P` | `3 + 0.3P` |
| Armor | 0 | `+10 + 0.1P` | `10 + 0.1P` |
| Attack knockback | 0 | `+0.1P` | `0.1P` |
| Knockback resistance | 0 | `+5 + 0.05P` | `5 + 0.05P` |
| Follow range | 32 | none | 32 |

`P` is sampled once from the accepted `NominalPhysicalRangedPower` contract at
creation. The snapshot and resulting attributes are saved, so reload does not
resample changed equipment. This is an intentional robustness divergence from
Spell Engine's owner-resolve-on-load behavior. No Martial Power, Iron's Spell
Power or Apothic arrow scaling is involved.

## AI and combat

Goal priorities mirror the source: float, lifecycle phase lock, owner teleport,
melee bite, look, owner follow, idle wander and look-around. Target priorities
are owner defense, closer-attacker revenge, owner attack mirroring and hostile
acquisition. Owner/allied/tamed targets are excluded. Autonomous acquisition is
restricted to hostile mobs; owner-directed and retaliation goals use the same
owner relation gate.

Follow starts above 10 blocks and stops at 4 blocks or closer. Its preferred
point is two blocks to the owner's right, using the last significant owner
velocity as forward and north as the initial fallback. At a distance strictly
greater than 32, teleport recovery searches a small safe set around the owner,
clears the current target and stops navigation. The source teleports directly;
the safe search is a narrow target-side collision safeguard.

The bite action has duration 14 ticks, windup 7 ticks, speed 1.5 and movement
speed 1.2 (0.6 during the swing). It uses the source growl/impact sounds and
vanilla mob damage attribution. It does not use projectile helpers or add
Hunter's Mark itself.

## Presentation and provenance

The exact Archers icon, direwolf texture and three Archers sounds are imported
under the MartialSpells namespace. Model cuboids and animation clips are
translated from the frozen Archers Java sources without changing coordinates or
keyframe timings. The run clip uses a local port of Spell Engine's loop-seam
interpolation. The renderer uses the source shadow radius and a local
translucent emissive, depth-writing layer. Spell Engine's shader compatibility
fallback is not added as a runtime dependency. The local emissive rendering,
animations, loop transitions, particles and sounds are runtime-accepted; this
acceptance does not establish untested shaderpack compatibility.

| Source presentation | Target | Status |
| --- | --- | --- |
| `textures/spell/spirit_wolf.png` | `textures/gui/spell_icons/spirit_wolf.png` | EXACT |
| `textures/entity/direwolf_spell.png` | `textures/entity/spirit_wolf.png` | EXACT |
| Direwolf cuboids | `SpiritWolfModel` | FAITHFUL_TRANSLATION |
| idle/run/bite/spawn clips | `SpiritWolfAnimations` + loop helper | FAITHFUL_TRANSLATION |
| translucent emissive layer | `SpiritWolfRenderType` | FAITHFUL_TRANSLATION |
| 20 DECELERATE PIPE + 20 SPHERE nature sparks | `RangerSourceVfx.spiritWolfSpawn` | FAITHFUL_TRANSLATION |
| 20 ASCEND PILLAR nature sparks | `RangerSourceVfx.spiritWolfDespawn` | FAITHFUL_TRANSLATION |
| summon/spawn/wolf sounds | local sound registry/resources | EXACT |

## Files and validation

Implementation files are `SpiritWolfSpell`, `SpiritWolfPending`,
`SpiritWolfEntity`, the fixed cooldown helper/event, entity/particle/sound
registries, `SpiritWolfModel`, `SpiritWolfAnimations`,
`SpiritWolfLoopAnimation`, `SpiritWolfRenderer`, `SpiritWolfRenderType`, and
the AR5 import/audit tools. Existing Ranger gameplay classes are left alone.

Run the static gate with:

```text
python .\tools\audit-archers-ranger-ar5.py
```

Run the build with:

```text
.\gradlew clean build --offline
```

Developer runtime matrix: verify one wolf at release and the second at exactly
+5 ticks; front/right placement and ground snapping; 20/600/20 phase changes
and model scale/reversal; follow start/stop at >10 and <=4; teleport above
>32 with target clear; owner attack/defense/revenge/hostile AI; owner
logout/death/dimension absence; no duplicate cleanup; P scaling and health
persistence; save/reload in active and despawn phases; save/reload inside the
five-tick pending window; fixed 900-tick cooldown with ordinary CDR; exact
model/texture/glow, spawn/despawn particles and sounds; and regressions for
Power Shot, Entangling Roots, Barrage, Rain, Magic Arrow, Entangling Arrow and
Caltrops.

## User-confirmed runtime closeout

All requested runtime checks passed:

- Registration, level 1, zero mana and instant cast; exactly two wolves at
  t=0 and t=+5, with front/right yaw-relative placement and terrain/collision
  recovery.
- The 20-tick spawn, 600-tick active and 20-tick despawn phases, without
  premature removal. Wolf two retains its five-tick lifecycle offset.
- Source icon, Direwolf model/texture, emissive rendering, spawn particles,
  idle/run/bite animations, acceptable loop transitions, one-handed healing
  release, despawn presentation and sounds.
- Autonomous hostile targeting, owner assist, retaliation, friendly/owner
  exclusion, melee pursuit, bite cadence and damage.
- Follow starts beyond 10 blocks, stops at 4 blocks or closer, uses the
  owner-relative follow point, and recovers beyond 32 blocks while clearing
  the previous attack target.
- Owner death/logout/dimension absence does not immediately despawn wolves;
  aging continues, ownership resumes appropriately and never rebinds.
- Active/despawn/pending-second-summon reload preserves lifetime, health,
  owner and acceptable target/state continuity, without restarting lifecycle,
  duplicating wolf one or repeating wolf two.
- Low/high power validates health `20 + P`, armor `10 + 0.1P`, attack damage
  `3 + 0.3P`, attack knockback `0.1P` and knockback resistance `5 + 0.05P`.
  Creation snapshots survive equipment changes and reload without resampling.
- Fixed 900-tick cooldown ignores ordinary Iron's cooldown reduction; the
  neutral future adjustment seam remains available without equipment checks.
- No one-pack cleanup or old-wolf replacement was introduced.
- Power Shot, Entangling Roots, Barrage, Rain of Arrows, Magic Arrow,
  Entangling Arrow and Caltrops all passed regression validation.

Closeout changes are documentation and the audit's private-asset ownership
check only. No gameplay is changed, no push is authorized and AR6 has not begun.

Final closeout validation: AR5 static audit PASS; `gradlew clean build --offline`
PASS (55 seconds, 12 executed tasks; existing deprecation warnings); and
`git diff --check` PASS. The audit verifies all 106 intentionally tracked
private assets against the accepted implementation commit without modifying
them. Only this document and the corresponding audit adjustment are changed.
