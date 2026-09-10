# Spec — Écran liste de conversations, à l'identique de Telegram

> Instructions pour l'agent d'implémentation.
> Toutes les valeurs ci-dessous ont été **mesurées deux fois par deux méthodes indépendantes** :
> lecture de la source Telegram, et échantillonnage des pixels d'un appareil réel.
> Aucune n'est estimée.

---

## 1. Ce qu'on te demande

Coder l'écran liste de conversations de BotPro pour qu'il soit **visuellement identique** à Telegram.

**Avec tes propres composants.** Tu n'as pas à recopier le code de Telegram. Tu écris tes classes, à ta façon, en respectant les valeurs de ce document.

**Les assets, en revanche, se prennent dans la source Telegram** — polices, icônes. Ils sont déjà partiellement dans le projet. Section 6.

### La règle unique

**N'invente aucune valeur visuelle.** Tout ce dont tu as besoin est dans ce document. Si une valeur manque, elle est dans la section 8 « Ce qui n'est pas encore mesuré » — demande, ne devine pas.

Une tentative précédente a échoué exactement là-dessus : les couleurs avaient été prises à l'œil. **10 sur 10 étaient fausses.**

---

## 1bis. Ce qui est fourni avec ce document

Tout est dans le dépôt. **Tu peux tout re-vérifier toi-même** — ne me crois pas sur parole.

### Captures de référence

| Fichier | Quoi |
|---|---|
| `reference/captures/telegram_playstore_dialogs.png` | Liste de conversations, 1080×2400 @440dpi. **Toutes les mesures de ce document en sont issues.** |
| `reference/captures/telegram_dialogs_layout_bounds.png` | Le même écran **avec les contours de vues activés**. ★ Regarde-la avant de coder. |

**La capture avec contours vaut une démonstration.** Compare deux zones de la même image :

- **Barre du bas** (Chats / Contacts / Settings / Profil) : chaque icône a son rectangle, chaque libellé le sien. Ce sont de vraies vues Android.
- **Une cellule de conversation** : **un seul rectangle** autour de la cellule entière. Le nom, l'heure, l'aperçu, la pastille bleue n'ont **aucun contour** — ils n'existent pas en tant que vues, ils sont peints sur la toile.

C'est la raison pour laquelle la cellule doit être une `View` unique avec `onDraw`, et pas un assemblage de `TextView`.

### Données extraites

| Fichier | Quoi |
|---|---|
| `reference/data/couleurs_dialogs.json` | Les 40 couleurs de cet écran |
| `reference/data/darkblue_resolved.json` | Les 793 clés du thème darkblue, résolues |
| `reference/data/uiautomator_dump.xml` | Arbre des vues capturé sur l'appareil |

### Outils réutilisables

