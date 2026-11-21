# YouTube – Search in Playlists

**Version 0.1.0-experimental — YouTube 21.16.256 uniquement.**

Le `.mpp` est un bundle Morphe compilé réel, avec code JVM, code DEX et extension Android. **Le fonctionnement complet sur téléphone n’est pas encore confirmé.**

## Implémenté

Loupe bleue en haut à droite du contenu de la playlist ; fenêtre dédiée ; recherche par titre **ou** nom de chaîne ; casse et accents ignorés ; déduplication et ordre conservé ; chargement progressif des continuations ; résultats cliquables dans YouTube ; annulation et actualisation. La recherche globale n’est pas modifiée.

Les requêtes reprennent la route InnerTube authentifiée déjà employée par Morphe pour les playlists. L’index reste en mémoire pendant la fenêtre ; les changements de session invalident ses résultats. Aucun cookie demandé, aucun secret enregistré.

## Tests réellement exécutés

| Vérification | Résultat |
|---|---|
| Compilation Java/Kotlin et D8, intégrité du bundle | Réussite |
| Chargement du `.mpp` par Morphe Desktop / Patcher 1.15.1 | Réussite |
| Application à l’APK fourni et reconstruction DEX | Réussite |
| Application combinée avec Channel search, Navigation bar, Hide layout components et GmsCore support (Morphe Patches 1.46.0) | Réussite, aucun patch en échec |
| 20 assertions sur fixtures : filtre, accents, ordre, 1 100 vidéos paginées, isolation, formats invalides | Réussite |
| Requête réelle sur une playlist publique de 11 vidéos : filtre par chaîne | 11 vidéos récupérées et trouvées |
| Pagination réelle d’une grande playlist publique | 19 réponses, 1 100 vidéos décodées et recherchables par chaîne |

La grande playlist publique avait encore des pages après la 1 100e vidéo : le test s’est arrêté volontairement, sans prétendre avoir parcouru toute cette playlist.

## Non testé sur appareil

Import réel dans Morphe Manager Android, démarrage de YouTube, visibilité et placement de la loupe, « À regarder plus tard », playlists privées, ouverture des vidéos et retour arrière, rotation et changement de compte en usage réel. La reconstruction avec les autres patchs ne garantit pas à elle seule l’absence de conflit d’interface.

## Limites

- La loupe est superposée au contenu, pas ajoutée au menu natif de la barre d’outils.
- Pas de cache permanent ni de mise à jour automatique pendant le parcours : actualiser après un ajout ou une suppression.
- Les formats serveur inconnus sont signalés ; aucune recherche globale de secours.
- Les métadonnées absentes limitent la recherche par chaîne.
- Limites de sécurité : 1 000 pages / environ 20 000 vidéos, signalées comme résultats partiels.
- Aucun test ni compatibilité déclarée pour 21.40.161.

## Validation sur ton téléphone

Importer le `.mpp`, sélectionner le patch avec YouTube 21.16.256, patcher puis installer selon la procédure habituelle. Ouvrir « À regarder plus tard » → nouvelle loupe bleue → saisir `Polo`. Attendre « Parcours terminé à cet instant », vérifier une correspondance par titre et une par chaîne, puis ouvrir un résultat et revenir. Si la session n’est pas disponible, fermer la fenêtre, recharger la playlist et réessayer.

Sources, instructions de compilation, fingerprints résolus et journaux de test sont dans le ZIP. Patch personnalisé non officiel, distribué avec les mentions GPL/Morphe.
