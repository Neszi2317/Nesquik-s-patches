# Validation de Permanently Hide Posts 0.1.1

Tests sur l’APKM `com.twitter.android` 12.43.0-prod.01 (version 312430001), avec Morphe Desktop 2.3.0 et son moteur Morphe Patcher 2.5.0. L’édition indépendante 0.1.1 et le bundle 0.4.1 exposent le patch à Morphe. [Catalogue indépendant](tests/dev6/list-standalone.txt) · [Catalogue complet](tests/dev6/list-collection.txt) · [Empreintes SHA-256](tests/dev6/SHA256SUMS.txt)

| Configuration exécutée | Résultat |
| --- | --- |
| Bundle 0.4.0, Piko dev.6 recommandé | [Échec reproduit à `Hidden.kt:54`](tests/dev6/reproduction-0.4.0.json) |
| MPP 0.1.1, Piko dev.6, filtre officiel seul | [2 patchs, APK signé](tests/dev6/min6.json) · [audit DEX](tests/dev6/min6-dex.txt) |
| MPP 0.1.1, Piko dev.6 recommandé, « Show sensitive media » décoché | [47 patchs, APK signé](tests/dev6/default6.json) · [audit DEX](tests/dev6/default6-dex.txt) |
| MPP 0.1.1, Piko dev.6, navigateur d’objets, filtre, personnalisation du menu et traducteur | [5 patchs, APK signé](tests/dev6/browse6.json) · [audit DEX](tests/dev6/browse6-dex.txt) |
| Bundle 0.4.1, Piko dev.6 recommandé, « Show sensitive media » décoché, six patchs X Nesquik | [52 patchs, APK signé](tests/dev6/full6.json) · [audit DEX](tests/dev6/full6-dex.txt) · [audit des autres patchs](tests/dev6/audit-x.txt) |
| Même sélection, sources chargées dans l’ordre inverse | [52 patchs, APK signé](tests/dev6/reversed6.json) · [audit DEX](tests/dev6/reversed6-dex.txt) |
| MPP 0.1.1, Piko dev.5, filtre et navigateur d’objets | [3 patchs, APK signé](tests/dev6/dev5.json) · [audit DEX](tests/dev6/dev5-dex.txt) |
| MPP 0.1.1 avec le navigateur d’objets, filtre officiel absent | [Arrêt attendu avec le nom du prérequis manquant](tests/dev6/missing6.json) |

L’audit contrôle la signature APK, les quatre appels du menu, l’initialisation du jeton d’action et la déclaration de l’écran de gestion. Il retrouve les 19 points d’appel du filtre permanent, le filtre officiel et, sur le bundle complet, le filtre avancé et le filtre des posts sensibles. Les classes JVM et l’extension DEX de Permanently Hide Posts sont identiques entre les deux MPP.

Les tests JVM couvrent 22 assertions, dont l’ID du post sélectionné, la visibilité des autres posts, la restauration, l’import et l’export. Une seconde JVM recharge les préférences simulées sur disque. Un test exécute le filtre avancé et le filtre permanent ensemble. Les tests hôtes des autres patchs X ont passé. [Tests du patch](tests/dev6/host.txt) · [Tests des autres patchs](tests/dev6/x-host.txt)

Le menu, le défilement, le redémarrage et la restauration dans X sur un appareil Android restent à vérifier. Aucun appareil Android ni gestionnaire 2.7.1 n’a été utilisé pendant ces essais.

## Historique 0.1.0

# Validation du 21 novembre 2026

Cible fournie : X 12.43.0-prod.01 (312430001), paquet com.twitter.android. Morphe Desktop 2.3.0, API de bundle 2.4.0, Piko 3.12.0-dev.5. Le MPP indépendant contient une seule entrée sélectionnable. [Liste produite par Morphe](tests/list-patches.txt) · [Empreintes des entrées et des APK](tests/SHA256SUMS.txt)

## Patching de l’APKM fourni

