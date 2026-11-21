# Rapport de compatibilité — 21 novembre 2026

## Cible et environnement

- Paquet : `com.reddit.frontpage`, version lue dans l'APK : `2026.47.0`, code `2647001`.
- Source : APKM fourni, SHA-256 `3a69bc4f8c45b03ec85f984c842984031b553200a423ff2b509af7a63e7a0bb7`.
- Morphe Desktop : 2.3.0, JDK Temurin 21.0.13+9, Kotlin 2.3.10, D8/R8 9.5.26.
- Patchs officiels : `patches-2.4.0-dev.6.mpp`, SHA-256 `ff2170301a6349da73ec3d2402663bf10db7f25281e2973630a5fc02ec429cde`.
- Référence officielle : https://github.com/MorpheApp/morphe-patches/releases/tag/v2.4.0-dev.6 . Cette publication ajoute explicitement la prise en charge expérimentale de 2026.47.0.

## Diagnostic des anciens patchs

| Patch précédent | Résultat sur 2026.47.0 | Correction |
|---|---|---|
| RedditContentFilter v2.2 | Refus explicite `wrong target version`, même avec force ; nombreux accès obfusqués 2026.39 devenus obsolètes | Port v2.3 : classes et champs vérifiés, recherche et coroutines adaptées, commentaires étendus à dix paramètres, métadonnées GraphQL et flairs remappés |
| Hide NSFW 1.0.0 | Échec de recherche d'une signature native | Port 1.1.0 : nouvelle liste persistante, son constructeur utilitaire, FeedPage à sept paramètres et Feed à douze paramètres ; 157 ponts natifs de parcours générés |
| Custom Home 0.1.1 | Signatures natives et navigation incompatibles | Port 0.2.0 : nouvelle signature NavEntry, factory et méthode d'analytics, liste de navigation à deux paramètres |

Le filtre ajoute désormais ses réglages avec un wrapper qui préserve `this` et compose avec le wrapper de Custom Home. Le corps des réglages Morphe reste dans la chaîne d'appels. Les deux sections de réglages sont présentes dans le code produit ; leur affichage Android reste à essayer.

L'ancien `reddit-keyword-filters-1.0.1.mpp` est un prédécesseur de ContentFilter, avec le même paquet runtime et le même nom de patch. Il n'est pas une quatrième fonction à cumuler. Les copies `(1)` et `(2)` de ContentFilter v2.2 avaient la même taille ; une seule version de cette fonction a été portée. Le patch personnalisé d'apparence des avatars a été écarté à la demande de l'utilisateur.

## Tests exécutés

| Vérification | Résultat | Preuve |
|---|---|---|
| 41 patchs officiels activés par défaut sur base.apk | Réussite, aucun patch échoué | `validation/official-all.json` |
| Chacun des trois patchs personnalisés avec réglages officiels | Réussite après correction | `validation/filter.json`, `nsfw.json`, `home.json` |
| 41 officiels + 3 personnalisés, mode STRIP_FAST, fingerprints validés | Réussite | `validation/combined.json` |
| Même combinaison, recompilation DEX complète FULL, listes + TXT | Réussite ; contenu des listes normalisé et vérifié dans le DEX | `validation/combined-configured.json`, `verify-options-dex.log` |
| APKM complet fourni, fusion et reconstruction | Réussite, 44 patchs, zéro échec | `validation/final-apkm.json`, `final-apkm.log`, `release-integration.log` |
| Références injectées et intégration des extensions | 834 références application résolues, 157 ponts NSFW, pas de doublons de classes/méthodes détectés | `validation/verify-bundle.log`, `release-integration.log` |
| Tests JVM du filtre (doublures des modèles remappés) | 82 contrôles réussis : titres, corps, flairs, communautés, recherche, branches de commentaires, cache GraphQL, retrait des règles, caractères littéraux, gros volumes | `validation/release-tests.log` |
| Options des listes et TXT | 9 contrôles réussis : normalisation, casse, fusion, BOM, UTF-8 invalide, validation noms, métadonnées | Même journal |
| NSFW JVM | 29 assertions : marqueurs natifs, listes non modifiées, faux marqueurs, structures imbriquées et cycles | Même journal |
| Accueil JVM | 28 cas de noms, 29 contrats runtime, test de préservation de `this` dans les réglages | Même journal |

Les vérifications de références DEX ne sont ni un vérificateur de types ART, ni une preuve de fonctionnement de l'interface. Les appels hérités du SDK Android ne sont pas validés par exécution. L'analyse de références réseau exclut la simple analyse locale d'URI ; aucun appel de réseau n'a été ajouté par ces trois extensions.

Les modes STRIP_FAST/FULL, la source APKM et les options de patch ont réellement été exécutés. Les données de test sont synthétiques et ne sont pas une navigation Reddit en direct. Les DEX de la reconstruction livrée ont été comparés aux DEX des paquets utilisés dans les essais configurés : identiques.

## Compatibilité avec les nouveautés officielles

La compilation commune inclut notamment : Hide home tabs, Disable modern home, Hide post buttons, Hide Videos for you shelf, Post side panel, Tablet grid feed, High quality avatars, le filtre officiel Keyword filters et Flair filters. La présence de High quality avatars correspond au patch officiel de résolution des images ; elle ne réintroduit pas le filtre personnalisé d'apparence des avatars.

Les options des deux filtres de mots-clés peuvent se cumuler : une publication masquée par l'un ne sera pas rétablie par l'autre. Leurs paramètres sont distincts. Les nouveaux réglages d'accueil peuvent changer les chemins utilisés à l'exécution ; les combinaisons de valeurs et variantes A/B servies par Reddit n'ont pas été exercées sur téléphone.

Les patchs universels désactivés par défaut (clonage, changement de nom, contournement de pinning, etc.) n'ont pas été tous activés. « 41 officiels » désigne précisément les 41 entrées présentes dans les rapports JSON, pas l'ensemble de toutes les options de tous les patchs Morphe.

## Contrôle restant sur le S24

1. Vérifier le lancement, puis ouvrir les réglages Morphe et les deux sections personnalisées.
2. Configurer une règle bénigne telle que `test-filtre-xyz` et un subreddit de test, vérifier les cartes normales/compactes, recherche et commentaires, puis retirer la règle et confirmer le retour des contenus.
3. Vérifier l'import TXT depuis le sélecteur Android : le contenu embarqué et le parseur ont été testés, pas le sélecteur du téléphone.
4. Choisir un autre subreddit d'accueil et vérifier démarrage, retour arrière, reprise et rotation, avec les options d'accueil officielles souhaitées.
5. Vérifier le filtrage des marqueurs NSFW sur les chemins utilisés, sans confondre une compilation réussie et une garantie de couverture de tous les types de flux. Le patch s'appuie sur les métadonnées Reddit ; il ne reconnaît pas une image suggestive non signalée.

En cas de crash, le journal Morphe/Android permettra de distinguer l'échec de chargement DEX, la variante d'interface native et un problème de données. Aucun test sur votre S24 ni émulateur n'a été prétendu.
