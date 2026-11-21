# Nesquik's patches

Collection personnelle de 11 patchs Morphe pour **X**, **Reddit** et **YouTube**. Le bundle unique `Nesquiks-patches-0.1.0.mpp` affiche les patchs de l'application et de la version sélectionnées dans Morphe.

## Compatibilité

| Application | Patchs | Versions déclarées |
| --- | --- | --- |
| X | Filtre avancé de mots-clés, masquage des posts sensibles, avatars et bannières, Anonymous Names, flou des médias | 12.43.0-prod.01 |
| Reddit | Filtre local de mots-clés, masquage NSFW, accueil personnalisé | 2026.47.0 |
| YouTube | Ask Native Action Bar 0.8.2 | 21.20.405, 21.30.216, 21.42.241, 21.44.528 |
| YouTube | Bouton Gemini dans la description, expérimental | 21.30.216 |
| YouTube | Recherche dans les playlists, expérimentale | 21.16.256 |

La version expérimentale de Gemini dans la description restaure un bouton déjà reçu par YouTube pour la même vidéo et la même configuration d'écran. Voir les limites dans [la documentation du patch](youtube/native-ask/description/README_FR.md).

## Importer dans Morphe

Dans **Sources → + → Remote**, ajouter `https://github.com/Neszi2317/Nesquik-s-patches`. Tu peux aussi [télécharger le MPP](bundles/Nesquiks-patches-0.1.0.mpp) et l’ajouter depuis ton appareil.

- **X**. Ajoute la source officielle **Piko 3.12.0-dev.5** et le bundle **Nesquik's patches**. Dans le même patching, sélectionne le patch officiel **NewX: Filter posts by keyword**, puis les patchs Nesquik souhaités. Leurs réglages apparaissent sous **Piko → Contenu** sur l'APK généré. Tu peux laisser la liste officielle vide et remplir le filtre avancé. Pour reproduire la configuration testée, laisse **NewX: Show sensitive media** décoché.
- **Reddit**. Garde la source officielle Morphe et sélectionne au moins un patch qui installe ses réglages, par exemple **Hide navigation buttons**, avec les patchs personnalisés. La compilation de référence utilise Morphe Patches **2.4.0-dev.6**.
- **YouTube**. Sur **21.30.216**, les deux patchs Ask/Gemini ont été appliqués avec **Ask flyout menu** et **Hide video action buttons** de Morphe Patches **2.1.0-dev.8**.

Les cinq patchs X sont sélectionnables individuellement. Leur code JVM est chargé par la source Nesquik. Cette source ajoute ses entrées au registre de l'APK généré par Piko et réutilise son mécanisme de filtrage. La version de Piko indiquée est celle vérifiée pour ce portage.

## Développement et publication

| Dossier | Contenu |
| --- | --- |
| [x/](x/) | Le portage X, ses extensions et le pont de réglages Piko |
| [reddit/](reddit/) | Les trois patchs Reddit et leurs scripts de construction |
| [youtube/](youtube/) | Ask Native, Gemini dans la description et recherche dans les playlists |
| [releases/](releases/) | Les six MPP Reddit et YouTube de référence utilisés pour construire le bundle |
| [tools/](tools/) | La compilation du bundle et les dépendances épinglées |
| [verification/](verification/) | Rapports Morphe et contrôle des réglages X dans l'APK |

Avec Java 21 et Python 3.11+ :

```sh
python3 tools/fetch-toolchain.py .toolchain
tools/build-collection.sh 0.1.0 .toolchain
```

Le workflow GitHub **Release Morphe bundle** publie le bundle initial. Pour les versions suivantes, le lancement manuel compile le MPP, publie la release et actualise `patches-bundle.json`.

## Vérifications de la première version

- **X 12.43.0**. 51 patchs appliqués, dont les cinq personnalisés et 46 officiels, en chargeant Piko et Nesquik comme deux sources.
- **Reddit 2026.47.0**. Les trois patchs personnalisés ont été appliqués avec les patchs officiels. Les réglages d'accueil et de filtrage utilisent Morphe Settings.
- **YouTube 21.30.216**. Ask Native et Gemini Description ont été appliqués ensemble avec les deux patchs officiels sélectionnés. La recherche dans les playlists conserve sa cible historique 21.16.256 et a été vérifiée au chargement du catalogue commun.

Les rapports concernent le patching et l'inspection statique des APK. Les tests d'utilisation sur Android restent à faire.

Licence GPL-3.0. Les notices de chaque projet et la licence RE2/J sont conservées dans leurs dossiers.