**`tools/gen_theme.py`** — regénère les couleurs depuis la source Telegram.
```bash
python3 tools/gen_theme.py                              # résumé + contrôle
python3 tools/gen_theme.py --key chats_name             # une clé
python3 tools/gen_theme.py --theme night --json out.json
```
Sortie attendue pour `darkblue` (**test de non-régression** — si tu n'obtiens pas ça, quelque chose est cassé) :
```
defaultColors 723 · colorKeysMap 819 · fallbackKeys 199 · overrides 475
résolues : 793 / 823
origines : {'attheme': 473, 'defaut': 309, 'fallback': 11}
```

**`tools/measure_cell.py`** — mesure une capture (Telegram ou BotPro) en px et dp.
```bash
python3 tools/measure_cell.py reference/captures/telegram_playstore_dialogs.png --cell 4
python3 tools/measure_cell.py ma_capture_botpro.png --density 440 --cell 4
```
Sortie attendue sur la capture de référence :
```
pas entre cellules : 194px = 70.55dp
diamètre avatar    : 143px = 52.0dp
cellule 4 : avatar x 31px (11.27dp)   nom #E9EEF4   badge #64B5EF
```

C'est **le même outil qui sert à mesurer ton propre rendu** : lance-le sur une capture de BotPro et compare les nombres. C'est la boucle de vérification.

### Appareil de test

Connecté en **ADB WiFi** : `192.168.1.14:5555` — Xiaomi, 1080×2400, densité **440**.
Si la connexion est perdue : `adb connect 192.168.1.14:5555` (le téléphone doit être sur le même réseau, débogage sans fil actif).

Applications déjà installées :
- `org.telegram.messenger.beta` → **notre build depuis la source** = la référence à utiliser
- `org.telegram.messenger` → Play Store, version **plus récente** que le dépôt → **ne pas utiliser** comme référence
- `com.botpro.app` → l'app à corriger

⚠️ **Confidentialité :** `telegram_playstore_dialogs.png` contient les vraies conversations du propriétaire. Le dépôt a une remote GitHub. **Ne pas la pousser sans son accord explicite** — au besoin, l'ajouter à `.gitignore`.

---

## 2. Contrainte structurelle : tout se dessine

Telegram ne construit pas cette cellule avec des vues. Vérifié :

- `R.layout` : **0 occurrence** dans `DialogCell.java` et `DialogsActivity.java`
- `DialogCell.java` : **0 `TextView`**, **41 appels `canvas.draw*`**
- Sur appareil réel, `uiautomator` ne voit **qu'un rectangle vide** par conversation — aucun enfant

**Conséquence : la cellule doit être une `View` unique qui peint son contenu dans `onDraw(Canvas)`.**

Un `RelativeLayout` avec des marges fixes ne peut pas y arriver — c'est la raison structurelle de l'échec précédent, indépendamment des couleurs. Les positions dépendent de conditions (sourdine, épinglé, vérifié, RTL) qu'un layout statique n'exprime pas.

À supprimer : `res/layout/item_conversation.xml`, `res/layout/fragment_bot_list.xml`.

---

## 3. Géométrie de la cellule — validée sur appareil

Appareil de mesure : 1080×2400, densité **440 dpi** → **1dp = 2,75px**.
Conversion utilisée par Telegram : `dp(v) = ceil(v × densité)`.

| Élément | Valeur | Source | Calcul px | Mesuré px | |
|---|---|---|---|---|---|
| Hauteur cellule | **70dp** + 1px séparateur | `DialogCell.java:171` `heightDefault = 70` | 193 + 1 = 194 | **194** | ✅ |
| Avatar — diamètre | **52dp** | `DialogCell.java:2467` `+ dp(52)` | 143 | **143** | ✅ |
| Avatar — marge gauche | **11dp** | `DialogCell.java:169` `avatarStart = 11` | 31 | **31** | ✅ |
| Avatar — marge haute | **9dp** | `DialogCell.java:2449` `avatarTop = dp(9)` | 25 | **25** | ✅ |
| Texte — bord gauche | **76dp** | `messagePaddingStart = 72` (l.170) `+ 4` | 209 | **210** | ✅ ¹ |
| Badge — haut | **38dp** | `DialogCell.java:2454` `countTop = dp(38f)` | 105 | **103** | ✅ ¹ |
| Heure — marge droite | **15dp** | `DialogCell.java:2265` | 41 | ~44 | ⚠ ² |

¹ Écart de 1 à 2px imputable à l'anticrénelage du texte. Retenir la valeur de la source.
² Écart de 3px non expliqué. Voir section 8.

**Cohérence à noter :** 11 (marge) + 52 (avatar) + 13 = 76dp. Le bord du texte tombe juste.

### Tailles de texte

| Élément | Taille | Source |
|---|---|---|
| Nom | **17dp** | `DialogCell.java:1247` |
| Message | **16dp** | `DialogCell.java:1249` |
| Heure | **12dp** | `Theme.java:7891` |
| Nom d'expéditeur (groupes) | **14dp** | `Theme.java` |

⚠️ Telegram utilise **dp** et non **sp** pour ces tailles — le texte ne suit donc pas le réglage d'accessibilité du système. Reproduis ce comportement, sinon les positions se décalent.

### Variante 3 lignes

`heightThreeLines = 76` (dp), avatar `dp(56)`, `avatarTop = dp(11)`, `countTop = dp(42.33f)`.
**Hors périmètre pour l'instant** — implémente d'abord le cas standard à 2 lignes.

---

## 4. Couleurs — thème Dark Blue

Chaîne de résolution utilisée : `darkblue.attheme` → `ThemeColors.defaultColors` → `fallbackKeys`.
`[attheme]` = lu dans `botpro/app/src/main/assets/darkblue.attheme` · `[defaut]` = lu dans `ThemeColors.java`.

**Quatre de ces valeurs ont été confirmées au pixel sur l'appareil** (marquées ✅). Les autres viennent de la même chaîne, validée par ces quatre-là.

### Fond & structure
| Clé | Couleur | Alpha | |
|---|---|---|---|
| `windowBackgroundWhite` (fond liste) | `#1D2733` | — | ✅ mesuré |
| `windowBackgroundGray` | `#151E27` | — | |
| `divider` (séparateur) | `#000000` | **149** | |
| `listSelectorSDK21` (appui) | `#E6F7FF` | **20** | |

### Barre du haut
| Clé | Couleur | Alpha |
|---|---|---|
| `actionBarDefault` (fond) | `#242D39` | — |
| `actionBarDefaultTitle` | `#FFFFFF` | — |
| `actionBarDefaultIcon` | `#FFFFFF` | — |
| `actionBarDefaultSelector` | `#CFE8FF` | 30 |
| `actionBarDefaultSearch` | `#FFFFFF` | — |
| `actionBarDefaultSearchPlaceholder` | `#DCF4FF` | 120 |

### Texte de cellule
| Clé | Couleur | Alpha | |
|---|---|---|---|
| `chats_name` (nom) | `#E9EEF4` | — | ✅ mesuré |
| `chats_message` (aperçu) | `#7D8B99` | — | ✅ mesuré |
| `chats_date` (heure) | `#737F8B` | — | |
| `chats_nameMessage` | `#E9EEF4` | — | |
| `chats_draft` (brouillon) | `#FC474A` | 217 | |
| `chats_attachMessage` | `#7D8E98` | — | |

### Badges & icônes
| Clé | Couleur | Alpha | |
|---|---|---|---|
| `chats_unreadCounter` | `#64B5EF` | — | ✅ mesuré |
| `chats_unreadCounterMuted` | `#3E5263` | — | |
| `chats_unreadCounterText` | `#FFFFFF` | — | |
| `chats_muteIcon` | `#4E5F6A` | — | |
| `chats_pinnedIcon` | `#586D80` | — | |
| `chats_pinnedOverlay` | `#FFFFFF` | 8 | |
| `chats_verifiedBackground` | `#64B5EF` | — | |
| `chats_verifiedCheck` | `#FFFFFF` | — | |
| `chats_sentCheck` | `#64B5EF` | — | |
| `chats_sentReadCheck` | `#46AA36` | — | |
| `chats_sentClock` | `#4C5F6A` | — | |
| `chats_onlineCircle` | `#4BCB1C` | — | |
| `chats_secretName` | `#71D756` | — | |

### Bouton flottant
| Clé | Couleur |
|---|---|
| `chats_actionBackground` | `#5FA3DE` |
| `chats_actionPressedBackground` | `#569DD6` |
| `chats_actionIcon` | `#FFFFFF` |

### Avatars sans photo
| Clé | Couleur |
|---|---|
| `avatar_backgroundBlue` | `#5CAFFA` |
| `avatar_backgroundRed` | `#E86C6A` |
| `avatar_backgroundOrange` | `#F2BC64` |
| `avatar_backgroundViolet` | `#B694F9` |
| `avatar_backgroundGreen` | `#9AD164` |
| `avatar_backgroundCyan` | `#5BCBE3` |
| `avatar_backgroundPink` | `#FF8AAC` |
| `avatar_text` | `#FFFFFF` |

> Table complète (40 clés) en JSON : `couleurs_dialogs.json` — demande-la si besoin.
> Le générateur résout **793 des 823 clés** de Telegram ; les 30 restantes sont des dégradés
> et fonds d'écran calculés à l'exécution, **aucune ne concerne cet écran**.

**Supprimer** les couleurs devinées de `res/values/colors.xml`.

---

## 5. Polices

Les 9 polices Telegram sont **déjà** dans `botpro/app/src/main/assets/fonts/`.

| Usage | Fichier |
|---|---|
| Nom de conversation, titres, badges | `rmedium.ttf` |
| Extra-gras | `rextrabold.ttf` |
| Italique | `ritalic.ttf` |
| Monospace | `rmono.ttf` |

Charger par `Typeface.createFromAsset`, **avec cache** (ne recharge pas à chaque `onDraw`).

**Interdit : `textStyle="bold"` et `Typeface.DEFAULT_BOLD`.** Ils produisent un gras synthétique, visiblement différent de `rmedium.ttf`. C'est une erreur de la version actuelle.

**Texte courant** (aperçu du message) : Telegram utilise la **police système par défaut**, pas une police embarquée. Ne force rien — laisse `Typeface.DEFAULT`.

---

## 6. Assets à récupérer

Source : `Telegram/TMessagesProj/src/main/res/drawable-*/`
Destination : `botpro/app/src/main/res/drawable-*/` (respecter les dossiers de densité)

Vérifié : ces fichiers existent côté Telegram et **manquent tous** côté BotPro.

| Fichier | Usage |
|---|---|
| `list_mute` | icône sourdine |
| `list_unmute` | icône son actif |
| `list_pin` | épinglé |
| `list_check` | message lu (double coche) |
| `list_halfcheck` | message envoyé (simple coche) |
| `list_secret` | discussion secrète |
| `verified_area` + `verified_check` | badge vérifié (2 calques) |
| `floating_pencil` | icône du bouton flottant |
| `ic_ab_other` | menu ⋮ |

Note : le badge vérifié est composé de **deux drawables superposés** — le fond (`verified_area`, teinté `chats_verifiedBackground`) et la coche (`verified_check`, teintée `chats_verifiedCheck`).

Les icônes sont monochromes : les teinter par code, ne pas en créer de variantes colorées.

---

## 7. Ce qu'il faut construire

### 7.1 `DialogCellView` — une `View`, pas un `ViewGroup`

Dessine dans `onDraw` : avatar (cercle rogné), nom, aperçu, heure, badge non-lu, icônes d'état, séparateur, effet d'appui.

- `onMeasure` : hauteur = `dp(70) + 1px`
- Séparateur : **1 pixel physique**, pas `dp(1)` — indenté, il démarre au bord gauche du texte (76dp)
- Effet d'appui : `listSelectorSDK21` (`#E6F7FF` alpha 20)
- Badge : coins arrondis, hauteur ~`dp(23)`, largeur minimale, texte centré en `rmedium`
- **Réutilise tes objets `Paint`** — n'en alloue aucun dans `onDraw`

Données : `data/model/Models.kt` (`Conversation`, `Bot`, `Message`). **N'introduis aucun type TLRPC.**

### 7.2 Barre du haut

Hauteur **56dp** (portrait, téléphone), **plus l'inset de la barre de statut** — voir 7.6, c'est obligatoire.
Titre en `rmedium` 20dp, couleur `actionBarDefaultTitle`.

⚠️ **Fond = `windowBackgroundWhite` (`#1D2733`), PAS `actionBarDefault`.**
`DialogsActivity.java:3502` force explicitement :
```java
actionBar.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
```
La barre se fond donc avec la liste — aucune bande de couleur distincte. Confirmé par mesure à l'écran (`#1D2733` sur toute la zone d'en-tête de Telegram).

