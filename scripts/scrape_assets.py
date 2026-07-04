#!/usr/bin/env python3
"""Scrape free, openly-licensed low-poly .glb assets for the game "Tame".

Source: poly.pizza — server-rendered HTML pages hosting CC0 / CC-BY models
(Quaternius' animated animal + nature packs, Kay Lousberg's kits, Google Poly)
as self-contained .glb files on static.poly.pizza.

Why this source: GitHub codeload/release zips (KayKit et al.) return 403
through this environment's egress proxy, and quaternius.com pack pages only
link to Google Drive folders (no direct file URLs), so scraping poly.pizza's
search pages is the approach that actually works end to end.

Pipeline per asset: search page -> pick result by preferred author ->
model page -> extract static.poly.pizza GLB url + license -> download ->
parse GLB binary for animation clip names -> validate (animated assets need
an "idle" clip and a "walk"/"run"/"gallop"/"trot" clip) -> curate into
game/assets/models/ and emit manifest.json + CREDITS.md.

Usage: python3 scripts/scrape_assets.py   [WORK_DIR=<tmp dir> to override]
"""
import json
import os
import re
import shutil
import struct
import sys
import tempfile
import time
from pathlib import Path

import requests

BASE = "https://poly.pizza"
UA = {"User-Agent": "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 TameAssetScraper/1.0"}
REPO = Path(__file__).resolve().parent.parent
ASSETS = REPO / "game" / "assets"
MODELS = ASSETS / "models"
WORK = Path(os.environ.get("WORK_DIR") or Path(tempfile.gettempdir()) / "tame_asset_work")

# slug -> (kind, display name, targetHeight, [(search term, preferred author), ...])
# Authors: Quaternius = CC0, Kay Lousberg (KayKit) = CC0, Poly by Google = CC-BY.
WANTED = {
    "player": ("player", "Adventurer", 1.7, [("Animated Woman", "Quaternius"),
                                             ("Animated Man", "Quaternius"),
                                             ("Adventurer", "Kay Lousberg")]),
    "fox":    ("animal", "Fox", 0.55, [("Fox", "Quaternius")]),
    "wolf":   ("animal", "Wolf", 0.9, [("Wolf", "Quaternius")]),
    "deer":   ("animal", "Deer", 1.4, [("Deer", "Quaternius")]),
    "stag":   ("animal", "Stag", 1.4, [("Stag", "Quaternius")]),
    "horse":  ("animal", "Horse", 1.6, [("Horse", "Quaternius")]),
    "sheep":  ("animal", "Sheep", 0.8, [("Sheep", "Quaternius")]),
    "pig":    ("animal", "Pig", 0.8, [("Pig", "Quaternius")]),
    "pine_tree":    ("prop", "Pine Tree", 6.0, [("Pine", "Quaternius")]),
    "twisted_tree": ("prop", "Twisted Tree", 5.0, [("Twisted Tree", "Quaternius")]),
    "rock_big":     ("prop", "Big Rock", 1.2, [("Rock Big", "Quaternius"), ("Boulder", "Quaternius"), ("Rock", "Quaternius")]),
    "rock_small":   ("prop", "Small Rock", 0.5, [("Rock Small", "Quaternius"), ("Rock Medium", "Quaternius")]),
    "bush":         ("prop", "Bush", 0.9, [("Bush", "Quaternius"), ("Bush", "Kay Lousberg")]),
    "flowers":      ("prop", "Flowers", 0.4, [("Flowers", "Quaternius"), ("Flower", "Quaternius")]),
    "grass":        ("prop", "Grass", 0.4, [("Grass", "Quaternius")]),
}
LOCOMOTION = ("walk", "run", "gallop", "trot")


def http_get(url, tries=3):
    for i in range(tries):
        try:
            r = requests.get(url, headers=UA, timeout=30)
            if r.status_code == 200:
                return r
        except requests.RequestException:
            pass
        time.sleep(1 + i)
    raise RuntimeError(f"GET failed: {url}")


def search(term):
    """Scrape a poly.pizza search results page -> [(model_id, title, author)]."""
    html = http_get(f"{BASE}/search/{requests.utils.quote(term)}").text
    out, seen = [], set()
    for mid, title, author in re.findall(
            r'href="/m/([A-Za-z0-9]+)">([^<]+)</a>(?:(?!href="/m/).)*?href="/u/[^"]+">([^<]+)</a>',
            html, re.S):
        if mid not in seen:
            seen.add(mid)
            out.append((mid, title.strip(), author.strip()))
    return out


def model_page(mid):
    """Scrape a model page -> dict(glb_url, license, animated, page, author)."""
    page = f"{BASE}/m/{mid}"
    html = http_get(page).text
    glb = re.search(r'https://static\.poly\.pizza/[a-f0-9-]+\.glb', html)
    if "publicdomain/zero" in html or ">CC0<" in html or "\"CC0\"" in html:
        lic = "CC0"
    elif "licenses/by/" in html:
        lic = "CC-BY"
    else:
        lic = None  # unusable license -> caller skips
    author = re.search(r'href="/u/[^"]+">([^<]+)</a>', html)
    return {"glb_url": glb.group(0) if glb else None, "license": lic,
            "animated": '"Animated":true' in html, "page": page,
            "author": author.group(1).strip() if author else "?"}


