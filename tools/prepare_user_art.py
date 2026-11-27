#!/usr/bin/env python3
"""
Prepares Sri's own Iron Man images (user-assets/ironman, personal use, NOT in git) as ready-to-draw
1920x1080 TV backdrops in app/src/user-assets-build/ironman/ (also NOT in git).

- hero_father.jpg / hero_my.jpg: "split hero" — the artwork is scaled DOWN onto the right two-thirds
  and fades into black on its left and bottom, so the character is never under the title or clock.
- showdown.jpg / crimson_duo.jpg: full-bleed 16:9 crops for the gallery and screensaver.
Images are never upscaled beyond their source resolution; files too small for 1080p are skipped.

Usage: python3 tools/prepare_user_art.py
"""
import json
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "user-assets" / "ironman"
OUT = ROOT / "app" / "src" / "user-assets-build" / "ironman"
W, H = 1920, 1080


def split(path, width, top, src_crop=None):
    art = Image.open(path).convert("RGB")
    if src_crop:
        art = art.crop(src_crop)
    h = int(width * art.height / art.width)
    art = art.resize((width, h), Image.LANCZOS)  # always a downscale here (sources are >= 1920 wide)
    mask = Image.new("L", (width, h), 255)
    px = mask.load()
    for x in range(width):
        for y in range(h):
            px[x, y] = int(255 * min(1.0, x / (width * 0.32)) * min(1.0, (h - y) / (h * 0.30)))
    bg = Image.new("RGB", (W, H), (6, 6, 8))
    bg.paste(art, (W - width, top), mask)
    return bg


def fill(path, centering=(0.5, 0.4)):
    from PIL import ImageOps
    im = Image.open(path).convert("RGB")
    if im.width < W or im.height < H:
        return None
    return ImageOps.fit(im, (W, H), Image.LANCZOS, centering=centering)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    jobs = [
        ("hero_father", "Iron Man · Vengeance", lambda: split(SRC / "iron-man-divine-vengeance-hl-1920x1080.jpg", 1240, 160)),
        ("hero_my", "Iron Man · Shadow", lambda: split(SRC / "iron-man-doctor-3840x2160-18335.jpg", 1300, 170, (0, 0, 3072, 2160))),
        ("showdown", "Iron Man · Showdown", lambda: fill(SRC / "iron-man-vs-doctor-3840x2160-25811.jpg")),
        ("crimson_duo", "Crimson Duo", lambda: fill(SRC / "moon-knight-x-iron-man-ma.jpg", (0.6, 0.4))),
        # Full-screen versions of the two hero images, for the Wallpapers picker and screensaver slideshow.
        ("vengeance_full", "Vengeance · Full screen", lambda: fill(SRC / "iron-man-divine-vengeance-hl-1920x1080.jpg")),
        ("shadow_full", "Shadow · Full screen", lambda: fill(SRC / "iron-man-doctor-3840x2160-18335.jpg")),
    ]
    meta = []
    for key, name, make in jobs:
        try:
            img = make()
        except FileNotFoundError:
            print("missing source for", key)
            continue
        if img is None:
            print("skipped (too small for 1080p):", key)
            continue
        img.save(OUT / f"{key}.jpg", quality=88, optimize=True, progressive=True)
        meta.append({"file": f"ironman/{key}.jpg", "id": f"ironman_{key}", "name": name,
                     "category": "Iron Man", "credit": "Your own image · personal use on this TV"})
        print("ok", key, (OUT / f"{key}.jpg").stat().st_size // 1024, "KB")
    (OUT / "user_art.json").write_text(json.dumps(meta, indent=1, ensure_ascii=False))


if __name__ == "__main__":
    main()
