# Journal de reprise
Cible vérifiée dans info.json : com.twitter.android 12.43.0-prod.01 (312430001). Base Git e941414. Dépendances épinglées dans tools/toolchain.json. Morphe Desktop 2.3.0 a patché, reconstruit et signé l’APKM avec Piko 3.12.0-dev.5, Browse tweet object et Filter posts by keyword. Le bundle personnel 0.2.0 contient les cinq modifications X. L’archive custom.1 est un autre bundle contenant aussi Piko.
Menu : réutiliser le chemin des handlers natifs de Browse tweet object. Ne pas détourner un type d’action natif ni activer le navigateur d’objets pour ajouter le masquage. Fil : examiner les lectures de la liste dans com.x.urt.e2 en plus du constructeur. L’énumération com.x.models.x5 utilise le constructeur (String, int), None ordinal 0.
Reprise : depuis la racine du dépôt, python3 tools/fetch-toolchain.py .toolchain. Implémenter les hooks, compiler le MPP puis tester les combinaisons et les scénarios sur appareil.

## Développement
Sources ajoutées : menu natif, identifiant k1.getId() et représentation décimale h6, projection des listes immuables, modules et citations, observation Compose, stockage privé, gestion et import/export. Le présentateur natif v.b porte exactement un k1. Le modèle canonique v0.B contient quotedStatusId. La liste source e2.b reste disponible pour restaurer les éléments. Le jeton du menu possède sa propre identité et emprunte uniquement l’ordinal de rendu de None.
Commande en cours : Piko-NewX/Permanently-Hide-Posts/build.sh .toolchain. Restent la validation Morphe, le contrôle du DEX et les essais d’interface.

## Compilation et essais
Le MPP contient les classes JVM, classes.dex pour le chargeur Android et une extension DEX. Morphe Desktop le liste. Les premières configurations minimale, combinée, inversée et custom.1 ont produit des APK signés. Inspection du combiné : quatre appels du menu natif, 19 lectures de liste et les deux autres filtres toujours présents. Stockage et interface utilisent un exécuteur sérialisé pour les écritures. Test hôte avec doublures de modèles et du système. Dernière passe sur le MPP livré en cours, y compris l'absence des prérequis et la régression des cinq patchs.
Reprise : tests/host.py .toolchain puis tests/integration.sh .toolchain X.apkm sortie [custom.1.mpp]. Exécuter InspectApk avec le JAR Morphe et android.jar. Vérifier ensuite physiquement le menu, le défilement, la persistance au redémarrage et les surfaces hors URT.

## Livraison 0.1.0
Les six configurations positives ont été reconstruites et signées. Les trois configurations incomplètes ont été refusées explicitement. L’audit statique a contrôlé les six APK. Les deux ordres du bundle personnel et de Piko ont donné un APK identique au SHA-256. Le chargement, les références, la signature et la compatibilité du patching sont vérifiés. Les 21 assertions du scénario hôte, le test de relance du Store et le test conjoint avec le véritable AdvancedFilter sont passés. Les tests de régression du dépôt et son audit des cinq patchs X sont passés.
Derniers fichiers : README principal, README et TESTS du module, tests et empreintes, PROGRESS.md. Détail des commandes dans README.md. Le MPP final a le SHA-256 65415af9c5bb69600564fd9a0f533a862cbdec8be50e47f1771c1c56bd476553.
Pour reprendre : repartir de cette branche ou décompresser l’archive, réutiliser les commandes de la section « Compiler et vérifier », puis installer sur le S24 et parcourir la dernière colonne de TESTS.md. Priorité : vérifier le tap depuis un profil, le retour à la timeline, l’absence de rectangle vide et les liens profonds. Aucun appareil Android n’a été connecté pendant la mission.

## Correction 0.1.1 pour Piko 3.12.0-dev.6

Reproduction confirmée avec le bundle 0.4.0 et la sélection recommandée de Piko. Le navigateur de tweet est facultatif depuis que le patch raccorde ses quatre opérations au menu natif de X. Le registre et les ponts du filtre Piko sont toujours vérifiés au patching.

Le MPP indépendant 0.1.1 et le catalogue 0.4.1 ont été compilés. L’APKM 12.43.0-prod.01 a été reconstruit et signé avec 52 patchs dans chacun des deux ordres de sources. Voir [le rapport de tests](TESTS.md) pour les configurations vérifiées et les essais restant à faire sur Android.
