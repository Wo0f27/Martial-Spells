# Warrior W4 — Throw Net implemented

Status: **IMPLEMENTED / VALIDATING**

Source freeze: `ZsoltMolnarrr/Rogues` commit `89ba33ad29adc42d7306660f5b74f28bd17b8ffa`.

W3 Demoralizing Shout is frozen as PASS. W4 adds only `martial_spells:throw_net` plus the minimal shared charged-release bridge required by the source. W5 and later Warrior techniques remain locked.

## Frozen behavior implemented

- source tier 2 mapped to Iron's `UNCOMMON`, one spell level, Martial/Warrior, zero mana;
- 9-tick / 0.45-second charged cast;
- minimum release ratio 0.2;
- releases below 20% use Iron's normal cancel path: no projectile and no W4 cooldown;
- source linear charge output with `output_scaling = 0.5`, giving `0.5 + 0.5 * chargeRatio`;
- projectile flight range `10 + 12 * chargeRatio` (12.4 minimum valid, 22 full);
- constant velocity 1.0, no gravity/drag, 1200-tick source safety cap;
- source default homing angle 1 degree/tick toward the AIM target captured at cast start;
- source custom projectile model and texture, spinning 12 degrees/tick through Forge 1.20.1's plain-`ResourceLocation` additional-model path;
- travel sound every 8 ticks;
- direct damage `0.1 * current Attack Damage * chargeOutput`;
- source knockback coefficient `0.1 * chargeOutput`, applied against vanilla's 0.4 damage-knockback baseline;
- Netted duration 3 seconds / 60 ticks;
- Netted health gate `100 + 2 * current Attack Damage`;
- damage still applies when a target is above the Netted health gate;
- Netted blocks locomotion and jumping without silencing attacks, item use, or spell casting;
- Netted cancels knockback while active, matching frozen Spell Engine `KnockbackImmunity`;
- exact frozen icons and four sound files committed directly;
- exact frozen `spell_effect/net_trap` model and texture render persistently while Netted is active;
- Netted visual reproduces the source one-shot model FX: starts at zero scale 1.1 blocks high, drops 0.6 over 6 ticks with `EASE_IN_QUAD`, snaps taut over 8 ticks with `EASE_OUT_BACK`, then holds until the effect expires;
- Netted model entity scaling preserves the source WIDTH baseline 0.5 / square-root curve, clamped 0.5x–3x;
- 12-second base cooldown with normal Iron's Cooldown Reduction;
- no Spell Engine or Spell Power runtime dependency.

## Charged-release bridge

Iron's 1.20.1 `CastType.LONG` normally cancels when the use key is released before completion. Frozen Rogues Throw Net instead resolves a charge release at any ratio >= 0.2.

W4 introduces `ReleaseChargedTechnique` and a mixin at Iron's `Utils.releaseUsingHelper`. The mixin intercepts only a currently-cast spell that explicitly implements that interface. Under-minimum releases fall through to Iron's original cancellation path unchanged. Valid releases execute the spell through Iron's normal `castSpell` cooldown/mana/event path and then complete the cast normally.

This bridge is intentionally narrow. It does not change ordinary Iron's spells and is the only shared infrastructure W5 Shattering Throw is expected to reuse.

## Frozen asset hashes

- spell icon: `6bffb24937a7aeddb959f05994bd846f21f8164e`
- effect icon: `6bffb24937a7aeddb959f05994bd846f21f8164e`
- projectile texture: `57dcfd0337d544ae2f51459b6d7dbb5b3a82e815`
- Netted model texture: `4b18d0b08bb71156f6c1424c4cac6ae075b6619a`
- `net_casting.ogg`: `5495c850ccfd2ab16becefb4831eccfc3e3834d0`
- `throw.ogg`: `ce5a789e74441a01f63624a608f8ba3ca1987a2d`
- `net_travel.ogg`: `304fb9c0eae54244c1d05b1252241f3ba84720d1`
- `net_impact.ogg`: `d0a9568906788c50def55cdaeb9a73bd51b6466d`

The projectile and Netted model JSON files necessarily have different blob hashes because their texture identifiers are translated from `rogues:` to `martial_spells:`; geometry/UV data are otherwise retained.

### Visual repair after first runtime pass

The first W4 runtime pass confirmed gameplay/root behavior but exposed two presentation defects: the projectile rendered with Minecraft's purple/black missing-texture appearance, and Netted had only ordinary potion particles.

The root cause of the texture failure is part of Spell Engine's 1.20.1 resource contract: its `assets/minecraft/atlases/blocks.json` explicitly adds the custom `spell_projectile` and `spell_effect` texture directories to Minecraft's block atlas. W4 now reproduces those two directory sources locally, so the copied frozen textures are actually stitched.

Forge 47's own `ModelBakery` additional-model patch stores these models under plain `ResourceLocation` keys, so W4 deliberately uses the same plain IDs rather than newer NeoForge `#standalone` semantics. The repair also mirrors Spell Engine's `TOWARDS_MOTION` projectile orientation and raw-model `-0.5/-0.5/-0.5` centering, and ports the frozen persistent Netted model-FX animation. A model-bake diagnostic logs an explicit error if either model still resolves to Minecraft's missing model.

## Validation gate

Run:

```powershell
git switch feature/warrior-spells-port
git pull --ff-only origin feature/warrior-spells-port
python .\tools\audit-warrior-w4.py
.\gradlew clean build
.\gradlew runClient
```

Runtime checks:

1. Charge and Demoralizing Shout remain unchanged from their frozen checkpoints.
2. Throw Net appears as a one-level Uncommon Martial/Warrior technique with zero mana.
3. Begin charging and release almost immediately, before 20% (~2 ticks): the cast should fizzle with no net projectile and no Throw Net cooldown.
4. Release at/above 20%: a net projectile should launch. Near-minimum release has ~12.4-block flight range and ~60% output; full 9-tick charge reaches 22 blocks and 100% output.
5. Verify the net projectile now uses the textured frozen model rather than a black/purple missing-texture cube, faces its travel direction, spins 12 degrees/tick, plays the source travel sound cadence, and subtly homes 1 degree/tick toward a target captured under the crosshair at cast start.
6. A low-health eligible target receives 3 seconds of Netted. The frozen net-trap model should drop onto it and snap taut over roughly the first 8 ticks, then remain visible for the rest of Netted. The target cannot locomote or jump and cannot be knocked back, but it can still attack, use items, and cast.
7. A target above `100 + 2 * current Attack Damage` receives direct damage/knockback but not Netted or the Netted impact sound.
8. Full-charge damage is approximately 10% of current Attack Damage before armor mitigation; partial charge scales it by `0.5 + 0.5 * chargeRatio`.
9. Projectile collision with a block removes the projectile; flight expires at its charge-scaled range.
10. Base cooldown is 12 seconds before Cooldown Reduction.
11. Shattering Throw, Mortal Strike, and Last Stand remain absent.

W4 remains **VALIDATING**, not PASS, until the user explicitly confirms this gate.
