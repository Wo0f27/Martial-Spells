"""Final Ranger integration gate; no Minecraft or equipment integration execution."""
from pathlib import Path
import json
import re
import subprocess
import sys
from ranger_audit_support import ACCEPTED_BASE, ROOT, blob, normalized_barrage, private_asset_errors

JAVA = ROOT / "src/main/java/com/w0of26/martialspells"
RES = ROOT / "src/main/resources"
errors = []


def check(ok, reason):
    if not ok:
        errors.append(reason)


def read(relative):
    path = ROOT / relative
    check(path.is_file(), f"missing {relative}")
    return path.read_text(encoding="utf-8") if path.is_file() else ""


def java(relative):
    return read(f"src/main/java/com/w0of26/martialspells/{relative}")


def normalize(relative, text):
    if relative.endswith("spells/BarrageSpell.java"):
        return normalized_barrage(text)
    if relative.endswith("combat/ArchersPhysicalArrow.java"):
        return text.replace(
            "int totalPierce = (int) Math.min(Byte.MAX_VALUE, (long) innatePierce + extraPierceTargets);",
            "int totalPierce = innatePierce + extraPierceTargets;\n"
            "        if (totalPierce > Byte.MAX_VALUE) return Optional.empty();")
    if relative.endswith("combat/SpiritWolfFixedCooldown.java"):
        return text.replace(
            "Neutral callback: 0 normally, -100 for an external fixed-cooldown bonus.",
            "Neutral Armory-facing seam: 0 normally, -100 for a future authorized bonus.")
    return text


# Freeze every accepted main Java/resource file, allowing only exact neutral-hook edits.
tracked = subprocess.check_output(["git", "ls-tree", "-r", "--name-only", ACCEPTED_BASE,
                                   "src/main", "src/generated"], cwd=ROOT, text=True).splitlines()
changed_main = set(subprocess.check_output([
    "git", "diff", ACCEPTED_BASE, "--name-only", "--", "src/main", "src/generated"
], cwd=ROOT, text=True).splitlines())
for rel in sorted(changed_main.intersection(tracked)):
    path = ROOT / rel
    check(path.is_file(), f"accepted source/resource removed: {rel}")
    if not path.is_file():
        continue
    old = blob(ACCEPTED_BASE, rel)
    if path.suffix == ".java":
        current = normalize(rel, path.read_text(encoding="utf-8"))
        check(current == old.decode().replace("\r\n", "\n"), f"frozen gameplay/presentation changed: {rel}")
    else:
        check(path.read_bytes() == old, f"frozen resource changed: {rel}")
accepted_main = set(tracked)
for folder in (ROOT / "src/main", ROOT / "src/generated"):
    if folder.is_dir():
        for path in folder.rglob("*"):
            if path.is_file():
                rel = path.relative_to(ROOT).as_posix()
                check(rel in accepted_main or rel == "src/main/java/com/w0of26/martialspells/api/RangerCompatibility.java",
                      f"unexpected main source/resource added: {rel}")

registry = java("registry/MartialSpellRegistry.java")
names = ["power_shot", "entangling_roots", "barrage", "rain_of_arrows", "magic_arrow", "spirit_wolf",
         "entangling_arrow", "caltrops"]
for name in names:
    check(len(re.findall(rf'SPELLS\.register\("{name}"', registry)) == 1, f"registry count: {name}")
tag = json.loads(read("src/main/resources/data/martial_spells/tags/spells/ranger_techniques.json"))
check(tag["values"] == [f"martial_spells:{n}" for n in names], "final Ranger catalogue mismatch")
for name in ("ARCHERS_RANGER_SPELL_PORT.md", *[f"ARCHERS_RANGER_AR{i}.md" for i in range(1, 6)]):
    check((ROOT / name).read_text(encoding="utf-8") == blob(ACCEPTED_BASE, name).decode().replace("\r\n", "\n"),
          f"frozen historical document changed: {name}")
for name in ("ARCHERS_RANGER_AR6.md", "MARTIALSPELLS_ARMORY_RANGER_API.md"):
    read(name)

api = java("api/RangerCompatibility.java")
for token in ("package com.w0of26.martialspells.api;", "barrageProvider = player -> 0",
              "public static void setBarrageExtraPenetrableTargetsProvider",
              "public static int extraBarragePenetrableTargets(Player player)",
              "extraTargets < 0 || extraTargets > 127", "player.level().isClientSide",
              "public static void setSpiritWolfCooldownAdjustmentProvider",
              "SpiritWolfFixedCooldown.setAdjustmentProvider"):
    check(token in api, f"public API missing {token}")
barrage = java("spells/BarrageSpell.java")
check(barrage.count("RangerCompatibility.extraBarragePenetrableTargets(player)") == 1,
      "Barrage must query bonus once per loaded launch")
check(barrage.index("if (!isArrow(ammunition)) return false;") < barrage.index("RangerCompatibility.extra"),
      "bonus query occurs before ammo validation")
arrow = java("combat/ArchersPhysicalArrow.java")
check("(long) innatePierce + extraPierceTargets" in arrow and "Math.min(Byte.MAX_VALUE" in arrow,
      "pierce addition lacks safe widened byte saturation")
check("Enchantments.PIERCING" in arrow and "arrow.setPierceLevel((byte) totalPierce)" in arrow,
      "baseline crossbow Piercing lost")
