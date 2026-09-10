#!/usr/bin/env python3
"""
Reconnaît le type de chaque message dans une capture, à partir de la
description d'accessibilité que Telegram attache à chaque cellule.

    python3 tools/classify_messages.py conv-05
    python3 tools/classify_messages.py --all          # inventaire sur toutes les captures

Ce que Telegram écrit dans content-desc, et qu'on exploite :
    "<texte> ⏎ Reçu at 22:38"                      -> texte entrant
    "<texte> ⏎ Sent at 22:39, Seen"                -> texte sortant, lu
    "ZIP file,<nom>, 425,6 MB"                     -> document
    "Forwarded from <auteur> ⏎ ..."                -> transfert
    "juin 26"                                       -> séparateur de date

⚠️ Les types non encore rencontrés (photo, vocal, sticker, sondage, clavier de bot)
   ressortent en « inconnu ». C'est voulu : mieux vaut un trou signalé qu'une
   étiquette inventée. Capture l'écran manquant plutôt que d'élargir une regex au jugé.
"""
import argparse, glob, re, sys
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path

CAP = Path("reference/conversation/captures")

MOIS = ("janvier février mars avril mai juin juillet août septembre octobre novembre décembre "
        "january february march april may june july august september october november december").split()


def cells(xml_path):
    root = ET.parse(xml_path).getroot()
    out = []
    for e in root.iter():
        m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", e.get("bounds", ""))
        # Telegram met la description de la cellule dans `text`, pas dans `content-desc`
        # (qui reste vide) — l'inverse de ce qu'on attendrait.
        cd = ((e.get("text") or "") or (e.get("content-desc") or "")).strip()
        cls = (e.get("class") or "").split(".")[-1]
        if m and cd and cls == "ViewGroup":
            x0, y0, x1, y1 = map(int, m.groups())
            # Une cellule de message occupe toute la largeur (0..1080)
            # et a 0 enfant réel (les enfants avec class="" sont des nœuds virtuels d'accessibilité pour les citations/liens).
            is_virtual_only = all((c.get("class") or "") == "" for c in e)
            if x0 == 0 and x1 == 1080 and is_virtual_only:
                reply_info = None
                for c in e:
                    c_desc = (c.get("content-desc") or "").strip()
                    if c_desc.startswith("Reply") or c_desc.startswith("Réponse"):
                        reply_info = c_desc
                out.append((y0, y1 - y0, cd, reply_info))
    return sorted(out)


def classify(cd, reply_info=None):
    """-> (type, direction, drapeaux)"""
    flat = cd.replace("\n", " ")
    flags = []

    if reply_info:
        flags.append("citation")

    low = flat.lower().strip()
    if not re.search(r"\b(at|à)\s+\d{1,2}[:h]\d{2}", flat):
        if any(mo in low for mo in MOIS) or low in ("aujourd'hui", "hier", "today", "yesterday"):
            return "séparateur de date", "—", []

    if flat.startswith("Forwarded from") or flat.startswith("Transféré de"):
        flags.append("transféré")

    direction = "?"
    if re.search(r"\bSent at\b|\bEnvoyé", flat):
        direction = "sortant"
    elif re.search(r"\bReçu at\b|\bReceived at\b", flat):
        direction = "entrant"

    if re.search(r",\s*(Seen|Lu)\b", flat):
        flags.append("lu")

    mtype = None
    if re.search(r"\b\w+ file,", flat):
        ext = re.search(r"\b(\w+) file,", flat).group(1)
        mtype = f"document ({ext})"
    elif re.search(r"\bPhoto\b", flat):
        mtype = "photo"
    elif re.search(r"\bVoice message\b|\bMessage vocal\b", flat):
        mtype = "vocal"
    elif re.search(r"\bVideo\b|\bVidéo\b", flat):
        mtype = "vidéo"
    elif re.search(r"\bSticker\b", flat):
        mtype = "sticker"
    elif re.search(r"\bGIF\b", flat):
        mtype = "gif"
    elif direction != "?":
        mtype = "texte"

    if re.match(r"^@\w+", flat):
        flags.append("mention")
    if re.match(r"^/\w+", flat):
        flags.append("commande")

    return (mtype or "inconnu"), direction, flags


def report(name, verbose=True):
    xml = CAP / f"{name}.xml"
    if not xml.exists():
        print(f"  {name}: introuvable")
        return Counter()
    rows = cells(xml)
    counts = Counter()
    if verbose:
        print(f"───── {name} — {len(rows)} cellules ─────")
    for row in rows:
        y, h, cd = row[0], row[1], row[2]
        reply_info = row[3] if len(row) > 3 else None
        t, d, fl = classify(cd, reply_info)
        counts[(t, d)] += 1
        if verbose:
            extra = ("  [" + ", ".join(fl) + "]") if fl else ""
            apercu = cd.replace("\n", " ⏎ ")[:44]
            print(f"  y{y:>5} h={h:>4}px  {t:<16} {d:<8}{extra:<26} {apercu}")
    if verbose:
        print()
    return counts


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("name", nargs="?")
    ap.add_argument("--all", action="store_true")
    a = ap.parse_args()

    if a.all or not a.name:
        total = Counter()
        for f in sorted(CAP.glob("conv-*.xml")):
            total += report(f.stem, verbose=False)
        print("═══ INVENTAIRE — tous écrans capturés ═══\n")
        for (t, d), n in sorted(total.items(), key=lambda kv: -kv[1]):
            print(f"  {n:>3} ×  {t:<18} {d}")
        inconnus = sum(n for (t, _), n in total.items() if t == "inconnu")
        print(f"\n  total {sum(total.values())} cellules · {inconnus} non reconnues")
        print("\nTypes des 38 de MessageObject encore JAMAIS capturés :")
        vus = {t.split(" (")[0] for (t, _) in total}
        for t in ("photo", "vidéo", "vocal", "sticker", "gif", "sondage", "contact",
                  "localisation", "musique", "vidéo ronde", "clavier de bot"):
            if t not in vus:
                print(f"  - {t}")
    else:
        report(a.name)


if __name__ == "__main__":
    main()
