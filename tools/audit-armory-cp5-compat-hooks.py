"""Static contract, pinned-bytecode, refmap and scope/provider regression checks; no Minecraft."""
from pathlib import Path
import hashlib
import json
import re
import subprocess
import sys
from ability_compat_audit_support import ADDITIONS, REPLACEMENTS, normalize_ability_hooks

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/com/w0of26/martialspells"
BASE = "7b11423a1000fd2a57477a34fbb0c6554f5b03f5"
errors = []
def check(ok, message):
    if not ok: errors.append(message)
def read(rel): return (JAVA / rel).read_text(encoding="utf-8")
def blob(rel):
    return subprocess.check_output(["git", "show", f"{BASE}:{rel}"], cwd=ROOT)

api = read("api/AbilityCompatibility.java")
contracts = [("CircleOfHealingPower", "double", "0.10"), ("PenanceCriticalChance", "double", "0.05"),
             ("DivineProtectionCooldown", "int", "-100"), ("JudgementCooldown", "int", "-60"),
             ("ChargeCooldown", "int", "-40"), ("ShoutDamageMultiplier", "double", "4.0"),
             ("ShadowStepCooldown", "int", "-60"), ("BearTrapPower", "double", "0.10")]
for name, kind, value in contracts:
    fn = "ToDoubleFunction" if kind == "double" else "ToIntFunction"
    zero = "0.0" if kind == "double" else "0"
    check(f"set{name}AdjustmentProvider({fn}<Player> provider)" in api, f"missing setter {name}")
    check(f"public static {kind} get{name}Adjustment(Player player)" in api, f"missing getter {name}")
    check(f"value != {zero} && value != {value}" in api, f"unsupported-value guard missing {name}")
check(api.count("private static volatile") == 8 and api.count("= player -> 0") == 8, "neutral provider count")
check(api.count("Objects.requireNonNull(provider)") == 8, "null providers accepted")
check("player.level().isClientSide" in api, "client provider queries allowed")

# Historical code must normalize exactly to the accepted base; no other edits are allowed.
allowed = set(REPLACEMENTS) | ADDITIONS | {"src/main/resources/martial_spells.mixins.json"}
changed = subprocess.check_output(["git", "diff", BASE, "--name-only", "--", "src/main"], cwd=ROOT, text=True).splitlines()
for rel in changed:
    check(rel in allowed, f"unrelated accepted gameplay/resource changed: {rel}")
    if rel in REPLACEMENTS or rel == "src/main/resources/martial_spells.mixins.json":
        check(normalize_ability_hooks(rel, (ROOT / rel).read_text(encoding="utf-8")) == blob(rel).decode().replace("\r\n", "\n"),
              f"edit differs from reviewed neutral insertion: {rel}")
for rel in ADDITIONS:
    check((ROOT / rel).is_file(), f"missing new class {rel}")
for p in (JAVA / "api/RangerCompatibility.java", JAVA / "spells/PaladinPenanceSpell.java",
          JAVA / "spells/PaladinHolySpellSupport.java", JAVA / "spells/BearTrapSpell.java"):
    check(p.read_text(encoding="utf-8") == blob(p.relative_to(ROOT).as_posix()).decode().replace("\r\n", "\n"), f"frozen code changed: {p.name}")
gradle = (ROOT / "build.gradle").read_text(encoding="utf-8")
packaging = '    // Keep the mapped consumer mod\'s strict external mixin remapping available.\n    dependsOn tasks.named(\'compileJava\')\n    from(layout.buildDirectory.file(\'tmp/compileJava/compileJava-refmap.json\')) {\n        rename { "${mod_id}.refmap.json" }\n    }\n'
check(gradle.count(packaging) == 1, "dev publication refmap inclusion missing")
check(gradle.replace(packaging, "", 1) == blob("build.gradle").decode().replace("\r\n", "\n"), "dependency graph changed beyond dev refmap packaging")
check((ROOT / "gradle.properties").read_text(encoding="utf-8") == blob("gradle.properties").decode().replace("\r\n", "\n"), "pinned dependency version changed")

mixin = read("mixin/ApothicPenanceCriticalChanceMixin.java")
context = read("combat/PenanceCritContext.java")
projectile = read("entity/PenanceProjectile.java")
check(mixin.count("@Redirect") == 1 and "ordinal = 0" in mixin, "redirect is not first attribute read only")
for token in ("require = 1, expect = 1, allow = 1", "attribute != ALObjects.Attributes.CRIT_CHANCE.get()",
              "event.getSource(), event.getEntity(), attacker", "attacker instanceof Player player",
              "base + AbilityCompatibility.getPenanceCriticalChanceAdjustment(player)", "return base;"):
    check(token in mixin, f"Penance isolation missing {token}")
