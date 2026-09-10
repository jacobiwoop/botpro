# Mission — Écran de conversation BotPro : approfondir puis construire

> Deux phases. **Phase A** : combler ce qui manque. **Phase B** : construire, avec des données
> simulées qui rejouent exactement ce qui a été capturé.
>
> Toutes les valeurs de départ sont dans `reference/conversation/CORRESPONDANCES.md`
> (10 captures analysées). **Lis-le en entier avant de commencer.**

---

## 0. La règle, inchangée

**N'invente aucune valeur visuelle.** Chaque chiffre vient soit de la source Telegram (avec
`fichier:ligne`), soit d'une mesure sur appareil. Ce qui n'est ni l'un ni l'autre va dans la
rubrique « non mesuré » — et tu le signales au lieu de le combler.

Cette règle a déjà rattrapé quatre erreurs, dont **deux dans les instructions elles-mêmes**
(barre d'onglets basse annoncée absente à tort, couleur d'en-tête erronée). Si une consigne de
ce document contredit une mesure, **c'est la mesure qui gagne** — signale-le.

L'écran liste de conversations a été porté avec succès selon cette méthode
(`SPEC_PORTAGE_DIALOGS.md`). `DialogCellView.kt` est ton modèle de référence : une `View`
unique qui peint dans `onDraw`.

---

## 1. Outils déjà disponibles

```bash
./tools/capture.sh <nom>                  # rendu + contours de vues + arbre des vues
python3 tools/analyze_capture.py <nom>    # sépare le peint des vraies vues, en dp
python3 tools/classify_messages.py <nom>  # type, direction, drapeaux de chaque message
python3 tools/classify_messages.py --all  # inventaire + liste des types jamais capturés
python3 tools/gen_theme.py --key <clé>    # couleur exacte d'une clé de thème
python3 tools/measure_cell.py <png>       # géométrie au pixel
```

Appareil : `adb connect 192.168.1.14:5555` — Xiaomi, 1080×2400, **densité 440 → 1dp = 2,75px**.
⚠️ L'ADB WiFi tombe souvent. Reconnecte et `adb shell input keyevent KEYCODE_WAKEUP` avant chaque capture.

Référence de comparaison : **`org.telegram.messenger.beta`** (notre build depuis `Telegram/`).
⚠️ Les 10 captures existantes viennent de `org.telegram.messenger` (Play Store), **plus récent
que la source**. Divergence déjà constatée sur l'écran liste. Revérifie avant de porter.

---

## 2. PHASE A — Approfondir (fan-out sur sous-agents)

### A1 ★ Boutons inline de bot — la lacune critique

**Jamais capturé, et c'est le seul élément bot qui soit peint** (`ChatMessageCell.drawBotButtons(Canvas…)`,
`BotButton` n'est pas une `View`). L'arbre des vues ne le montrera pas : mesure au pixel obligatoire.

À obtenir : un bot qui répond avec des boutons **dans la bulle**. Puis mesurer : hauteur,
largeur, écarts, rayons, couleurs de fond et de texte, disposition multi-colonnes, état pressé.

Ne confonds pas avec le **clavier de réponse** (conv-09), qui lui est en vraies vues.

### A2 Rayons des bulles — mesure à refaire

Les rayons relevés en conv-02 sont **faux** : le fond d'écran à motif a faussé la détection de bord.
Refais-les autrement — par exemple sur une capture où une bulle chevauche une zone de fond uniforme,
ou en isolant la bulle par sa couleur exacte (`#232E3B` entrante).

À produire : rayon des 4 angles, entrante et sortante, cas isolé **et** cas groupé (les angles
changent selon la position dans une salve).

### A3 Le dégradé sortant — ancrage à déterminer

Établi : la couleur d'une bulle sortante dépend de sa position verticale, identiquement d'une
conversation à l'autre (conv-05 vs conv-07).

**Non établi** : le dégradé est-il ancré à l'écran, ou au contenu qui défile ?
→ Capture **la même bulle** à deux positions de défilement différentes. Si sa couleur change,
l'ancrage est l'écran ; si elle reste, c'est le contenu.

Cette réponse détermine toute l'implémentation du dégradé. Ne la devine pas.

### A4 Fond d'écran

Le motif est un `TLRPC.TL_wallPaper` **téléchargé depuis les serveurs Telegram** — absent du code
source et des `.attheme` (vérifié : aucun marqueur `WPS`). Plus `MotionBackgroundDrawable`,
dégradé animé à 4 couleurs.

Trancher entre : extraire du cache de l'app · fournir un motif propre à BotPro · dégradé uni sans motif.
**Documente le coût de chaque option**, ne choisis pas seul.

### A5 Types de messages manquants

`classify_messages.py --all` liste ce qui n'a jamais été capturé : vocal, sticker, gif, sondage,
contact, localisation, musique, vidéo ronde.
Pour un client de bots, **priorise** : vocal, sticker, photo (déjà vu une fois), aperçu de lien.
Signale les autres sans les détailler.

### A6 Bug connu de l'outil

Les messages **contenant une citation** ne sont pas classés (trous dans conv-08).
Leur attribut `text` se comporte autrement. Élucide et corrige `classify_messages.py`.

---

## 3. PHASE B — Construire

### B1 ★ Le jeu de données simulé — à faire en premier

BotPro n'a pas de données réelles. **Sans jeu d'essai, rien n'est vérifiable.**

Crée un jeu de conversations de démonstration **qui rejoue exactement ce que montrent les captures**,
pour pouvoir comparer côte à côte. Étends `data/model/Models.kt` — **sans jamais introduire de type TLRPC**.

