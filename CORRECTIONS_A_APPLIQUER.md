# Correctifs — après vérification sur appareil

> Le portage est **globalement réussi** : hauteur de cellule, avatar, couleurs de texte et badge
> sont exacts au pixel. Trois défauts restent, mesurés sur l'appareil de test.
>
> Capture du rendu actuel : relance `adb exec-out screencap -p > botpro.png` après correction.

---

## ✅ Ce qui est déjà exact — ne pas y toucher

| Indicateur | Attendu | Mesuré sur BotPro |
|---|---|---|
| Fond de liste | `#1D2733` | `#1D2733` ✅ |
| Pas entre cellules | 194px (70dp + 1px) | 194px ✅ |
| Diamètre avatar | 143px (52dp) | 143px ✅ |
| Position x avatar | 31px (11dp) | 31px ✅ |
| Bord gauche du texte | 209px (76dp) | 76dp ✅ |
| Couleur du nom | `#E9EEF4` | `#E9EEF4` ✅ |
| Couleur du badge | `#64B5EF` | `#64B5EF` ✅ |
| FAB — taille et marges | 56dp, marges 16dp | 56×56dp, 16dp ✅ |
| Cellule sans vues enfants | oui | oui ✅ |

---

## 🔴 Défaut 1 — L'en-tête passe sous la barre de statut

**Le plus visible.** Le titre « Mes Bots » chevauche l'heure ; l'icône de recherche entre en collision avec le wifi et la batterie.

**Cause :** `targetSdk = 35`. Android 15 impose l'affichage bord à bord — l'app dessine derrière les barres système. Rien dans le code ne compense l'inset.

**Mesure :** barre de statut = **111px** sur l'appareil de test (1080×2400 @440dpi).
L'en-tête fait bien 56dp (154px) mais démarre à y=0 : **43px seulement restent visibles**.

**Correction** — ne code pas 111px en dur, la valeur change selon l'appareil :

```kotlin
ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
    val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
    headerView.updatePadding(top = bars.top)
    recyclerView.updatePadding(bottom = bars.bottom)
    insets
}
```

- Le **fond** de l'en-tête doit s'étendre derrière la barre de statut — pas de bande vide.
- Le **contenu** (titre, icônes) descend de `bars.top`.
- Hauteur totale de l'en-tête = `bars.top + dp(56)`.
- Sur la liste : `clipToPadding = false`, pour qu'elle défile sous la barre de navigation.

**Vérification :** après correction, le titre doit être entièrement sous l'heure, sans chevauchement.

---

## 🟠 Défaut 2 — Mauvaise couleur d'en-tête (erreur de la spec, pas de l'agent)

L'en-tête de BotPro affiche une bande `#242D39` distincte de la liste. Telegram n'en a pas : son en-tête se fond avec le fond de la liste.

**C'était une erreur de ma spec** — j'avais indiqué `actionBarDefault`. La source dit autre chose, `DialogsActivity.java:3502` :

```java
actionBar.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
```

**Correction :** fond de l'en-tête = `windowBackgroundWhite` = **`#1D2733`**, pas `#242D39`.

**Double confirmation :** la source le dit, et l'échantillonnage de la capture Telegram donne `#1D2733` sur toute la zone d'en-tête.

**Vérification :**
```bash
python3 -c "
from PIL import Image
p=Image.open('botpro.png').convert('RGB').load()
print('%02X%02X%02X'%p[500,130])   # doit afficher 1D2733
"
```

---

## 🟡 Défaut 3 — Le FAB est un carré arrondi, pas un cercle

Taille et marges sont bonnes (56dp, 16dp). C'est la **forme** qui diffère.

**Mesure du profil** — largeur de la forme à différentes hauteurs :

| Hauteur | Mesuré | Attendu si cercle |
|---|---|---|
| 25 % | 154px | ~133px |
| 50 % | 154px | 154px |
| 75 % | 154px | ~133px |

Une largeur constante signale un carré aux angles arrondis. Un cercle se resserre aux extrémités.

**Correction :** dessiner un vrai cercle — `canvas.drawCircle(cx, cy, dp(28), paint)`, ou une forme circulaire, pas un `RoundRect` à grand rayon.

**Vérification :** relancer le test de profil ; les largeurs à 25 % et 75 % doivent être nettement inférieures à celle du milieu.

---

## Ordre suggéré

1. **Défaut 1** — insets (le seul qui gêne réellement l'usage)
2. **Défaut 2** — une constante à changer
3. **Défaut 3** — forme du FAB

Puis re-vérifier l'ensemble :
```bash
adb exec-out screencap -p > botpro.png
python3 tools/measure_cell.py botpro.png --density 440 --cell 4
```
Les valeurs du tableau « déjà exact » ne doivent pas avoir bougé.

---

## Note

Ces trois points ont été trouvés **par la boucle de vérification**, pas à l'œil.
C'est exactement à ça qu'elle sert : le défaut 2 était une erreur dans mes propres instructions,
que seule la comparaison mesurée a révélée. Continue de mesurer plutôt que d'ajuster à l'œil.
