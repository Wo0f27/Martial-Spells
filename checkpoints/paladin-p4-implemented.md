# CP11 P4 — Paladin/Priest Constructs and Summons

Status: **IMPLEMENTED / VALIDATING**

Frozen upstream:
- repository: `ZsoltMolnarrr/Paladins`
- commit: `2807417a1dd9a65204c002ded487da0e6ae467a1`
- lineage: `1.20.1-modern`

Target:
- Minecraft Forge 1.20.1
- Iron's Spells 'n Spellbooks 1.20.1-3.16.3
- school: `irons_spellbooks:holy`
- no Spell Engine or Spell Power runtime dependency

P0-P3 remain PASS. P5 stays locked until P4 receives local build and runtime PASS.

## Frozen P4 contract

### Barrier
- Legendary Holy spell.
- 0.5-second cast.
- 4-block source range.
- 40-second base cooldown.
- Spawns an 8x4 barrier volume centered on the caster for 10 seconds.
- Every 4 ticks, friendly/protected living entities inside receive a 5-tick
  protection refresh.
- Frozen protected damage classes:
  - player attack,
  - explosion,
  - common/Forge magic,
  - Iron's school-tagged magic.
- Hostile living intruders inside the volume are pushed outward at source
  strength 0.30.
- Friendly projectiles may pass through the Barrier; hostile projectiles
  collide/are stopped.
- Barrier cannot be pushed or destroyed early by ordinary damage.
- Source activate/idle/impact/deactivate sounds are retained.
- Presentation is the source-style golden segmented dome with the final-second
  expiration pulse.

### Battle Banner
- Legendary Holy spell.
- Instant cast.
- 45-second base cooldown.
- Ground-snapped placement 2 blocks ahead of caster, source yaw offset +20°.
- Spawn phase: 43 ticks.
- Active phase: 10 seconds / 200 ticks.
- Despawn phase: 43 ticks.
- Aura refresh every 10 ticks.
- Aura effect duration: 2 seconds.
- Base radius 3 blocks.
- Extra radius: `1 * min(4, source-equivalent Holy power)`, matching Spell
  Engine's executable `combinedRadius` arithmetic.
- Spell Power 1.20.1 uses 1.0 as default Healing Power while Iron's uses the
  CP11 reference power 5.0, so P4 source-unit formulas normalize with
  `Iron Holy power / 5`. Baseline Banner radius is therefore 4 blocks rather
  than incorrectly starting at the 7-block cap.
- Vertical range is 30% of resolved radius.
- Friendly targets receive:
  - +40% base Attack Speed,
  - +40% base Knockback Resistance,
  - +40% Iron's cast-time haste translation,
  - +40% Iron's cooldown-reduction haste translation,
  - +40% RangedWeaponAPI draw haste.
- The Forge RangedWeaponAPI port ultimately maps ranged haste to Apothic
  Attributes `DRAW_SPEED`; Battle Banner therefore adds its own +40%
  MULTIPLY_BASE draw-speed modifier. This stacks independently instead of
  competing with an existing RangedWeaponAPI Haste status effect.
- Source banner geometry/texture and fullbright presentation are restored.
  The exact upstream Blockbench keyframe clips remain final presentation polish;
  P4 uses the source geometry with lifecycle scaling and a chained flag wave.

### Lightwell
- Legendary Holy spell.
- Instant cast.
- Spell cooldown is fixed at 45 seconds and ignores generic Iron's cooldown
  reduction, preserving the source summon-uptime rule.
- Ground-snapped placement 1.5 blocks ahead, facing with the caster.
- Spawn phase: 20 ticks.
- Active phase: 12 seconds / 240 ticks.
- Despawn phase: 20 ticks.
- Stationary, no gravity, no collision, not pushable, untargetable and
  invulnerable.
- Only wounded friendly targets within 12 blocks are eligible.
- Target is reacquired after each mote launch; the translation chooses the
  nearest currently wounded friendly each time.
- Translated well healing power:
  `1.0 + 0.5 * owner source-equivalent Holy power`, where source-equivalent
  power is `Iron Holy power / 5`. At the unmodified CP11 reference this is
  1.5 source power and each Mote heals 0.525 before external heal modifiers.
- Internal Holy Mote cadence starts at 30 ticks / 1.5 seconds and is modified
  by the owner's Iron's cooldown-reduction attribute as the target-system
  analogue of source Healing Haste.
- Source spawn/ambient/despawn audio, spawn burst, and active-only existence
  particles are retained.
