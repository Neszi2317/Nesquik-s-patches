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
