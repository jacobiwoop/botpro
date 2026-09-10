# Mission — Reconnaissance de l'écran de conversation

> **Ceci n'est pas une mission d'implémentation.** On ne code rien pour l'instant.
> Objectif : produire un inventaire complet et mesuré, qui servira ensuite de spec.
>
> Méthode imposée : **fan-out sur sous-agents**, un par domaine, puis synthèse.

---

## 0. Pourquoi cette mission existe

L'écran liste de conversations a été porté avec succès en suivant `SPEC_PORTAGE_DIALOGS.md`.
Cette réussite reposait sur un fait : **l'écran tenait dans une seule capture** et une poignée de mesures.

**L'écran de conversation ne tient pas dans une capture.** Ordre de grandeur :

| | Écran liste | Écran conversation |
|---|---|---|
| Activité | 14 435 l. | **47 254 l.** |
| Cellule | 6 498 l. | **29 572 l.** |
| Barre de saisie | — | 15 647 l. |
| **Noyau** | **20 933 l.** | **107 861 l.** |

**38 types de messages** (`MessageObject.TYPE_*`), 6 classes de cellules, ~30 sous-états de dessin,
64 méthodes d'action, et un package `ui/bots/` de 23 fichiers.

Capturer un écran au hasard ne renseigne sur presque rien. D'où l'inventaire systématique.

---

## 1. Règles qui ne changent pas

Elles ont fait leurs preuves sur l'écran précédent — applique-les à l'identique.

1. **N'invente aucune valeur.** Chaque chiffre vient soit de la source (avec `fichier:ligne`), soit d'une mesure sur appareil. Ce qui n'est ni l'un ni l'autre va dans la rubrique « non mesuré ».
2. **Double validation.** Une valeur est fiable quand la source **et** la mesure écran concordent. Note les désaccords, ne les arbitre pas seul.
3. **Référence = `org.telegram.messenger.beta`** (notre build depuis `Telegram/`). Jamais celui du Play Store, qui est plus récent que le dépôt.
4. **Ne modifie rien sous `Telegram/`.** Lecture seule.

### Outils disponibles
```bash
python3 tools/gen_theme.py --key <clé>          # couleur exacte d'une clé
python3 tools/measure_cell.py <png> --density 440
adb connect 192.168.1.14:5555                   # appareil de test (Xiaomi, 1080x2400, 440dpi)
adb shell input keyevent KEYCODE_WAKEUP         # il se met en veille et coupe l'ADB
adb exec-out screencap -p > x.png
adb shell setprop debug.layout true && adb shell service call activity 1599295570
```
⚠️ L'ADB WiFi tombe régulièrement. Reconnecte et réveille l'écran avant chaque capture.

---

## 2. ⚠️ Piège identifié d'avance

Le générateur de thème résout **793 des 823 clés**. Les 30 non résolues sont, presque toutes,
**les couleurs de cet écran** :

```
chat_outBubbleGradient1 / 2 / 3 · chat_outBubbleGradientAnimated
chat_wallpaper · chat_wallpaper_gradient_to1 / to2 / to3 · chat_wallpaper_gradient_rotation
chat_serviceBackground · chat_serviceBackgroundSelected
chat_inBubbleSelectedOverlay · chat_outBubbleSelectedOverlay
```

Elles **n'ont pas de valeur statique** : Telegram les calcule à l'exécution à partir du fond d'écran.

**C'est un objet d'enquête à part entière** (piste 7). Ne les devine surtout pas —
c'est le seul endroit où la méthode qui a marché jusqu'ici ne s'applique pas telle quelle.

---

## 3. Découpage en sous-agents

Lance **une piste par sous-agent**, en parallèle. Chacun rend une fiche au format de la section 4.

