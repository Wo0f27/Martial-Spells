#!/usr/bin/env python3
"""Import private AR2 presentation assets from the exact frozen source revisions.

This private port follows PALADIN_PRIEST_SPELL_PORT.md's source-asset policy.
Do not run this for a public redistribution without clearing source rights.
"""

from pathlib import Path
import json
import subprocess

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/martial_spells"
ARCHERS = Path(r"C:\Modding\RPG-Ports\upstream\Archers")
ENGINE = Path(r"C:\Modding\RPG-Ports\upstream\SpellEngine")
ARCHERS_SHA = "ee8fb4081422ba0ab8ced57fe773a1c048d7bf74"
ENGINE_SHA = "d057c13b434cadc8eeaea6df8deab6992f8bda7e"


def frozen_blob(repo, revision, path):
    return subprocess.run(["git", "-c", f"safe.directory={repo}", "-C", str(repo),
                           "show", f"{revision}:{path}"],
                          check=True, capture_output=True).stdout


def copy(repo, revision, source, target):
    dest = ASSETS / target
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_bytes(frozen_blob(repo, revision, source))
    print(f"{repo.name}@{revision[:12]}:{source} -> {dest.relative_to(ROOT)}")


archers_base = "common/src/main/resources/assets/archers"
for name in ("power_shot", "entangling_roots"):
    copy(ARCHERS, ARCHERS_SHA, f"{archers_base}/textures/spell/{name}.png",
         f"textures/gui/spell_icons/{name}.png")
for source, target in (
    ("textures/mob_effect/hunters_mark_stash.png", "textures/mob_effect/power_shot_stash.png"),
    ("textures/mob_effect/hunters_mark.png", "textures/mob_effect/hunters_mark.png"),
    ("textures/mob_effect/entangling_roots.png", "textures/mob_effect/entangling_roots.png"),
    ("textures/spell_effect/hunters_mark.png", "textures/spell_effect/hunters_mark.png"),
    ("textures/spell_effect/entangling_roots.png", "textures/spell_effect/entangling_roots.png"),
    ("sounds/marker_shot.ogg", "sounds/marker_shot.ogg"),
    ("sounds/entangling_roots.ogg", "sounds/entangling_roots.ogg"),
):
    copy(ARCHERS, ARCHERS_SHA, f"{archers_base}/{source}", target)

for name in ("hunters_mark", "entangling_roots"):
    model = json.loads(frozen_blob(
        ARCHERS, ARCHERS_SHA, f"{archers_base}/models/spell_effect/{name}.json"))
    model["textures"] = {key: value.replace("archers:", "martial_spells:")
                         for key, value in model["textures"].items()}
    dest = ASSETS / f"models/spell_effect/{name}.json"
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")

engine_base = "common/src/main/resources/assets/spell_engine/textures/particle"
for i in range(8):
    source = f"magic/vertical_stripe_{i}.png"
    copy(ENGINE, ENGINE_SHA, f"{engine_base}/{source}",
         f"textures/particle/ranger_source/{source}")
for i in range(14):
    source = f"misc/roots_{i}.png"
    copy(ENGINE, ENGINE_SHA, f"{engine_base}/{source}",
         f"textures/particle/ranger_source/{source}")
