from pathlib import Path
import json
import sys
import zipfile

root = Path(__file__).resolve().parents[1]
libs = root / "build" / "libs"
errors = []

candidates = sorted(
    [
        p for p in libs.glob("martial_spells-*.jar")
        if not p.name.endswith("-sources.jar")
        and not p.name.endswith("-dev.jar")
        and not p.name.endswith("-slim.jar")
    ],
    key=lambda p: p.stat().st_mtime,
    reverse=True,
) if libs.is_dir() else []

if not candidates:
    print("CP11 P5 PACKAGED JAR AUDIT FAILED")
    print(" - no packaged Martial Spells jar found under build/libs")
    sys.exit(1)

jar_path = candidates[0]

required_spell_icons = {
    f"assets/martial_spells/textures/gui/spell_icons/{spell}.png"
    for spell in (
        "holy_shock", "flash_heal", "circle_of_healing",
        "blessed_strikes", "divine_protection", "judgement", "immolation",
        "holy_beam", "levitate", "penance",
        "barrier", "battle_banner", "lightwell",
    )
}
required_effect_icons = {
    f"assets/martial_spells/textures/mob_effect/{effect}.png"
    for effect in (
        "blessed_strikes", "divine_protection", "levitate",
        "priest_absorption", "battle_banner",
    )
}

with zipfile.ZipFile(jar_path) as jar:
    bad_member = jar.testzip()
    if bad_member is not None:
        errors.append(f"ZIP CRC failure: {bad_member}")

    names = set(jar.namelist())
    required_entries = {
        "META-INF/mods.toml",
        "pack.mcmeta",
        "com/w0of26/martialspells/registry/MartialSpellRegistry.class",
        "com/w0of26/martialspells/integration/PaladinPriestSpellCatalog.class",
        "com/w0of26/martialspells/spells/PaladinHolyShockSpell.class",
        "com/w0of26/martialspells/spells/PaladinBlessedStrikesSpell.class",
        "com/w0of26/martialspells/spells/PaladinHolyBeamSpell.class",
        "com/w0of26/martialspells/spells/PaladinBarrierSpell.class",
        "com/w0of26/martialspells/spells/PaladinLightwellSpell.class",
        "assets/martial_spells/lang/en_us.json",
    } | required_spell_icons | required_effect_icons

    missing = sorted(required_entries - names)
    if missing:
        errors.append(f"required P5 packaged entries missing: {missing}")

    for obsolete in (
        "assets/martial_spells/textures/misc/paladin_item_glow.png",
        "assets/martial_spells/sounds/blessed_strike_start.ogg",
        "assets/martial_spells/sounds/blessed_strike_casting.ogg",
        "assets/martial_spells/sounds/blessed_strike_release.ogg",
    ):
        if obsolete in names:
            errors.append(f"obsolete Blessed Strikes asset packaged: {obsolete}")

    source_files = [name for name in names if name.endswith(".java")]
    if source_files:
        errors.append(f"source .java files leaked into runtime jar: {source_files[:10]}")

    try:
        mods = jar.read("META-INF/mods.toml").decode("utf-8")
    except Exception as exc:
        errors.append(f"cannot read packaged META-INF/mods.toml: {exc}")
        mods = ""

    if "${" in mods:
        errors.append("unexpanded Gradle placeholder remains in packaged mods.toml")
    if 'modId="irons_spellbooks"' not in mods:
        errors.append("packaged Martial Spells metadata missing Iron's dependency")
    if 'modId="paladins"' in mods:
        errors.append("Paladins must remain a dev fixture, not a hard Martial Spells dependency")

    try:
        lang = json.loads(jar.read("assets/martial_spells/lang/en_us.json").decode("utf-8"))
        if "spell.martial_spells.holy_shock" not in lang:
            errors.append("packaged language missing Holy Shock")
        if "spell.martial_spells.lightwell" not in lang:
            errors.append("packaged language missing Lightwell")
    except Exception as exc:
        errors.append(f"cannot validate packaged Martial Spells lang: {exc}")

print(f"CP11 P5 packaged Martial Spells jar: {jar_path.name}")
print(f" - size: {jar_path.stat().st_size} bytes")
print(f" - required player spell icons: {len(required_spell_icons)}")
print(f" - required visible effect icons: {len(required_effect_icons)}")

if errors:
    print("CP11 P5 PACKAGED JAR AUDIT FAILED")
    for error in errors:
        print(" -", error)
    sys.exit(1)

print("CP11 P5 PACKAGED JAR AUDIT PASSED")
