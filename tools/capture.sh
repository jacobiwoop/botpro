#!/usr/bin/env bash
# Capture complète d'un écran : rendu normal + contours de vues + arbre des vues.
#
#   ./tools/capture.sh <nom-de-l-ecran>
#
# Produit dans reference/conversation/captures/ :
#   <nom>.png          rendu normal
#   <nom>-bounds.png   avec les contours de vues (debug.layout)
#   <nom>.xml          arbre des vues (uiautomator)
#   <nom>.txt          résumé : app au premier plan, densité, taille

set -u
NAME="${1:-capture}"
DEV="${ADB_DEV:-$(adb devices | grep -m1 -E '^[0-9.:]+\s+device' | cut -f1)}"
if [ -z "$DEV" ]; then
  DEV="10.114.186.204:5555"
  adb connect "$DEV" >/dev/null 2>&1
fi
OUT="reference/conversation/captures"
mkdir -p "$OUT"

adb -s "$DEV" shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1
sleep 0.5

if ! adb -s "$DEV" shell true >/dev/null 2>&1; then
  echo "appareil injoignable — vérifie le wifi et le débogage sans fil"
  exit 1
fi

FOCUS=$(adb shell dumpsys window 2>/dev/null | grep -m1 mCurrentFocus | sed 's/^ *//')
DENS=$(adb shell wm density 2>/dev/null | tr -d '\r')
SIZE=$(adb shell wm size 2>/dev/null | tr -d '\r')

# 1. rendu normal
adb exec-out screencap -p > "$OUT/$NAME.png" 2>/dev/null

# 2. arbre des vues
adb shell uiautomator dump /sdcard/_ui.xml >/dev/null 2>&1
adb pull /sdcard/_ui.xml "$OUT/$NAME.xml" >/dev/null 2>&1
adb shell rm /sdcard/_ui.xml >/dev/null 2>&1

# 3. contours de vues
adb shell setprop debug.layout true >/dev/null 2>&1
adb shell service call activity 1599295570 >/dev/null 2>&1
sleep 2
adb exec-out screencap -p > "$OUT/$NAME-bounds.png" 2>/dev/null
adb shell setprop debug.layout false >/dev/null 2>&1
adb shell service call activity 1599295570 >/dev/null 2>&1

{
  echo "capture : $NAME"
  echo "$FOCUS"
  echo "$DENS"
  echo "$SIZE"
} > "$OUT/$NAME.txt"

echo "$FOCUS"
echo "$DENS · $SIZE"
for f in "$NAME.png" "$NAME-bounds.png" "$NAME.xml"; do
  if [ -s "$OUT/$f" ]; then
    printf "  ok   %-26s %s\n" "$f" "$(du -h "$OUT/$f" | cut -f1)"
  else
    printf "  ÉCHEC %-26s\n" "$f"
  fi
done
