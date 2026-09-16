# Rogue R7 — Bear Trap PASS

Status: **PASS / FROZEN**

User-confirmed runtime validation: **2026-09-16**.

Frozen source: `ZsoltMolnarrr/Rogues` commit `89ba33ad29adc42d7306660f5b74f28bd17b8ffa`.

Validated R7 baseline:

- one-level Rare Rogue Martial technique, zero mana, 15-second base cooldown;
- three traps placed 2 blocks from the caster at 0/120/240 degrees with 0/3/6-tick deployment delays;
- each trap samples its own local collision surface for uneven terrain, slabs and stairs;
- 20-tick spawn, 20-second active lifetime, one trigger per trap, 15-tick normal despawn, and 30-tick sprung close/hold/despawn sequence;
- 0.6 horizontal trigger radius with source-shaped reduced vertical reach;
- translated physical dual-melee damage with explicit target-velocity restoration for zero added knockback;
- 3-second Trapped effect when the source control cap permits it: `100 + 2 x dual-melee power`;
- Trapped blocks walking and jumping while preserving attacks, item use and Iron's spell casting;
- caster, allies, spectators and invalid/dead targets are excluded;
- exact frozen Rogues Bear Trap spell icon, entity texture, release/impact/spawn/despawn sounds, and the exact Spell Engine `dual_handed_ground_release` PlayerAnimator animation are retained without adding Spell Engine as a runtime dependency;
- Bear Trap name, description, guide text, Trapped effect localization, tooltip values and death messages are present;
- Iron's spell UI uses the exact frozen Bear Trap icon at `textures/gui/spell_icons/bear_trap.png`;
- cumulative R2-R7 static audit passes after the accepted R7 corrections;
- local Forge 1.20.1 / Java 17 build and client runtime behavior were user-confirmed before PASS.

R7 is frozen. Do not alter its behavior during R8 except to fix a demonstrated regression or final-audit defect.

R8 — Fidelity / final audit is now unlocked.
