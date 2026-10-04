"""Import only Spirit Wolf's private-use presentation assets from frozen revisions."""
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/martial_spells"
ARCHERS = Path(r"C:\Modding\RPG-Ports\upstream\Archers")
ENGINE = Path(r"C:\Modding\RPG-Ports\upstream\SpellEngine")


def import_blob(repository: Path, revision: str, source: str, target: str,
                strip_blank_whitespace: bool = False) -> None:
    data = subprocess.check_output(
        ["git", "-c", f"safe.directory={repository.as_posix()}", "show", f"{revision}:{source}"],
        cwd=repository,
    )
    if strip_blank_whitespace:
        data = b"\n".join(line.rstrip() for line in data.splitlines()) + b"\n"
    destination = ASSETS / target
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(data)
    print(f"{source} -> {destination.relative_to(ROOT)}")


ARCHERS_SHA = "ee8fb4081422ba0ab8ced57fe773a1c048d7bf74"
ENGINE_SHA = "d057c13b434cadc8eeaea6df8deab6992f8bda7e"
prefix = "common/src/main/resources/assets/archers/"
for source, destination in (
    ("textures/spell/spirit_wolf.png", "textures/gui/spell_icons/spirit_wolf.png"),
    ("textures/entity/direwolf_spell.png", "textures/entity/spirit_wolf.png"),
    ("sounds/spirit_wolf_spawn.ogg", "sounds/spirit_wolf_spawn.ogg"),
    ("sounds/spirit_wolf_summon_1.ogg", "sounds/spirit_wolf_summon_1.ogg"),
    ("sounds/spirit_wolf_summon_2.ogg", "sounds/spirit_wolf_summon_2.ogg"),
):
    import_blob(ARCHERS, ARCHERS_SHA, prefix + source, destination)
import_blob(ENGINE, ENGINE_SHA,
            "common/src/main/resources/assets/spell_engine/player_animations/one_handed_healing_release.json",
            "player_animation/one_handed_healing_release.json", strip_blank_whitespace=True)
