# CP11-P0 implemented — Paladin/Priest spell archaeology

Parent: JUS-178
Checkpoint: JUS-189

Frozen source:
- ZsoltMolnarrr/Paladins
- commit 2807417a1dd9a65204c002ded487da0e6ae467a1

Target:
- Martial Spells Forge 1.20.1
- Iron's Spells 1.20.1-3.16.3

Result:
- 15 frozen spell definitions inventoried,
- 14 player-facing spells identified,
- lightwell_orb classified as an internal helper,
- Paladin Libram / Priest Holy Book / Holy Wand / Holy Staff ownership frozen,
- Spell Engine tier -> Iron's rarity translation frozen,
- initial max-level-1 fidelity rule frozen,
- source healing-stone reagent explicitly dropped,
- Iron's Holy school selected as the target spell school,
- hybrid source mechanics reserved for explicit custom logic,
- implementation divided into P1-P5 vertical checkpoints.

No spell gameplay was added in P0.
