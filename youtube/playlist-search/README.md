# YouTube – Search in Playlists · 0.1.0-experimental

Patch personnalisé non officiel pour YouTube **21.16.256**, compilé avec Morphe Patcher **1.15.1**. Il ajoute une loupe bleue dans le coin supérieur droit du contenu des playlists et une fenêtre indépendante de recherche.

**Statut : compilation, application à l’APK et tests du moteur effectués ; interface Android, import dans Morphe Manager et compte privé non validés sur appareil.** Ce n’est pas une version déclarée fonctionnelle de bout en bout.

## Utilisation

1. Ajouter `YouTube-Search-in-Playlists.mpp` comme source locale de patchs dans Morphe Manager.
2. Sélectionner l’APK original YouTube 21.16.256 et le patch `YouTube – Search in Playlists`, avec vos patchs habituels.
3. Patcher et installer par la procédure habituelle. Ouvrir une playlist puis la nouvelle loupe bleue dans son contenu.
4. Saisir un titre ou un nom de chaîne. Les résultats s’actualisent pendant le chargement des pages.
5. Attendre « Parcours terminé à cet instant » pour considérer le parcours exhaustif. « Résultats partiels » reste affiché en cas d’arrêt, d’erreur ou de limite.

L’index ne dure que pendant l’ouverture de la fenêtre : les recherches successives réutilisent les métadonnées déjà chargées. `Actualiser` recommence depuis la première page ; fermer puis rouvrir récupère aussi l’état actuel de la playlist. Aucun cache disque ni stockage de secrets.

## Comportement

- Titre **OU** nom de chaîne, casse et accents ignorés, correspondance de sous-chaîne.
- Déduplication par identifiant vidéo, ordre de la playlist conservé.
- Parcours des continuations ; annulation, progression, arrêt sur token répété.
- Résultats exclusivement issus des conteneurs de vidéos de la playlist ; aucune recherche globale de secours.
- Ouverture avec l’identifiant vidéo et celui de la playlist, explicitement dans le package courant, y compris avec GmsCore support.
- Contrôle local au fragment ; aucune modification des méthodes de recherche globale ou de Channel Search.
- Requêtes `browse` authentifiées avec les en-têtes de la session native, destinées uniquement à `youtubei.googleapis.com`, sans transfert sur redirection.
- Changement de session détecté : index invalidé. Aucun token écrit dans les logs ou fichiers.

## Limites

- « À regarder plus tard », playlists privées, affichage de la loupe, démarrage, retour arrière et rotation nécessitent encore une validation sur téléphone connecté. Le mécanisme est implémenté mais pas confirmé sur un compte réel.
- Loupe superposée au contenu en haut à droite ; pas un élément ajouté au menu natif de la barre d’outils. Son placement exact selon les variantes d’interface reste à valider.
- Le protocole JSON reprend le client `ANDROID 20.26.46` utilisé par les sources Morphe PlaylistRoutes, même si l’APK hôte est 21.16.256. Les formats inconnus sont refusés explicitement.
- Une vidéo sans métadonnée de chaîne ne peut pas correspondre par nom de chaîne. Les vidéos sans identifiant exploitable sont comptées séparément.
- Pas de surveillance en temps réel des ajouts/suppressions pendant un parcours ; actualiser après modification. Pas de garantie de snapshot atomique si la playlist change durant la pagination.
- Garde-fous : 1 000 pages / environ 20 000 entrées ; dépassement signalé comme partiel.
- 21.40.161 exclue de la compatibilité déclarée.

## Compilation reproductible

Linux, Python 3, curl, unzip et un **JDK 21 complet**. Depuis ce dossier :

```sh
export JAVA_HOME=/chemin/vers/jdk-21
./bootstrap.sh
./build.sh
./test.sh
```

`TASK_TOOLS` permet de choisir le répertoire des dépendances ; par défaut `../tools`. Le JDK doit être accessible via `tools/jdk` (lien créé par bootstrap si JAVA_HOME est fourni).

Dépendances publiques épinglées par le script : Morphe Desktop 1.18.1 (contient Patcher 1.15.1 et smali), Kotlin 2.4.21 avec sortie de métadonnées 2.2, D8/R8 8.13.17, stubs Android 4.1.1.4 et org.json 20250517 pour les tests JVM. Les classes JSON ne sont pas embarquées : Android les fournit. Les empreintes des fichiers téléchargés figurent dans `dependency-sha256.json`.

La compilation assemble les classes JVM du patch, son DEX Android, le DEX de l’extension et le manifeste dans un vrai bundle `.mpp`, selon le format de Morphe. Les classes du patcher ne sont pas embarquées. Les mêmes opérations de compilation D8/JAR sont utilisées que dans le plugin Gradle Morphe ; le script évite les dépôts Gradle authentifiés.

```sh
../tools/jdk/bin/java -Xmx3g -jar ../tools/morphe-cli.jar patch \
  -p dist/YouTube-Search-in-Playlists.mpp --unsigned --bytecode-mode FULL \
  -o test-output.apk /chemin/YouTube-21.16.256.apk
```

Ce dernier APK est un artefact de validation non signé, pas un APK fourni pour installation.

## Origine / licence

Code personnalisé pour cette demande. Hooks inspirés de Channel Search, BuildRequest et AuthHook, et requête adaptée de PlaylistRoutes dans [Morphe Patches v1.46.0](https://github.com/MorpheApp/morphe-patches/tree/v1.46.0).

Format du bundle : [Morphe Patches Gradle Plugin](https://github.com/MorpheApp/morphe-patches-gradle-plugin). Distribution sous GPL-3.0 avec les mentions additionnelles Morphe : voir `LICENSE` et `NOTICE`. Ce patch n’est pas un produit officiel Morphe.
