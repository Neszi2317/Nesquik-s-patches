# Universal Auto-Translated Captions 0.4.0

Patch Morphe natif pour `com.google.android.youtube`. APK testé : **21.16.256**.
Livrable : `dist/universal-auto-translated-captions-0.4.0.mpp`.

## Installation et essai rapide

1. Dans Morphe Manager, importer le `.mpp` 0.4.0 comme source de patchs locale.
2. Sélectionner **Universal auto-translated captions**. Désactiver la version 0.3.0 et tout ancien patch personnalisé de traduction des sous-titres.
3. Repartir de l'APK YouTube **21.16.256 non patché**, avec les autres patchs habituels nécessaires à votre installation (notamment votre configuration GmsCore sans root). Ne pas repatcher l'application déjà modifiée.
4. Installer l'APK produit par votre Manager avec sa signature habituelle.
5. Sur une vidéo dont les sous-titres français sont disponibles : **Sous-titres → Traduction automatique → Bengali / বাংলা**. Vérifier des phrases réellement traduites, puis avancer dans la vidéo.
6. Vérifier aussi arabe → français, français → arabe et anglais → bengali. Changer de vidéo et rouvrir le menu pour vérifier l'absence de doublons.

Le `.mpp` seul ne change pas le package ou la signature de YouTube et ne remplace pas la configuration d'installation sans root déjà utilisée. L'APK signé lors du test local n'est pas distribué : utilisez le résultat de votre propre Manager.

## Cause prouvée dans 0.3.0

La méthode injectée `morphe_completeTranslations` disposait déjà d'un catalogue de **156 codes cibles**, incluant `bn`. Mais le catalogue n'était parcouru que lorsque la liste serveur était **vide**. Une liste Android non vide mais partielle faisait terminer la méthode après sa propre boucle. Ajouter uniquement `bn` au catalogue n'aurait donc rien corrigé.

Correction 0.4.0 : à l'épuisement de la boucle serveur, le branchement passe de `:done` à `:catalogue`. Les pistes natives restent prioritaires, les langues serveur sont traitées d'abord, puis le catalogue existant complète les manques. Le contrôle d'unicité par code, insensible à la casse, reste inchangé.

Cette limitation du code est démontrée. La réponse réseau de la vidéo française précise de l'utilisateur n'a pas été capturée : son rôle dans ce cas particulier n'a donc pas été observé directement.

## Comportements conservés

- Fingerprints `&tlang=` et `AUTO_TRANSLATE_CAPTIONS_OPTION`, résolution structurelle des modèles de pistes : aucun nom de classe obfusqué codé en dur dans le patch.
- Menu, objets de pistes, identifiants traduits et chargement natifs YouTube.
- Ajout de `&tlang=<cible>` à l'URL de sous-titres source sans en supprimer les paramètres existants.
- Préférence d'une piste manuelle sur une piste ASR lorsque plusieurs sources sont disponibles.
- Déduplication des ajouts face aux pistes déjà proposées et aux codes serveur répétés.
- Pas de traduction artificielle vers une langue déjà disponible nativement.
- Garde hors ligne ; rejet d'une source sans URL exploitable ou déjà traduite.
- Refus explicite si un ancien helper Morphe de sous-titres est détecté.
- Aucun nouveau service réseau, aucune clé, aucun root et aucun serveur tiers.

## Validation exécutée

- Décompilation JVM et DEX du `.mpp` original, comparaison des mécanismes.
- Reconstruction des sources Kotlin, compilation JVM puis compilation DEX avec D8.
- Chargement du paquet dans Morphe Desktop ; patch unique correctement reconnu.
- Application sur l'APK 21.16.256 avec Morphe Desktop **1.18.1 / Patcher 1.15.1** (compilation du patch contre Patcher 1.15.0), recompilation complète des 9 DEX, alignement et signature de l'APK.
- Vérification cryptographique de la signature de l'APK d'entrée et de l'APK de sortie avec Android `apksig`.
- Comparaison de toutes les méthodes du gestionnaire de sous-titres : le patch recompilé depuis ces sources produit le même bytecode que la correction minimale du binaire 0.3.0.
- **22 assertions** sur les instructions des deux helpers extraites de l'APK patché : cas de régression liste partielle, quatre couples demandés, unicité, pistes existantes, liste vide, absence de sous-titres, mode hors ligne, URL invalides/déjà traduites, préférence de source manuelle, langues serveur hors catalogue.

