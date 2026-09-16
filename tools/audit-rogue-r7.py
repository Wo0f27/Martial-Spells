from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
errors = []

def text(path: str) -> str:
    p = ROOT / path
    if not p.exists():
        errors.append(f"missing file: {path}")
        return ""
    return p.read_text(encoding="utf-8")

def require(haystack: str, needle: str, label: str):
    if needle not in haystack:
        errors.append(f"missing {label}: {needle}")

# Keep the previous accepted Rogue checkpoints in the cumulative gate.
r6 = ROOT / "tools" / "audit-rogue-r6.py"
if r6.exists():
    result = subprocess.run([sys.executable, str(r6)], cwd=ROOT)
    if result.returncode != 0:
        errors.append("R6 cumulative audit failed")
else:
    errors.append("missing tools/audit-rogue-r6.py")

spell = text("src/main/java/com/w0of26/martialspells/spells/BearTrapSpell.java")
entity = text("src/main/java/com/w0of26/martialspells/entity/BearTrapEntity.java")
effect = text("src/main/java/com/w0of26/martialspells/effects/TrappedEffect.java")
mixin = text("src/main/java/com/w0of26/martialspells/mixin/LivingEntityTrappedMixin.java")
renderer = text("src/main/java/com/w0of26/martialspells/client/render/BearTrapRenderer.java")
client_anim = text("src/main/java/com/w0of26/martialspells/client/animation/BearTrapClientAnimations.java")

for needle, label in [
    ("TRAP_COUNT = 3", "three-trap count"),
    ("PLACEMENT_DISTANCE = 2.0F", "two-block placement"),
    ("PLACEMENT_YAW_STEP = 120.0F", "120-degree spacing"),
    ("PLACEMENT_DELAY_STEP_TICKS = 3", "0/3/6 placement cadence"),
    ("BASE_COOLDOWN_SECONDS = 15", "15-second cooldown"),
    ("CONTROL_HEALTH_BASE = 100.0F", "control base"),
    ("CONTROL_POWER_MULTIPLIER = 2.0F", "control power multiplier"),
    ("groundTrapPosition", "per-trap ground sampling"),
    ("dual_handed_ground_release", "release animation key"),
]:
    require(spell, needle, label)

for needle, label in [
    ("SPAWN_TICKS = 20", "20-tick spawn"),
    ("ACTIVE_TICKS = 400", "20-second active lifetime"),
    ("NORMAL_DESPAWN_TICKS = 15", "15-tick normal despawn"),
    ("ATTACK_TICKS = 30", "30-tick sprung despawn"),
    ("IMPACT_INTERVAL_TICKS = 2", "two-tick impact scan"),
    ("ROOT_DURATION_TICKS = 60", "three-second root"),
    ("TRIGGER_RADIUS = 0.6", "0.6 trigger radius"),
    ("setDeltaMovement(beforeVelocity)", "zero-knockback restoration"),
]:
    require(entity, needle, label)

require(effect, "-2.0D", "source movement-speed modifier")
require(effect, "MULTIPLY_BASE", "source movement operation")
require(mixin, 'method = "travel"', "movement-only root hook")
require(mixin, 'method = "jumpFromGround"', "jump root hook")
if 'method = "isImmobile"' in mixin:
    errors.append("root still uses global isImmobile and may block non-movement actions")
require(renderer, "BearTrapModel", "Bear Trap renderer")
require(client_anim, "dual_handed_ground_release", "Bear Trap client release animation")

assets = [
    "src/main/resources/assets/martial_spells/textures/spell/bear_trap.png",
    "src/main/resources/assets/martial_spells/textures/entity/bear_trap.png",
    "src/main/resources/assets/martial_spells/sounds/bear_trap_release.ogg",
    "src/main/resources/assets/martial_spells/sounds/bear_trap_impact.ogg",
    "src/main/resources/assets/martial_spells/sounds/bear_trap_spawn_1.ogg",
    "src/main/resources/assets/martial_spells/sounds/bear_trap_spawn_2.ogg",
    "src/main/resources/assets/martial_spells/sounds/bear_trap_spawn_3.ogg",
    "src/main/resources/assets/martial_spells/sounds/bear_trap_despawn_1.ogg",
    "src/main/resources/assets/martial_spells/sounds/bear_trap_despawn_2.ogg",
    "src/main/resources/assets/martial_spells/sounds/bear_trap_despawn_3.ogg",
    "src/main/resources/assets/martial_spells/player_animation/dual_handed_ground_release.json",
]
for asset in assets:
    if not (ROOT / asset).is_file():
        errors.append(f"missing synchronized R7 asset: {asset}")

if errors:
    print("R7 STATIC AUDIT FAILED")
    for error in errors:
        print(f" - {error}")
    raise SystemExit(1)

print("R7 static: Bear Trap registered as a single-level Rare Rogue Martial technique")
print("Placement: three traps / 2-block radial offset / 0-3-6 tick cadence / 120-degree spacing / local terrain grounding")
print("Entity: 20-tick spawn / 20-second active lifetime / one impact / 15-tick normal or 30-tick sprung despawn")
print("Impact: zero-knockback physical damage / 3-second movement+jump-only root / 100 + 2x dual-melee control cap")
print("Presentation: exact frozen Rogues icon, entity texture and sounds + exact dual_handed_ground_release PlayerAnimator JSON")
print("R7 STATIC AUDIT PASSED")
