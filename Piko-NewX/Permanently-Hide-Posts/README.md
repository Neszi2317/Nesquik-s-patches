# Permanently Hide Posts pour X 12.43

Le menu ⋮ d’un post reçoit l’action **Masquer définitivement ce post**. Le patch mémorise son identifiant sur l’appareil. Dans les listes URT prises en charge, il retire la ligne et conserve les autres publications de l’auteur. Lorsqu’une citation ou un repost contient une référence à un identifiant masqué, il retire la ligne qui le contient.

## Installation depuis le catalogue

Dans Morphe, actualise la source distante Nesquik’s patches jusqu’à la version 0.4.1 ou ultérieure. Sélectionne le patch et le filtre Piko indiqué ci-dessous, puis repatche X. Si tu utilises le catalogue, désactive toute copie locale du même patch.

## Installation du MPP indépendant

1. Dans Morphe, ajoute [Permanently-Hide-Posts-0.1.1.mpp](Permanently-Hide-Posts-0.1.1.mpp) comme source locale. Utilise le mode d’installation standard et l’APKM original **com.twitter.android 12.43.0-prod.01**, versionCode **312430001**.
2. Ajoute **Piko 3.12.0-dev.6** et coche **NewX: Filter posts by keyword**. Ce patch fournit les points d’intégration des listes et du registre. Tu peux laisser vide sa liste de mots-clés. **NewX: Browse tweet object** est facultatif.
3. Coche **NewX: Permanently Hide Posts**. Pour utiliser aussi les cinq autres patchs personnels, utilise le catalogue **Nesquik’s patches 0.4.1**, qui contient déjà ce patch.
4. Lance le patching, installe l’APK signé puis ouvre X. Les réglages du patch sont dans **Piko → Contenu → Permanently Hidden Posts**. L’écran permet de désactiver le filtre, de voir le nombre et la liste des identifiants, de toucher un identifiant pour le retirer, de vider la liste avec confirmation et de gérer les fichiers de sauvegarde.

Charge une seule copie de Piko dans une session. Le dépôt historique `crimera/piko-newx` est archivé et NewX est distribué dans `crimera/piko`. La configuration minimale a également été testée avec `dev.5` et son option de débogage. [Piko 3.12.0-dev.6](https://github.com/crimera/piko/releases/tag/v3.12.0-dev.6) · [Dépôt historique](https://github.com/crimera/piko-newx)

## Stockage et sauvegarde

Les identifiants et l’état du filtre sont stockés dans les SharedPreferences privées de X, sous le nom fr.nesquik.hidden.v1. La liste est commune aux comptes utilisés dans cette installation de X. Les changements sont sérialisés et écrits sur disque hors du thread d’affichage. La confirmation « Post masqué » intervient après l’écriture. Le défilement consulte un ensemble en mémoire. Android peut supprimer ces données lors de la désinstallation ou de l’effacement des données. Exporte d’abord la liste.

Le sélecteur de documents Android permet de choisir la destination. Le format v1 est un fichier texte UTF-8. La première ligne est obligatoire, suivie d’un identifiant décimal positif par ligne.

```text
nesquik-hidden-posts-v1
1234567890123456789
1234567890123456790
```

L’import fusionne la liste et ignore les doublons. Une ligne invalide, une version inconnue, une erreur de lecture ou un dépassement de limite interrompt l’import avant écriture. Limites : 100 000 identifiants au total, fichier de 3 Mio et identifiants jusqu’à 9223372036854775807. Les lignes vides et les fins de ligne Windows sont acceptées. Le fichier ne contient aucun nom, texte, média ou jeton de compte.

## Fonctionnement et couverture

- Le patch ajoute un groupe de menu avec un jeton propre, puis raccorde son libellé, son icône et son action au menu natif. Les autres actions gardent leur comportement, y compris celles de Piko. Le présentateur contient la publication sélectionnée, dont le patch lit la valeur décimale de `getId()`.
- Le filtre intervient sur 19 lectures de la liste immuable e2.b dans neuf classes d’affichage URT, de discussion et de vidéo. Il reconstruit uniquement les listes ou modules modifiés, retire les modules vides et ajuste les identifiants des conversations verticales. Il conserve la liste source. Une révision observable de Compose déclenche la réévaluation lors des changements et permet aussi la restauration.
- Le filtre avancé de mots-clés continue à agir en amont, lors de la construction des données. Le patch s’applique à son résultat. Le bundle 0.4.1 intègre déjà le MPP.
- Les deux fils d’accueil, les profils, les résultats et les réponses qui empruntent ce chemin sont les surfaces visées. Le comportement interactif de chacune reste à confirmer sur appareil. Un post ouvert directement par lien profond peut avoir un en-tête distinct de la liste des réponses. Les notifications Android, le web, les messages privés et les écrans qui rendent un modèle indépendant de la liste URT sortent de la couverture vérifiée.

## Compiler et vérifier

Depuis la racine du dépôt, avec Java 21 (module jdk.compiler), Python 3.11+, bash, curl et zip :

```bash
d=Piko-NewX/Permanently-Hide-Posts
python3 tools/fetch-toolchain.py .toolchain
"$d/build.sh" .toolchain
python3 "$d/tests/host.py" .toolchain
bash x/test-host.sh .toolchain
tools/build-collection.sh 0.4.1 .toolchain
"$d/tests/integration.sh" .toolchain /chemin/X.apkm sortie dist/Nesquiks-patches-0.4.1.mpp
"$d/tests/menu-coexistence.sh" .toolchain /chemin/X.apkm sortie
java -jar .toolchain/morphe.jar list-patches --patches "$d/Permanently-Hide-Posts-0.1.1.mpp" -p -v
mkdir -p sortie/audit
java -m jdk.compiler/com.sun.tools.javac.Main -cp .toolchain/morphe.jar -d sortie/audit "$d/tests/InspectApk.java"
java -cp sortie/audit:.toolchain/morphe.jar:.toolchain/android.jar InspectApk sortie/full6.apk combined
```

La compilation écrit le MPP à côté de ce README. Le quatrième argument du test d’intégration désigne le bundle de la collection à vérifier. Les doublures d’API dans stubs/ servent exclusivement à la compilation. Le MPP ne contient aucune copie des classes Android, Piko ou X.

## Version 0.1.1

Le menu natif fonctionne au patching avec la sélection recommandée de Piko `dev.6`. La version 0.1.0 exigeait que l’option de débogage `Browse tweet object`, désactivée par défaut, soit sélectionnée. Le message d’erreur citait `dev.5` alors que les interfaces concernées étaient identiques dans les deux versions. Le filtre par mots-clés reste requis et continue à traiter la timeline en amont. [Résultats détaillés](TESTS.md) · [Journal de reprise](PROGRESS.md)

La version 0.1.0 a introduit le menu, la projection, le stockage et l’écran de gestion pour X 12.43.0-prod.01.
