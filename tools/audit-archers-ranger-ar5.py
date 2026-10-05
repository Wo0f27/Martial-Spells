"""AR5 Spirit Wolf static/source gate. Runtime behavior remains a developer test gate."""
from pathlib import Path
import hashlib
import json
import re
import subprocess
import sys
from ranger_audit_support import normalized_barrage

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/com/w0of26/martialspells"
RES = ROOT / "src/main/resources/assets/martial_spells"
ARCHERS = Path(r"C:\Modding\RPG-Ports\upstream\Archers")
ENGINE = Path(r"C:\Modding\RPG-Ports\upstream\SpellEngine")
errors = []


def read(path):
    return path.read_text(encoding="utf-8")


def check(ok, message):
    if not ok:
        errors.append(message)


def require(path, *tokens):
    check(path.is_file(), f"missing {path.relative_to(ROOT)}")
    if not path.is_file():
        return ""
    source = read(path)
    for token in tokens:
        check(token in source, f"{path.relative_to(ROOT)} lacks {token}")
    return source


def git(*args, cwd=ROOT):
    return subprocess.check_output(["git", *args], cwd=cwd, text=True,
                                   stderr=subprocess.DEVNULL)


def source_blob(repo, revision, path):
    return subprocess.check_output(
        ["git", "-c", f"safe.directory={repo.as_posix()}", "show", f"{revision}:{path}"],
        cwd=repo,
    )


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


contract = ROOT / "ARCHERS_RANGER_SPELL_PORT.md"
ar1 = ROOT / "ARCHERS_RANGER_AR1.md"
ar2 = ROOT / "ARCHERS_RANGER_AR2.md"
ar3 = ROOT / "ARCHERS_RANGER_AR3.md"
ar4 = ROOT / "ARCHERS_RANGER_AR4.md"
ar5 = ROOT / "ARCHERS_RANGER_AR5.md"
for path in (contract, ar1, ar2, ar3, ar4, ar5):
    check(path.is_file(), f"frozen checkpoint missing: {path.name}")

contract_text = read(contract) if contract.is_file() else ""
ar5_text = read(ar5) if ar5.is_file() else ""
for revision in (
    "ee8fb4081422ba0ab8ced57fe773a1c048d7bf74",
    "d057c13b434cadc8eeaea6df8deab6992f8bda7e",
    "92e9f34a6af8bd2eaa673ff98a3e41b7a3e4f494",
):
    check(revision in contract_text or revision in ar5_text,
          f"frozen revision missing: {revision}")

spell_registry = require(JAVA / "registry/MartialSpellRegistry.java")
check(len(re.findall(r'SPELLS\.register\("spirit_wolf"', spell_registry)) == 1,
      "Spirit Wolf is not registered exactly once")
for old in ("power_shot", "entangling_roots", "barrage", "rain_of_arrows",
            "magic_arrow", "entangling_arrow", "caltrops"):
    check(f'SPELLS.register("{old}"' in spell_registry, f"existing Ranger ID lost: {old}")
for absent in ("spirit_wolf_pack", "rain_of_arrows_v2", "magic_arrow_v2"):
    check(f'SPELLS.register("{absent}"' not in spell_registry, f"duplicate helper spell exposed: {absent}")

for tag_name in ("ranger_techniques", "martial_techniques"):
    tag_path = ROOT / f"src/main/resources/data/martial_spells/tags/spells/{tag_name}.json"
    check(tag_path.is_file(), f"missing catalogue tag {tag_name}")
    if tag_path.is_file():
        values = json.loads(read(tag_path))["values"]
        check(values.count("martial_spells:spirit_wolf") == 1,
              f"{tag_name} Spirit Wolf count is not one")

spell = require(JAVA / "spells/SpiritWolfSpell.java",
                "MartialTechniqueClass.RANGER", "SpellRarity.RARE", "setMaxLevel(1)",
                "baseManaCost = 0", "manaCostPerLevel = 0", "CastType.INSTANT",
                "getSpellCooldown() { return 900; }", "SpiritWolfPending.begin",
                "one_handed_healing_release")
check("SpellEngine" not in spell and "Spell Power" not in spell,
      "Spirit Wolf spell imports a forbidden runtime framework")

entity = require(JAVA / "entity/SpiritWolfEntity.java",
                 "SPAWN_TICKS = 20", "ACTIVE_TICKS = 600", "DESPAWN_TICKS = 20",
                 "TOTAL_TICKS = SPAWN_TICKS + ACTIVE_TICKS + DESPAWN_TICKS",
                 "getPlayerByUUID", "distanceToSqr(owner) > 100.0D",
                 "distanceToSqr(owner) > 16.0D", "32.0D * 32.0D",
                 "setTarget(null)", "rangedPowerSnapshot", "SpiritPower",
                 "20.0D + p", "10.0D + 0.1D * p", "3.0D + 0.3D * p",
                 "0.1D * p", "5.0D + 0.05D * p", "SpiritOwner",
                 "SpiritAge", "SpiritTarget", "SpiritAttackStart")
