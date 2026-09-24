# CP11-P2 implemented — Retribution and protection core

Parent: JUS-178
Checkpoint: JUS-191
Branch: cp11/paladin-priest-spells

Implemented:
- martial_spells:blessed_strikes
- martial_spells:divine_protection
- martial_spells:judgement
- martial_spells:immolation

Frozen behavior translated:
- Blessed Strikes: 2.5-second channel, five seals at 0.5-second intervals, 15-second seal lifetime, six-seal hard cap, one seal consumed next tick per melee swing, 0.5 hybrid bonus-damage coefficient and 0.5 knockback.
- Divine Protection: instant, 8-second duration, 30-second cooldown, 1-3 fully negated incoming attacks from floor(0.5 x effective Holy multiplier) capped at amplifier 2.
- Judgement: required hostile target within 16 blocks, 0.5-second cast, 12-block / 1.2-block-per-tick ten-tick meteor descent, 6-block squared falloff, 0.9 melee-dominant hybrid coefficient, +50% undead power, 3-second stun gate of 50 + 2 x Attack Damage.
- Immolation: instant 5-block area, 0.5 vertical multiplier, 1.2 Holy-dominant hybrid enemy damage, 0.5 hybrid ally heal, four-second burn, +50% undead power and guaranteed source-default 1.5x undead critical multiplier.

Target-system notes:
- all four are registered under Iron's Holy school,
- hybrid power weighted averages are exact,
- generic Spell Power random crit blending is not reproduced because Iron's 1.20.1 has no spell-crit attribute surface,
- Immolation's explicit guaranteed undead critical is retained at the frozen source default 1.5x multiplier,
- Judgement P2 uses a server-authoritative timed meteor path; exact upstream projectile model/rendering is reserved for P5,
- Blessed Strikes weapon glow presentation is reserved for P5.

Private source assets:
- four P2 spell icons,
- blessed_strike_start/casting/release,
- divine_protection_release/impact,
- judgement_impact,
- immolation_release.

P2 remains In Progress until the developer runs the P1/P2 audits, clean build, runClient and focused runtime tests, then explicitly confirms PASS.
