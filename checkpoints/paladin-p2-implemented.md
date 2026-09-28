# CP11-P2 — Retribution and protection core

Status: **PASS**

Parent: JUS-178  
Checkpoint: JUS-191  
Branch: `cp11/paladin-priest-spells`

Implemented:
- `martial_spells:blessed_strikes`
- `martial_spells:divine_protection`
- `martial_spells:judgement`
- `martial_spells:immolation`

## Final accepted translation

- Blessed Strikes is the developer-approved target-system variant: **instant** self-buff, 30 mana, 12-second cooldown, immediately grants five seals for 15 seconds, one seal consumed next tick per successful melee swing so same-tick Better Combat cleaves all receive the bonus impact. It retains the 0.5 Holy-dominant hybrid bonus-damage coefficient and 0.5 knockback.
- Blessed Strikes intentionally has no casting animation, casting VFX/SFX, weapon glint, or hit presentation. Its source status-effect icon is retained.
- Divine Protection: instant, 8-second duration, 30-second cooldown, 1-3 fully negated incoming attacks from floor(0.5 x effective Holy multiplier), capped at amplifier 2.
- Judgement: hostile target within 16 blocks, 0.5-second cast, ten-tick meteor descent, 6-block squared falloff, 0.9 melee-dominant hybrid coefficient, +50% undead power, and source stun gate.
- Immolation: instant 5-block area, mixed ally heal/enemy Holy damage, four-second burn, +50% undead power, and explicit undead critical behavior.

All four remain Iron's Holy spells. Hybrid weighting and the previously accepted P2 mechanics are unchanged.

## Presentation/resource result

- Judgement uses the frozen projectile model/trail and accepted impact VFX.
- Divine Protection uses the accepted orbiting shield presentation.
- Immolation uses the accepted source 637+676 area effects.
- Blessed Strikes presentation is intentionally removed by developer decision.
- Both visible P2 status effects, Blessed Strikes and Divine Protection, use their frozen upstream mob-effect icons.

Developer-confirmed PASS during the integrated P1-P4 runtime parity pass.