### Piste 1 — Ossature de l'écran
`ChatActivity.java`
Barre du haut (avatar, titre, sous-titre « en ligne / frappe… », actions), fond d'écran,
liste des messages, bandeau de message épinglé, bouton « descendre », compteur de non-lus,
gestion des insets système (**la barre de statut a déjà posé problème sur l'écran précédent**).

### Piste 2 — Bulles : géométrie et regroupement
`ChatMessageCell.java`
Formes de bulle entrante/sortante, rayons des angles, **pointe (tail)**, largeur maximale,
marges, et surtout le **regroupement** : premier / milieu / dernier message d'une salve,
qui change les angles et l'affichage de l'avatar.

### Piste 3 — Types de contenu
`MessageObject.java` + `ChatMessageCell.java`
Les 38 `TYPE_*`. **Priorise pour un client de bots** :
`TEXT` · `PHOTO` · `VIDEO` · `FILE` · `VOICE` · `STICKER` · `GIF` · aperçu de lien · `DATE`.
Signale les autres sans les détailler.

### Piste 4 — Barre de saisie
`ChatActivityEnterView.java` (15 647 l.)
⚠️ **Contrairement aux cellules, elle utilise de vraies vues** (62 `addView`, 1 `R.layout`).
Champ de texte, emoji, pièce jointe, envoi, micro, menu de commandes de bot, hauteurs, états.

### Piste 5 — Claviers et boutons de bots ★
`ui/bots/` (23 fichiers) + `ui/Cells/BotButton.java`
`BotButtons.java` · `BotKeyboardView.java` · `BotCommandsMenuView.java` ·
`ChatActivityBotWebViewButton.java`
Clavier inline sous message, clavier de réponse, menu de commandes, bouton WebApp.
**C'est la piste la plus importante pour BotPro** — traite-la en premier si tu dois arbitrer.

### Piste 6 — Comportements et interactions
`ChatActivity.java` (64 méthodes d'action)
Réponse par glissement, menu au appui long, sélection multiple, saut à un message,
indicateur de frappe, accusés de lecture, en-tête de date flottant, séparateur de non-lus,
défilement, édition, suppression.
**Décris le comportement observable**, pas l'implémentation.

### Piste 7 — Couleurs, thème et fond d'écran ★ difficile
Toutes les clés `chat_*` de `ThemeColors.java` + le mécanisme des 12 clés non résolues (section 2).
Comment le dégradé des bulles sortantes est-il calculé ? D'où vient `chat_serviceBackground` ?
**Si tu ne trouves pas, dis-le.** Une réponse « non élucidé, voici les pistes » vaut mieux qu'une invention.

### Piste 8 — Cellules annexes
`ChatActionCell.java` (messages de service) · `ChatUnreadCell.java` (barre « messages non lus ») ·
`ChatLoadingCell.java` · en-têtes de date.

---

## 4. Format de rendu — une fiche par piste

Fichier : `reference/conversation/piste-N-<nom>.md`

```markdown
# Piste N — <nom>

## Résumé
3 lignes : ce que couvre ce domaine, son poids, sa difficulté.

## Éléments
Pour chaque élément visuel ou comportemental :
- **Nom** — à quoi ça sert
- Source : `fichier.java:ligne`
- Valeurs : dimensions en dp, couleurs par clé de thème
- Conditions d'apparition
- Capture : `reference/conversation/captures/<nom>.png` si obtenue

## Mesuré sur appareil
| Élément | Source | Calcul px | Mesuré px | OK ? |

## Non mesuré / non élucidé
Liste franche. **C'est la section la plus importante de ta fiche.**

## Recommandation pour BotPro
Indispensable / utile / hors sujet — avec justification.
```

---

## 5. Captures — méthodiquement

Une capture par état, pas une capture générale. Range-les dans
`reference/conversation/captures/` avec un nom explicite : `bulle-sortante-groupee.png`,
`clavier-inline-3-boutons.png`, `barre-saisie-vocal.png`…

Active les contours de vues (`debug.layout`) pour distinguer **ce qui est peint** de
**ce qui est une vraie vue** — la distinction est capitale ici, puisque les cellules sont
peintes mais la barre de saisie non.

⚠️ **Confidentialité** : les conversations personnelles du propriétaire apparaissent dans les
captures. Privilégie des salons publics, des bots, ou « Messages enregistrés ».
Ne pousse aucune capture sans son accord — vérifie `.gitignore`.

---

## 6. Synthèse finale

Après les 8 pistes, produis `reference/conversation/SYNTHESE.md` :

1. **Tableau récapitulatif** — tous les éléments, leur poids, leur priorité pour BotPro
2. **Périmètre proposé** — ce qu'on implémente, ce qu'on reporte, ce qu'on abandonne
3. **Les inconnues** — consolidées depuis les 8 pistes
4. **Estimation** — par rapport à l'écran liste, déjà porté, qui sert d'étalon

**Ne propose pas de plan d'implémentation.** La décision de périmètre revient au propriétaire.

---

## 7. Critère de réussite

La mission est réussie si, à la lecture de la synthèse, on peut **décider quoi construire**
sans rouvrir la source Telegram.

Elle est ratée si elle contient une valeur inventée — même une seule, même plausible.
Sur l'écran précédent, deux erreurs sont passées dans mes propres instructions
(la barre d'onglets basse, la couleur de l'en-tête). Elles n'ont été rattrapées que par la mesure.
**Mesure. Ne juge pas à l'œil.**
