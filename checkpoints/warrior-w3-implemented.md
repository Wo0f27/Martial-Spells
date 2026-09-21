# Warrior W3 — Demoralizing Shout implemented

Status: **IMPLEMENTED / VALIDATING**

Source freeze: `ZsoltMolnarrr/Rogues` commit `89ba33ad29adc42d7306660f5b74f28bd17b8ffa`.

W2 Charge is frozen as PASS. W3 adds only `martial_spells:demoralizing_shout`; W4 and later Warrior techniques remain locked.

## Frozen behavior implemented

- instant, self-centered hostile area;
- one spell level, source tier 3 mapped to Iron's `RARE`;
- zero mana;
- 12-block range with 0.5 vertical-range multiplier;
- obstacle-aware line of sight matching the established Martial Spells area-target translation;
- Demoralized duration: 8 seconds / 160 ticks;
- -20% base Attack Damage per effect level using `MULTIPLY_BASE`;
- exact source ADD behavior: first valid application is amplifier 0, later applications add one amplifier, source cap 5 means maximum amplifier 5 / Demoralized VI;
- debuff health gate: `50 + 2 * current Attack Damage`;
- direct damage: `0.05 * current Attack Damage`;
- damage applies independently of the debuff health gate;
- zero added knockback by restoring the target's pre-impact velocity after successful damage;
- 12-second base cooldown with normal Iron's Cooldown Reduction;
- exact frozen spell/effect icon, `shout_release.ogg`, and `demoralize_impact.ogg`;
- locally translated Rage-smoke release rings and target impact burst;
- no Spell Engine or Spell Power runtime dependency.

## Source amplifier-cap note

Spell Engine stores status-effect amplifiers zero-based. Frozen Rogues writes:

`amplifier = 1, amplifier_cap = 5, apply_mode = ADD`.

The frozen ADD implementation makes the first application amplifier 0, then increments by 1 until amplifier 5. Therefore the exact source maximum is **Demoralized VI / six effective -20% levels**, not five effective levels. W3 preserves the source implementation rather than silently changing that cap.

## Validation gate

Run:

```powershell
git pull --ff-only origin feature/warrior-spells-port
python .\tools\audit-warrior-w3.py
.\gradlew clean build
.\gradlew runClient
```

Runtime checks:

1. Charge remains unchanged from W2 PASS.
2. Demoralizing Shout appears as a one-level Rare Martial/Warrior technique with zero mana.
3. Cast near non-allied living targets inside 12 blocks; targets outside range, behind solid obstruction, or allied to the caster are not affected.
4. A target below the control-health cap receives Demoralized I for about 8 seconds and its base Attack Damage modifier is -20%.
5. Repeated casts after cooldown increase the effect one level at a time and refresh the 8-second duration; source maximum is Demoralized VI.
6. A target above the health gate receives the small direct damage but not Demoralized.
7. Direct damage equals about 5% of current Attack Damage before armor mitigation and adds no knockback.
8. Verify frozen release/impact sounds and Rage-smoke VFX.
9. Base cooldown is 12 seconds before Cooldown Reduction.
10. Throw Net, Shattering Throw, Mortal Strike, and Last Stand remain absent.

W3 remains **VALIDATING**, not PASS, until the user explicitly confirms the gate.
