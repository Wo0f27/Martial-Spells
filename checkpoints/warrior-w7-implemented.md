# Warrior W7 — Last Stand implemented

Status: **PASS / FROZEN**

Rogues source freeze: `89ba33ad29adc42d7306660f5b74f28bd17b8ffa`.

Exact Spell Engine dependency freeze: `1.10.5.034+1.20.1`, commit `270a6d61b00f241c1c8adb87573ad6de2d547f66`.

W0-W7 are frozen as PASS. W7 adds only Last Stand and the local presentation/runtime support required to reproduce it. W8 final fidelity audit is now unlocked.

## Exact channel timing

Spell Engine .034 constructs a five-tick channel over 50 cast ticks with interval 10 and half-interval offset -5. Therefore the authoritative channel impacts occur at:

```text
5, 15, 25, 35, 45
```

Release is at tick 50.

Iron's 1.20.1 native CONTINUOUS scheduler does not match this: a 50-tick continuous spell begins its 10-tick cadence too early and can settle around tick 41. W7 therefore uses Iron's LONG cast state as transport and schedules the exact source impacts from `onServerCastTick`. A still-held cast is explicitly completed at tick 50 so the channel cannot stretch beyond 2.5 sec.

## Stack/effect behavior

Each channel impact applies ADD semantics for 10 seconds:

```text
no current Last Stand -> amplifier 0
next pulse            -> amplifier 1
next                  -> amplifier 2
next                  -> amplifier 3
next                  -> amplifier 4 (Last Stand V)
cap                    -> amplifier 4
```

Each effective stack grants:

- +20% base Max Health, MULTIPLY_BASE;
- +20% base Knockback Resistance, MULTIPLY_BASE.

A stale comment in `RogueEffects.java` says each stack also gives -10% damage taken. The frozen executable `EffectConfig` does not register that modifier, and a later source-history check still shows no damage-taken modifier. W7 follows executable behavior and does **not** invent damage reduction.

## Healing

Spell source uses:

```text
heal coefficient = 0.2 * HEALTH power
HEALTH power = current Max Health
channel output multiplier = 10 ticks / 20 = 0.5
effective pulse heal = 0.1 * current Max Health
```

Impact ordering is status effect first, heal second. Therefore each pulse heals 10% of the newly increased **post-stack** maximum health. A full channel from an unstacked state on an entity with base max health H nominally heals:

```text
0.1*(1.2H) + 0.1*(1.4H) + 0.1*(1.6H) + 0.1*(1.8H) + 0.1*(2.0H)
= 0.8H
```

subject to ordinary health capping at the current maximum. The local translation is deterministic because Martial Spells intentionally does not import Spell Power's critical-heal subsystem.

## Proportional cooldown

Full base cooldown is 60 sec and normal Iron's Cooldown Reduction remains available.

On early release:

```text
progress = clamp(elapsedTicks / 50, 0, 1)
cooldown = round(effectiveFullIronCooldown * progress)
```

Already-earned stacks and healing remain.

## Frozen assets

- Last Stand spell/effect icon: `5f6e39441c5368b379731ad086db332079223b18`
- `last_stand_start.ogg`: `f6ce90b0a0c89e66cc60e74bedc64b04d03871cf`
- `last_stand_casting.ogg`: `ff5dafe52ce8e3f22a867238f8d4bf1e6fd7dbac`
- `last_stand_release.ogg`: `19f4d8deead19c22eff36454e5f3c2e8cf1176d1`
- ground-charge animation: `7294d126bf932f843e82f3864f5a5b5961f35839`
- shout-release animation: `292e820254a1088297ad518b1deb49f612b9739d`
- smoke_medium frames are copied exactly from Spell Engine .034.
- area_effect_700's 22 frames are copied exactly from Spell Engine .034.

## Runtime validation gate

```powershell
git switch feature/warrior-spells-port
git pull --ff-only origin feature/warrior-spells-port

python .\tools\audit-warrior-w7.py

.\gradlew clean build
.\gradlew runClient
```

Validate:

1. W2-W6 still behave as frozen.
2. Last Stand appears as one-level EPIC Martial/Warrior, zero mana, 60-second full base cooldown.
3. Start channeling: movement input should be rooted while incoming knockback remains possible.
4. The exact ground-charge animation, start audio, sustained casting audio, inward cyan-blue sparks, and cyan-blue smoke should appear during the channel.
5. Watch effect levels/timing: pulses should land at ~0.25, 0.75, 1.25, 1.75 and 2.25 sec, reaching Last Stand V on a full channel.
6. Max Health should rise by +20% base per effective stack. No extra damage-reduction mechanic should appear.
7. Healing should occur after each stack increases Max Health, so later pulses can heal more than earlier pulses.
8. Release early after one/two/etc pulses: earned stacks/healing remain, release animation/sound plays, and cooldown is proportionally shorter.
9. Hold continuously: cast must end at ~2.5 sec and receive the full effective 60-second base cooldown.
10. Last Stand lasts 10 sec from the latest pulse and emits the blue ground aura approximately once per second.
11. Recasting after cooldown follows ADD/cap/refresh semantics without exceeding amplifier 4.
12. W8/final changes remain absent.

W7 is **PASS / FROZEN**. The user confirmed the runtime gate works as intended. The full-channel health result was specifically verified: from a 20/20 baseline, five post-stack heals total 16 HP while max health rises to 40, so the source-faithful endpoint is 36/40 HP rather than a full refill.