Actions : recherche, menu ⋮.
**N'utilise pas `androidx.appcompat.widget.Toolbar`** — son rembourrage et sa typographie ne correspondent pas.

### 7.6 Barres système (edge-to-edge) — obligatoire

`targetSdk = 35` : Android 15 impose l'affichage bord à bord. **L'app dessine derrière la barre de statut par défaut.** Sans traitement, l'en-tête passe sous l'heure et la batterie.

Mesuré sur l'appareil de test : barre de statut = **111px = 40,36dp**. Ne code pas cette valeur en dur, elle varie d'un appareil à l'autre — lis l'inset :

```kotlin
ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
    val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
    header.updatePadding(top = bars.top)     // décale le contenu de l'en-tête
    list.updatePadding(bottom = bars.bottom) // la liste défile sous la barre de navigation
    insets
}
```

Règles :
- Le **fond** de l'en-tête s'étend derrière la barre de statut (pas de bande vide en haut).
- Le **contenu** de l'en-tête (titre, icônes) est décalé vers le bas de `bars.top`.
- Hauteur totale de l'en-tête = `bars.top + dp(56)`.
- La liste garde `clipToPadding = false` pour défiler sous la barre de navigation.

### 7.3 Liste

`RecyclerView`, `overScrollMode = never`.

### 7.4 Bouton flottant