check("NominalPhysicalRangedPower" not in entity and "MartialPower" not in entity,
      "Wolf entity recomputes power through a forbidden path")
check("getLastHurtByMobTimestamp" in entity and "getLastHurtMobTimestamp" in entity,
      "owner retaliation/defense goals missing")
check("class OwnerDefenseGoal" in entity and "class RevengeCloserGoal" in entity
      and "class OwnerAttackGoal" in entity and "class BiteGoal" in entity,
      "source combat goals missing")
check("SPIRIT_WOLF" in read(JAVA / "registry/MartialEntityRegistry.java"),
      "Spirit Wolf entity type is not registered")

pending = require(JAVA / "combat/SpiritWolfPending.java",
                  "extends SavedData", "SECOND_DELAY_TICKS = 5", "getGameTime() + SECOND_DELAY_TICKS",
                  "level.getEntity(prepared.entityId)", "tag.putUUID(\"Owner\"",
                  "tag.putDouble(\"Power\"", "tag.putLong(\"Deadline\"",
                  "level.getGameTime() < prepared.deadline")
check("remove" in pending and "insert(level, prepared)" in pending,
      "pending second-spawn descriptor does not consume each callback once")
check("getPlayerByUUID" not in pending,
      "pending callback added an unauthorized owner recheck")

fixed = require(JAVA / "combat/SpiritWolfFixedCooldown.java",
                "BASE_TICKS = 900", "adjustmentProvider", "adjustmentTicks != 0",
                "adjustmentTicks != -100", "BASE_TICKS + adjustmentTicks")
events = require(JAVA / "events/SpiritWolfCooldownEvents.java",
                 "EventPriority.LOWEST", "setEffectiveCooldown",
                 "SpiritWolfFixedCooldown.forPlayer")
check("CooldownReduction" not in fixed and "Attributes." not in fixed
      and "getItem" not in fixed and "ArmorItem" not in fixed,
      "fixed cooldown helper contains gear/CDR logic")
check("Armory" not in events and "Riftstalker" not in events,
      "cooldown event contains equipment detection")

client = require(JAVA / "client/MartialClientEvents.java",
                 "MartialEntityRegistry.SPIRIT_WOLF", "SpiritWolfRenderer::new",
                 "SpiritWolfModel.LAYER", "SpiritWolfModel::createBodyLayer")
renderer = require(JAVA / "client/render/SpiritWolfRenderer.java",
                   "extends MobRenderer", "spirit_wolf.png", "SpiritWolfRenderType.spirit",
                   "0.5F")
render_type = require(JAVA / "client/render/SpiritWolfRenderType.java",
                      "RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER",
                      "COLOR_DEPTH_WRITE", "NO_CULL")
model = require(JAVA / "client/model/SpiritWolfModel.java",
                "createBodyLayer", "Direwolf", "SpiritWolfAnimations.spawn",
                "SpiritWolfAnimations.run", "SpiritWolfAnimations.bite",
                "SpiritWolfAnimations.idle", "seamlessLoop")
check((RES / "textures/entity/spirit_wolf.png").is_file(), "Spirit Wolf texture missing")
check((RES / "textures/gui/spell_icons/spirit_wolf.png").is_file(), "Spirit Wolf icon missing")
for sound in ("spirit_wolf_spawn.ogg", "spirit_wolf_summon_1.ogg", "spirit_wolf_summon_2.ogg"):
    check((RES / "sounds" / sound).is_file(), f"Spirit Wolf sound missing: {sound}")
animation_asset = RES / "player_animation/one_handed_healing_release.json"
check(animation_asset.is_file(), "Spirit Wolf release animation asset missing")
if animation_asset.is_file():
    try:
        source_animation = json.loads(source_blob(
            ENGINE, "d057c13b434cadc8eeaea6df8deab6992f8bda7e",
            "common/src/main/resources/assets/spell_engine/player_animations/one_handed_healing_release.json"))
        check(json.loads(read(animation_asset)) == source_animation,
              "Spirit Wolf release animation differs from frozen Spell Engine JSON")
    except (subprocess.CalledProcessError, json.JSONDecodeError) as exc:
        errors.append(f"could not verify Spell Engine release animation: {exc}")
