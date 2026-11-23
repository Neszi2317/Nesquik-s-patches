# Nesquik's patches

Collection personnelle de 19 entrées de patchs Morphe pour **X**, **Reddit**, **YouTube**, **Firefox** et **Discord**. Le bundle unique affiche les patchs de l'application et de la version sélectionnées dans Morphe.

## Compatibilité

| Application | Patchs | Versions déclarées |
| --- | --- | --- |
| X | Filtre avancé de mots-clés, masquage des posts sensibles, avatars et bannières, Anonymous Names, flou des médias, masquage permanent par identifiant | 12.43.0-prod.01 |
| Reddit | Filtre local de mots-clés, masquage NSFW, accueil personnalisé | 2026.47.0 |
| YouTube | Ask Native Action Bar 0.8.2 | 21.20.405, 21.30.216, 21.42.241, 21.44.528 |
| YouTube | Bouton Gemini dans la description, expérimental | 21.30.216 |
| YouTube | Recherche dans les playlists, expérimentale | 21.16.256 |
| YouTube | [Traduction automatique des sous-titres 0.4.0](youtube/auto-translated-captions/) | 21.16.256 et 21.46.522 testées par patching |
| YouTube | [Sous-titres arrondis](https://github.com/Neszi2317/Rounded-Subtiles/tree/v1.0.0-dev.6) | 21.20.405, 21.44.528, 21.45.82, 21.46.522 |
| Firefox | [Blocage local de sites 0.1.3](firefox/) | 159.0, 160.0, 160.0.1 |
| Discord | [Masquage des utilisateurs bloqués ou ignorés](discord/) | 351.11 - Stable |

La version expérimentale de Gemini dans la description restaure un bouton déjà reçu par YouTube pour la même vidéo et la même configuration d'écran. Voir les limites dans [la documentation du patch](youtube/native-ask/description/README_FR.md).

## Patch X indépendant

[Permanently Hide Posts 0.1.1](Piko-NewX/Permanently-Hide-Posts/) ajoute « Masquer définitivement ce post » au menu ⋮ de X 12.43.0-prod.01. Pour la correction compatible Piko 3.12.0-dev.6, actualise la source Nesquik’s patches en **0.4.1**. Coche **NewX: Permanently Hide Posts** et **NewX: Filter posts by keyword**. **Browse tweet object** est facultatif. Si tu utilisais le MPP local, désactive sa source séparée avant de repatcher.

## Importer dans Morphe

Dans **Sources → + → Remote**, ajouter [l’URL du catalogue](https://raw.githubusercontent.com/Neszi2317/Nesquik-s-patches/refs/heads/main/patches-bundle.json). Tu peux aussi [télécharger le MPP depuis la dernière release](https://github.com/Neszi2317/Nesquik-s-patches/releases/latest) et l’ajouter depuis ton appareil.

- **X**. Ajoute la source officielle **Piko 3.12.0-dev.6** et le bundle **Nesquik's patches**. Dans le même patching, sélectionne le patch officiel **NewX: Filter posts by keyword**, puis les patchs Nesquik souhaités. Leurs réglages apparaissent sous **Piko → Contenu** sur l'APK généré. Tu peux laisser la liste officielle vide et remplir le filtre avancé. Pour reproduire la configuration testée, laisse **NewX: Show sensitive media** décoché.
- **Reddit**. Garde la source officielle Morphe et sélectionne au moins un patch qui installe ses réglages, par exemple **Hide navigation buttons**, avec les patchs personnalisés. La compilation de référence utilise Morphe Patches **2.4.0-dev.6**.
- **YouTube**. Sur **21.30.216**, les deux patchs Ask/Gemini ont été appliqués avec **Ask flyout menu** et **Hide video action buttons** de Morphe Patches **2.1.0-dev.8**. Sur **21.46.522**, la traduction et la variante correspondante des sous-titres arrondis ont été appliquées ensemble.
- **Firefox**. Configure la liste des sites à bloquer dans les options de **Firefox - Local Website Blocker** avant de lancer le patching. Sur Firefox 160.0.1, utilise la préversion Heval 3.1.0-dev.1 pour combiner les deux sources.
- **Discord**. Ajoute la source officielle [Venus Patches](https://github.com/VenusIsJaded/Venus-Patches) et sélectionne **Venus settings** avec **! Hide blocked or ignored users**. La combinaison a été testée avec Venus **1.21.1**.

Les six patchs X sont sélectionnables individuellement. Leur code JVM est chargé par la source Nesquik. Cette source ajoute ses entrées au registre de l'APK généré par Piko et réutilise son mécanisme de filtrage. La version de Piko indiquée est celle vérifiée pour ce portage.

## Développement et publication

| Dossier | Contenu |
| --- | --- |
| [x/](x/) | Le portage X, ses extensions et le pont de réglages Piko |
| [reddit/](reddit/) | Les trois patchs Reddit et leurs scripts de construction |
| [youtube/](youtube/) | Ask Native, Gemini dans la description, recherche dans les playlists et traduction automatique des sous-titres |
| [firefox/](firefox/) et [discord/](discord/) | Compatibilité et configuration des patchs |
| [releases/](releases/) | Les dix MPP de référence utilisés pour assembler le bundle avec les patchs X |
| [tools/](tools/) | La compilation du bundle et les dépendances épinglées |
| [verification/](verification/) | Rapports Morphe et contrôle des réglages X dans l'APK |

Avec Java 21 et Python 3.11+ :

```sh
python3 tools/fetch-toolchain.py .toolchain
tools/build-collection.sh 0.4.2 .toolchain
```

Le workflow GitHub **Release Morphe bundle** publie le MPP et actualise `patches-bundle.json` quand un nouveau bundle versionné est ajouté dans `bundles/`. Le lancement manuel reconstruit le bundle pour la version demandée.

## In-App Media Blur dans la version 0.4.2

Actualise la source, sélectionne **NewX: In-App Media Blur**, puis appuie sur le bouton Flou à la place de Grok dans le détail de la publication. Le rendu des médias dans les réponses suit désormais l’état du bouton sur les deux chemins d’affichage pris en charge. [Détails et vérifications](x/README.md)

## Vérifications de la première version

- **X 12.43.0**. 51 patchs appliqués, dont les cinq personnalisés et 46 officiels, en chargeant Piko et Nesquik comme deux sources.
- **Reddit 2026.47.0**. Les trois patchs personnalisés ont été appliqués avec les patchs officiels. Les réglages d'accueil et de filtrage utilisent Morphe Settings.
- **YouTube 21.30.216**. Ask Native et Gemini Description ont été appliqués ensemble avec les deux patchs officiels sélectionnés. La recherche dans les playlists conserve sa cible historique 21.16.256 et a été vérifiée au chargement du catalogue commun.

## Vérifications de la version 0.2.0

- **Firefox 159.0**. Le patch a intégré deux règles d’un fichier TXT et Morphe a signé l’APK. [Rapport Firefox](verification/firefox-0.2.0.json).
- **Discord 351.11 - Stable**. Les réglages Venus et le patch Hide Users ont été appliqués ensemble. [Rapport Discord](verification/discord-0.2.0.json).
- **YouTube 21.46.522**. La traduction automatique et les sous-titres arrondis ont été appliqués ensemble. Les trois autres variantes de sous-titres arrondis figurent au catalogue. [Rapport YouTube](verification/youtube-0.2.0.json).

Les rapports concernent le patching et l'inspection statique des APK. Les tests d'utilisation sur Android restent à faire.

Consulter les licences dans les dossiers de chaque patch et les manifestes des composants dans `META-INF/notices/` du MPP. Le blocage Firefox déclare la MPL-2.0.
