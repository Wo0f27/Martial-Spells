#!/usr/bin/env python3
"""Static AR1 scope and foundation audit. Run from any working directory."""

from pathlib import Path
import re
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/com/w0of26/martialspells"
errors = []


def check(ok, message):
    if not ok:
        errors.append(message)


def read(path):
    check(path.is_file(), f"missing {path.relative_to(ROOT)}")
    return path.read_text(encoding="utf-8") if path.is_file() else ""


contract = read(ROOT / "ARCHERS_RANGER_SPELL_PORT.md")
for token in (
    "5955e6a5f24e02fe21843d30d8df01a4d7624d97",
    "9337b5809b20a5df2af631ca0a5359539f1934df",
    "ee8fb4081422ba0ab8ced57fe773a1c048d7bf74",
    "d057c13b434cadc8eeaea6df8deab6992f8bda7e",
    "1.10.5+1.20.1-forge",
):
    check(token in contract, f"frozen contract lacks {token}")

gradle = read(ROOT / "build.gradle")
props = read(ROOT / "gradle.properties")
check("ranged_weapon_api_version=1.1.4-forge-private.1" in props, "RWA line changed")
check(not re.search(r"(?im)^\s*(?:implementation|compileOnly|runtimeOnly|api)\s+[^\n]*[\"'][^\"']*(?:spell[_-]?engine|spell[_-]?power)", gradle),
      "Spell Engine or Spell Power dependency added")

registry = read(JAVA / "registry/MartialSpellRegistry.java")
for ability in ("barrage", "entangling_arrow", "caltrops"):
    check(f'SPELLS.register("{ability}"' in registry, f"existing Ranger ID missing: {ability}")
for ability in ("power_shot", "entangling_roots", "rain_of_arrows", "magic_arrow", "spirit_wolf"):
    check(f'SPELLS.register("{ability}"' not in registry, f"full Ranger ability registered in AR1: {ability}")

power = read(JAVA / "combat/NominalPhysicalRangedPower.java")
for token in ("BOW_FALLBACK = 6.0D", "CROSSBOW_FALLBACK = 9.0D", "config.damage() > 0.0F",
              "ARROW_DAMAGE", "selectedWeapon(caster)", "weaponDamage(selectedWeapon(caster)) * arrowDamageAttribute(caster)"):
    check(token in power, f"nominal physical ranged power contract lacks {token}")
check("MartialPower" not in power and "ARROW_VELOCITY" not in power, "nominal P includes forbidden scaling")

arrow = read(JAVA / "combat/ArchersPhysicalArrow.java")
for token in ("createArrow(level, ammunition, shooter)", "customArrow(arrow)",
              "ScalingUtil.arrowDamageMultiplier", "* ratio * techniqueMultiplier",
              "setPierceLevel", "RangerArrowMetadata.setIframeBypass"):
    check(token in arrow, f"physical arrow path lacks {token}")
check("MartialPower" not in arrow and "ARROW_DAMAGE" not in arrow and "arrowDamageAttribute" not in arrow,
      "Archers physical arrow path double-applies power or Apothic damage")

metadata = read(JAVA / "combat/RangerArrowMetadata.java")
for token in ("getPersistentData()", "stampTransfer", "eligibleShot", "iframeBypass", "extraPierceTargets"):
    check(token in metadata, f"projectile metadata lacks {token}")

scheduler = read(JAVA / "combat/RangerTickScheduler.java")
for token in ("ResourceKey<Level>", "UUID casterId", "long deadline", "ServerLevel",
              "cancelCaster", "ServerStoppingEvent"):
    check(token in scheduler, f"scheduler lacks {token}")

wolf = read(JAVA / "combat/SpiritWolfFixedCooldown.java")
check("BASE_TICKS = 900" in wolf and "equipmentReductionTicks != 100" in wolf,
      "fixed wolf cooldown contract changed")
check(not re.search(r"(?i)riftstalker|armory|setbonus|armoritem|cooldown_reduction", wolf),
      "wolf cooldown helper contains equipment/set detection")

for path in (JAVA / "combat/NominalPhysicalRangedPower.java", JAVA / "combat/ArchersPhysicalArrow.java",
             JAVA / "combat/RangerArrowMetadata.java", JAVA / "combat/RangerTickScheduler.java"):
    check(not re.search(r"(?i)riftstalker|armory", read(path)),
          f"Armory-specific detection found in {path.name}")

status = subprocess.run(["git", "status", "--porcelain", "-uall"], cwd=ROOT,
                        capture_output=True, text=True, check=True)
private_assets = [line for line in status.stdout.splitlines()
                  if line[3:].startswith("src/main/resources/assets/martial_spells/")
                  and any(part in line[3:].lower() for part in
                          ("paladin", "priest", "holy_", "divine_", "penance", "battle_banner",
                           "lightwell", "judgement", "barrier", "immolation", "blessed_strikes"))]
check(all(line.startswith("?? ") for line in private_assets),
      "private Paladin/Priest assets are staged or tracked-modified")
cached = subprocess.run(["git", "diff", "--cached", "--name-only"], cwd=ROOT,
                        capture_output=True, text=True, check=True)
check(not any("src/main/resources/assets/martial_spells/" in name for name in cached.stdout.splitlines()),
      "binary assets staged")

if errors:
    for error in errors:
        print(f"FAIL: {error}")
    sys.exit(1)
print("PASS: Archers Ranger AR1 static scope/foundation audit")
print(f"Private Paladin/Priest asset entries remain untracked: {len(private_assets)}")
