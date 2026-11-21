# Native Gemini Description Persistence 0.1.0 — expérimental

**Ce livrable ne réalise pas encore le forçage universel demandé.** Il conserve uniquement le vrai pied de panneau « Résume la vidéo » précédemment fourni par YouTube pour **la même vidéo et la même configuration d'écran**. Aucune interface n'est recréée. Les vidéos jamais capturées restent inchangées.

## Installation et essai

1. Importer `Native_Gemini_Description_0.1.0_experimental.mpp` comme source supplémentaire dans Morphe Manager.
2. Sur l'APK **original YouTube 21.30.216**, sélectionner `Force native Gemini summary button (Description)` et le patch officiel **Ask flyout menu**. Garder Ask Native Action Bar 0.8.2 et les traductions 0.4.0 si souhaité. Installer la mise à jour en conservant les données et le même nom de package/signature.
3. Laisser **Masquer la section Ask** désactivé, ainsi que tout filtre personnalisé masquant `input_composer_button.e` ou `youchat_entrypoint.e`.
4. Avec le VPN, charger une vidéo où YouTube fournit vraiment le bouton et ouvrir sa Description. Fermer puis relancer YouTube sans VPN ; rouvrir **cette même vidéo**, dans la même orientation, langue, largeur et thème. Vérifier affichage et clic.

Aucun réglage supplémentaire ni faux bouton n'est ajouté. Le patch n'envoie aucune requête Gemini tout seul. Le clic reste celui contenu dans le modèle natif.

## Résultats vérifiés le 14 novembre 2026

| Contrôle | Résultat |
| --- | --- |
| Compilation helper Java + Kotlin + DEX | Réussie |
| Toutes les classes JVM du MPP | Version 61 maximum (Java 17) |
| Chargement par Morphe PatchLoader sur **JRE 17.0.17+10 réel** | Réussi, une entrée de patch |
| 24 assertions sur données protobuf synthétiques | Réussies |
| Application et reconstruction APK original 21.30.216 | Réussies, `success: true` |
| Composition testée | Morphe Patches 2.1.0-dev.8 : Ask flyout menu + Hide video action buttons ; Ask0.8.2 ; captions0.4.0 ; ce patch |
| Inspection DEX | 15 observations de retours protobuf ; 1 binder natif ; 15 hooks Ask0.8.2 conservés |
| Comparaison au DEX original | 10 méthodes de rendu/cycle de vie du footer natif inchangées |
| Absence d'interface personnalisée | Pas de références View/widget/graphics ni de nouveau gestionnaire de clic dans l'extension |
| Affichage, clic, redémarrage et VPN sur Android/S24 | **Non exécutés** |
| YouTube 21.20.405 | **Non testé, non déclaré compatible dans ce MPP** |
| Ensemble des 109 patchs de l'utilisateur | **Non testé** |

Le test de persistance JVM simule les SharedPreferences puis vide le cache mémoire : ce n'est pas un redémarrage Android réel. Le test DEX ne prouve ni l'affichage ni l'acceptation de la requête Gemini par le serveur.

## Modifications ciblées

Le code de YouTube associe le panneau `video-description-ep-identifier` à un pied de panneau protobuf. Sur l'APK fourni, le chemin est : panneau champ 10 → extension 260791572 → champ 1 → Elements 153515154. Le binder natif `Lajta.ml(Lbdwl;Lbgbl;Latsx;)V` et le renderer `Lajto` ont été inspectés.

L'observateur lit les listes natives de panneaux de la réponse watch (champs 25 et 30), et associe chaque capture à l'identifiant de vidéo de **cette même réponse**. Le snapshot conserve le footer natif entier et ses paramètres de présentation 39/47. Avant le binder, il rétablit ces données seulement si le footer manque. Tout footer déjà fourni est préservé. L'icône, le texte, les commandes et les autres données du footer sont conservés tels quels ; aucun appel à `openAskPanel`, aucune nouvelle action et aucun dessin ne sont générés.

Les données sont stockées dans des SharedPreferences privées (`neszi_ask_description_v1`), avec un maximum de **16 captures vidéo/configuration** et 256 Kio par footer. Une désinstallation, un effacement des données ou l'éviction du cache supprime les captures. Une mise à jour YouTube, un changement de langue, thème, orientation ou dimensions impose une nouvelle capture. La reconnaissance actuelle cible les libellés natifs français et anglais du résumé.

## Ce qui reste inconnu / bloquant

- Le renderer natif montre le footer lorsqu'il reçoit un modèle Elements et le masque sinon. Aucun interrupteur géographique propre à Gemini n'a été établi dans ce chemin. **Cela ne prouve pas l'absence de contrôles ailleurs.**
- Aucun couple de réponses réelles avec/sans VPN de ton compte n'est disponible ici. L'absence du modèle côté serveur est une hypothèse cohérente avec le symptôme, pas une cause réseau prouvée.
- Le footer reçu peut dépendre de templates Elements, d'entités ou de jetons contextuels. Leur disponibilité après redémarrage et la validité des commandes sauvegardées restent à vérifier sur appareil. Une commande ancienne peut être refusée.
- Le patch ne transfère pas une commande d'une vidéo vers une autre. Pour rendre le bouton universel tout en conservant son comportement natif, il faut d'abord identifier le modèle réellement reçu et les données de commande propres à chaque vidéo. Modifier des champs inconnus ou inventer un bouton ne respecterait pas la demande.

## Diagnostic court

Sur un ordinateur avec ADB, après reproduction :

```sh
adb logcat -d -s AskDescription:I '*:S'
```

Messages utiles : `native_footer_saved`, `native_footer_restored_same_video`, `description_received_without_footer`, `description_footer_unrecognized`, `no_native_footer_saved_for_this_video_and_configuration`. « restored » confirme uniquement la remise des données au binder, **pas** que l'interface a été dessinée.

## Reconstruction

Depuis le dossier `source/` du ZIP (le dossier `toolchain/` est créé à côté) :

```sh
python3 native/bootstrap_toolchain.py
bash description/build.sh
bash description/tests/run.sh
```

Le script `description/tests/apply.sh` utilise les APK/MPP d'entrée sous `../recovery/` ; ils ne sont pas redistribués dans cette archive. Journaux : `description/logs/`. Sources du nouveau patch : `description/src/`.

Sources primaires examinées : APK original fourni ; `DescriptionComponentsFilter.java` et `EngagementPanel.java` de Morphe v2.1.0-dev.8, archivés sous `description/research/` ; https://github.com/MorpheApp/morphe-patches/issues/1370 . Les sources Ask0.8.2 restent dans `native/` pour continuité, sans modifications fonctionnelles.
