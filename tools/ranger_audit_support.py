"""Final integration scope shared by historical Ranger checkpoint audits."""
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
ACCEPTED_BASE = "7e3653d60a5ff56c1da6f4aab1cafd4bf1c1fc41"
PRIVATE_COMMIT = "f1f9ad5306d31ec988ede1bb7457f87043495d1b"


def blob(revision, path):
    return subprocess.check_output(["git", "show", f"{revision}:{path}"], cwd=ROOT)


def normalized_barrage(source):
    """Permit only the AR6 default-neutral compatibility call and its import."""
    return source.replace(
        "import com.w0of26.martialspells.api.RangerCompatibility;\n", ""
    ).replace(
        "DAMAGE_MULTIPLIER, 0,\n"
        "                RangerCompatibility.extraBarragePenetrableTargets(player), true, true",
        "DAMAGE_MULTIPLIER, 0, 0, true, true",
    )


def accepted_gameplay_unchanged(relative):
    path = f"src/main/java/com/w0of26/martialspells/{relative}"
    current = (ROOT / path).read_text(encoding="utf-8")
    accepted = blob(ACCEPTED_BASE, path).decode().replace("\r\n", "\n")
    if relative == "spells/BarrageSpell.java":
        current = normalized_barrage(current)
    return current == accepted


def private_asset_errors():
    """The 106 accepted private assets must remain tracked, unchanged, unstaged."""
    files = subprocess.check_output([
        "git", "diff", "--name-only", "--diff-filter=A",
        "92e9f34a6af8bd2eaa673ff98a3e41b7a3e4f494", PRIVATE_COMMIT,
        "--", "src/main/resources",
    ], cwd=ROOT, text=True).splitlines()
    private = {p for p in files if Path(p).suffix in (".png", ".ogg")
               and "spirit_wolf" not in p}
    tracked = set(subprocess.check_output(["git", "ls-files"], cwd=ROOT, text=True).splitlines())
    staged = set(subprocess.check_output(
        ["git", "diff", "--cached", "--name-only"], cwd=ROOT, text=True).splitlines())
    errors = []
    if len(private) != 106:
        errors.append("accepted private inventory must contain 106 assets")
    for rel in sorted(private):
        path = ROOT / rel
        if rel not in tracked or not path.is_file() or path.read_bytes() != blob(PRIVATE_COMMIT, rel):
            errors.append(f"accepted private asset missing/untracked/changed: {rel}")
        if rel in staged:
            errors.append(f"unrelated private asset staged: {rel}")
    return errors
