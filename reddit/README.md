# Correctifs Reddit 2026.47.0 — 21 novembre 2026

Trois patchs personnalisés portés et compilés pour `com.reddit.frontpage` **2026.47.0 (2647001)**. Le patch expérimental d'apparence des avatars est exclu à votre demande.

| Fonction | Fichier à importer | Ancienne version remplacée |
|---|---|---|
| Mots-clés, communautés, commentaires, flair et import TXT | `patches/RedditContentFilter-v2.3.mpp` | ContentFilter v2.2 et les anciens reddit-keyword-filters |
| Suppression des publications signalées NSFW dans les flux pris en charge | `patches/reddit-hide-nsfw-1.1.0.mpp` | hide-nsfw 1.0.0 |
| Accueil sur un subreddit choisi et masquage de Home | `patches/reddit-custom-home-0.2.0.mpp` | custom-home 0.1.1 |

## Installation dans Morphe

1. Utiliser une version à jour de Morphe Manager. Les paquets utilisent l'API de patcher 2.x ; l'application des patchs a été testée avec Morphe Desktop 2.3.0.
2. Pour les patchs officiels, sélectionner **2.4.0-dev.6**, version testée. La prise en charge de Reddit 2026.47.0 y est expérimentale. La stable 2.3.0 annonce 2026.46.0 ; elle n'a pas été validée ici pour 2026.47.0.
3. Remplacer les anciennes sources personnalisées par ces trois fichiers `.mpp`. Ne pas cumuler les anciennes et nouvelles versions de la même fonction. Les anciens filtres 1.x et 2.x partagent leur paquet runtime et leur nom de patch : ils sont remplacés par v2.3, pas ajoutés à côté.
4. Partir du fichier APKM **original** fourni. Sélectionner les trois patchs personnalisés et les patchs officiels désirés. Inclure au moins un patch officiel qui ajoute les réglages Morphe, par exemple **Hide navigation buttons** ; l'accueil personnalisé en dépend.
5. Ouvrir la roue dentée de **Reddit - Local keyword filters** pour renseigner les listes et/ou importer les TXT UTF-8. Les quatre options sont conservées. Dans Reddit, les réglages de filtrage et d'accueil restent accessibles depuis les réglages Morphe.
6. Laisser Morphe fusionner, reconstruire et signer l'application, puis installer normalement. Conserver les listes de filtrage et la clé de signature utilisées auparavant si vous voulez mettre à jour l'installation existante.

Les clés de préférences des deux extensions sont conservées. Le masquage NSFW est actif dès que le patch est inclus. L'accueil personnalisé utilise `nightfeeling` comme valeur initiale, modifiable dans ses réglages. L'inclusion d'un patch officiel installe ses options ; elle ne signifie pas que toutes ses options sont activées simultanément dans Reddit.

## Ce qui est vérifié

**Compilation et intégration réussies : 41 patchs officiels activés par défaut + ces 3 patchs = 44 patchs, aucun échec**, directement à partir de votre APKM, avec validation des fingerprints. Une seconde chaîne de vérification a compilé entièrement les DEX en mode FULL et vérifié l'import des listes.

Tests JVM réussis : filtre **82 assertions**, options et import **9**, NSFW **29**, normalisation accueil **28**, contrats runtime accueil **29**, et un test du wrapper des réglages.

**Le démarrage sur Android, le défilement, les écrans de réglages et le rendu réel ne sont pas testés sur téléphone.** Aucun téléphone ni émulateur Android n'était disponible. Les doubles JVM ne constituent pas une exécution Android. Voir `RAPPORT.md` pour le périmètre exact et les contrôles à faire sur le S24.

## Sources et reproduction

Linux x86_64, Python 3 et accès Internet aux dépendances officielles :

```sh
python3 fetch-tools.py
python3 build.py
python3 run-tests.py
python3 integration.py /chemin/reddit-original-2026.47.0.apkm
```

`TOOLS_DIR` peut désigner un dossier d'outils déjà installé. `toolchain.json` fixe toutes les URL et empreintes SHA-256. La vérification d'intégration produit un APK **non signé**, réservé à l'analyse. Les livrables à importer dans Morphe sont les `.mpp`.

Le code source, les stubs explicitement utilisés à la compilation, les tests, les licences, les résultats et les outils d'analyse sont inclus. Les APK propriétaires et les volumineuses dépendances ne sont pas redistribués.

La carte de correspondance des types de l'analyse est disponible dans `analysis/type-map-39-to-47.json.gz`. Pour lire le JSON, utiliser `gzip -dc analysis/type-map-39-to-47.json.gz`.
