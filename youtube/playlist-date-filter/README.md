# YouTube Playlist Search & Date Filter

Version du module **0.1.0**, dans **Nesquik’s patches 0.4.3**. La cible déclarée est YouTube **21.30.216** avec la source officielle Morphe **2.4.0-dev.9**.

## Installation

1. Dans Morphe Manager, mets à jour la source « Nesquik’s patches ». Son [catalogue](https://raw.githubusercontent.com/Neszi2317/Nesquik-s-patches/refs/heads/main/patches-bundle.json) distribue le bundle de la collection.
2. Ajoute ou active la variante de développement des patchs officiels qui contient **Playlist search**. La version validée pour ce module est [2.4.0-dev.9](https://github.com/MorpheApp/morphe-patches/releases/tag/v2.4.0-dev.9). Tu peux importer son MPP localement pour épingler la version.
3. Dans la même opération de patching de YouTube **21.30.216**, sélectionne **Playlist search** dans la source officielle et **YouTube: Playlist Search & Date Filter** dans la source Nesquik. Pour une installation sans root, sélectionne également le support GmsCore selon ton installation habituelle.
4. Ouvre une playlist et touche sa loupe Morphe, à côté de la recherche générale YouTube. Dans **Rechercher dans la playlist**, saisis un texte, une date ou les deux, puis touche **Rechercher**.
5. Pour retrouver la recherche textuelle habituelle, ouvre la même boîte de dialogue, touche **Effacer les dates**, puis **Rechercher**. **Annuler** abandonne les changements de la boîte de dialogue.

## Comportement

Le texte et les dates sont indépendants. Avec deux dates, les deux jours limites sont inclus. Avec une seule date, le filtre commence ou se termine ce jour-là. Si les dates sont inversées, le dialogue affiche l’erreur et bloque la recherche. La période appliquée est retenue par playlist tant que l’activité YouTube reste en mémoire.

La recherche conserve l’ordre et les doublons de la playlist. Le mode période affiche les correspondances au fil des pages et garde des boutons **Précédent** et **Suivant** lorsque plus de 60 résultats correspondent. Les résultats gardent la lecture dans la playlist d’origine.

Le filtre utilise le jour civil de publication fourni explicitement par YouTube. Les entrées sans date disponible apparaissent dans le compteur **dates inconnues** et les entrées illisibles dans un compteur distinct. Les résultats ne contiennent que les correspondances confirmées. YouTube peut omettre certaines vidéos indisponibles du listing. Si une continuation échoue, la feuille signale la couverture partielle et conserve les correspondances déjà trouvées. Le filtre par date d’ajout est absent, car la route réutilisée ne fournit pas cette information de façon fiable.

## Intégration et attribution

Le patch officiel est livré dans [Morphe 2.4.0-dev.9](https://github.com/MorpheApp/morphe-patches/tree/94c14a4f) avec le code du [PR 5200](https://github.com/MorpheApp/morphe-patches/pull/5200). L’original ajoute la loupe au menu d’une page playlist, suit le browse ID et ouvre `SearchDialog`, partagé avec la recherche dans les chaînes et les commentaires. Son moteur parcourt les continuations avec la requête de playlist du compte actif, recherche chaque mot dans le titre ou la chaîne et affiche la feuille `SearchResultsSheet`. La recherche officielle conserve ses listes complètes cinq minutes en mémoire.

L’extension remplace l’appel à la boîte de dialogue dans le seul point d’entrée de la recherche des playlists. Le bouton, les chaînes existantes, le thème et le composant Morphe `CustomDialog` restent à leur emplacement. Le mode texte seul rappelle le callback officiel avec son cache et ses propres résultats. Le mode date réutilise la pagination, la comparaison textuelle, le modèle vidéo et le rendu Morphe. Le moteur date consulte d’abord le texte et réutilise les dates exactes pendant 24 heures en mémoire. Il demande les autres dates à YouTube hors du thread principal, au maximum quatre requêtes en parallèle. Un code 429 ou 503 déclenche une pause de deux minutes. Une longue recherche à froid peut prendre plusieurs minutes. La pagination de playlist réutilise le compte courant lorsqu’il est disponible. La requête complémentaire de date ne transmet pas les en-têtes du compte ni le texte de recherche.

Le code adapté est distribué sous GPLv3. Les avis et l’origine du code sont conservés dans [LICENSE](LICENSE) et [NOTICE](NOTICE).

## Compilation reproductible

Depuis la racine du dépôt avec un JDK 21 dans le PATH.

```sh
python3 tools/fetch-toolchain.py .toolchain
youtube/playlist-date-filter/build.sh .toolchain dist/YouTube-Playlist-Search-Date-Filter-0.1.0.mpp
youtube/playlist-date-filter/tests/host.sh .toolchain
tools/build-collection.sh 0.4.3 .toolchain
```

Le build du module génère des références de compilation depuis le MPP officiel épinglé. Le runtime officiel vient de la source Morphe sélectionnée lors du patching. Le catalogue de la collection utilise le workflow de publication habituel.