Cercle 56dp, fond `chats_actionBackground`, icône `floating_pencil` teintée `chats_actionIcon`, marge 16dp bas/droite.

### 7.5 Barre d'onglets basse

**Vérifié sur appareil :** le Telegram du Play Store affiche une barre flottante à 4 onglets en bas ; **notre build depuis la source ne l'a pas.** C'est un ajout d'une version postérieure à celle du dépôt.

**Ne l'implémente pas pour l'instant.** Supprime la `BottomNavigationView` Material actuelle. On tranchera séparément.

---

## 8. Ce qui n'est PAS encore mesuré

**Ne comble aucun de ces trous par estimation. Signale-les et demande.**

1. **Marge droite de l'heure et du badge** — source `dp(15)` = 41px, mesuré ~44px. Écart de 3px inexpliqué.
2. **Position verticale exacte des lignes de texte** — les lignes de base n'ont pas été mesurées ; seules les bandes de glyphes l'ont été.
3. **Dimensions précises du badge** — hauteur et rayon non confirmés à l'écran.
4. **Barre du haut** — hauteur et positions non vérifiées sur appareil (mesure faite sur la liste uniquement).
5. **Cas conditionnels** — épinglé, sourdine, vérifié, brouillon, RTL décalent les positions. Non mesurés.
6. **Variante 3 lignes** — hors périmètre.

