#!/usr/bin/env python3
"""Builds app/src/main/assets/initial_sunnah_items.json from content_source.json.

Hand-authored duas are copied as-is. Items with a "quran" block get their Uthmani text,
transliteration and Saheeh International translation from the AlQuran.cloud API
(Tanzil-based), so Quranic text is never typed by hand. A dua with an "audioQuran" block
gets the verses it quotes attached as ayahs, so its "Play Audio" recites those verses.

Usage: python3 tools/seed/generate_seed.py
Every item must carry a deed priority, a one-line hadith virtue with its reference, and a
Sabab & Adab tip; the script refuses to write a bundle that is missing any of them.

Run it whenever content_source.json changes, review the diff, and commit the output.
Bump "version" in content_source.json and ContentSeeder.SEED_VERSION together.
"""

import json
import re
import sys
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SOURCE = Path(__file__).resolve().parent / "content_source.json"
OUTPUT = ROOT / "app" / "src" / "main" / "assets" / "initial_sunnah_items.json"

API = "https://api.alquran.cloud/v1"
EDITIONS = ("quran-uthmani", "en.transliteration", "en.sahih")
AYAH_AUDIO = "https://cdn.islamic.network/quran/audio/128/ar.alafasy/{number}.mp3"
SURAH_AUDIO = "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/{surah}.mp3"

# Sanity check against the canonical verse counts.
EXPECTED_AYAH_COUNT = {1: 7, 2: 286, 18: 110, 43: 89, 56: 96, 67: 30, 112: 4, 113: 5, 114: 6}

# Items with more ayahs than this keep only the opening ayah as their item-level preview.
INLINE_TEXT_MAX_AYAHS = 20

ARABIC_INDIC_DIGITS = str.maketrans("0123456789", "٠١٢٣٤٥٦٧٨٩")

REQUIRED_TEXT_FIELDS = ("title", "hadithVirtue", "virtueReference", "sababAndAdabTip")
PRIORITIES = {"FARD", "WAJIB", "SUNNAH_MUAKKADAH", "MUSTAHAB"}
MAX_DEFAULT_ACTIVE = 3

_surah_cache: dict[int, dict] = {}


def fetch_surah(surah: int) -> dict:
    """Returns {"uthmani": [...], "translit": [...], "sahih": [...], "numbers": [...]}."""
    if surah in _surah_cache:
        return _surah_cache[surah]
    url = f"{API}/surah/{surah}/editions/{','.join(EDITIONS)}"
    with urllib.request.urlopen(url, timeout=30) as response:
        payload = json.load(response)
    if payload.get("code") != 200:
        sys.exit(f"API error for surah {surah}: {payload}")
    by_edition = {e["edition"]["identifier"]: e["ayahs"] for e in payload["data"]}
    uthmani = by_edition["quran-uthmani"]
    expected = EXPECTED_AYAH_COUNT.get(surah)
    if expected is not None and len(uthmani) != expected:
        sys.exit(f"Surah {surah}: expected {expected} ayahs, got {len(uthmani)}")
    data = {
        "uthmani": [a["text"].lstrip("﻿") for a in uthmani],
        "translit": [clean(a["text"]) for a in by_edition["en.transliteration"]],
        "sahih": [clean(a["text"]) for a in by_edition["en.sahih"]],
        "numbers": [a["number"] for a in uthmani],
    }
    _surah_cache[surah] = data
    return data


def clean(text: str) -> str:
    return re.sub(r"<[^>]+>", "", text).strip()


def basmala() -> tuple[str, str, str]:
    fatiha = fetch_surah(1)
    return fatiha["uthmani"][0], fatiha["translit"][0], fatiha["sahih"][0]


def strip_basmala(surah: int, ayah: int, text: str) -> str:
    """Tanzil prefixes ayah 1 of every surah (except 1 and 9) with the basmala."""
    if ayah != 1 or surah in (1, 9):
        return text
    prefix = basmala()[0]
    if not text.startswith(prefix):
        sys.exit(f"{surah}:1 does not start with the expected basmala; inspect manually")
    return text[len(prefix):].strip()


def parse_range(spec: str) -> tuple[int, int | None, int | None]:
    """"67" -> whole surah, "2:255" -> one ayah, "2:285-286" -> inclusive range."""
    if ":" not in spec:
        return int(spec), None, None
    surah, ayahs = spec.split(":")
    first, _, last = ayahs.partition("-")
    return int(surah), int(first), int(last or first)


