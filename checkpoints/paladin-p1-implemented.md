# CP11-P1 implemented — Holy healing core

Parent: JUS-178
Checkpoint: JUS-190
Branch: cp11/paladin-priest-spells

Implemented:
- martial_spells:heal
- martial_spells:holy_shock
- martial_spells:flash_heal
- martial_spells:circle_of_healing

Source behavior retained:
- Heal: 16-block friendly aim, self fallback, 1.0s cast, 4s cooldown, 0.5 coefficient.
- Holy Shock: 16-block aim, self fallback, 1.5s cast, 3s cooldown; friendly 0.4 heal or hostile 0.8 Holy damage + 0.5 knockback.
- Flash Heal: 16-block friendly aim, self fallback, 0.5s cast, 6s cooldown, 1.2 heal coefficient.
- Circle of Healing: 8-block area, 0.6 vertical multiplier, caster included, 0.5s cast, 10s cooldown, 0.4 heal coefficient.

Iron's translation:
- all four use irons_spellbooks:holy,
- max level 1,
- Iron's Utils.shouldHealEntity supplies relation semantics,
- healing posts SpellHealEvent,
- Holy Shock damage uses SpellDamageSource/DamageSources,
- shared target power reference = 5.0,
- mana 15 / 20 / 30 / 40 is target-side balance rather than source fidelity.

Private source assets:
- four frozen Paladins spell icons copied into martial_spells namespace,
- holy_shock_heal.ogg and holy_shock_damage.ogg copied into martial_spells namespace,
- sync script pinned to frozen source commit.

P1 is not PASS until the developer runs the P0/P1 audits, clean build, runClient, focused target/relation tests, and confirms PASS.
