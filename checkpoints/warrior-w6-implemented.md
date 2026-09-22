# Warrior W6 — Mortal Strike implemented

Status: **PASS / FROZEN**

Rogues source freeze: `89ba33ad29adc42d7306660f5b74f28bd17b8ffa`.

Exact Spell Engine dependency freeze: `1.10.5.034+1.20.1`, commit `270a6d61b00f241c1c8adb87573ad6de2d547f66`.

W0-W6 are frozen as PASS. W6 adds only Mortal Strike plus the local exact Bleed behavior needed by that source spell. W7 Last Stand is now unlocked; W8 remains locked.

## Frozen Mortal Strike behavior

- one-level EPIC Martial/Warrior technique, zero mana;
- 10-tick / 0.5-second STANDARD-style cast translated to Iron's LONG cast so early release cancels;
- source windup animation during cast and vertical slash animation at completion;
- 3-block 1.20.1 melee reach;
- hitbox: length factor 1.0, width factor 0.5, height factor 1.5, arc 120 degrees;
- delayed contact at 30% of the caster's current vanilla melee attack cycle;
- normal vanilla player weapon attack, fully charged for each selected target;
- temporary +50% `MULTIPLY_TOTAL` Attack Damage for the skill contact;
- target iframe bypass/restore identical in shape to the accepted Mutilate translation;
- exact start / whoosh / impact sounds, with source impact-sound cap 3;
- 15-second base cooldown.

## Exact Spell Engine 1.10.5.034 Bleed

The previously-open W0 risk was resolved against commit `270a6d61b00f241c1c8adb87573ad6de2d547f66`.

Exact source constants:

```text
tick interval:            25 ticks
base damage per stack:    1.0
stationary multiplier:    0.5
maximum multiplier:       2.0
movement cap:             2 blocks/second horizontal
lethal:                   yes
damage source:            vanilla magic
```

For a zero-based effect amplifier `A`, effective stacks are `A + 1` and each damage tick is:

```text
speed = horizontal blocks/tick * 20
fraction = min(speed, 2.0) / 2.0
multiplier = lerp(fraction, 0.5, 2.0)
damage = (A + 1) * multiplier
```

Mortal Strike applies Bleed for 120 ticks / 6 seconds with source SET/default-refresh semantics:

```text
amplifier = 1 + floor(0.25 * physicalMeleePowerAtImpact)
```

Spell Engine keeps Mortal Strike's temporary +50% Attack Damage modifier installed until after melee impacts execute. Because PHYSICAL_MELEE reads live generic Attack Damage, the amplifier intentionally uses the **boosted** impact Attack Damage.

Persistent Bleed visuals use the existing dependency-free local dripping-blood particle appearance already frozen for W5, but with the exact .034 Bleed cadence: every 5 entity ticks, `amplifier + 1` particles at 0.1-0.3 speed. Mortal Strike application emits the source 40-particle 0.2-0.4 impact burst.

## Frozen assets

- Mortal Strike icon: `d3f35879c2cbeb1c3d78f5d465d510dca39db4d2`
- Bleed icon: `7384b4e1ea32644b5a3cc05907e402355c25bb8c`
- `mortal_strike_swing.ogg`: `450936a836623b4215680c8c76c7c54d43df975d`
- `mortal_strike_whoosh.ogg`: `52f05d483959c9c856246e4ee610359fad1a56b7`
- `mortal_strike_impact.ogg`: `1b89880a1d631872a25ee2fc539d9fd6d92234b0`
- windup animation: `68fb82550e479c3a52b1d0c3a1db86b0323e8c38`
- slash animation: `f15ceccfdafb393eea6938bedac61249d047985c`

## Validation gate

```powershell
git switch feature/warrior-spells-port
git pull --ff-only origin feature/warrior-spells-port

python .\tools\audit-warrior-w6.py

.\gradlew clean build
.\gradlew runClient
```

Runtime checks:

1. W2-W5 remain unchanged.
2. Mortal Strike appears as one-level EPIC Martial/Warrior, zero mana, 15-second base cooldown.
3. Begin the 0.5-second windup and release the use key early: the cast should cancel rather than attack.
4. Complete the cast: exact vertical windup should transition to the vertical slash; swing sound starts with casting, whoosh starts the melee delivery.
5. With a normal weapon, a target inside 3 blocks / 120-degree forward arc is hit after 30% of the current attack cycle. Targets outside the vertical hitbox/arc or behind terrain are not hit.
6. Compare an ordinary fully charged vanilla hit with Mortal Strike using the same weapon. Mortal Strike should route normal weapon behavior but with +50% total Attack Damage for the skill contact.
7. Multiple valid targets in the source hitbox may be struck; impact sound is capped at the first three targets.
8. Living targets receive 6 seconds of Bleed. Reapplication uses normal SET behavior and refresh semantics rather than ADD stacking.
9. Verify Bleed lethality and movement scaling with controlled targets: stationary victims take the low end; fast-moving victims bleed harder.
10. Blood presentation: 40-particle burst on Mortal Strike Bleed application plus recurring drips while Bleed remains.
11. Last Stand remains absent.

W6 is **PASS / FROZEN**. The user confirmed runtime behavior. Before freeze, the animation sequence was re-verified: Rogues Mortal Strike uses `spell_engine:two_handed_slash_vertical_windup` during the 0.5-second cast and `spell_engine:two_handed_slash_vertical_slash` for melee release; the committed Martial Spells animation blobs exactly match Spell Engine 1.10.5.034 upstream hashes.
