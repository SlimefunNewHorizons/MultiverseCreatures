#!/usr/bin/env python3
"""Assembles the GitHub Pages site into _site/.

    mvn test                      # writes target/stand-viewer/models.json
    python tools/site/build.py    # docs/ + the viewer's data -> _site/
    python -m http.server -d _site

What goes in: everything in docs/, the model data `mvn test` exports (rounded, and without the
Destroyer's high-detail variant, which is seven megabytes on its own) and the painted Stand skins.
The server monitor's page also goes in (monitor/): empty on its own, it only draws a snapshot when it is
opened with the link /msc tps hands an admin, whose key is in the fragment and never reaches any server.
What never goes in: Mojang's textures (the viewer draws stand-in blocks instead).
"""
import json
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "_site"
MODELS = ROOT / "target" / "stand-viewer" / "models.json"
SKINS = ROOT / "tools" / "stand-skins" / "out"


def main() -> int:
    if not MODELS.exists():
        print(f"{MODELS} is missing: run `mvn test` first.", file=sys.stderr)
        return 1
    if OUT.exists():
        shutil.rmtree(OUT)
    shutil.copytree(ROOT / "docs", OUT)
    (OUT / ".nojekyll").write_text("")
    # The monitor's page is an empty shell without a snapshot link (the data and its key are only in
    # the link /msc tps hands an admin), so it can be published for the snapshot mode to open.
    (OUT / "monitor").mkdir()
    shutil.copy2(ROOT / "src" / "main" / "resources" / "monitor" / "index.html", OUT / "monitor" / "index.html")

    data = json.loads(MODELS.read_text(encoding="utf-8"), parse_float=lambda s: round(float(s), 4))
    data["stands"] = [m for m in data["stands"] if not m["key"].endswith("-detailed")]
    viewer = OUT / "viewer"
    (viewer / "models.json").write_text(json.dumps(data, separators=(",", ":")), encoding="utf-8")

    skins = viewer / "skins"
    for f in SKINS.rglob("*"):
        if f.is_file() and f.suffix in (".png", ".json") and f.name != "textures.json":
            target = skins / f.relative_to(SKINS)
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(f, target)

    total = sum(f.stat().st_size for f in OUT.rglob("*") if f.is_file())
    print(f"_site ready: {len(data['stands'])} models, {total / 1048576:.1f} MB")
    return 0


if __name__ == "__main__":
    sys.exit(main())
