#!/usr/bin/env python3
"""
Mesure la géométrie et les couleurs d'une cellule de conversation
dans une capture d'écran, en pixels puis converties en dp.

Sert à deux choses :
  - vérifier ce qu'affiche Telegram (référence)
  - vérifier ce qu'affiche BotPro (comparaison)

Usage :
    python3 tools/measure_cell.py reference/captures/telegram_playstore_dialogs.png
    python3 tools/measure_cell.py capture_botpro.png --density 440
    python3 tools/measure_cell.py capture.png --cell 4        # une cellule précise

Pour capturer :
    adb exec-out screencap -p > capture.png
    adb shell wm density        # relever la densité, la passer en --density

⚠️ Les premières « cellules » détectées sont souvent l'en-tête (photo de profil,
   bannières, onglets de dossiers) et non de vraies conversations. Leurs mesures
   sont incohérentes — c'est normal. Utilise --cell 4 ou plus, et fie-toi surtout
   au « pas entre cellules » et au « diamètre avatar », qui sont calculés
   globalement et donc fiables.
"""
import argparse, sys
from collections import Counter

try:
    from PIL import Image
except ImportError:
    sys.exit("Pillow requis :  pip install Pillow")


def hexa(c):
    return "#%02X%02X%02X" % c


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("image")
    ap.add_argument("--density", type=int, default=440, help="dpi de l'appareil (adb shell wm density)")
    ap.add_argument("--cell", type=int, help="n'analyser qu'une cellule (index)")
    args = ap.parse_args()

    scale = args.density / 160.0
    def dp(v):
        return round(v / scale, 2)

    im = Image.open(args.image).convert("RGB")
    W, H = im.size
    px = im.load()
    print(f"{args.image}  {W}x{H}  densité {args.density}  ->  1dp = {scale}px\n")

    # --- fond : couleur dominante de la moitié droite, hors bords ---
    bg = Counter(px[x, y] for y in range(H // 3, 2 * H // 3, 3) for x in range(W // 2, W - 60, 3)).most_common(1)[0][0]
    print(f"fond dominant : {hexa(bg)}")

    def delta(c):
        return max(abs(c[i] - bg[i]) for i in range(3))

    # --- avatars : blocs verticaux dans la colonne de gauche ---
    left = int(25 * scale / 2.75), int(180 * scale / 2.75)
    profile = [max(delta(px[x, y]) for x in range(*left)) for y in range(H)]
    runs, cur = [], None
    for y, v in enumerate(profile):
        if v > 25:
            cur = [y, y] if cur is None else [cur[0], y]
        else:
            if cur and cur[1] - cur[0] > 60:
                runs.append(tuple(cur))
            cur = None
    if cur and cur[1] - cur[0] > 60:
        runs.append(tuple(cur))

    if len(runs) < 3:
        sys.exit("moins de 3 avatars détectés — la capture montre-t-elle bien la liste ?")

    pitch = Counter(runs[i + 1][0] - runs[i][0] for i in range(len(runs) - 1)).most_common(1)[0][0]
    diam = Counter(b - a + 1 for a, b in runs).most_common(1)[0][0]
    print(f"avatars détectés : {len(runs)}")
    print(f"  pas entre cellules : {pitch}px = {dp(pitch)}dp")
    print(f"  diamètre avatar    : {diam}px = {dp(diam)}dp")
    print("  (un avatar plus grand que les autres = anneau de story, l'ignorer)\n")

    # --- détail par cellule ---
    targets = [args.cell] if args.cell is not None else range(min(3, len(runs) - 1))
    for i in targets:
        if i >= len(runs):
            continue
        atop = runs[i][0]
        xs = [x for x in range(0, int(260 * scale / 2.75))
              if max(delta(px[x, y]) for y in range(runs[i][0], runs[i][1] + 1)) > 25]
        if not xs:
            continue
        print(f"═══ cellule {i} ═══")
        print(f"  avatar : x {xs[0]}px ({dp(xs[0])}dp du bord gauche)   y {atop}px")

        y0, y1 = atop - int(9 * scale), atop - int(9 * scale) + pitch - 1
        tx = xs[0] + diam + int(8 * scale)
        rows = [max(delta(px[x, y]) for x in range(tx, W - 20)) for y in range(y0, min(y1 + 1, H))]
        bands, cur = [], None
        for k, v in enumerate(rows):
            if v > 30:
                cur = [k, k] if cur is None else [cur[0], k]
            else:
                if cur and cur[1] - cur[0] >= 8:
                    bands.append(tuple(cur))
                cur = None
        if cur and cur[1] - cur[0] >= 8:
            bands.append(tuple(cur))

        for b0, b1 in bands:
            cols = [x for x in range(tx, W)
                    if max(delta(px[x, y]) for y in range(y0 + b0, y0 + b1 + 1)) > 30]
            solid = [px[x, y] for y in range(y0 + b0, y0 + b1 + 1)
                     for x in range(tx, W) if delta(px[x, y]) > 60]
            col = Counter(solid).most_common(1)[0][0] if solid else None
            print(f"  bande y+{b0:<3}..{b1:<3}  x {cols[0]}..{cols[-1]}  "
                  f"gauche={dp(cols[0])}dp  marge_droite={dp(W - 1 - cols[-1])}dp  "
                  f"couleur={hexa(col) if col else '-'}")
        print()


if __name__ == "__main__":
    main()
