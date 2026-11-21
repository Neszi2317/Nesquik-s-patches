# Permanently Hide Posts pour X 12.43

Le menu ⋮ d’un post reçoit l’action **Masquer définitivement ce post**. Le patch mémorise son identifiant sur l’appareil. Dans les listes URT prises en charge, il retire la ligne et conserve les autres publications de l’auteur. Lorsqu’une citation ou un repost contient une référence à un identifiant masqué, il retire la ligne qui le contient.

## Installation

1. Dans Morphe, ajoute [Permanently-Hide-Posts-0.1.0.mpp](Permanently-Hide-Posts-0.1.0.mpp) comme source locale. Utilise le mode d’installation standard et l’APKM original **com.twitter.android 12.43.0-prod.01**, versionCode **312430001**.
2. Ajoute la source **Piko 3.12.0-dev.5** et coche **NewX: Browse tweet object** et **NewX: Filter posts by keyword**. Ils fournissent les points d’intégration du menu et des listes. Tu peux laisser vide la liste de mots-clés officielle. Si l’entrée de débogage « Browse Tweet Object » te gêne, tu peux la désactiver dans les réglages Piko après installation.
3. Coche **NewX: Permanently Hide Posts**. Pour utiliser aussi les cinq patchs personnels, ajoute **Nesquik’s patches 0.2.0** dans la même opération et sélectionne ceux que tu utilises. Le filtre avancé est facultatif.
4. Lance le patching, installe l’APK signé puis ouvre X. Les réglages du patch sont dans **Piko → Contenu → Permanently Hidden Posts**. L’écran permet de désactiver le filtre, de voir le nombre et la liste des identifiants, de toucher un identifiant pour le retirer, de vider la liste avec confirmation et de gérer les fichiers de sauvegarde.

L’ancien bundle **Piko-NewX-12.43.0-custom.1.mpp** a également été testé comme source combinée de Piko et des cinq patchs personnels. Dans cette configuration, ajoute le nouveau MPP et sélectionne les mêmes composants dans le bundle combiné. Charge une seule copie de Piko dans une session.

Le dépôt historique crimera/piko-newx annonce son archivage et la fusion de NewX dans crimera/piko. La version ci-dessus est celle validée pour ce portage. [Piko 3.12.0-dev.5](https://github.com/crimera/piko/releases/tag/v3.12.0-dev.5) · [Dépôt historique](https://github.com/crimera/piko-newx)

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

- Le bouton utilise le constructeur des groupes du menu natif et un jeton propre. Les quatre handlers Piko existants gardent leur traitement pour les autres actions. Le présentateur contient la publication sélectionnée, dont le patch lit la valeur décimale de getId().
- Le filtre intervient sur 19 lectures de la liste immuable e2.b dans neuf classes d’affichage URT, de discussion et de vidéo. Il reconstruit uniquement les listes ou modules modifiés, retire les modules vides et ajuste les identifiants des conversations verticales. Il conserve la liste source. Une révision observable de Compose déclenche la réévaluation lors des changements et permet aussi la restauration.
- Le filtre avancé de mots-clés continue à agir en amont, lors de la construction des données. Le patch s’applique à son résultat. Le catalogue principal 0.2.0 reste utilisable indépendamment de ce MPP.
- Les deux fils d’accueil, les profils, les résultats et les réponses qui empruntent ce chemin sont les surfaces visées. Le comportement interactif de chacune reste à confirmer sur appareil. Un post ouvert directement par lien profond peut avoir un en-tête distinct de la liste des réponses. Les notifications Android, le web, les messages privés et les écrans qui rendent un modèle indépendant de la liste URT sortent de la couverture vérifiée.

## Compiler et vérifier

Depuis la racine du dépôt, avec Java 21 (module jdk.compiler), Python 3.11+, bash, curl et zip :

```bash
d=Piko-NewX/Permanently-Hide-Posts
python3 tools/fetch-toolchain.py .toolchain
"$d/build.sh" .toolchain
python3 "$d/tests/host.py" .toolchain
bash x/test-host.sh .toolchain
"$d/tests/integration.sh" .toolchain /chemin/X.apkm sortie
"$d/tests/menu-coexistence.sh" .toolchain /chemin/X.apkm sortie
java -jar .toolchain/morphe.jar list-patches --patches "$d/Permanently-Hide-Posts-0.1.0.mpp" -p -v
mkdir -p sortie/audit
java -m jdk.compiler/com.sun.tools.javac.Main -cp .toolchain/morphe.jar -d sortie/audit "$d/tests/InspectApk.java"
java -cp sortie/audit:.toolchain/morphe.jar:.toolchain/android.jar InspectApk sortie/combined.apk combined
```

La compilation écrit le MPP à côté de ce README. Le script d’intégration accepte un quatrième argument facultatif, le bundle historique custom.1. Les doublures d’API dans stubs/ servent exclusivement à la compilation. Le MPP ne contient aucune copie des classes Android, Piko ou X.

## Version 0.1.0

Première version ciblée sur l’ABI de X 12.43.0-prod.01. Elle ajoute le menu, la projection réactive, le stockage et l’écran de gestion. Une mise à jour de X ou de Piko nécessite un nouveau contrôle des signatures et des lectures de liste. [Résultats détaillés](TESTS.md) · [Journal de reprise](PROGRESS.md)
