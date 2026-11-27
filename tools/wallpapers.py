#!/usr/bin/env python3
"""
Wallpaper pipeline for ARJUN VISION TV (Wikimedia Commons official API only, no scraping).

  candidates  <out_dir>          search each theme term, keep only free licences
                                 (CC0, public domain, CC BY, CC BY-SA; never NC/ND),
                                 >= 2400 px wide, landscape; save thumbnails + contact sheets
  build       <picks.json> <assets_dir>
                                 download the picked files, centre/focal crop to 1920x1080,
                                 save JPEG + wallpapers.json (title, creator, licence, source URL)

Every bundled image keeps its attribution; the app shows it on screen.
"""
import io, json, re, sys, time, urllib.error, urllib.parse, urllib.request
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageOps

UA = "ArjunVisionTV/0.3 (personal TV launcher wallpaper curation)"
API = "https://commons.wikimedia.org/w/api.php"

TERMS = {
    "nature": ["mountain lake sunrise", "milky way mountains night", "ocean coast sunset rocks", "misty forest sunlight", "northern lights aurora"],
    "futuristic": ["city skyline night lights", "neon city night street", "galaxy nebula", "modern architecture night"],
    "gaming": ["neon lights purple blue", "tropical sunset palm silhouette", "light trails highway night"],
    "seasonal": ["christmas lights snow night", "diwali diya lamps night", "pongal festival", "fireworks night sky", "kolam rangoli"],
}

FREE = re.compile(r"^(cc0|public domain|pd|cc by(-sa)? [0-9.]+|cc by(-sa)?)", re.I)


def _open(url, timeout):
    """GET with polite back-off: Wikimedia answers 429 when we go too fast."""
    for attempt in range(6):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": UA})
            return urllib.request.urlopen(req, timeout=timeout).read()
        except urllib.error.HTTPError as e:
            if e.code != 429 or attempt == 5:
                raise
            wait = int(e.headers.get("Retry-After") or 0) or 10 * (attempt + 1)
            print(f"  429 rate limit, waiting {wait}s", flush=True)
            time.sleep(wait)


def get_json(params):
    return json.loads(_open(API + "?" + urllib.parse.urlencode(params), 30))


def fetch(url):
    return _open(url, 60)


def strip_html(s):
    return re.sub(r"\s+", " ", re.sub(r"<[^>]+>", "", s or "")).strip()


def search(term, width):
    d = get_json({
        "action": "query", "format": "json", "formatversion": 2,
        "generator": "search", "gsrnamespace": 6, "gsrlimit": 40,
        "gsrsearch": term + " filetype:bitmap",
        "prop": "imageinfo", "iiprop": "url|size|extmetadata", "iiurlwidth": width,
    })
    out = []
    for p in d.get("query", {}).get("pages", []):
        i = p["imageinfo"][0]
        m = i.get("extmetadata", {})
        lic = m.get("LicenseShortName", {}).get("value", "")
        if "nc" in lic.lower().replace("-", " ").split() or "nd" in lic.lower().replace("-", " ").split():
            continue
        if not FREE.match(lic.strip()):
            continue
        if i["width"] < 2400 or i["width"] < i["height"] * 1.4:
            continue
        out.append({
            "title": p["title"][5:],
            "thumb": i.get("thumburl"),
            "width": i["width"], "height": i["height"],
            "license": lic,
            "license_url": m.get("LicenseUrl", {}).get("value", ""),
            "creator": strip_html(m.get("Artist", {}).get("value", "")) or "Unknown",
            "page": i.get("descriptionurl"),
        })
    return out


def candidates(out_dir):
    out = Path(out_dir); out.mkdir(parents=True, exist_ok=True)
    all_c = {}
    for cat, terms in TERMS.items():
        items = []
        for t in terms:
            for c in search(t, 480)[:6]:
                if c["title"] not in {x["title"] for x in items}:
                    c["term"] = t
                    items.append(c)
            time.sleep(0.5)
        for n, c in enumerate(items):
            c["id"] = f"{cat}{n:02d}"
            try:
                img = Image.open(io.BytesIO(fetch(c["thumb"]))).convert("RGB")
                img = ImageOps.fit(img, (480, 270))
                img.save(out / f"{c['id']}.jpg", quality=80)
            except Exception as e:
                c["error"] = str(e)
            time.sleep(0.2)
        all_c[cat] = items
        sheet(out, cat, [c for c in items if "error" not in c])
    (out / "candidates.json").write_text(json.dumps(all_c, indent=1))
    print({k: len(v) for k, v in all_c.items()})


def sheet(out, cat, items, cols=4):
    rows = (len(items) + cols - 1) // cols
    S = Image.new("RGB", (cols * 490, max(1, rows) * 300), (20, 20, 20))
    d = ImageDraw.Draw(S)
    try:
        f = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 22)
    except Exception:
        f = ImageFont.load_default()
    for n, c in enumerate(items):
        x, y = (n % cols) * 490 + 5, (n // cols) * 300 + 5
        S.paste(Image.open(out / f"{c['id']}.jpg"), (x, y))
        d.rectangle([x, y + 240, x + 480, y + 270], fill=(0, 0, 0))
        d.text((x + 6, y + 243), f"{c['id']}  {c['license']}", fill=(255, 255, 0), font=f)
    S.save(out / f"sheet_{cat}.jpg", quality=85)


def focal_crop(img, w=1920, h=1080, bias_y=0.45):
    # scale to cover, then crop with a slight upward bias (sky/subject usually above centre)
    s = max(w / img.width, h / img.height)
    img = img.resize((round(img.width * s), round(img.height * s)), Image.LANCZOS)
    left = (img.width - w) // 2
    top = int((img.height - h) * bias_y)
    return img.crop((left, top, left + w, top + h))


def build(picks_path, assets_dir):
    picks = json.loads(Path(picks_path).read_text())
    out = Path(assets_dir); out.mkdir(parents=True, exist_ok=True)
    meta = []
    for p in picks:
        info = get_json({
            "action": "query", "format": "json", "formatversion": 2, "titles": "File:" + p["title"],
            "prop": "imageinfo", "iiprop": "url|size|extmetadata", "iiurlwidth": 2560,
        })["query"]["pages"][0]["imageinfo"][0]
        m = info.get("extmetadata", {})
        lic = m.get("LicenseShortName", {}).get("value", "")
        assert FREE.match(lic.strip()), f"licence changed: {p['title']} {lic}"
        name = p["id"] + ".jpg"
        if not (out / name).exists():  # resumable: already-downloaded files are kept
            img = Image.open(io.BytesIO(fetch(info["thumburl"]))).convert("RGB")
            img = focal_crop(img, bias_y=p.get("bias_y", 0.45))
            img.save(out / name, quality=84, optimize=True, progressive=True)
            time.sleep(2.0)
        meta.append({
            "file": name, "id": p["id"], "name": p["name"], "category": p["category"],
            "title": p["title"], "creator": strip_html(m.get("Artist", {}).get("value", "")) or "Unknown",
            "license": lic, "licenseUrl": m.get("LicenseUrl", {}).get("value", ""),
            "source": "Wikimedia Commons", "page": info.get("descriptionurl"),
        })
        print("ok", name, lic, (out / name).stat().st_size // 1024, "KB")
        time.sleep(0.5)
    (out / "wallpapers.json").write_text(json.dumps(meta, indent=1, ensure_ascii=False))


if __name__ == "__main__":
    {"candidates": candidates, "build": build}[sys.argv[1]](*sys.argv[2:])