Chaque ligne réussie ci-dessous correspond à un APK réellement reconstruit et signé. Les fichiers JSON sont les rapports Morphe, avec retrait du chemin temporaire local. Aucun APK propriétaire ni clé de signature de développement n’est publié.

| Configuration | Résultat |
| --- | --- |
| Composants Piko du menu et du filtre + MPP indépendant, sans bundle personnel | [3 patchs appliqués](tests/minimal.json) |
| Piko + cinq patchs personnels du bundle 0.2.0 + MPP | [8 patchs appliqués](tests/combined.json) |
| Les mêmes trois sources, fournies dans l’ordre inverse | [8 patchs appliqués](tests/reversed.json) |
| Bundle historique custom.1 + MPP | [8 patchs appliqués](tests/custom.json) |
| Avec le traducteur et la personnalisation du menu officiels | [5 patchs appliqués](tests/menu.json) |
| Sélection usuelle Piko, cinq patchs personnels et nouveau MPP, « Show sensitive media » décoché | [53 patchs appliqués](tests/default.json) |
| MPP seul, puis avec un seul des deux prérequis | Arrêt explicite au patching dans [les trois](tests/missing.json) [configurations](tests/missing-Browse.json) [incomplètes](tests/missing-Filter.json) |

Les deux ordres testés avec le bundle personnel 0.2.0 ont produit des APK de même SHA-256.

L’inspection du DEX de l’APK combiné retrouve un exemplaire de chaque classe, les appels du menu natif, le jeton d’action, la déclaration de l’écran et 19 lectures de liste reliées au filtre. L’audit du dépôt vérifie également les réglages et les hooks existants, y compris le flou des médias et les filtres avancé et sensible. [Audit du nouveau patch](tests/audit-combined.txt) · [Audit des anciens patchs](tests/audit-existing.txt)

## Scénarios fonctionnels

Les tests JVM exécutent le code Java livré avec des modèles et des services Android simulés. Le test des deux filtres exécute aussi le véritable AdvancedFilter du dépôt. [Sortie des tests](tests/host-results.txt) · [Régression des cinq patchs](tests/existing-host-results.txt)

| Scénario | Résultat mesuré | Vérification sur S24 |
| --- | --- | --- |
| Choisir le menu sur un post | ID de la publication choisie enregistré, autres actions déléguées, groupes conservés | Libellé, icône, fermeture du menu et disparition immédiate à l’écran |
| Plusieurs posts, nouvelle liste, retour au profil | Les IDs restent filtrés sur une nouvelle projection. Un post sans ID masqué reste visible | Pour vous, Abonnements, profil et recherche avec de vrais comptes |
| Citations, reposts, conversations | Retrait dans les fixtures, compactage des modules, conservation d’une ligne voisine et du curseur | En-têtes, liens profonds, réponses paginées et positions de défilement |
| Désactivation, réactivation, retrait, effacement | Le code restaure ou refiltre la liste initiale | Recomposition réelle et confirmation d’effacement |
| Relance et redémarrage Android | Le Store relit la liste et le réglage après redémarrage d’une JVM avec préférences sur disque simulées | Fermeture forcée et redémarrage du S24 |
| Export et import | Aller-retour, CRLF, doublons, grands IDs exacts. Fichiers invalides, trop grands ou de format inconnu refusés avant fusion | Sélecteur Android et fournisseurs de documents réels |
| Filtre par mots-clés simultané | Les deux filtres retirent chacun son post dans les deux ordres, un troisième reste visible | Parcours réel dans X et ressenti du défilement |
| Démarrage X avec les autres fonctionnalités | Signatures vérifiées, classes et appels présents dans l’APK | Lancement, chargement des réglages et stabilité sur Android 17 |

Aucune session de X ou du gestionnaire Morphe sur appareil physique n’a été exécutée. Les hashes des APK identifient les exemplaires inspectés. Une recompilation peut produire des hashes différents, notamment à cause de la signature et des horodatages.
