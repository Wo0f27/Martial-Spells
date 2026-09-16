from pathlib import Path
import json
import sys

ROOT = Path(__file__).resolve().parents[1]
errors = []


def require(condition: bool, message: str) -> None:
    if not condition:
        errors.append(message)


def read(path: str) -> str:
    target = ROOT / path
    require(target.is_file(), f"missing file: {path}")
    return target.read_text(encoding="utf-8") if target.is_file() else ""


technique_class = read("src/main/java/com/w0of26/martialspells/technique/MartialTechniqueClass.java")
require("WARRIOR" in technique_class, "MartialTechniqueClass is missing WARRIOR")
require(technique_class.count("WARRIOR") == 1, "WARRIOR should appear exactly once in MartialTechniqueClass")

technique_tags = read("src/main/java/com/w0of26/martialspells/technique/MartialTechniqueTags.java")
require("WARRIOR_TECHNIQUES" in technique_tags, "MartialTechniqueTags is missing WARRIOR_TECHNIQUES")
require('createTagKey("warrior_techniques")' in technique_tags, "Warrior tag key does not target warrior_techniques")

warrior_tag_path = "src/main/resources/data/martial_spells/tags/spells/warrior_techniques.json"
warrior_tag_text = read(warrior_tag_path)
try:
    warrior_tag = json.loads(warrior_tag_text)
    require(warrior_tag.get("replace") is False, "warrior_techniques tag must use replace=false")
    require(warrior_tag.get("values") == [], "W1 warrior_techniques tag must remain empty")
except json.JSONDecodeError as exc:
    errors.append(f"invalid JSON in {warrior_tag_path}: {exc}")

power = read("src/main/java/com/w0of26/martialspells/combat/PhysicalMeleePower.java")
require("Attributes.ATTACK_DAMAGE" in power, "PhysicalMeleePower must read vanilla Attack Damage")
require("getAttribute(Attributes.ATTACK_DAMAGE)" in power, "PhysicalMeleePower must use a null-safe AttributeInstance lookup")
require("attackDamage == null ? 0.0D" in power, "PhysicalMeleePower must return zero when Attack Damage is absent")
require("healthBase + powerMultiplier * get(caster)" in power, "control-health-limit formula drifted from frozen source")
require("MartialAttributeRegistry" not in power, "PhysicalMeleePower must not include Martial Spell Power")
require("DualMeleePower" not in power, "PhysicalMeleePower must remain single-hand only")

spell_registry = read("src/main/java/com/w0of26/martialspells/registry/MartialSpellRegistry.java")
for token in (
    "SHATTERING_THROW",
    "THROW_NET",
    "DEMORALIZING_SHOUT",
    "LAST_STAND",
    "MORTAL_STRIKE",
):
    require(token not in spell_registry, f"W1 must not register Warrior gameplay yet: found {token}")

# `CHARGE` alone is too generic to use as a safe static token check. Instead,
# verify the six planned spell implementation classes do not exist yet.
for filename in (
    "ShatteringThrowSpell.java",
    "ThrowNetSpell.java",
    "ChargeSpell.java",
    "DemoralizingShoutSpell.java",
    "LastStandSpell.java",
    "MortalStrikeSpell.java",
):
    require(not (ROOT / "src/main/java/com/w0of26/martialspells/spells" / filename).exists(),
            f"W1 must not contain Warrior gameplay class {filename}")

build_gradle = read("build.gradle")
mods_toml = read("src/main/resources/META-INF/mods.toml")
for dependency_token in ("spell_engine", "spell-engine", "spell_power", "spell-power"):
    require(dependency_token not in build_gradle.lower(), f"W1 introduced forbidden dependency token in build.gradle: {dependency_token}")
    require(dependency_token not in mods_toml.lower(), f"W1 introduced forbidden dependency token in mods.toml: {dependency_token}")

if errors:
    print("Warrior W1 audit FAILED")
    for error in errors:
        print(f" - {error}")
    sys.exit(1)

print("Warrior W1 audit PASS")
print("Architecture only: WARRIOR classification/tag + source-faithful single-hand physical-melee adapter.")
print("No Warrior gameplay is registered in W1.")
