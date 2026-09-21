# Warrior W2 — Charge implemented

Status: **IMPLEMENTED / VALIDATING**

Source freeze: `ZsoltMolnarrr/Rogues` commit `89ba33ad29adc42d7306660f5b74f28bd17b8ffa`.

W2 ports only `martial_spells:charge`. W3 and later Warrior techniques remain locked until the user explicitly passes this checkpoint.

## Frozen behavior implemented

- instant self-cast;
- one spell level, mapped from source tier 3 to Iron's `RARE`;
- zero mana during the fidelity port;
- 10 seconds / 200 ticks duration (user-approved W2 balance override; frozen source was 2 seconds / 40 ticks);
- +50% base Movement Speed using `MULTIPLY_BASE`;
- +50% base Knockback Resistance using `MULTIPLY_BASE`;
- amplifier 0 / SET-style reapplication, so Charge refreshes instead of stacking another copy;
- 12-second base cooldown, using normal Iron's cooldown handling;
- Warrior classification and Warrior/Martial technique tags;
- upstream Charge icon/effect icon;
- upstream `charge_activate.ogg` through a pinned hash-checked asset sync;
- dependency-native one-handed instant-release animation plus custom Martial Spells release VFX: Rage-tinted Charge sign, floating stripes, decelerating sparks, and a 50-particle smoke ring;
- no Improved Charge skill-tree behavior.

## Validation gate

Run from the repository root:

```powershell
git pull --ff-only origin feature/warrior-spells-port
powershell -ExecutionPolicy Bypass -File .\tools\sync-warrior-w2-assets.ps1
python .\tools\audit-warrior-w2.py
.\gradlew clean build
.\gradlew runClient
```

Runtime checks:

1. Confirm Charge appears as a Martial/Warrior technique with the correct icon and no mana cost.
2. Record baseline Movement Speed and Knockback Resistance, then cast Charge.
3. For about 10 seconds, verify Movement Speed and Knockback Resistance each gain +50% of their base value.
4. Recast only after cooldown permits; the effect must remain a single amplifier-0 Charge effect rather than stacking.
5. Verify the activation sound and release particles play.
6. Verify the base cooldown is 12 seconds and normal Cooldown Reduction can reduce it.
7. Confirm no Shattering Throw, Throw Net, Demoralizing Shout, Mortal Strike, or Last Stand Warrior technique is newly available yet.

W2 remains **VALIDATING**, not PASS, until the user explicitly confirms this gate.


## W2 validation revision

After the first runtime test, the user explicitly approved two intentional W2 changes before PASS:

- replace the vanilla CRIT/POOF approximation with dedicated Charge release particles;
- extend Charge from the frozen two-second duration to ten seconds, accepting the balance increase.

All other frozen Charge mechanics remain unchanged.