check("CRIT_DAMAGE.get()" not in mixin, "crit damage intercepted")
for token in ("scope.source == source", "scope.target == target", "scope.owner == attacker",
              "source.getEntity() == attacker", "ThreadLocal<Scope>", "implements AutoCloseable",
              "ACTIVE.remove()", "ACTIVE.set(previous)"):
    check(token in context, f"scope invariant missing {token}")
check("try (PenanceCritContext.Scope ignored = PenanceCritContext.enter(source, target, owner))" in projectile,
      "Penance context lacks exception-safe ownership")
check("DamageSources.applyDamage(target, damage, source)" in projectile and projectile.count("PenanceCritContext.enter") == 1,
      "Penance direct damage changed or duplicate scope")
for rel in ("api/AbilityCompatibility.java", "combat/PenanceCritContext.java", "mixin/ApothicPenanceCriticalChanceMixin.java", "entity/PenanceProjectile.java"):
    text = read(rel)
    for forbidden in ("nextFloat(", "nextDouble(", "AttributeModifier", "CRIT_DAMAGE.get()", "net.minecraft.client"):
        check(forbidden not in text, f"forbidden crit/client mutation {forbidden} in {rel}")

circle = read("spells/PaladinCircleOfHealingSpell.java")
check(circle.count("getCircleOfHealingPowerAdjustment(player)") == 1, "Circle callback duplicated")
check(circle.count("                healCoefficient\n") == 2, "Circle coefficient missing for self/allies")
shout = read("spells/DemoralizingShoutSpell.java")
check("1.0 + AbilityCompatibility.getShoutDamageMultiplierAdjustment(player)" in shout, "Shout source +4 arithmetic lost")
check("double controlHealthLimit = getControlHealthLimit(caster);" in shout, "Shout debuff power changed")
trap = read("entity/BearTrapEntity.java")
check(trap.count("getBearTrapPowerAdjustment(owner)") == 1, "Bear Trap adjustment repeated")
check("float impactPower = basePower * (float) (1.0 + adjustment)" in trap, "trap power arithmetic")
check("basePower * BearTrapSpell.CONTROL_POWER_MULTIPLIER * (float) adjustment" in trap, "source trap control cap not adjusted")
cooldown = read("events/AbilityCompatibilityCooldownEvents.java")
for registry, concept in (("DIVINE_PROTECTION", "DivineProtection"), ("JUDGEMENT", "Judgement"),
                          ("CHARGE", "Charge"), ("SHADOW_STEP", "ShadowStep")):
    check(f"MartialSpellRegistry.{registry}.get()" in cooldown and f"get{concept}CooldownAdjustment(player)" in cooldown,
          f"cooldown identity {registry}")
check("Math.max(0, event.getEffectiveCooldown() + adjustment)" in cooldown, "cooldown ordering/zero floor")
check("EventPriority.LOW" in cooldown and "adjustment != 0" in cooldown, "cooldown priority/default neutrality")

for rel in ADDITIONS:
    text = (ROOT / rel).read_text(encoding="utf-8")
    for forbidden in ("net.armory", "armory_rpgs:", "getArmorSlots", "ArmorSets", "net.spell_engine", "net.spell_power", "net.minecraft.client"):
        check(forbidden not in text, f"external ownership/client leak {forbidden}: {rel}")
resources = json.loads((ROOT / "src/main/resources/martial_spells.mixins.json").read_text(encoding="utf-8"))
check(resources["mixins"].count("ApothicPenanceCriticalChanceMixin") == 1, "duplicate/missing mixin registration")

# Inspect the exact production jar, not a newer Apothic branch. Verify both attribute reads/order.
cache = Path.home() / ".gradle/caches/modules-2/files-2.1/dev.shadowsoffire/ApothicAttributes/1.20.1-1.3.7"
jars = list(cache.rglob("ApothicAttributes-1.20.1-1.3.7.jar"))
check(len(jars) == 1, "exact Apothic artifact missing/ambiguous")
if len(jars) == 1:
    check(hashlib.sha1(jars[0].read_bytes()).hexdigest() == "1dc0744d17f0861fe6b067798d39abce9d8e0e86", "Apothic artifact changed")
    code = subprocess.check_output(["javap", "-p", "-c", "-classpath", str(jars[0]),
                                   "dev.shadowsoffire.attributeslib.impl.AttributeEvents"], text=True)
    method = code.split("public void apothCriticalStrike(", 1)[1].split("public void vanillaCritDmg", 1)[0]
    reads = re.findall(r"// Method net/minecraft/world/entity/LivingEntity\.m_21133_", method)
    check(len(reads) == 2, "pinned Apothic attribute read count changed")
    check(method.index("Attributes.CRIT_CHANCE") < method.index("Attributes.CRIT_DAMAGE"), "pinned Apothic chance/damage order")
    check("DamageSource.m_7639_" in method, "pinned Apothic attacker accessor changed")
    print("Pinned Apothic 1.3.7 SHA-1 / first chance read / untouched second damage read: inspected")
