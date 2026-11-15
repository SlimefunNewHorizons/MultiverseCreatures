"""
Uploads the head skins of a model sculpted in code (Radahn, drawn with HeadSculpt) and writes the
table the plugin reads their textures from.

A player head only shows a skin that Mojang hosts. `mvn test` writes every skin the model wears to
target/head-skins/<name>.png, each named after its own pixels; this uploads through MineSkin
(unlisted) the ones the table lacks and drops the ones no longer worn. Run it whenever the model's
art changes: RadahnModelTest fails until it has been.

    mvn test -Dtest=StandViewerExportTest
    set MINESKIN_API_KEY=...            (a free key from https://account.mineskin.org)
    python website/models/stand-skins/heads.py
"""

import os
import time
from pathlib import Path

from mineskin import upload

ROOT = Path(__file__).parents[3]
SKINS = ROOT / "target" / "head-skins"
TABLE = ROOT / "src" / "main" / "resources" / "radahn" / "skins.txt"


def write(table, names):
    TABLE.parent.mkdir(parents=True, exist_ok=True)
    TABLE.write_text("".join(f"{name}={table[name]}\n" for name in names if name in table), encoding="utf-8")


def main():
    key = os.environ.get("MINESKIN_API_KEY")
    if not key:
        raise SystemExit("Set MINESKIN_API_KEY to a MineSkin API key first.")
    files = sorted(SKINS.glob("*.png"))
    if not files:
        raise SystemExit(f"No skins in {SKINS}: run `mvn test -Dtest=StandViewerExportTest` first.")
    table = dict(line.split("=", 1) for line in TABLE.read_text(encoding="utf-8").split()) if TABLE.exists() else {}
    names = [path.stem for path in files]
    missing = [path for path in files if path.stem not in table]
    for index, path in enumerate(missing):
        print(f"uploading {path.name} ({index + 1} of {len(missing)})", flush=True)
        table[path.stem], delay = upload(key, path, "msc-" + path.stem)
        # Written after every skin, so a run that is cut short loses nothing.
        write(table, names)
        time.sleep(max(delay, 3.5))
    write(table, names)
    print(f"{len(names)} skins in {TABLE}, {len(missing)} uploaded")


if __name__ == "__main__":
    main()