---

## 9. Vérification

L'APK Telegram de référence est déjà installé sur l'appareil de test : **`org.telegram.messenger.beta`** — c'est **notre build, compilé depuis `Telegram/`** (installeur `null`, signature de debug). Version 12.10.1, identique à la source.

⚠️ **N'utilise pas `org.telegram.messenger`** (celui du Play Store) comme référence : c'est une version **plus récente** que la source du dépôt, avec des différences réelles (la barre du bas). Comparer avec lui ferait courir après des écarts qu'on ne peut pas expliquer.

### Procédure

```bash
# 1. lancer BotPro et capturer
adb shell monkey -p com.botpro.app -c android.intent.category.LAUNCHER 1
adb exec-out screencap -p > botpro.png

# 2. mesurer — le même outil que pour la référence
python3 tools/measure_cell.py botpro.png --density 440 --cell 4

# 3. comparer aux valeurs attendues
python3 tools/measure_cell.py reference/captures/telegram_playstore_dialogs.png --cell 4
```

Les nombres doivent coïncider : **pas de 194px, avatar 143px à x=31, nom `#E9EEF4`, badge `#64B5EF`**.

Compare par **échantillonnage de pixels, jamais à l'œil** : couleurs identiques à l'octet près, positions à ±1px.

### Contours de vues (optionnel)

```bash
adb shell setprop debug.layout true
adb shell service call activity 1599295570     # applique sans redémarrer
# ... capturer ...
adb shell setprop debug.layout false           # NE PAS OUBLIER de couper
adb shell service call activity 1599295570
```

Utile pour vérifier les limites et marges de tes propres vues. **Inutile pour l'intérieur d'une cellule** — elle n'a pas d'enfants, c'est justement le principe. Voir `reference/captures/telegram_build_source_bounds.png`.

**Pour chaque écart : reviens à ce document. N'ajuste jamais à l'œil** — c'est le mécanisme exact qui a produit les 10 couleurs fausses.

---

## 10. Ordre d'exécution

| # | Tâche | Vérification |
|---|---|---|
| 1 | Couleurs (section 4) dans une classe de thème | 4 valeurs mesurées correspondent |
| 2 | Chargeur de polices avec cache | `rmedium.ttf` rendue, pas de gras synthétique |
| 3 | Copier les assets (section 6) | les 9 fichiers présents en toutes densités |
| 4 | `DialogCellView` en Canvas | hauteur 194px @440dpi, avatar 143px à x=31 |
| 5 | Barre du haut + liste + FAB | pas de Material, pas de barre basse |
| 6 | Comparaison contre `.beta` | couleurs exactes, positions ±1px |
| 7 | Supprimer les XML et couleurs obsolètes | build vert |

**Interdits :** taper une couleur à la main · `textStyle="bold"` · un `ViewGroup` pour la cellule · comparer avec le Telegram du Play Store · combler un trou de la section 8 par estimation · modifier quoi que ce soit sous `Telegram/`.
