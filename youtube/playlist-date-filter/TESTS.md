# Vérifications du filtre de dates

## Version 0.2.3

| APK YouTube | Morphe Patches | Vérification |
| --- | --- | --- |
| 21.30.216 | 2.4.0-dev.9 | Patching et signature avec le patch historique. Une redirection de dialogue, une confirmation native et deux chemins d’ajout Morphe. |
| 21.47.483 | 2.4.0-dev.12 | Patching et signature avec les deux patchs de la tablette. Même vérification de la recherche et des confirmations. Le tracé arrondi et les deux alignements centrés sont présents dans le DEX. |
| 21.46.522 | — | Patching et signature de la variante des sous-titres arrondis. |

Les 67 assertions JVM de la suite historique passent. Les résultats de recherche sur un compte privé et l’affichage sur la tablette restent à vérifier en utilisation réelle.

Pour reproduire le test 21.47.483 avec le bundle complet, utilise le MPP officiel 2.4.0-dev.12.

```sh
java -jar .toolchain/morphe.jar patch --exclusive --validate --bytecode-mode FULL \
  -p patches-2.4.0-dev.12.mpp -e 'Playlist search' -e 'GmsCore support' \
  -e 'Save to Watch later' -e 'Feed video actions' \
  -p bundles/Nesquiks-patches-0.4.7.mpp -e 'YouTube: Playlist Search & Date Filter (21.47.483)' \
  -e 'Rounded YouTube Captions (21.47.483)' \
  -o out-21.47.483.apk -r patch-report.json youtube-21.47.483.apk

java -Xmx3g -cp .toolchain/morphe.jar \
  youtube/playlist-date-filter/tests/VerifyApk.java out-21.47.483.apk 21.47.483
```

## Version 0.2.2

La cible reste YouTube **21.30.216** et Morphe **2.4.0-dev.9**. La validation de la version précédente est disponible dans la [release 0.4.3](https://github.com/Neszi2317/Nesquik-s-patches/blob/v0.4.3/youtube/playlist-date-filter/TESTS.md).

| Vérification | Résultat |
| --- | --- |
| Filtre par dates de publication | 26 assertions Java |
| Import Google Takeout | 13 assertions sur les en-têtes français, les horodatages, les dates strictes, le BOM, les guillemets, les doublons incohérents et les colonnes attendues |
| Rapprochement local des dates | 15 assertions sur les dates inconnues, la première association, le fuseau, les entrées retirées puis réajoutées et la priorité des confirmations |
| Confirmation d’un ajout | 13 assertions sur les réponses natives et JSON, dont échec, autre playlist, doublon sans résultat d’ajout, suppression, vidéo différente et ajout de plusieurs vidéos |
| Bundle Morphe | Archives MPP validées et patch listé pour YouTube 21.30.216 via la CLI |
| Validation de l’APK | Compilation, patching signé et audit des appels d’import et de confirmation dans l’APK fourni |
| Essai avec un compte YouTube réel | Le chargement des pages privées et l’affichage des résultats restent à vérifier sur téléphone. |

La version 0.2.1 rejetait l’import si le chargement complet de la playlist échouait après lecture du CSV. La version 0.2.2 enregistre les dates dans la base locale dès que le CSV est validé. Elle n’associe chaque date qu’au moment où l’identifiant de l’entrée apparaît dans une page. Une pagination interrompue conserve les résultats déjà trouvés et affiche le manque de couverture.

Le callback dédié aux éditions de playlists traite la réponse typée du serveur. Le filtre ne stocke aucun horodatage au moment où YouTube prépare la requête. Il choisit les entrées nouvellement ajoutées confirmées dans la réponse, rapproche chaque résultat de la vidéo demandée et conserve l’horodatage existant si le même identifiant d’entrée est rencontré de nouveau.

## Reproduire

```sh
youtube/playlist-date-filter/tests/host.sh .toolchain

java -jar .toolchain/morphe.jar patch --exclusive --validate --bytecode-mode FULL \
  -p .toolchain/official-playlist.mpp -e 'Playlist search' -e 'GmsCore support' \
  -e 'Save to Watch later' -e 'Feed video actions' \
  -p bundles/Nesquiks-patches-0.4.6.mpp -e 'YouTube: Playlist Search & Date Filter' \
  -o out.apk -r patch-report.json youtube-21.30.216.apk

java -Xmx3g -cp .toolchain/morphe.jar \
  youtube/playlist-date-filter/tests/VerifyApk.java out.apk
```

## Essai sur téléphone

1. Choisir une ancienne entrée de la liste connue dans le CSV. Dans « À regarder plus tard », ouvrir **Date d’ajout** et importer le CSV. Vérifier le nombre de dates stockées. Chercher ensuite le jour de l’entrée, fermer et rouvrir YouTube, puis répéter la recherche sans réimporter.
2. Ajouter une nouvelle vidéo par le bouton natif **Enregistrer**, fermer l’application, la relancer et chercher les ajouts du jour. Répéter avec le raccourci Morphe si activé.
3. Tester le doublon et le retrait suivi d’un nouvel ajout. Le doublon conserve la date existante, tandis qu’un nouvel identifiant d’entrée reçoit la date du nouvel ajout.
4. Ajouter une vidéo sur un deuxième appareil, puis vérifier le compteur des dates inconnues. Réimporter un export plus récent pour récupérer la date si nécessaire.
