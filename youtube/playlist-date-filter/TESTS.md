# Vérifications de la version 0.2.1

La cible reste YouTube **21.30.216** et Morphe **2.4.0-dev.9**. La validation de la version précédente est disponible dans la [release 0.4.3](https://github.com/Neszi2317/Nesquik-s-patches/blob/v0.4.3/youtube/playlist-date-filter/TESTS.md).

| Vérification | Résultat |
| --- | --- |
| Filtre par dates de publication | 26 assertions Java |
| Import Google Takeout | 13 assertions sur les en-têtes français, les horodatages, les dates strictes, le BOM, les guillemets, les doublons incohérents et les colonnes attendues |
| Confirmation d’un ajout | 13 assertions sur les réponses natives et JSON, dont échec, autre playlist, doublon sans résultat d’ajout, suppression, vidéo différente et ajout de plusieurs vidéos |
| Validation de l’APK | Compilation, patching signé et audit des chemins de confirmation sur l’APK fourni |
| Essai avec un compte YouTube réel | À faire sur appareil. Les fixtures reproduisent les champs vérifiés dans le DEX local. |

L’export Takeout français ayant signalé le problème a été lu en entier hors de l’application, sans reprendre ses données dans les tests du dépôt.

Le callback dédié aux éditions de playlists traite la réponse typée du serveur. Le filtre ne stocke aucun horodatage au moment où YouTube prépare la requête. Il choisit les entrées nouvellement ajoutées confirmées dans la réponse, rapproche chaque résultat de la vidéo demandée et conserve l’horodatage existant si le même identifiant d’entrée est rencontré de nouveau.

## Reproduire

```sh
youtube/playlist-date-filter/tests/host.sh .toolchain

java -jar .toolchain/morphe.jar patch --exclusive --validate --bytecode-mode FULL \
  -p .toolchain/official-playlist.mpp -e 'Playlist search' -e 'GmsCore support' \
  -e 'Save to Watch later' -e 'Feed video actions' \
  -p bundles/Nesquiks-patches-0.4.5.mpp -e 'YouTube: Playlist Search & Date Filter' \
  -o out.apk -r patch-report.json youtube-21.30.216.apk

java -Xmx3g -cp .toolchain/morphe.jar \
  youtube/playlist-date-filter/tests/VerifyApk.java out.apk
```

## Essai sur téléphone

1. Choisir une ancienne entrée de la liste connue dans le CSV. Importer le CSV et chercher le jour correspondant en mode **Date d’ajout**.
2. Ajouter une nouvelle vidéo par le bouton natif **Enregistrer**, fermer l’application, la relancer et chercher les ajouts du jour. Répéter avec le raccourci Morphe si activé.
3. Tester le doublon et le retrait suivi d’un nouvel ajout. Le doublon conserve la date existante, tandis qu’un nouvel identifiant d’entrée reçoit la date du nouvel ajout.
4. Ajouter une vidéo sur un deuxième appareil, puis vérifier le compteur des dates inconnues. Réimporter un export plus récent pour récupérer la date si nécessaire.
