# CP11 P3 — Priest channel and control

Status: **IMPLEMENTED / VALIDATING**

Frozen source: `ZsoltMolnarrr/Paladins` commit
`2807417a1dd9a65204c002ded487da0e6ae467a1`.

P0-P2 are frozen as PASS. P3 adds only Holy Light (`holy_beam`),
Levitate, and Penance plus the two required effects and Penance projectile.
P4/P5 remain locked until explicit P3 runtime PASS.

## Frozen behavior implemented

- Spell Engine channel deliveries are translated using equal-interval midpoint
  scheduling over Iron's effective cast duration.
- Early channel release keeps already delivered impacts and applies a
  proportional share of target-side mana and the current effective Iron's
  cooldown.
- Holy Light: Rare Holy, 32-block live BEAM, 5-second source duration,
  25 deliveries, source 0.2 channel multiplier, 0.4 heal / 0.8 damage
  coefficients and source 0.5 knockback before channel normalization. Every
  valid entity intersecting the live beam before the first blocking collider is
  affected; hostile damage preserves Spell Engine's default iframe bypass.
- Levitate: Rare Holy, 1.5-second source duration, four midpoint releases,
  horizontal movement lock, reset velocity to +0.15Y on each release,
  five-second Levitate refresh, Slow Falling carrier on Minecraft 1.20.1 and
  three-second soft landing after Levitate ends.
- Penance: Epic Holy, required sticky hostile target within 20 blocks,
  1.5-second source duration, three midpoint projectile releases, velocity
  0.8, 16 degrees/tick homing, a 20-block projectile travel cap, source 0.55
  damage and 0.2 knockback with the source 0.5 channel multiplier.
- Each successful Penance bolt radiates only the absorption action to friendly
  living entities in an eight-block spherical area with no distance falloff.
- Penance absorption lasts six seconds and grants two absorption health per
  effective stack. The executable source floors its two scaling expressions
  independently: each bolt adds `1 + floor(0.1 * Holy power)` stacks, while
  the amplifier cap is `2 + floor(0.3 * Holy power)`. This intentionally
  follows executable Spell Engine behavior even where the upstream comment's
  "exactly one full volley" shorthand diverges because of integer truncation.
- Target-side mana values are Holy Light 40, Levitate 25, Penance 45. These are
  Iron's balance values and are not claimed as upstream reagent fidelity.
- Source spell/effect icons and Paladins-owned P3 sounds are synchronized from
  the frozen commit.
- P1-P3 visual parity is now part of the P3 validation gate. Spell Engine's
  generic Holy/healing particle language is reconstructed with Forge/vanilla
  particles so no Spell Engine runtime dependency is added.
- Holy Light now draws a blocked golden beam path and source-style heal/damage
  impacts; Levitate has persistent Holy lift particles.
- Penance now renders the frozen Lightwell-orb model at source scale/orbit/spin
  with a synchronized double-helix Holy trail and source-style shield pulse.
- Judgement now renders the frozen projectile model on a presentation-only
  meteor entity while its previously validated P2 damage/timing logic remains
  server-authoritative.

## Validation gate

Run:

```powershell
git pull --ff-only origin cp11/paladin-priest-spells
powershell -ExecutionPolicy Bypass -File .\tools\sync-paladin-p3-assets.ps1
python .\tools\audit-paladin-p3.py
.\gradlew clean build
.\gradlew runClient
```

Runtime checks:

1. P1 and P2 retain their previously accepted behavior.
2. Holy Light appears as a one-level Rare Holy spell, costs 40 mana for a full
   channel, and can be held for five seconds.
3. Holding Holy Light on an ally heals in many small pulses; holding it on a
   hostile target deals many small damage pulses. Moving aim during the channel
   changes which entity receives later pulses.
4. Put two or more valid entities directly along the same unobstructed Holy
   Light beam. Each should receive the same pulse; placing a solid wall between
   the caster and a farther target must prevent that farther target from being
   affected.
5. A full Holy Light channel produces 25 deliveries. Rapid 4-tick damage pulses
   must all land rather than being swallowed by vanilla hurt i-frames. Releasing
   around half way keeps delivered impacts and produces roughly half the normal
   effective cooldown and mana cost.
6. Levitate appears as a one-level Rare Holy spell and horizontally roots the
   caster while channeling.
7. A full Levitate channel gives four distinct upward kicks. Each kick resets
   movement rather than accelerating from the prior kick.
8. Releasing Levitate stops further ascent; the five-second Levitate effect
   remains and the player descends under Slow Falling. The source maintains a
   three-second Slow Falling buffer only when that carrier is refreshed; on a
   full four-kick channel the executable refresh guard means the post-Levitate
   tail can be shorter than three seconds. It must still land softly.
9. Penance requires a hostile target within 20 blocks and keeps that target
   locked through the channel.
10. A full Penance channel launches exactly three visible Lightwell-orb bolts.
   They launch along the caster's look, travel at 0.8 blocks/tick, curve toward
   the locked target at up to 16 degrees/tick, and terminate after 20 blocks of
   projectile travel if they have not impacted.
11. Each Penance bolt damages only the hostile primary target. A nearby friendly
    player/entity within eight blocks of the impact receives/refreshes Priest
    Absorption instead of taking splash damage.
12. Repeated bolts increase the absorption stack to the one-volley cap; the
    effect lasts six seconds. Allies outside eight blocks receive no shield.
13. Penance's projectile must render the actual orbiting Lightwell orb with a
    double-helix Holy trail and must never display a purple/black missing-model
    cube. Judgement must likewise render its source meteor model rather than the
    old END_ROD-only placeholder.
14. Early release of Holy Light, Levitate, and Penance gives proportional mana
    and effective cooldown rather than a free partial cast or the full cooldown.
15. Recheck P1/P2 presentation: Holy Shock has distinct heal/damage impacts;
    Flash Heal has a healing pillar; Circle of Healing visibly marks its radius;
    Blessed Strikes draws light into the weapon and maintains a seal aura;
    Divine Protection has apply/pop glimmer; Immolation erupts as a radial
    Holy-fire effect; Judgement has its model, trail and large impact burst.

P3 remains **VALIDATING**, not PASS, until the user explicitly confirms this
runtime gate.
