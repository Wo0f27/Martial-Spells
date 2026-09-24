# CP11-P1 implemented — Holy healing core

Parent: JUS-178
Checkpoint: JUS-190
Branch: cp11/paladin-priest-spells

Implemented:
- martial_spells:holy_shock
- martial_spells:flash_heal
- martial_spells:circle_of_healing

Source behavior retained:
- Holy Shock: 16-block aim, self fallback, 1.5s cast, 3s cooldown; friendly 0.4 heal or hostile 0.8 Holy damage + 0.5 knockback.
- Flash Heal: 16-block friendly aim, self fallback, 0.5s cast, 6s cooldown, 1.2 heal coefficient.
- Circle of Healing: 8-block area, 0.6 vertical multiplier, caster included, 0.5s cast, 10s cooldown, 0.4 heal coefficient.

Iron's translation:
- all three custom spells use irons_spellbooks:holy,
- max level 1,
- Iron's Utils.shouldHealEntity supplies relation semantics,
- healing posts SpellHealEvent,
- Holy Shock damage uses SpellDamageSource/DamageSources,
- shared target power reference = 5,
- mana 20 / 30 / 40 is target-side balance rather than source fidelity.

Private source assets:
- three frozen Paladins spell icons copied into martial_spells namespace,
- holy_shock_heal.ogg and holy_shock_damage.ogg copied into martial_spells namespace,
- sync script pinned to frozen source commit.

Intentional omission:
- upstream Paladins Heal is not registered as martial_spells:heal because it overlaps Iron's native Holy healing role,
- P5 will map the Holy Wand to irons_spellbooks:blessing_of_life as the closest native targeted-heal replacement.

Developer confirmed the retained P1 spells work correctly after local validation.
