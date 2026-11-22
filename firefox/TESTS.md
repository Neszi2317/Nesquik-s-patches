# Vérification du bloqueur Firefox 0.1.3

## Patching avec Morphe Desktop 2.3.0

| APK Mozilla | Source Heval | Résultat |
| --- | --- | --- |
| Firefox 159.0, ARM64 | Aucune | Bloqueur appliqué et APK signé |
| Firefox 160.0, ARM64 | 3.0.0 | Bloqueur et sept patchs Firefox appliqués, APK signé |
| Firefox 160.0.1, ARM64 (2016196674) | 3.1.0-dev.1 | Bloqueur, sept patchs Firefox et Remove screenshot restriction appliqués, APK signé |
| Firefox 160.0.1, x86_64 (2016196678) | 3.1.0-dev.1 | Même combinaison appliquée, APK signé |

Sur 160.0.1, la combinaison a été répétée avec le MPP indépendant puis avec le bundle Nesquik’s patches 0.4.0. Le filtre DNS Heval était en mode `fallback` sur 160.0.1 et en mode strict sur 160.0. Les essais n’ont pas utilisé `--force`. Les APK 160.0.1 signés ont passé `apksigner verify`.

L’archive `omni.ja` reste lisible et intègre. Le patch change uniquement l’amorçage GeckoView dans cette archive et ajoute le module du bloqueur. Le cas sans règles configure un module avec une liste vide.

## Vérification du filtrage

`node firefox/tests/rules.mjs` vérifie 16 URL avec le filtre, la politique de contenu et l’observateur réseau. L’ancienne version échoue sur la règle `https://example.net/`, corrigée dans 0.1.3.

Le module extrait de l’APK combiné a aussi été chargé dans Firefox Desktop 160.0.1. Un serveur HTTP de test a confirmé le rejet des six navigations interdites et le chargement des cinq navigations autorisées, dont un sous-domaine, une règle de la forme `https://domaine/`, un chemin, un subreddit et une redirection. Les scénarios autorisés incluaient un chemin au préfixe similaire, un subreddit au nom similaire et une redirection vers une page autorisée. Deux requêtes HTTPS ont aussi été vérifiées, une autorisée et une bloquée.

Le lancement sur Android reste à confirmer. L’émulateur Android 15 n’a pas répondu aux commandes de préparation après le démarrage. Sur l’émulateur Android 9, le transfert de l’APK a réussi, puis la commande d’installation a expiré.

## Empreintes SHA-256 des fichiers testés

| Fichier | SHA-256 |
| --- | --- |
| Firefox 160.0.1 Mozilla ARM64 | `1c811bbdc8d7488b9340d379889cf04105525596d21a7c596b3a664d833858ea` |
| Firefox 160.0.1 Mozilla x86_64 | `283b19f8313e8ce6e8a913bb178ce4a79c5cecd82c49f821de7fafb068c73fcd` |
| Firefox-Local-Website-Blocker-0.1.3.mpp | `914f1d4be20a1a0a105083676a948b5978500320aa2cd781a7b058d6fec82cc0` |
| Nesquiks-patches-0.4.0.mpp | `876ab0c05d0321063bff2076c44b1e84478edb8d173294e8d62528b489993030` |
