# Warrior W2 — Charge implemented

Status: **PASS**

Source freeze: `ZsoltMolnarrr/Rogues` commit `89ba33ad29adc42d7306660f5b74f28bd17b8ffa`.

W2 ports only `martial_spells:charge`. The user accepted the final no-overhead-sign revision and explicitly instructed the project to proceed to W3.

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
- upstream `charge_activate.ogg` committed directly into the repository and pinned to its frozen Git blob hash;
- dependency-native one-handed instant-release animation plus custom Martial Spells release VFX: floating Rage-tinted stripes, decelerating sparks, and a 50-particle smoke ring; the source above-head speed sign is intentionally omitted;
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

W2 is **PASS** and frozen. W3 may proceed without changing accepted Charge behavior.


## W2 validation revision

After the first runtime test, the user explicitly approved two intentional W2 changes before PASS:

- replace the vanilla CRIT/POOF approximation with dedicated Charge release particles;
- extend Charge from the frozen two-second duration to ten seconds, accepting the balance increase.

All other frozen Charge mechanics remain unchanged.


## Bundled sound revision

The exact frozen `charge_activate.ogg` is now tracked directly in Martial Spells. The W2 asset script no longer downloads from `raw.githubusercontent.com`; it only verifies the bundled file's Git blob SHA, so validation does not depend on GitHub Raw being reachable from the local network.


## Above-head sign revision

After validating the revised VFX, the user preferred that Charge show no icon/sign above the character. The custom speed-sign particle, provider, definition, and texture were therefore removed. Charge now keeps only the Rage-tinted streaks, sparks, smoke ring, activation sound, and buff presentation.
