# Cross-Device Detox Sync

Patch Morphe indépendant pour Digital Detox 11.42 (592), paquet `com.urbandroid.ddc`. Une case dans la confirmation habituelle permet de lancer la même période de détox sur les appareils associés. Chaque appareil applique sa propre liste d’applications autorisées, ses pénalités et ses réglages de protection. La synchronisation est désactivée par défaut.

## Installer sur le Galaxy S24 et la Galaxy Tab S9 FE

1. Sur chaque appareil, actualiser **Nesquik’s patches** dans Morphe Manager, sélectionner l’APK Digital Detox **11.42 (592)** et cocher **Cross-Device Detox Sync**. Autre possibilité, importer le [MPP indépendant](../releases/Cross-Device-Detox-Sync-0.1.0.mpp). Utiliser une seule de ces deux sources pour ce patch.
2. Installer l’APK produit. Pour une installation officielle existante, la signature de Morphe peut empêcher une mise à jour sur place. Avant de remplacer l’application, attendre la fin de toute session et préserver les données utiles avec les moyens proposés par Digital Detox. La conservation de l’historique et des achats après changement de signature reste à vérifier. Conserver la même clé de signature Morphe pour les mises à jour.
3. Ouvrir Digital Detox sur les deux appareils. Configurer les protections natives souhaitées, autoriser les notifications et vérifier qu’une détox locale fonctionne.
4. Installer et ouvrir **ntfy** sur les deux appareils. Dans ses paramètres, utiliser **https://ntfy.sh** comme serveur par défaut et activer **UnifiedPush** si ce réglage est désactivé. Digital Detox crée son abonnement à la première ouverture de sa rubrique de synchronisation.
5. Pour le démarrage automatique en veille, exclure **Digital Detox** de l’optimisation de batterie dans Android. Sur Samsung, vérifier également que **ntfy** et **Digital Detox** ne figurent pas parmi les applications en veille profonde. La rubrique de synchronisation indique si l’exemption de Digital Detox manque. Les libellés système varient selon One UI.