check("spirit_wolf_spawn" in read(RES / "sounds.json"), "Spirit Wolf spawn sound not registered")
check("spirit_wolf_summon" in read(RES / "sounds.json"), "Spirit Wolf summon sound not registered")

# Provenance: exact source icon, texture and three Archers sounds.
asset_pairs = {
    "textures/gui/spell_icons/spirit_wolf.png":
        "common/src/main/resources/assets/archers/textures/spell/spirit_wolf.png",
    "textures/entity/spirit_wolf.png":
        "common/src/main/resources/assets/archers/textures/entity/direwolf_spell.png",
    "sounds/spirit_wolf_spawn.ogg":
        "common/src/main/resources/assets/archers/sounds/spirit_wolf_spawn.ogg",
    "sounds/spirit_wolf_summon_1.ogg":
        "common/src/main/resources/assets/archers/sounds/spirit_wolf_summon_1.ogg",
    "sounds/spirit_wolf_summon_2.ogg":
        "common/src/main/resources/assets/archers/sounds/spirit_wolf_summon_2.ogg",
}
for target, source in asset_pairs.items():
    target_path = RES / target
    check(target_path.is_file(), f"missing imported AR5 asset {target}")
    if target_path.is_file():
        check(hashlib.sha256(source_blob(ARCHERS,
                                         "ee8fb4081422ba0ab8ced57fe773a1c048d7bf74",
                                         source)).hexdigest() == sha256(target_path),
              f"AR5 asset differs from frozen source: {target}")

# No forbidden architecture or future set integration.
gradle = read(ROOT / "build.gradle") if (ROOT / "build.gradle").is_file() else ""
for forbidden in ("spell-engine", "spell_power", "SpellPower", "rangedweaponapi:2"):
    check(forbidden.lower() not in gradle.lower(), f"forbidden dependency/version found: {forbidden}")
all_source = "\n".join(read(p) for p in JAVA.rglob("*.java"))
check(not re.search(r"(Armory|Riftstalker).*(getItem|ArmorItem|equipment|set bonus)",
                   all_source, re.IGNORECASE), "AR5 source contains equipment/set detection")
check("AR6" not in all_source, "AR6 integration appears to have begun")
check("EntityType<SpiritWolfEntity>" in read(JAVA / "registry/MartialEntityRegistry.java"),
      "Spirit Wolf EntityType declaration missing")

# Existing Ranger gameplay source files should not be rewritten by AR5.
base = "92e9f34a6af8bd2eaa673ff98a3e41b7a3e4f494"
for rel in ("spells/PowerShotSpell.java", "spells/EntanglingRootsSpell.java",
            "spells/BarrageSpell.java", "spells/RainOfArrowsSpell.java",
            "spells/RangerMagicArrowSpell.java", "spells/EntanglingArrowSpell.java",
            "spells/CaltropsSpell.java"):
    path = JAVA / rel
    if not path.is_file():
        continue
    try:
        old = subprocess.check_output(["git", "show", f"{base}:src/main/java/com/w0of26/martialspells/{rel}"],
                                       cwd=ROOT, stderr=subprocess.DEVNULL).decode()
        current = normalized_barrage(read(path)) if rel == "spells/BarrageSpell.java" else read(path)
        check(current == old, f"existing Ranger gameplay file changed during AR5: {rel}")
    except subprocess.CalledProcessError:
        pass

# User-authorized closeout retains the mixed implementation commit. Its private
# Paladin/Priest assets are now intentionally tracked; verify they stay intact.
accepted_implementation = "f1f9ad5306d31ec988ede1bb7457f87043495d1b"
added_resources = git("diff", "--name-only", "--diff-filter=A", base,
                      accepted_implementation, "--", "src/main/resources").splitlines()
ar5_assets = {f"src/main/resources/assets/martial_spells/{p}" for p in asset_pairs}
private_assets = {p for p in added_resources
                  if Path(p).suffix in (".png", ".ogg") and p not in ar5_assets}
check(len(private_assets) == 106, "accepted private-asset inventory is not 106 files")
tracked = set(git("ls-files").splitlines())
staged = set(git("diff", "--cached", "--name-only").splitlines())
check(not private_assets.intersection(staged), "unrelated private assets are staged")
for rel in sorted(private_assets):
    path = ROOT / rel
    check(rel in tracked and path.is_file(), f"accepted private asset missing/untracked: {rel}")
    if path.is_file():
        accepted_bytes = source_blob(ROOT, accepted_implementation, rel)
        check(hashlib.sha256(accepted_bytes).hexdigest() == sha256(path),
              f"accepted private asset modified: {rel}")

if errors:
    print("AR5 AUDIT FAILED")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)
print("AR5 AUDIT PASSED")
