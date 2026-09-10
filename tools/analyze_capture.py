#!/usr/bin/env python3
"""
Analyse une capture faite par tools/capture.sh :
sépare ce qui est peint (vues sans enfants) de ce qui est de vraies vues,
et sort toutes les dimensions en px et dp.

    python3 tools/analyze_capture.py conv-01
    python3 tools/analyze_capture.py conv-01 --density 440
"""
import argparse, re, sys, xml.etree.ElementTree as ET
from pathlib import Path

CAP = Path("reference/conversation/captures")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("name")
    ap.add_argument("--density", type=int, default=440)
    a = ap.parse_args()

    xml = CAP / f"{a.name}.xml"
    if not xml.exists():
        sys.exit(f"introuvable : {xml}")

    scale = a.density / 160.0
    def dp(v):
        return round(v / scale, 1)

    root = ET.parse(xml).getroot()

    def bounds(n):
        m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", n.get("bounds", ""))
        return tuple(map(int, m.groups())) if m else None

    nodes = [(n, b) for n in root.iter() if (b := bounds(n))]
    info = (CAP / f"{a.name}.txt")
    if info.exists():
        print(info.read_text().strip())
    print(f"\n{len(nodes)} noeuds · densité {a.density} · 1dp = {scale}px\n")

    SCREEN_W = max(b[2] for _, b in nodes)
    SCREEN_H = max(b[3] for _, b in nodes)
    LEAF = ("EditText", "ImageButton", "ImageView", "TextView", "Button", "CheckBox")

    painted, views, skipped = [], [], 0
    for n, (x0, y0, x1, y1) in nodes:
        w, h = x1 - x0, y1 - y0
        cls = (n.get("class") or "").split(".")[-1]
        label = (n.get("text") or n.get("content-desc") or "").strip()[:30]
        kids = len(list(n))

        # conteneurs de décor : plein écran, sans libellé -> ni peint ni mesurable
        if w >= SCREEN_W * 0.98 and h >= SCREEN_H * 0.9 and not label:
            skipped += 1
            continue

        rec = (x0, y0, x1, y1, w, h, cls, label)
        # cellule peinte : aucun enfant, mais annoncée à l'accessibilité
        if kids == 0 and label and cls not in LEAF:
            painted.append(rec)
        elif cls in LEAF and label:
            views.append(rec)

    print(f"=== CONTENU PEINT — {len(painted)} cellules (0 enfant, Canvas) ===")
    if not painted:
        print("  aucune")
    for x0, y0, x1, y1, w, h, cls, label in sorted(painted, key=lambda r: r[1]):
        print(f"  y {y0:>4}..{y1:<5} h={h:>4}px = {dp(h):>6}dp  {cls:<11} {label}")

    print(f"\n=== VRAIES VUES — {len(views)} éléments mesurables ===")
    for x0, y0, x1, y1, w, h, cls, label in sorted(views, key=lambda r: r[1]):
        print(f"  x {x0:>4}..{x1:<5} y {y0:>4}..{y1:<5} {dp(w):>6}x{dp(h):<6}dp  {cls:<12} {label}")

    tops = [r for r in views if r[1] < 400]
    if tops:
        first = min(r[1] for r in tops)
        print(f"\nBarre du haut : contenu démarre à y={first}px = {dp(first)}dp")
        print("  -> doit valoir la hauteur de la barre de statut (insets gérés)")
    if skipped:
        print(f"\n({skipped} conteneurs de décor ignorés)")


if __name__ == "__main__":
    main()
