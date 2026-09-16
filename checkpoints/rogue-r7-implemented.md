# Rogue R7 — Bear Trap

Status: **IMPLEMENTED / VALIDATING**

Frozen source: `ZsoltMolnarrr/Rogues` commit `89ba33ad29adc42d7306660f5b74f28bd17b8ffa`.

R7 preserves the source contract:

- one-level Rare Rogue Martial technique, zero mana, 15-second base cooldown;
- three traps placed 2 blocks from the caster at 0/120/240 degrees;
- placement begins at 0/3/6 ticks;
- every trap samples its own local collision surface so uneven terrain, slabs and stairs do not inherit caster Y;
- 20-tick spawn, 20-second active lifetime, one trigger, 15-tick normal despawn;
- sprung traps use the source-shaped 30-tick close/hold/despawn sequence;
- 0.6 horizontal trigger radius and source-shaped reduced vertical range;
- damage uses the translated physical dual-melee power and restores target velocity so the trap adds zero knockback;
- Trapped lasts 3 seconds when the source control cap allows it: `100 + 2 x dual-melee power`;
- Trapped suppresses movement input and jumping only. It deliberately does not use global `LivingEntity#isImmobile`, so attacking, item use and Iron's spell casting remain available;
- caster, allies, spectators and invalid/dead targets are excluded;
- exact Rogues spell icon, Bear Trap entity texture, release/impact/spawn/despawn audio are synchronized by `tools/sync-rogue-r7-assets.ps1`;
- exact Spell Engine `dual_handed_ground_release` PlayerAnimator JSON is synchronized as a text presentation asset without adding Spell Engine as a runtime dependency;
- source Spell Engine `magic_spark` impact particles remain dependency-free through the existing vanilla particle translation.

R7 is not PASS until local clean build, runClient, focused runtime tests, presentation checks and regression tests are user-confirmed.

R8 remains locked.