def glb_animations(path):
    """Parse a .glb binary; return animation clip names. Raises if not GLB."""
    buf = Path(path).read_bytes()
    if buf[:4] != b"glTF":
        raise ValueError(f"{path} is not a binary glTF")
    jlen, jtype = struct.unpack("<II", buf[12:20])
    if jtype != 0x4E4F534A:  # 'JSON'
        raise ValueError(f"{path}: first chunk is not JSON")
    data = json.loads(buf[20:20 + jlen])
    return [a.get("name", f"clip{i}") for i, a in enumerate(data.get("animations", []))]


def clips_ok(anims):
    low = [a.lower() for a in anims]
    return (any("idle" in a for a in low)
            and any(w in a for a in low for w in LOCOMOTION))


def acquire(slug, kind, height, candidates):
    """Try search candidates until one downloads and validates; return entry."""
    for term, author in candidates:
        for mid, title, hit_author in search(term)[:6]:
            if author.lower() not in hit_author.lower():
                continue
            if title.lower() != term.lower() and kind != "prop":
                continue  # animals/player: exact title only, avoid e.g. "Fox Hat"
            info = model_page(mid)
            if not info["glb_url"] or info["license"] is None:
                continue
            if kind in ("player", "animal") and not info["animated"]:
                continue
            dest = WORK / f"{slug}.glb"
            dest.write_bytes(http_get(info["glb_url"]).content)
            try:
                anims = glb_animations(dest)
            except ValueError as e:
                print(f"  ! {slug}: {e}", file=sys.stderr)
                continue
            if kind in ("player", "animal") and not clips_ok(anims):
                print(f"  ! {slug} ({title} by {hit_author}): missing idle/walk clips {anims}", file=sys.stderr)
                continue
            time.sleep(0.3)  # be polite
            return {"slug": slug, "kind": kind, "title": title, "height": height,
                    "anims": anims, "author": info["author"], "page": info["page"],
                    "license": info["license"], "tmp": dest,
                    "size": dest.stat().st_size}
        time.sleep(0.3)
    print(f"  ! no usable model found for {slug}", file=sys.stderr)
    return None


def main():
    WORK.mkdir(parents=True, exist_ok=True)
    MODELS.mkdir(parents=True, exist_ok=True)
    got, seen_pages = [], set()
    for slug, (kind, name, height, candidates) in WANTED.items():
        print(f"[{kind}] {slug} ...")
        e = acquire(slug, kind, height, candidates)
        if e and e["page"] not in seen_pages:  # no duplicate models under two slugs
            seen_pages.add(e["page"])
            e["name"] = name
            got.append(e)
            print(f"  = {e['title']} by {e['author']} [{e['license']}] "
                  f"{e['size']//1024} KB, clips={e['anims']}")

    manifest = {"player": None, "animals": [], "props": []}
    for e in got:
        shutil.copyfile(e["tmp"], MODELS / f"{e['slug']}.glb")
        entry = {"name": e["name"], "file": f"models/{e['slug']}.glb",
                 "targetHeight": e["height"]}
        if e["kind"] in ("player", "animal"):
            entry["animations"] = e["anims"]
        entry["source"] = e["page"]
        entry["license"] = e["license"]
        if e["kind"] == "player":
            manifest["player"] = entry
        else:
            manifest["animals" if e["kind"] == "animal" else "props"].append(entry)
    (ASSETS / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n")

    rows = "".join(
        f"| {e['name']} (`models/{e['slug']}.glb`) | {e['author']} | {e['page']} | {e['license']} |\n"
        for e in got)
    (ASSETS / "CREDITS.md").write_text(
        "# Asset Credits\n\n"
        "All models were downloaded from [poly.pizza](https://poly.pizza), which mirrors\n"
        "the authors' free packs as self-contained `.glb` files.\n"
        "CC0 assets require no attribution (credited anyway, with thanks).\n\n"
        "| Asset | Author | Source | License |\n|---|---|---|---|\n" + rows +
        "\nLicenses: [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/), "
        "[CC BY 3.0](https://creativecommons.org/licenses/by/3.0/)\n")

    total = sum(e["size"] for e in got)
    n_anim = sum(1 for e in got if e["kind"] == "animal")
    print(f"\nDone: player={'yes' if manifest['player'] else 'MISSING'}, "
          f"{n_anim} animals, {len(manifest['props'])} props, "
          f"total {total/1e6:.1f} MB -> {MODELS}")
    if not manifest["player"] or n_anim < 4:
        sys.exit("ERROR: minimum asset set not met")


if __name__ == "__main__":
    main()
