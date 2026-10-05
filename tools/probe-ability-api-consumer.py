"""Compile an isolated consumer against the Maven Local mapped ability and Ranger APIs."""
from pathlib import Path
import argparse
import json
import os
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument("--maven-local", type=Path,
                    default=Path(os.environ["USERPROFILE"]) / ".m2/repository")
args = parser.parse_args()
properties = dict(line.split("=", 1) for line in
                  (ROOT / "gradle.properties").read_text().splitlines()
                  if "=" in line and not line.lstrip().startswith("#"))
group, artifact, version = (properties[k] for k in ("mod_group_id", "mod_id", "mod_version"))
coordinate = f"{group}:{artifact}:{version}"
repository = args.maven_local.resolve()
artifact_dir = repository / group.replace(".", "/") / artifact / version
api_class = "com/w0of26/martialspells/api/AbilityCompatibility.class"
for suffix in ("", "-dev", "-sources"):
    jar = artifact_dir / f"{artifact}-{version}{suffix}.jar"
    with zipfile.ZipFile(jar) as archive:
        expected = api_class.replace(".class", ".java") if suffix == "-sources" else api_class
        assert expected in archive.namelist(), f"public API missing from {jar}"
        ranger = "com/w0of26/martialspells/api/RangerCompatibility" + (".java" if suffix == "-sources" else ".class")
        assert ranger in archive.namelist(), f"Ranger API missing from {jar}"
        if suffix != "-sources":
            assert "META-INF/mods.toml" in archive.namelist()
            refmap = json.loads(archive.read("martial_spells.refmap.json"))
            assert "com/w0of26/martialspells/mixin/ApothicPenanceCriticalChanceMixin" in refmap["mappings"]
            assert "assets/martial_spells/textures/gui/spell_icons/spirit_wolf.png" in archive.namelist()

fixture = ROOT / "build/ability-consumer"
source = fixture / "src/main/java/ConsumerProbe.java"
source.parent.mkdir(parents=True, exist_ok=True)
source.write_text('''import com.w0of26.martialspells.api.RangerCompatibility;
import com.w0of26.martialspells.api.AbilityCompatibility;

public final class ConsumerProbe {
    public static void install() {
        AbilityCompatibility.setCircleOfHealingPowerAdjustmentProvider(player -> 0.10);
        AbilityCompatibility.setPenanceCriticalChanceAdjustmentProvider(player -> 0.05);
        AbilityCompatibility.setDivineProtectionCooldownAdjustmentProvider(player -> -100);
        AbilityCompatibility.setJudgementCooldownAdjustmentProvider(player -> -60);
        AbilityCompatibility.setChargeCooldownAdjustmentProvider(player -> -40);
        AbilityCompatibility.setShoutDamageMultiplierAdjustmentProvider(player -> 4.0);
        AbilityCompatibility.setShadowStepCooldownAdjustmentProvider(player -> -60);
        AbilityCompatibility.setBearTrapPowerAdjustmentProvider(player -> 0.10);
        RangerCompatibility.setBarrageExtraPenetrableTargetsProvider(player -> 0);
        RangerCompatibility.setBarrageExtraPenetrableTargetsProvider(player -> 1);
        RangerCompatibility.setSpiritWolfCooldownAdjustmentProvider(player -> 0);
        RangerCompatibility.setSpiritWolfCooldownAdjustmentProvider(player -> -100);
    }
}
''', encoding="utf-8")
classpath = (ROOT / "build/ar6-consumer-classpath.txt").read_text().split(os.pathsep)
assert all("build/classes/java/main" not in p.replace("\\", "/") for p in classpath)
fixture.joinpath("settings.gradle").write_text("rootProject.name = 'ability-api-consumer-probe'\n")
fixture.joinpath("build.gradle").write_text(
    "plugins { id 'java' }\njava.toolchain.languageVersion = JavaLanguageVersion.of(17)\n"
    "repositories { mavenLocal() }\n"
    "dependencies {\n"
    f"    implementation '{coordinate}:dev'\n"
    "    compileOnly files(" + ", ".join("'" + p.replace("\\", "/").replace("'", "\\'") + "'"
                                         for p in classpath) + ")\n}\n"
    "tasks.register('verifyPublishedApi') {\n"
    "    doLast {\n"
    "        def jars = configurations.runtimeClasspath.files\n"
    "        assert jars.size() == 1\n"
    "        assert jars.first().canonicalPath.startsWith(System.getProperty('maven.repo.local'))\n"
    "        println 'API dependency resolved exclusively from Maven Local: ' + jars.first()\n"
    "    }\n}\n", encoding="utf-8")
command = [str(ROOT / "gradlew.bat"), "-p", str(fixture),
           f"-Dmaven.repo.local={repository}", "compileJava", "verifyPublishedApi", "--offline", "--no-daemon"]
subprocess.run(command, cwd=ROOT, check=True)
fixture.joinpath("result.json").write_text(json.dumps({
    "coordinate": coordinate, "repository": str(repository),
    "public_api_compile": "PASS", "artifacts": "main/dev/sources PASS",
}, indent=2), encoding="utf-8")
print("Ability + Ranger external API compile probe PASS")
