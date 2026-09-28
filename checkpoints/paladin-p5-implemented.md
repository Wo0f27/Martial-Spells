# CP11 P5 — Bindings and final integration

Status: **PASS**

Parent: JUS-178  
Checkpoint: JUS-194  
Martial Spells branch: `cp11/paladin-priest-spells`  
Paladins equipment branch: `port/forge-1.20.1`

P0-P4 are developer-confirmed PASS. P5 does not add another spell family; it connects the accepted spell port to the finished Paladins equipment module, freezes source-equivalent grouping, and provides the final package/regression gate.

## P5 integration contract

### Source-equivalent grouping

- Holy Wand family -> native Iron's `irons_spellbooks:blessing_of_life`. Upstream `paladins:heal` remains intentionally unported.
- Holy Staff family -> `martial_spells:holy_shock`.
- Paladin Libram group: Flash Heal, Blessed Strikes, Divine Protection, Judgement, Battle Banner, Immolation.
- Priest Holy Book group: Holy Light, Circle of Healing, Barrier, Lightwell, Levitate, Penance.
- Holy Mote / `lightwell_orb` remains an internal Lightwell helper and is not player-facing.

`PaladinPriestSpellCatalog` records this contract without creating a second spell registry or another magic school.

### Equipment soft integration

The Paladins equipment module retains ownership of all seven holy foci. Its CP11 integration revision makes the existing focus item implement Iron's `IPresetSpellContainer`:

- all four wands create one locked level-1 Blessing of Life slot;
- all three staves create one locked level-1 Holy Shock slot;
- the staff spell is resolved by registry id, not by importing Martial Spells classes;
- if Martial Spells is absent, staff equipment remains valid and simply does not create the missing preset container;
- neither mod declares a new hard dependency on the other.

### Resource regression

P5 reuses the complete P1-P4 source-asset sync. During the final resource audit, Divine Protection was found to have the same omitted status-effect icon class of bug previously fixed for Blessed Strikes; the P2 sync now restores both source mob-effect icons.

Blessed Strikes remains the developer-approved mechanics-only instant buff. Its abandoned glint/VFX/sound resources must not return.

## Local validation gate

Build the Paladins integration jar first:

```powershell
# Paladins-And-Priests repo
git pull --ff-only origin port/forge-1.20.1
python .\tools\audit-cp11-spell-bindings.py
.\gradlew clean build
python .\tools\audit-cp11-jar.py
```

Then validate Martial Spells:

```powershell
# Martial-Spells repo
git pull --ff-only origin cp11/paladin-priest-spells
powershell -ExecutionPolicy Bypass -File .\tools\sync-paladin-p5-assets.ps1
python .\tools\audit-paladin-p5.py
.\gradlew clean build
.\gradlew runClient
```

### Focus runtime checks

1. Fresh copies of every wand must expose exactly one locked Blessing of Life spell and no duplicate Heal.
2. Fresh copies of every Holy Staff must expose exactly one locked Martial Spells Holy Shock.
3. Cast from a wand and verify Iron's native Blessing of Life behavior.
4. Cast Holy Shock from each staff against a friendly and hostile target; the accepted heal/damage split must remain intact.
5. Verify Holy Spell Power remains MAINHAND-only with the previously accepted focus tier values.
6. Craft/smith representative upgraded foci and verify newly produced stacks receive the correct preset spell.
7. Load at least one focus stack from a saved world and verify the preset container persists without duplication.
8. Confirm Holy Mote never appears as a player-bindable focus spell.

### Final regression checks

- Blessed Strikes: instant five-charge mechanics-only buff and visible status icon.
- Divine Protection: visible status icon and accepted orbiting-shield behavior.
- P1-P4 accepted mechanics/presentation remain unchanged.
- no Spell Engine/Spell Power runtime dependency is introduced.
- no missing-model or missing-texture errors in `latest.log`.

### Server/package gate

After client validation:

```powershell
.\gradlew runServer
python .\tools\audit-paladin-p5-jar.py
```

Use the mapped `-dev.jar` only for Martial Spells `runClient` / `runServer`. For the normal private Forge smoke test, use the standard reobfuscated `paladins-3.1.1-forge-private.cp11.jar` together with the built Martial Spells jar.

Developer-confirmed **PASS** on 2026-09-28. The bindings, integrated client runtime, resource regression, and final P5 behavior were accepted. CP11 is complete.
