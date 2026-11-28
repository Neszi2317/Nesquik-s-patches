# Essais du 28 novembre 2026

APK Samsung Internet 31.0.2.41 arm64, SHA-256 `8f460d77ce60da87aa5d25b39da7cd2668bda447aafc64ff7b288c026b1d1802`. Morphe Desktop 2.3.0, JDK 21, SBrowserX 1.3.9 au commit `f01dc5a7`.

| Essai | Résultat observé |
| --- | --- |
| MPP indépendant | APK reconstruit et signé. Signature v2 vérifiée par apksig. |
| Collection 0.4.14 avec SBrowserX | Les 16 patchs de SBrowserX et le bloqueur ont été appliqués ensemble. 17 succès, zéro échec. Signature v2 vérifiée. |
| Règles | Sur les classes extraites de chacun des deux APK, 22 cas d’URL réussis avec puis sans support Knox simulé. Domaines, sous-domaines, chemins, requêtes, subreddits, utilisateurs dans l’URL, ports, IPv4, IPv6, schémas internes et valeurs nulles. |
| Navigation et redirections | Exécution sous Unicorn des deux callbacks ARM64 du moteur fourni, avec un pont JNI simulé vers les classes extraites et un serveur HTTP local. Sur chaque APK, accès direct et redirection HTTP 302 testés vers des destinations autorisées et bloquées. Le serveur a servi les destinations autorisées et reçu zéro requête pour les destinations interdites. |
| Options Morphe | Saisie manuelle + TXT fusionnés, doublons éliminés. Liste vide : activation d’origine conservée. Un port invalide dans une règle fait échouer le patching. |
| Charge | 50 000 règles, dont 5 000 chemins Reddit, intégrées à un véritable APK. 300 000 décisions sur six URL récurrentes après échauffement, moyenne de 0,205 µs sur JVM hôte. |
| Intégrité | Avec le bloqueur seul, manifeste et resources.arsc identiques à l’original. La collection conserve les 23 définitions précédentes, leurs options et compatibilités et les 434 classes JVM existantes, identiques octet pour octet. |
| Essai sur Android | Aucun téléphone ni émulateur Android opérationnel accessible. Le démarrage du navigateur, la navigation interactive en mode normal et Secret, et les ralentissements sur Galaxy restent à valider sur appareil. |

Le filtre agit sur le chemin commun des requêtes et ne lit pas l’état du mode Secret. Les essais de composants ne lancent pas l’interface Samsung. Le script fourni dans tests/ permet de rejouer les vérifications JVM des décisions et de la délégation Knox.