- Source base geometry, base texture, glow texture, fullbright emissive layer,
  and 40-tick floating bob are restored. Exact upstream model keyframe clips
  remain final presentation polish.

### Holy Mote (`lightwell_orb`)
- Internal helper spell identity only; not intended for player binding.
- Common/tier-0 translated identity, Holy school.
- Range/travel cap: 12 blocks.
- Projectile speed: 1.0 blocks/tick.
- Executable launch pitch offset: +30° upward. The executable JSON value wins
  over the broader source comment.
- No gravity: the apparent arc comes from the upward launch and delayed homing.
- Homing cap: 16°/tick.
- Homing begins after traveling 15% of the initial distance to the selected
  target, matching Spell Engine's executable relative-distance rule.
- One terrain bounce.
- Friendly living targets only; irrelevant hostile entities are passed over.
- Healing: `0.35 * translated Lightwell healing power`.
- Source Lightwell-orb model, fullbright glow, five-particle Holy trail, heal
  pillar and Holy glimmer are retained.
- Spell Engine's generic healing release sound is translated to Iron's native
  Holy cast sound; the heal impact uses the already-synced Paladins Holy heal
  sound rather than adding Spell Engine audio as a dependency.

## Local validation

Run:

```powershell
git pull --ff-only origin cp11/paladin-priest-spells
powershell -ExecutionPolicy Bypass -File .\tools\sync-paladin-p4-assets.ps1
python .\tools\audit-paladin-p4.py
.\gradlew clean build
```

Only after audit and build pass, launch the client.

### Runtime gate

1. P1-P3 regression: previously passed spells still cast and render normally.
2. Barrier:
   - visible golden segmented dome appears for roughly 10 seconds;
   - no missing texture;
   - activate/idle/deactivate sounds are audible;
   - caster/allies inside are protected from player attacks, explosions and
     Iron's magic;
   - ordinary unprotected damage classes still behave normally;
   - hostile mobs entering the volume are pushed outward;
   - friendly arrows/projectiles can pass;
   - hostile arrows/projectiles are stopped;
   - impact feedback occurs without destroying the Barrier;
   - final second visibly pulses/fades and the entity disappears cleanly.
3. Battle Banner:
   - appears about 2 blocks ahead, ground-snapped and rotated relative to caster;
   - placement phase grows in, active phase lasts about 10 seconds, then it
     scales away;
   - source texture/model renders without missing texture or z-fighting severe
     enough to obscure the flag;
   - Holy presence particles and presence audio occur while active;
   - allies inside gain Battle Banner and keep it refreshed;
   - leaving the aura lets the 2-second buff expire;
   - Attack Speed and Knockback Resistance increase;
   - Iron's cast/cooldown haste increases while buffed;
   - RangedWeaponAPI bow/crossbow draw haste is visibly faster;
   - higher Holy power expands radius up to the source +4 block cap.
4. Lightwell:
   - appears 1.5 blocks ahead and remains stationary;
   - one-second spawn phase, roughly 12-second active phase, one-second despawn;
   - base + emissive glow render with gentle floating bob;
   - cannot be damaged, pushed or selected as a normal combat target;
   - does not fire at full-health allies;
   - chooses wounded friendly targets within 12 blocks;
   - repeatedly redistributes healing instead of permanently fixating on one
     already-healed ally;
   - ambient particles occur only during ACTIVE phase;
   - outer Lightwell cooldown stays exactly 45 seconds even with cooldown
     reduction equipment.
5. Holy Mote:
   - visible Lightwell-orb model launches upward from the well;
   - starts homing only after the relative-distance delay rather than snapping
     immediately;
   - turns smoothly toward the selected ally at a 16°/tick cap;
   - can visibly bounce once from terrain and continue;
   - terminates at about 12 blocks of total travel if it misses;
   - passes irrelevant hostile entities;
   - on a friendly hit, heals once, produces healing pillar/glimmer, and
     disappears;
   - no black/purple missing-model cube;
   - high Iron's cooldown reduction makes the Lightwell fire motes faster
     without shortening the Lightwell spell's fixed 45-second outer cooldown.
6. Persistence/cleanup:
   - save/reload during Barrier, Banner, or Lightwell lifetime does not crash;
   - temporary entities eventually despawn and do not accumulate indefinitely;
   - no renderer/model-bake errors appear in `latest.log`.

P4 remains **VALIDATING**, not PASS, until the user confirms this runtime gate.