Le relais public ntfy propose un accès gratuit sans compte. Aucun serveur personnel ni projet Firebase à créer. ntfy conserve une connexion pour UnifiedPush. La consommation réelle sur les deux Samsung n’a pas été mesurée. [Application Android](https://play.google.com/store/apps/details?id=io.heckel.ntfy) · [Documentation ntfy](https://docs.ntfy.sh/subscribe/phone/) · [Tarifs](https://ntfy.sh/#pricing)

## Associer les deux appareils

1. Ouvrir **Digital Detox → Paramètres → Synchronisation des appareils** sur chacun. Si le bouton **Afficher mon code d’association** manque, ouvrir ntfy, vérifier le serveur par défaut, revenir et toucher **Connecter à ntfy**.
2. Sur le S24, toucher **Afficher mon code d’association → Copier**. Transférer le code complet par un canal personnel puis, sur la tablette, toucher **Associer un appareil** et coller le code.
3. Refaire l’opération en sens inverse avec le code de la tablette. Chaque appareil autorise explicitement la clé publique de l’autre. Le code contient aussi une adresse de réception à garder privée. L’association utilise un code copiable. Elle ne nécessite pas de caméra.
4. Toucher le nouvel appareil, le **Renommer** puis choisir **Vérifier la connexion**. **L’appareil a répondu** confirme un aller-retour authentifié. La date de dernier contact permet de distinguer une réponse récente d’une ancienne. Refaire le test dans l’autre sens.
5. Régler **Synchroniser par défaut** séparément sur chaque appareil si souhaité. **Dissocier** retire immédiatement l’autorisation locale et les commandes en attente associées. Pour supprimer la relation des deux côtés, dissocier aussi sur l’autre appareil. Après réinstallation ou changement d’adresse de réception, échanger les codes à nouveau.

## Lancer une session

Choisir sa durée habituelle. Dans la confirmation native, cocher **Synchroniser avec mes autres appareils** pour solliciter tous les appareils associés. Décocher pour une session uniquement locale. Une annulation du dialogue ne transmet aucune commande. Les lancements planifiés, Tasker et les sessions reçues ne sont pas rediffusés.

Le téléphone et la tablette doivent avoir une heure système correcte. Le message contient l’échéance UTC de la session locale créée. À réception, le second appareil recalcule la durée restante. Une session déjà active y est conservée, même si elle finit plus tôt. Cette version synchronise les nouvelles sessions manuelles jusqu’à 90 jours.

| État | Signification |
| --- | --- |
| Réception non confirmée | La session locale a démarré. L’autre appareil peut être hors ligne, endormi ou encore en cours d’association. |
| Commande reçue | L’autre appareil a reçu une commande signée et vérifie son démarrage natif. |
| Session distante confirmée | L’autre appareil a observé le service Digital Detox en marche et une échéance native correspondant à la demande à trois secondes près. |
| Une détox est déjà en cours | La session existante continue avec ses réglages et sa durée. |
| Ouvre Digital Detox sur l’autre appareil | Android restreint le démarrage en arrière-plan ou une autorisation native manque. L’appareil conserve la commande jusqu’à l’échéance. Une notification invite à ouvrir l’application. |
| Démarrage natif impossible à confirmer | Le service ou l’échéance attendue n’a pas été observé. Vérifier l’état natif sur l’appareil distant. |
| Échéance passée sans confirmation | Le démarrage distant n’a pas été confirmé à temps. Une commande expirée ne démarre plus rien. |

## Transport et intégrité

| Solution étudiée | Décision pour cette version |
| --- | --- |
| FCM direct | Nécessite un projet Firebase dédié et un expéditeur authentifié. Les identifiants de l’éditeur ne sont pas disponibles à ce patch. |
| Relais personnel et interrogation périodique | Faisable, mais l’interrogation seule dépend des fenêtres de travail d’Android et nécessite un hébergement. |
| Réseau local | Insuffisant lorsque le S24 utilise les données mobiles hors du Wi-Fi de la tablette. |
| ntfy et UnifiedPush | Retenu. Adresse HTTPS par appareil, réception Android ciblée, cache temporaire du relais et reprise des envois côté expéditeur. |

Chaque installation génère une clé de signature EC P-256 dans Android Keystore. Les messages ECDSA lient l’expéditeur, le destinataire, le type, l’identifiant et les deux dates. L’adresse HTTPS acceptée est limitée à un sujet UnifiedPush du serveur ntfy.sh. Les commandes ne contiennent ni liste d’applications, ni code de sortie, ni données de paiement. Le relais peut lire les heures et les métadonnées de livraison. La signature protège l’authenticité et l’intégrité. Il n’y a pas de chiffrement de bout en bout du contenu.

Les commandes en cours et les réponses sont journalisées dans SQLite, sous le dossier privé exclu des sauvegardes Android. La file réessaie après échec réseau. En l’absence d’accusé final, une tâche réseau persistante réémet aussi la commande encore valide. Android décide de l’heure effective des nouvelles tentatives. Le récepteur conserve l’identifiant avant d’appeler le lancement natif. Une interruption à cet instant privilégie l’absence de double lancement et peut produire un état incertain. Une révocation locale supprime les entrées du pair.

L’application distante peut demander uniquement une nouvelle détox. Le protocole comprend aussi les sondes de connexion et les accusés. Une seule méthode d’entrée manuelle émet les demandes de démarrage. Aucune commande de fin, de modification d’une session, de remplacement de whitelist ou de paiement n’est implémentée.

L’APK confirme le trajet `StartDetoxReceiver → ChallengeService.startCustom`, l’extra long `time_extra` en millisecondes, et `Challenge.getEndTime()` pour l’échéance enregistrée. Le patch appelle le récepteur natif sur le thread principal après validation de l’autorisation du pair et de l’état de session. Les branchements utilisateur couvrent les deux chemins du bouton de confirmation (durée prédéfinie et personnalisée). Les réglages sont intégrés aux variantes de ressources générique et v26. [Automatisation officielle d’Urbandroid](https://play.google.com/store/apps/details?id=com.urbandroid.ddc) · [Restrictions Android](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start) · [Cache ntfy](https://docs.ntfy.sh/config/#message-cache)

## Compiler

Depuis la racine du dépôt, utiliser JDK 21, Bash et Python 3.

```sh
python3 tools/fetch-toolchain.py .toolchain
digital-detox/build.sh "$PWD/.toolchain" "$PWD/releases/Cross-Device-Detox-Sync-0.1.0.mpp"
java -jar .toolchain/morphe.jar patch --exclusive \
  -p releases/Cross-Device-Detox-Sync-0.1.0.mpp -e 'Cross-Device Detox Sync' \
  -o digital-detox-patched.apk original.apk
```

Les classes de liaison sous `stubs/` servent uniquement à la compilation. Les classes de Digital Detox sont fournies par l’APK d’origine. Le [rapport de tests](TESTS.md) détaille la couverture et les vérifications restant à faire sur matériel.
