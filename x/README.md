# Patchs X de Nesquik

Les cinq patchs ciblent `com.twitter.android` 12.43.0-prod.01. Ajouter le bundle Nesquik et la source Piko 3.12.0-dev.5 dans Morphe. Dans le même patching, sélectionner au minimum **NewX: Filter posts by keyword** dans Piko, puis les patchs personnalisés souhaités.

## Intégration

- L'adaptateur ajoute les titres dans les ressources anglaises et françaises, puis enregistre les cinq réglages dans le catalogue **Piko → Contenu**. Il vérifie que le catalogue officiel a été initialisé avant d'y ajouter les entrées.
- Les filtres sensibles et par mots-clés personnalisés complètent le pipeline de filtrage de la version de Piko testée. Les résolveurs nécessaires à cette opération proviennent du tag Piko `v3.12.0-dev.5`. Le script récupère quelques classes de support dans l'artefact publié à la compilation. Les patchs Piko sélectionnables et ses extensions Android sont fournis par la source officielle.
- Les clés des réglages restent celles du portage 12.43 précédent. Les préférences de l'application se conservent lors d'une mise à jour avec le même paquet et la même signature.
- Le flou reste expérimental. La modification vise l'emplacement de Grok dans le détail du post et les médias des réponses. Son rendu et le geste de révélation restent à vérifier sur Android.

## Build et audit

```sh
x/build.sh .toolchain/piko.mpp .toolchain/morphe.jar .toolchain Nesquik-X.mpp
x/test-host.sh .toolchain
java -Xmx2600m --class-path .toolchain/morphe.jar x/AuditPort.java X-patched.apk
```

Le [tag Piko épinglé](https://github.com/crimera/piko/tree/v3.12.0-dev.5) fournit le code amont. Le module est distribué sous GPL-3.0 et la licence RE2/J est conservée.
