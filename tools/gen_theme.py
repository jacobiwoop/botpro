#!/usr/bin/env python3
"""
Génère les couleurs exactes d'un thème Telegram.

Chaîne de résolution reproduite depuis Telegram :
    1. valeur du fichier .attheme si la clé y figure
    2. sinon defaultColors[key] de ThemeColors.createDefaultColors()
    3. sinon suivre fallbackKeys (alias déclarés dans Theme.java) et recommencer

Usage :
    python3 tools/gen_theme.py                        # thème darkblue, résumé
    python3 tools/gen_theme.py --theme night          # autre thème
    python3 tools/gen_theme.py --json sortie.json     # export complet
    python3 tools/gen_theme.py --key chats_name       # une clé précise

Résultat attendu pour darkblue (sert de test de non-régression) :
    723 defaultColors · 819 colorKeysMap · 199 fallbackKeys · 475 overrides
    -> 793 / 823 clés résolues (473 attheme + 309 défaut + 11 fallback)
Si tu n'obtiens pas ces nombres, le parseur est cassé.
"""
import argparse, json, re, sys
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
THEME_COLORS = ROOT / "Telegram/TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ThemeColors.java"
THEME = ROOT / "Telegram/TMessagesProj/src/main/java/org/telegram/ui/ActionBar/Theme.java"
ASSETS = ROOT / "botpro/app/src/main/assets"


def read(p):
    if not p.exists():
        sys.exit(f"introuvable : {p}")
    return p.read_text(encoding="utf-8", errors="ignore")


def parse_sources():
    src = read(THEME_COLORS)
    defaults = {
        m[0]: int(m[1], 16) & 0xFFFFFFFF
        for m in re.findall(r"defaultColors\[(?:Theme\.)?key_(\w+)\]\s*=\s*(0x[0-9a-fA-F]+)", src)
    }
    names = dict(re.findall(r'colorKeysMap\.put\((?:Theme\.)?key_(\w+),\s*"([^"]+)"', src))
    fallbacks = dict(
        re.findall(r"fallbackKeys\.put\((?:Theme\.)?key_(\w+),\s*(?:Theme\.)?key_(\w+)\)", read(THEME))
    )
    return defaults, names, fallbacks


def parse_attheme(name):
    path = ASSETS / f"{name}.attheme"
    out = {}
    for line in read(path).splitlines():
        if "=" not in line:
            continue
        k, v = line.split("=", 1)
        try:
            out[k.strip()] = int(v.strip()) & 0xFFFFFFFF
        except ValueError:
            pass  # lignes non numériques (image de fond encodée, etc.)
    return out


def build(theme="darkblue"):
    defaults, names, fallbacks = parse_sources()
    overrides = parse_attheme(theme)

    def resolve(key, depth=0):
        if depth > 8:
            return None, "boucle"
        if names.get(key, key) in overrides:
            return overrides[names.get(key, key)], "attheme"
        if key in defaults:
            return defaults[key], "defaut"
        if key in fallbacks:
            # l'origine reste "fallback" : c'est l'alias qui a fourni la valeur,
            # pas la clé demandée. Sans ça les comptes ne sont plus comparables.
            color, _ = resolve(fallbacks[key], depth + 1)
            return color, ("fallback" if color is not None else "-")
        return None, "-"

    keys = sorted(set(defaults) | set(names))
    return {k: resolve(k) for k in keys}, dict(
        defaults=len(defaults), names=len(names), fallbacks=len(fallbacks), overrides=len(overrides)
    )


def fmt(argb):
    a, r, g, b = (argb >> 24) & 255, (argb >> 16) & 255, (argb >> 8) & 255, argb & 255
    return f"#{r:02X}{g:02X}{b:02X}" + ("" if a == 255 else f"  alpha={a}")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--theme", default="darkblue")
    ap.add_argument("--json")
    ap.add_argument("--key")
    args = ap.parse_args()

    resolved, stats = build(args.theme)

    if args.key:
        color, origin = resolved.get(args.key, (None, "inconnue"))
        print(f"{args.key} = {fmt(color) if color is not None else 'non défini'}   [{origin}]")
        return

    ok = {k: v for k, v in resolved.items() if v[0] is not None}
    print(f"thème : {args.theme}")
    print(f"  defaultColors {stats['defaults']} · colorKeysMap {stats['names']} · "
          f"fallbackKeys {stats['fallbacks']} · overrides {stats['overrides']}")
    print(f"  résolues : {len(ok)} / {len(resolved)}")
    print(f"  origines : {dict(Counter(o for _, o in ok.values()))}")

    manquantes = [k for k, v in resolved.items() if v[0] is None]
    if manquantes:
        print(f"\n  {len(manquantes)} non résolues (dégradés / fond d'écran calculés à l'exécution) :")
        print("   ", ", ".join(manquantes[:12]) + (" …" if len(manquantes) > 12 else ""))

    if args.json:
        Path(args.json).write_text(
            json.dumps({k: "#%08X" % v[0] for k, v in sorted(ok.items())}, indent=1), encoding="utf-8"
        )
        print(f"\nécrit : {args.json}  ({len(ok)} clés)")


if __name__ == "__main__":
    main()
