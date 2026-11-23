# Vérifications de la version 0.1.0

| Vérification | Résultat |
| --- | --- |
| Tests hôte Java | 26 assertions réussies sur les deux bornes inclusives, un seul jour, une seule borne, les bornes inversées, la réinitialisation, les dates inconnues et l’extraction de la date exacte |
| Métadonnées YouTube en direct | Le champ `playerMicroformatRenderer.publishDate` contenait un horodatage explicite pour trois vidéos publiques. Test depuis l’environnement de build |
| Construction et archive MPP | Module autonome et collection compilés. Les archives passent le contrôle ZIP |
| Chargement dans Morphe Desktop 2.3.0 | Nouvelle entrée visible pour YouTube 21.30.216, désactivée par défaut. La collection affiche 20 entrées |
| Chargeur DEX de Morphe Manager 2.7.1 | Exécuté avec ART sur Android 15. Les 20 entrées du MPP publié sont chargées, et le nouveau patch est trouvé par son nom |
| Catalogue distant | `patches-bundle.json` référence 0.4.3. Les deux MPP téléchargés depuis GitHub sont identiques aux fichiers testés |
| Longue playlist publique | La route de pagination d’origine a renvoyé 1 520 entrées sur 26 pages lors d’un essai réseau. Le test ne mesure pas la vitesse des recherches par date sur Android |
| Patching de l’APK fourni | APK YouTube 21.30.216 patché et signé, avec validation des fingerprints et mode bytecode FULL |
| Patching avec d’autres patchs | Réussi avec Playlist search, GmsCore support, Ask flyout menu, Force native Gemini summary button (Description), ZZ Ask native action button et Universal auto-translated captions |
| Patch officiel absent | Le patching échoue avec le message qui indique d’activer Playlist search |
| Audit de l’APK combiné | Signature valide, 1 hook dans la barre de playlist, 1 appel à la boîte de dialogue augmentée, 195 références à l’extension officielle résolues et aucune classe DEX dupliquée |
| Régression de la collection | 440 classes JVM et extensions déjà présentes dans 0.4.2 identiques octet pour octet dans 0.4.3 |

Le test de l’APK ne couvre que 21.30.216. Dans Morphe 2.4.0-dev.9, la recherche officielle déclare aussi 20.26.46, 20.51.39, 21.23.492, 21.28.208, 21.44.528, 21.45.82 et 21.46.522.

## Reproduire le patching

Depuis la racine du dépôt, une fois le JDK 21 et les dépendances installés.

```sh
java -jar .toolchain/morphe.jar patch --exclusive --validate --bytecode-mode FULL \
  -p .toolchain/official-playlist.mpp -e 'Playlist search' -e 'GmsCore support' -e 'Ask flyout menu' \
  -p bundles/Nesquiks-patches-0.4.3.mpp \
  -e 'YouTube: Playlist Search & Date Filter' \
  -e 'Force native Gemini summary button (Description)' \
  -e 'ZZ Ask native action button' -e 'Universal auto-translated captions' \
  -o out.apk -r patch-report.json youtube-21.30.216.apk

java -Xmx3g -cp .toolchain/morphe.jar \
  youtube/playlist-date-filter/tests/VerifyApk.java out.apk
```

## Scénarios sur Android

L’installation des APK par ADB a été interrompue par le service Android de gestion des paquets sur l’émulateur logiciel. Le contrôle du chargeur DEX a bien abouti via ART. Les interactions à l’écran et la recherche datée sur une liste privée restent à valider sur un appareil.

1. Ouvre YouTube patché et une playlist publique contenant des vidéos dont les jours de publication sont connus.
2. Avec la loupe de playlist, recherche uniquement un nom dans le titre ou dans la chaîne. Compare avec le résultat du patch officiel seul.
3. Efface le texte et cherche une période qui inclut exactement une vidéo à chaque borne. Vérifie l’ordre. Répète avec une seule borne, une date identique pour les deux champs, puis une période vide.
4. Combine un mot et les deux dates. Inverse les bornes puis vérifie que la boîte de dialogue reste ouverte et signale l’erreur. Efface les dates et relance une recherche texte.
5. Rouvre les dates, touche **Annuler** après une modification, puis ouvre une autre playlist. Vérifie que le dernier filtre validé est conservé dans la première playlist et que l’autre a son propre filtre.
6. Sur une liste privée ou **Vidéos J’aime**, vérifie que le moteur atteint les entrées de fin. Essaie une playlist de plus de 1 500 vidéos. Les résultats peuvent arriver par pages. Vérifie le compteur de dates inconnues en présence de vidéos indisponibles. Les entrées que YouTube masque côté serveur peuvent manquer du total.
7. Répète en clair et en sombre. Ferme la feuille pendant une recherche longue et rouvre-la. Vérifie la navigation vers une vidéo depuis un résultat.

## Reproduire le contrôle du chargeur de Manager

Le test utilise le chargeur de Morphe Manager présent dans son APK officiel 2.7.1, via `app_process` sur un émulateur Android. Depuis la racine du dépôt, sur un émulateur de test joignable par ADB.

```sh
mkdir -p /tmp/morphe-manager-probe
javac --release 8 -d /tmp/morphe-manager-probe youtube/playlist-date-filter/tests/ManagerProbe.java
java -cp .toolchain/r8.jar com.android.tools.r8.D8 --min-api 29 --lib .toolchain/android.jar \
  --output /tmp/morphe-manager-probe /tmp/morphe-manager-probe/ManagerProbe.class
adb push /tmp/morphe-manager-probe/classes.dex /data/local/tmp/playlist-probe.dex
adb push morphe-manager-2.7.1.apk /data/local/tmp/playlist-manager.apk
adb push bundles/Nesquiks-patches-0.4.3.mpp /data/local/tmp/playlist-bundle.mpp
adb shell chmod 444 /data/local/tmp/playlist-bundle.mpp
adb shell 'CLASSPATH=/data/local/tmp/playlist-probe.dex:/data/local/tmp/playlist-manager.apk app_process /system/bin ManagerProbe /data/local/tmp/playlist-bundle.mpp'
```
