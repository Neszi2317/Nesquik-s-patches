# Patchs Reddit 2026.48.0

Les trois patchs personnalisés prennent en charge `com.reddit.frontpage` **2026.48.0 (2648001)** dans Nesquik’s patches **0.4.8**. Le catalogue conserve également la cible **2026.47.0**.

| Patch Morphe | Module |
|---|---|
| Reddit - Local keyword filters | `RedditContentFilter-v2.4.mpp` |
| Hide NSFW posts | `reddit-hide-nsfw-1.2.0.mpp` |
| Custom Home & Disable Main Feed | `reddit-custom-home-0.3.0.mpp` |

1. Actualise la source Nesquik’s patches et garde **Morphe Patches 2.4.0** pour Reddit 2026.48.0.
2. Pars de l’APKM original et sélectionne les fonctions souhaitées. Pour **Custom Home**, active aussi un patch officiel qui installe les réglages Morphe, par exemple **Hide navigation buttons**.
3. Pour incorporer une liste, configure les options du filtre avant le patching. Les options acceptent du texte et des fichiers TXT UTF-8, avec une règle par ligne. Réutilise ta clé de signature pour mettre à jour une installation existante.

Les clés de préférences des deux extensions sont conservées. Le masquage NSFW est actif dès que le patch est inclus. L'accueil personnalisé utilise `nightfeeling` comme valeur initiale, modifiable dans ses réglages. L'inclusion d'un patch officiel installe ses options ; elle ne signifie pas que toutes ses options sont activées simultanément dans Reddit.

## Vérification

Les trois patchs ont été appliqués à l’APKM **2026.48.0** avec les patchs officiels activés par défaut et avec validation des fingerprints. Le test inspecte ensuite les DEX générés et le chaînage des réglages. L’utilisation sur un téléphone reste à vérifier.

Le [rapport de la version 2026.47.0](RAPPORT.md) décrit le portage précédent.

## Sources et reproduction

Linux x86_64, Python 3 et accès Internet aux dépendances officielles :

```sh
python3 fetch-tools.py
python3 build.py
python3 run-tests.py
python3 integration.py /chemin/reddit-original-2026.48.0.apkm
```

`TOOLS_DIR` peut désigner un dossier d'outils déjà installé. `toolchain.json` fixe toutes les URL et empreintes SHA-256. La vérification d'intégration produit un APK **non signé**, réservé à l'analyse. Les livrables à importer dans Morphe sont les `.mpp`.

Le code source, les stubs explicitement utilisés à la compilation, les tests, les licences, les résultats et les outils d'analyse sont inclus. Les APK propriétaires et les volumineuses dépendances ne sont pas redistribués.

La carte de correspondance des types de l'analyse est disponible dans `analysis/type-map-39-to-47.json.gz`. Pour lire le JSON, utiliser `gzip -dc analysis/type-map-39-to-47.json.gz`.
