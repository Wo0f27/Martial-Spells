# Warrior W1 — IMPLEMENTED / VALIDATING

W1 establishes architecture only; it intentionally adds no Warrior gameplay.

Implemented:

- `MartialTechniqueClass.WARRIOR`.
- `MartialTechniqueTags.WARRIOR_TECHNIQUES`.
- empty `data/martial_spells/tags/spells/warrior_techniques.json`.
- `PhysicalMeleePower` adapter matching the frozen single-hand `spell_power:physical_melee` POWER source and common control-health-limit formula.
- authoritative `WARRIOR_SPELL_PORT.md` with W0-approved contracts and checkpoint gates.

Deliberately deferred:

- Warrior spell registrations.
- effects, projectiles, animation, VFX/SFX, networking, and cooldown behavior.
- shared charged-technique runtime, until the first concrete charged vertical slice reveals the actual common denominator.

Validation required before W1 PASS:

- `python tools/audit-warrior-w1.py`
- `.\gradlew clean build`
- `.\gradlew runClient`
- confirm no Warrior spell appears or activates yet
- confirm accepted Rogue techniques still load normally

W2 remains locked until the user explicitly marks W1 PASS.
