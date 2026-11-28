# Vérification de Cross-Device Detox Sync 0.1.1

APK fourni : Digital Detox 11.42, code 592. Les deux bundles ont été appliqués à cet APK avec Morphe Desktop, puis les APK reconstruits ont été signés et inspectés. Le bundle complet a été testé avec le mode de bytecode par défaut et le MPP indépendant avec le mode FULL.

## Correctif de l’ouverture des réglages (collection 0.4.13)

Le rapport de l’utilisateur identifie une ActivityNotFoundException. L’application installée utilise le paquet `com.urbandroid.ddc.morphe`. La préférence de la version 0.1.0 désignait `com.urbandroid.ddc/fr.nesquik.detox.Devices`. Le nom du paquet et celui de la classe sont désormais associés par Android à partir du contexte de la préférence.

| Vérification | Résultat |
| --- | --- |
| Ancienne collection avec le patch officiel **Clone app** | Le manifeste et les deux variantes de la préférence reproduisent la divergence de paquets. En utilisant ces anciennes ressources, le test du clic échoue exactement sur le paquet attendu. |
| 0.1.1 sur le paquet d’origine, 0.4.13 avec **Clone app** | Patching, reconstruction et signature des deux APK réussis. Les entrées générique et v26 utilisent la nouvelle préférence. L’activité reste privée. |
| Ouverture simulée sous Android 16 (API 36) | Le test charge les ressources compilées de chaque APK avec le contexte de son paquet, utilise le PreferenceInflater et le performClick d’AndroidX, vérifie le composant lancé, puis crée l’activité à partir de cet intent. Titre français et option « Synchroniser par défaut » visibles dans les deux cas. Les classes de l’écran et d’AndroidX sont extraites de l’APK patché pour le test. |
| Régressions | Les six tests de session repassent. L’audit du paquet original retrouve les mêmes 369 méthodes natives comparées. Les 442 classes et extensions vérifiées des autres patchs sont identiques entre les collections 0.4.12 et 0.4.13. |

Aucun essai physique sur le Samsung de l’utilisateur pour ce correctif. Le test porte sur l’ouverture de la rubrique et ne mesure pas la livraison d’une session via ntfy sur One UI.

## Vérifications initiales de la version 0.1.0

| Scénario | Vérification effectuée |
| --- | --- |
| Compilation et patching | Bundle indépendant et collection 0.4.12 compilés, chargés et appliqués. Les deux rapports Morphe indiquent un succès pour le patching, la reconstruction et la signature. |
| Transfert dans les deux sens | Deux identités EC distinctes se sont envoyé des commandes via le client HTTPS du patch et un vrai serveur ntfy 2.19.2 local. Accusé signé relu et vérifié. |
| Réception différée | Une commande a été publiée avant la lecture par le destinataire. Relecture depuis le cache ntfy et calcul vérifié à une heure simulée de reconnexion. |
| Commandes invalides | Rejet testé pour un destinataire incorrect, un signataire inconnu, une échéance modifiée, une commande expirée ou future et des verbes non prévus. |
| Comportement Android | Six tests Robolectric API 36 couvrent la case décochée, la case cochée, les doublons, la réouverture de SQLite, la session déjà active, l’absence de permission, la révocation, le jeton invalide et le passage de l’arrière-plan au premier plan. Le service natif, le stockage de clés et le départ réseau sont simulés dans cette suite. |
| Intégrité de l’APK | Les déclarations originales du manifeste, les permissions et les bibliothèques natives ont été comparées. Les listes d’instructions de 369 méthodes des services, récepteurs, réglages et modèle de session comparés sont identiques. Cinq appels ont été ajoutés dans trois méthodes d’interface existantes. |
| Interface et catalogue | Le routage des préférences a été vérifié dans les variantes générique et v26 des APK reconstruits. Les libellés français et anglais existent. Les 442 fichiers de classes et d’extensions existants contrôlés dans la collection 0.4.11 sont identiques dans la 0.4.12. |

## Reste à vérifier sur les Samsung

Il n’y a eu aucun essai sur un S24 ou une Tab S9 FE physique. La livraison par l’application ntfy installée, la latence entre réseaux mobiles et Wi-Fi, la veille profonde, le redémarrage complet, la présentation visuelle clair/sombre et téléphone/tablette, les achats et le comportement réel des protections pendant une session restent à valider. Les tests de persistance ont rouvert la base SQLite. Le test de relais a utilisé du HTTPS sur la boucle locale.

Pour le premier essai, vérifier **L’appareil a répondu** dans chaque sens, puis lancer une courte session de cinq minutes pendant que le second appareil est éveillé. Refaire l’essai avec ce dernier en veille et le S24 sur les données mobiles. L’indicateur **Session distante confirmée** exige que le récepteur observe son service natif actif et son échéance. Si Android demande une ouverture manuelle, le patch l’indique explicitement.

## Reproduire

Les sorties conservées se trouvent dans [tests/results](tests/results/). Les commandes sont à lancer depuis la racine du dépôt, avec JDK 21 dans le PATH.

```sh
digital-detox/tests/relay.sh "$PWD/.toolchain"
digital-detox/tests/android/run.sh "$PWD/.toolchain"
python3 digital-detox/tests/audit.py original.apk digital-detox-patched.apk "$PWD/.toolchain"
digital-detox/tests/android/settings-routing.sh "$PWD/.toolchain" "$PWD/digital-detox/build/android-test" "$PWD/digital-detox-patched.apk" "$PWD/digital-detox-clone.apk"
```

Le test de relais utilise le binaire ntfy et un certificat local éphémère. Le test Android utilise Maven et Robolectric 4.16.1. L’audit d’APK utilise androguard 4.1.4.

| Fichier | SHA-256 |
| --- | --- |
| APK original | `9684155eb8f1ce5c5b87f1cf6ed9b48d384417ac0474bcbf82da052360e5ddde` |
| Cross-Device-Detox-Sync-0.1.0.mpp | `0dace7e4a1e944dd97ad5911b143f5319326db91b6a97ef88d04697d28e6b439` |
| Nesquiks-patches-0.4.12.mpp | `44fb6972cd0a1276d8c785b439b730ea44ccf811d4c20db18e302b727e485b8c` |
| Cross-Device-Detox-Sync-0.1.1.mpp | `fb066e175a5f334f3df24dcf69874aec211fe6f864177df8d68d8f3fd0daffb2` |
| Nesquiks-patches-0.4.13.mpp | `2551b81e6838d051e2dc3d39713571be8a6e708164f601e93a3599be9ee520bb` |