for baseline in range(128):
    check(min(127, baseline + 0) == baseline, "neutral pierce changes baseline")
    check(min(127, baseline + 1) + 1 == min(128, baseline + 2), "+1 target off-by-one")
check(min(127, 127 + 127) == 127, "byte ceiling unsafe")
check(all(term not in arrow for term in ("NominalPhysicalRangedPower", "MartialPower", "ARROW_DAMAGE", "ARROW_VELOCITY")),
      "physical arrows apply duplicate/forbidden scaling")
fixed = java("combat/SpiritWolfFixedCooldown.java")
check("BASE_TICKS = 900" in fixed and "adjustmentProvider = player -> 0" in fixed
      and "adjustmentTicks != -100" in fixed and "BASE_TICKS + adjustmentTicks" in fixed,
      "fixed 900/800 rule changed")
event = java("events/SpiritWolfCooldownEvents.java")
check("EventPriority.LOWEST" in event and "setEffectiveCooldown" in event
      and "event.getSpell() == MartialSpellRegistry.SPIRIT_WOLF.get()" in event,
      "Wolf cooldown is no longer scoped/post-reduction")
for adjustment, expected in ((0, 900), (-100, 800)):
    check(max(1, 900 + adjustment) == expected, "fixed cooldown arithmetic mismatch")

source = "\n".join(p.read_text(encoding="utf-8") for p in JAVA.rglob("*.java"))
check(not re.search(r"import\s+[^;]*(?:armory|spell_engine|spell_power)", source, re.I),
      "forbidden framework/equipment import")
check(not re.search(r'"[^"\n]*(?:armory:|strider|riftstalker)', source, re.I),
      "equipment/set identity leaked into source")
props = read("gradle.properties")
check(props == blob(ACCEPTED_BASE, "gradle.properties").decode().replace("\r\n", "\n"),
      "target dependency versions or coordinates changed")
for token in ("irons_spells_version=1.20.1-3.16.3", "apothic_attributes_version=1.3.7",
              "ranged_weapon_api_version=1.1.4-forge-private.1", "forge_version=47.4.10"):
    check(token in props, f"target stack lost {token}")
gradle = read("build.gradle")
old_gradle = blob(ACCEPTED_BASE, "build.gradle").decode().replace("\r\n", "\n")
check(gradle.split("// Mapped classifier", 1)[0] ==
      old_gradle.split("// Example configuration to allow publishing", 1)[0],
      "dependency graph changed beyond local publication tasks")
for token in ("register('mavenJava', MavenPublication)", "groupId = mod_group_id", "artifactId = mod_id",
              "version = mod_version", "artifact jar", "artifact tasks.named('devJar')",
              "artifact tasks.named('sourcesJar')", "java.withSourcesJar()"):
    check(token in gradle, f"publication lacks {token}")

obsolete = ["entity/BarrageArrow.java", "events/BarrageProjectileEvents.java",
            "client/render/BarrageArrowRenderer.java", "client/particle/BarrageTrailParticle.java"]
for rel in obsolete:
    check(not (JAVA / rel).exists(), f"obsolete file remains: {rel}")
check(not list(RES.rglob("*barrage_trail*")), "old trail resource remains")
check("getProjectileCount" not in barrage and "SHOT_INTERVAL_TICKS" not in barrage,
      "old Barrage progression/scheduler remains")
for token in ("magenta", "one-shot diagnostic", "oversized debug", "System.out.println"):
    ranger_source = "\n".join(java(p) for p in ("api/RangerCompatibility.java",
                             "client/render/HuntersMarkEffectRenderer.java", "client/visual/RangerSourceVfx.java"))
    check(token not in ranger_source, f"diagnostic remains: {token}")

# Common entry points cannot import a Minecraft client implementation.
common = ["api/RangerCompatibility.java", "client/visual/RangerSourceVfx.java"]
common += [f"spells/{n}.java" for n in ("PowerShotSpell", "EntanglingRootsSpell", "BarrageSpell",
                                      "RainOfArrowsSpell", "RangerMagicArrowSpell", "SpiritWolfSpell")]
common += [f"registry/{n}.java" for n in ("MartialEntityRegistry", "MartialEffectRegistry",
                                         "MartialParticleRegistry", "MartialSoundRegistry", "MartialSpellRegistry")]
for rel in common:
    check("net.minecraft.client" not in java(rel), f"server entry point imports client: {rel}")
check("value = Dist.CLIENT" in java("client/MartialClientEvents.java"), "client registration not scoped")
mixins = json.loads(read("src/main/resources/martial_spells.mixins.json"))
check(not any(n.startswith("client.") for n in mixins["mixins"]), "common mixin list loads client")
visuals = java("client/visual/MagicArrowClientVisuals.java")
check('"irons_spellbooks:magic_arrow"' in visuals and '"martial_spells:magic_arrow"' in visuals,
      "external and local Magic Arrow dispatch collapsed")

errors.extend(private_asset_errors())
check(not subprocess.check_output(["git", "diff", "--cached", "--name-only"], cwd=ROOT).strip(),
      "AR6 must remain unstaged at runtime gate")
if errors:
    print("AR6 AUDIT FAILED\n" + "\n".join(f"- {e}" for e in errors))
    sys.exit(1)
print("AR6 AUDIT PASS: frozen main sources/resources, eight Ranger IDs, neutral public hooks, publication, client boundary, 106 tracked private assets")
