# Warrior W5 — Shattering Throw implemented

Status: **PASS / FROZEN**

Source freeze: `ZsoltMolnarrr/Rogues` commit `89ba33ad29adc42d7306660f5b74f28bd17b8ffa`.

W0-W5 are frozen as PASS. W5 adds only `martial_spells:shattering_throw` plus its projectile, Shattered Armor effect, and local blood-particle translation. W6 Mortal Strike is now unlocked; W7+ remain locked.

## Frozen behavior implemented

- source tier 2 mapped to Iron's `UNCOMMON`, one spell level, Martial/Warrior, zero mana;
- 10-tick / 0.5-second charged cast;
- Spell Engine default minimum release ratio 0.2;
- Spell Engine default charge `output_scaling = 1.0`, so damage/knockback scale directly with charge ratio;
- charge-scaled range `12 + 12 * chargeRatio` (14.4 minimum valid, 24 full);
- projectile velocity 0.8, no gravity/drag, 1200-tick safety cap;
- release-time AIM target with 2 degrees/tick homing; AIM is intentionally non-sticky during the charge;
- exactly one source-style block bounce using face-normal vector reflection and speed preservation;
- held-item projectile visuals capture only the main-hand item registry ID and render that item type's default model; the actual stack stays in the player's hand/inventory;
- `ALONG_MOTION` held-item orientation + `FIXED` item display transform + -36 degrees/tick spin;
- source near-camera projectile-render guard for the first two ticks;
- full-charge direct damage `1.0 * current Attack Damage`;
- source default direct-damage knockback `0.4 * chargeRatio`;
- source default direct-damage iframe bypass is preserved while restoring the target's prior iframe timer after impact;
- Shattered Armor lasts 8 seconds / 160 ticks;
- Shattered Armor health gate `100 + 2 * current Attack Damage`;
- Shattered Armor is amplifier 0 / SET semantics and reduces base Armor by 30% using `MULTIPLY_BASE`;
- direct damage, knockback, and `throw_impact` remain independent of the Shatter health gate;
- impact Shatter VFX emits 10 dark-red dripping-blood particles at 0.05-0.3 initial speed;
- active Shattered Armor emits one dark-red dripping-blood particle per tick at amplifier 0, scaled by `amplifier + 1` for parity with the frozen spawner;
- source `dripping_blood` appearance is translated locally: `minecraft:drip_hang`, #590000, scale 0.11 +/-33%, DRIFT, gravity 0.8, collision, lifetime 20 ticks;
- release and every-8-tick travel use the already-frozen W4 `throw.ogg`;
- exact frozen `throw_impact.ogg`, spell icon, and Shatter effect icon are committed directly;
- 8-second base cooldown with normal Iron's Cooldown Reduction;
- no Spell Engine or Spell Power runtime dependency.

## Shared charged-release infrastructure

W5 reuses W4's already-accepted `ReleaseChargedTechnique` / Iron's `Utils.releaseUsingHelper` bridge. No behavior in the bridge was changed for W5.

Under-minimum releases still fall through to Iron's normal early-cancel path. Valid partial releases execute through Iron's normal `castSpell` path, preserving cooldown/mana/event handling.

## Frozen asset hashes

- spell icon: `d43c529e6ba3b6970be7b68809c38a4ccab0ca0f`
- Shattered Armor effect icon: `d43c529e6ba3b6970be7b68809c38a4ccab0ca0f`
- `throw.ogg` reused from W4: `ce5a789e74441a01f63624a608f8ba3ca1987a2d`
- `throw_impact.ogg`: `4266dde3a8b9682b4dfd46342d5630534ab4a965`

## Validation gate

Run:

```powershell
git switch feature/warrior-spells-port
git pull --ff-only origin feature/warrior-spells-port
python .\tools\audit-warrior-w5.py
.\gradlew clean build
.\gradlew runClient
```

Runtime checks:

1. W2 Charge, W3 Demoralizing Shout, and W4 Throw Net still behave exactly as their frozen checkpoints.
2. Shattering Throw appears as one-level Uncommon Martial/Warrior, zero mana, 8-second base cooldown.
3. Release before 20% (~2 ticks): fizzle, no projectile, no Shattering Throw cooldown.
4. Valid partial release: projectile launches; near-minimum release is ~20% damage and ~14.4 blocks of range. Full 10-tick charge reaches 24 blocks and 100% Attack-Damage output.
5. Hold a visible weapon/tool in the main hand. The projectile must visually be that item type while the real stack remains in the player's hand/inventory. Changing or dropping the real item after launch must not change the projectile's captured item type.
6. Verify ALONG_MOTION facing, rapid -36 degree/tick spin, velocity 0.8, subtle 2 degree/tick homing toward the target aimed at when released, and the source near-camera guard.
7. Fire at a wall at an angle. The first block contact must reflect/bounce once without losing speed; a second block contact must terminate the projectile.
8. Hit a normal eligible mob. It should take armor-respecting direct damage/knockback and receive 8 seconds of Shattered Armor. Its base Armor is reduced by 30%, with dark-red blood impact/drip particles.
9. Hit a target whose max health exceeds `100 + 2 * current Attack Damage`. It should still take direct damage/knockback and play `throw_impact`, but must not receive Shattered Armor or its blood application burst.
10. Reapply Shattered Armor: amplifier remains 0 while duration refreshes.
11. Verify `throw.ogg` at release/travel and `throw_impact.ogg` on entity impact.
12. Mortal Strike and Last Stand remain absent.

W5 is **PASS / FROZEN**. The user confirmed the runtime gate works. The relatively noticeable blood drips seen on an Iron Golem are accepted as non-blocking/source-consistent presentation and do not require a W5 code change.