Le test Python interprète les instructions DEX générées, avec des doublures pour les constructeurs YouTube, les libellés Locale et le menu source. Il ne teste ni Android ART, ni l'interface sur téléphone, ni les réponses de traduction de YouTube. L'ancienne branche 0.3.0 est rétablie dans un cas de régression pour démontrer la différence.

## Limites précises

- **Aucun appareil Android n'a été utilisé.** Installation réelle, ouverture du menu et lecture de traductions restent à confirmer. Signature/recompilation correctes ne sont pas une preuve d'installation ou d'exécution sur téléphone.
- Le catalogue de 156 codes est celui du 0.3.0 fourni, conservé pour minimiser les régressions. Ce n'est pas une liste Web téléchargée dynamiquement ou vérifiée aujourd'hui. Toute langue supplémentaire renvoyée par le serveur reste acceptée ; une nouvelle langue absente du serveur Android ET de ce catalogue nécessitera une mise à jour.
- Certaines cibles affichées peuvent ne pas fournir de sous-titres pour une vidéo donnée. La disponibilité réelle dépend de YouTube ; le patch ne sait pas la déterminer avant le chargement natif.
- Les mécanismes arabe → français restent inchangés et passent les tests de génération ; la traduction réseau effective n'a pas été retestée.
- Le patch déclare le package sans restriction de version, comme 0.3.0. **Seule 21.16.256 a été vérifiée par application.** Les autres versions ne sont pas garanties.
- Aucun test combiné avec tous les patchs Morphe/GmsCore existants n'a été exécuté.

## Compilation reproductible

Prérequis : Linux/macOS (ou WSL), JDK **21+**, Python 3, accès aux téléchargements officiels.

```sh
python3 scripts/fetch-tools.py
./build.sh
```

Le script vérifie les SHA-256 des dépendances téléchargées. Il utilise :

- Morphe Desktop **1.18.0**, qui embarque **Morphe Patcher 1.15.0**.
- Kotlin **2.2.0**, cible JVM 11, `-Xcontext-parameters`.
- `-Xskip-metadata-version-check` : nécessaire car les métadonnées Kotlin de la dépendance Morphe indiquent 2.4.0. Les appels utilisés ont été vérifiés par application réelle du patch.
- D8 **8.7.18**, `--min-api 26` pour le paquet de patchs (pas le minSdk de YouTube).

Le script peut réutiliser un outillage local via `MORPHE_JAR`, `R8_JAR`, `KOTLINC` et `JAVA_HOME`. L'archive `.mpp` comporte les classes JVM, `classes.dex`, les métadonnées Kotlin et le manifeste 0.4.0 ; elle n'embarque pas les bibliothèques de développement.

## Rejouer les tests sur votre APK

Utiliser Desktop **1.18.1** pour ces essais (`morphe-test.jar`), inclus dans les téléchargements du script. Lors de notre essai, Desktop 1.18.0 a annoncé un succès mais produit un APK tronqué : la vérification indépendante l’a détecté. Le même patch produit une archive valide et une signature vérifiée avec 1.18.1. La cause interne de cette anomalie Desktop n’a pas été recherchée ; elle ne doit pas être confondue avec le résultat des tests du patch.

```sh
java -Xmx3g -jar tools/morphe-test.jar patch /chemin/youtube-21.16.256.apk \
  -p dist/universal-auto-translated-captions-0.4.0.mpp \
  -e 'Universal auto-translated captions' --exclusive --bytecode-mode FULL \
  -o build/youtube-test.apk -r build/patch-result.json
mkdir -p build/test-classes
javac -cp tools/morphe-test.jar -d build/test-classes tests/Inspect.java
java -Xmx2g -cp tools/morphe-test.jar:build/test-classes Inspect \
  build/youtube-test.apk build/manager.txt
python3 tests/test_generated_dex.py build/manager.txt
java -cp tools/morphe-test.jar:build/test-classes Inspect sig build/youtube-test.apk
```

Les doublures du test Python correspondent précisément à l'ABI de 21.16.256 ; les adapter si l'on teste une autre version. `evidence/` contient les résultats et le paquet original fourni. Les noms de classe spécifiques à YouTube n'apparaissent que dans ces preuves/tests, jamais dans les fingerprints du patch.

## Provenance et licence

Les sources originales n'étaient pas disponibles. Les fichiers Kotlin de cette archive sont une **reconstruction à partir des classes du paquet 0.3.0 fourni**, puis la correction 0.4.0. Ce ne sont pas les fichiers source originaux récupérés.

Le manifeste d'origine indique `Author: Independent prototype` et `License: GPL-3.0-only`. Cette version conserve cette licence ; voir `LICENSE` et `NOTICE`.