refmap_path = ROOT / "build/tmp/compileJava/compileJava-refmap.json"
check(refmap_path.is_file(), "build refmap missing; run permitted build")
if refmap_path.is_file():
    maps = json.loads(refmap_path.read_text(encoding="utf-8"))["mappings"].get("com/w0of26/martialspells/mixin/ApothicPenanceCriticalChanceMixin", {})
    check(any("getAttributeValue" in k and "m_21133_" in v for k, v in maps.items()), "vanilla read not SRG-remapped for production")

# Real API/context sources compiled against tiny type fixtures; proves pure invariants, not Minecraft runtime.
fixture = ROOT / "build/ability-compat-scope-test"
src = fixture / "src"
def put(name, text):
    p = src / name; p.parent.mkdir(parents=True, exist_ok=True); p.write_text(text)
put("net/minecraft/world/level/Level.java", "package net.minecraft.world.level; public class Level { public boolean isClientSide; }")
put("net/minecraft/world/entity/LivingEntity.java", "package net.minecraft.world.entity; public class LivingEntity { public net.minecraft.world.level.Level level = new net.minecraft.world.level.Level(); public net.minecraft.world.level.Level level() { return level; } }")
put("net/minecraft/world/entity/player/Player.java", "package net.minecraft.world.entity.player; public class Player extends net.minecraft.world.entity.LivingEntity {}")
put("net/minecraft/world/damagesource/DamageSource.java", "package net.minecraft.world.damagesource; public class DamageSource { private net.minecraft.world.entity.LivingEntity owner; public DamageSource(net.minecraft.world.entity.LivingEntity owner) { this.owner=owner; } public net.minecraft.world.entity.LivingEntity getEntity() {return owner;} }")
for rel in ("api/AbilityCompatibility.java", "combat/PenanceCritContext.java"):
    put("com/w0of26/martialspells/" + rel, read(rel))
test = """import com.w0of26.martialspells.api.AbilityCompatibility;
import com.w0of26.martialspells.combat.PenanceCritContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
public class ScopeTest {
static void require(boolean b) { if (!b) throw new AssertionError(); }
static void rejects(Runnable r) { try { r.run(); throw new AssertionError(); } catch (IllegalArgumentException expected) {} }
public static void main(String[] args) {
Player p = new Player(); LivingEntity t = new LivingEntity(); LivingEntity other = new LivingEntity();
DamageSource a = new DamageSource(p), b = new DamageSource(p);
require(!PenanceCritContext.matches(a,t,p));
try (var outer = PenanceCritContext.enter(a,t,p)) {
 require(PenanceCritContext.matches(a,t,p));
 require(!PenanceCritContext.matches(b,t,p)); require(!PenanceCritContext.matches(a,other,p));
 require(!PenanceCritContext.matches(a,t,other));
 try { try (var inner = PenanceCritContext.enter(b,other,p)) {
   require(!PenanceCritContext.matches(a,t,p)); require(PenanceCritContext.matches(b,other,p));
   throw new RuntimeException();
 } } catch (RuntimeException expected) {}
 require(PenanceCritContext.matches(a,t,p));
}
require(!PenanceCritContext.matches(a,t,p));
"""
for name,kind,value in contracts:
    zero = "0.0" if kind == "double" else "0"
    test += f"require(AbilityCompatibility.get{name}Adjustment(p) == {zero});\n"
    test += f"AbilityCompatibility.set{name}AdjustmentProvider(x -> {value}); require(AbilityCompatibility.get{name}Adjustment(p) == {value});\n"
    test += f"AbilityCompatibility.set{name}AdjustmentProvider(x -> 123); rejects(() -> AbilityCompatibility.get{name}Adjustment(p));\n"
    if kind == "double":
        for invalid in ("Double.NaN", "Double.POSITIVE_INFINITY"):
            test += f"AbilityCompatibility.set{name}AdjustmentProvider(x -> {invalid}); rejects(() -> AbilityCompatibility.get{name}Adjustment(p));\n"
    test += f"AbilityCompatibility.set{name}AdjustmentProvider(x -> {zero});\n"
test += """p.level.isClientSide=true;
try { AbilityCompatibility.getPenanceCriticalChanceAdjustment(p); throw new AssertionError(); }
catch (IllegalStateException expected) {}
System.out.println("Provider values/defaults/client guard and nested identity/exception cleanup PASS");
} }
"""
put("ScopeTest.java", test)
subprocess.run(["javac", "-d", str(fixture / "classes"), *map(str, src.rglob("*.java"))], check=True)
subprocess.run(["java", "-cp", str(fixture / "classes"), "ScopeTest"], check=True)
if errors:
    print("ABILITY COMPAT AUDIT FAIL\n" + "\n".join(errors)); sys.exit(1)
print("ABILITY COMPAT AUDIT PASS: eight isolated hooks, neutral behavior, pinned Apothic redirect/remapping, Ranger preserved")
