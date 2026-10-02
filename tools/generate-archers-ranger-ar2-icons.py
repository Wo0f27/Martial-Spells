#!/usr/bin/env python3
"""Legacy generator for AR2's original substitute icons.

Exact private-use source icons now come from import-archers-ranger-ar2-presentation.py.
This script intentionally does not rewrite any presentation-parity asset.
"""

from pathlib import Path
import struct
import zlib

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/martial_spells/textures"


def chunk(kind, payload):
    return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", zlib.crc32(kind + payload) & 0xFFFFFFFF)


def draw_icon(path, kind):
    pixels = []
    for y in range(16):
        row = bytearray()
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            radius = dx * dx + dy * dy
            if kind in ("shot", "stash"):
                ring = 25 <= radius <= 45
                shaft = abs(y - 8) <= 1 and 3 <= x <= 11
                head = 10 <= x <= 13 and abs(y - 8) <= 13 - x
                color = (160, 35, 45, 255) if ring else (245, 107, 83, 255) if shaft or head else (0, 0, 0, 0)
            elif kind == "mark":
                ring = 20 <= radius <= 34
                cross = (abs(dx) <= 0.5 and abs(dy) <= 5) or (abs(dy) <= 0.5 and abs(dx) <= 5)
                color = (206, 50, 54, 255) if ring else (255, 164, 102, 255) if cross else (0, 0, 0, 0)
            else:
                left = abs(x - (5 + (y // 4) % 3)) <= 1 and 3 <= y <= 13
                right = abs(x - (11 - (y // 4) % 3)) <= 1 and 3 <= y <= 13
                knot = 5 <= x <= 10 and 8 <= y <= 10
                color = (62, 133, 58, 255) if left or right else (120, 177, 82, 255) if knot else (0, 0, 0, 0)
            row.extend(color)
        pixels.append(b"\0" + bytes(row))
    png = (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
           + chunk(b"IDAT", zlib.compress(b"".join(pixels), 9)) + chunk(b"IEND", b""))
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)


if __name__ == "__main__":
    raise SystemExit("AR2 uses exact frozen icons; run import-archers-ranger-ar2-presentation.py")