Fixtures à produire, une par élément mesuré :

| Fixture | Rejoue | Référence |
|---|---|---|
| Texte court entrant / sortant | cellule 107px, pointe de bulle | conv-07 |
| Texte long multi-lignes entrant | bulle 8 et 12 lignes | conv-02 |
| Salve de messages consécutifs | groupage, angles modifiés | conv-05 |
| Séparateurs de date | pastille 30,9dp | toutes |
| Message avec accusé de lecture | double coche `#87C7FF` | conv-05 |
| Document avec nom + taille | icône ronde, nom, poids | conv-05 |
| Message transféré | en-tête « Transféré de » | conv-05 |
| Message avec citation | barre verte 3,3dp, fond `#2C3D40` | conv-08 |
| Commande de bot envoyée | `/stop`, `/resume` | conv-08 |
| Mention et commande cliquables | texte bleu dans la bulle | conv-05, conv-08 |
| **Clavier de réponse de bot** | 3 boutons, 44dp, `#293645` | conv-09 |
| **Menu de commandes** | 10 commandes, lignes 37,5 / 56,4dp | conv-10 |
| **Boutons inline** | à définir après A1 | — |
| Conversation vide | panneau « aucun message » | conv-06 |
| Bot vs humain | sous-titre « bot », pas d'appel | conv-08 |

Le but n'est pas de faire joli : c'est de pouvoir lancer BotPro et **mesurer le même élément
que sur la capture Telegram correspondante**.

### B2 Ordre de construction

| # | Élément | Vérification |
|---|---|---|
| 1 | Modèle + fixtures (B1) | l'écran affiche les cas |
| 2 | Fond (dégradé, motif selon A4) | couleur au point échantillonné |
| 3 | `MessageCellView` — texte, bulle, pointe, heure | 107px, pointe +15dp à droite |
| 4 | Dégradé sortant | couleur varie avec la position (cf. A3) |
| 5 | Séparateur de date | 30,9dp |
| 6 | Groupage, accusés de lecture, citation, transfert | `#87C7FF`, barre verte 3,3dp |
| 7 | Barre du haut — 2 variantes (humain / bot) | pas d'appel en bot ; titre 218,5dp |
| 8 | Barre de saisie — **3 états** | champ 233,8 / 96,4 / 138,9 dp |
| 9 | Clavier de réponse de bot | 44dp, 122,9dp, écarts 4dp, marges 8dp |
| 10 | Menu de commandes | lignes 37,5 / 56,4dp, poignée 21,8dp |
| 11 | Boutons inline (peints) | selon A1 |
| 12 | Mode sélection | bulle `#232E3B` → `#314A61` |

### B3 Contraintes structurelles — non négociables

**La cellule de message est une `View` unique qui peint dans `onDraw`.**
Vérifié : `ChatMessageCell` = 0 `R.layout`, 0 `TextView`, 115 appels Canvas ; et sur appareil,
chaque message est un `ViewGroup` à **0 enfant**. Un assemblage de `TextView` ne reproduira ni
les pointes, ni le dégradé positionnel, ni les lignes de base.

**La barre du haut flotte au-dessus de la liste.** Établi en conv-04 : quand le clavier s'ouvre,
la liste défile et son contenu **glisse sous la barre** (le séparateur de date passe de 85px à 66px,
tronqué). Donc : liste pleine hauteur, `clipToPadding = false`, rembourrage haut = hauteur de barre.
Un empilement vertical est faux.

**Les insets système sont obligatoires.** `targetSdk 35` impose le bord à bord. Le contenu de la
barre du haut démarre à `bars.top` (111px sur l'appareil de test — **ne code pas cette valeur**,
lis l'inset). Cette erreur a déjà été commise sur l'écran liste.

**Deux mécanismes distincts pour les claviers de bots** :
clavier de réponse = vraies vues (`LinearLayout`) · boutons inline = peints au Canvas.

**Le champ de saisie n'est pas de largeur fixe** : 233,8dp normal, 96,4dp avec bot, 138,9dp avec
clavier ouvert. Et le bouton Menu a trois apparences (☰+libellé, ☰ seul, ✕+libellé).

---

## 4. Boucle de vérification

```bash
adb shell monkey -p com.botpro.app -c android.intent.category.LAUNCHER 1
adb exec-out screencap -p > botpro_conv.png
python3 tools/measure_cell.py botpro_conv.png --density 440
```

Compare **chiffre à chiffre** avec `CORRESPONDANCES.md`. Couleurs identiques à l'octet,
positions à ±1px. **N'ajuste jamais à l'œil** — c'est le mécanisme exact qui a produit
10 couleurs fausses dans la première tentative.

Pour un élément peint, l'arbre des vues ne sert à rien : mesure sur le PNG.

---

## 5. Livrables

1. `reference/conversation/captures/` — les nouvelles captures de la phase A
2. `reference/conversation/CORRESPONDANCES.md` — enrichi (A1→A6), **sans écraser l'existant**
3. Le code de l'écran, avec les fixtures
4. `reference/conversation/RAPPORT.md` :
   - tableau mesuré vs obtenu, par élément
   - **ce qui reste non mesuré ou non conforme** — la section la plus importante
   - captures avant/après

**Ne déclare pas « conforme » un élément que tu n'as pas mesuré.** Dis « non vérifié ».

---

## 6. Critère de réussite

Réussi si l'on peut poser BotPro et Telegram côte à côte sur le même appareil et retrouver les
mêmes chiffres sur les éléments traités.

Raté s'il contient une valeur inventée — même une seule, même plausible.