def fetch_ayahs(ranges: list[tuple[int, int | None, int | None]]) -> list[dict]:
    ayahs = []
    for surah, first, last in ranges:
        data = fetch_surah(surah)
        first = first or 1
        last = last or len(data["uthmani"])
        for n in range(first, last + 1):
            i = n - 1
            ayahs.append({
                "surahNumber": surah,
                "ayahNumber": n,
                "arabicText": strip_basmala(surah, n, data["uthmani"][i]),
                "transliteration": data["translit"][i],
                "translation": data["sahih"][i],
                "audioUrl": AYAH_AUDIO.format(number=data["numbers"][i]),
            })
    return ayahs


def quran_item(item: dict) -> dict:
    ranges = [parse_range(r) for r in item.pop("quran")["ranges"]]
    ayahs = fetch_ayahs(ranges)
    item["ayahs"] = ayahs
    item["surahNumber"] = ranges[0][0] if len({r[0] for r in ranges}) == 1 else None
    item["arabicText"], item["transliteration"], item["translation"] = inline_text(ayahs)

    whole_surah = len(ranges) == 1 and ranges[0][1] is None
    if whole_surah:
        item["audioUrl"] = SURAH_AUDIO.format(surah=ranges[0][0])
    elif len(ayahs) == 1:
        item["audioUrl"] = ayahs[0]["audioUrl"]
    else:
        item["audioUrl"] = None  # Played as a per-ayah playlist.
    return item


def inline_text(ayahs: list[dict]) -> tuple[str, str, str]:
    """Full text for short passages, opening ayah for long surahs."""
    if len(ayahs) > INLINE_TEXT_MAX_AYAHS:
        a = ayahs[0]
        return a["arabicText"], a["transliteration"], a["translation"]

    b_ar, b_tr, b_en = basmala()
    arabic, translit, english = [], [], []
    for a in ayahs:
        if a["ayahNumber"] == 1 and a["surahNumber"] not in (1, 9):
            if arabic:
                arabic.append("\n")
                translit.append("\n")
                english.append("\n")
            arabic.append(b_ar + " ")
            translit.append(b_tr + " ")
            english.append(b_en + " ")
        marker = str(a["ayahNumber"]).translate(ARABIC_INDIC_DIGITS)
        arabic.append(f"{a['arabicText']} ﴿{marker}﴾ ")
        translit.append(f"{a['transliteration']} ")
        english.append(f"{a['translation']} ")
    return ("".join(s).replace(" \n", "\n").strip() for s in (arabic, translit, english))


def validate(item: dict) -> None:
    """Rejects items missing the fields every card shows."""
    for field in REQUIRED_TEXT_FIELDS:
        if not str(item.get(field, "")).strip():
            sys.exit(f"{item['id']}: missing {field}")
    if item.get("priority") not in PRIORITIES:
        sys.exit(f"{item['id']}: priority must be one of {sorted(PRIORITIES)}")
    if item["kind"] == "PRAYER" and not item.get("anchorPrayer"):
        sys.exit(f"{item['id']}: prayers need an anchorPrayer")
    if item["kind"] not in ("PRAYER", "DHIKR") and not item.get("situations"):
        sys.exit(f"{item['id']}: needs at least one situation")


def main() -> None:
    source = json.loads(SOURCE.read_text(encoding="utf-8"))
    items = []
    for index, raw in enumerate(source["items"]):
        item = dict(raw)
        validate(item)
        item["sortOrder"] = (index + 1) * 10
        item.setdefault("situations", [])
        if "quran" in item:
            item = quran_item(item)
        else:
            audio_quran = item.pop("audioQuran", None)
            item["ayahs"] = fetch_ayahs([parse_range(r) for r in audio_quran["ranges"]]) if audio_quran else []
            item.setdefault("audioUrl", None)
            item.setdefault("surahNumber", None)
        items.append(item)

    ids = [i["id"] for i in items]
    if len(ids) != len(set(ids)):
        sys.exit("Duplicate item ids in content_source.json")
    defaults = source["defaultActiveHabitIds"]
    missing = set(defaults) - set(ids)
    if missing:
        sys.exit(f"defaultActiveHabitIds not found: {missing}")
    if len(defaults) > MAX_DEFAULT_ACTIVE:
        sys.exit(f"At most {MAX_DEFAULT_ACTIVE} defaultActiveHabitIds (the Active 3 slots)")

    bundle = {
        "version": source["version"],
        "attribution": "Quran text: Tanzil Project (tanzil.net), via AlQuran.cloud. "
                       "Translation: Saheeh International. Recitation: Mishary Alafasy.",
        "defaultActiveHabitIds": defaults,
        "items": items,
    }
    OUTPUT.write_text(json.dumps(bundle, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    print(f"Wrote {OUTPUT.relative_to(ROOT)} (v{source['version']}): {len(items)} items, "
          f"{sum(len(i['ayahs']) for i in items)} ayahs")


if __name__ == "__main__":
    main()
