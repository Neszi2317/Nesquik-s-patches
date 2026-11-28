# Samsung Internet - Local Website Blocker

Le patch 0.1.0 filtre localement les requêtes HTTP et HTTPS de Samsung Internet **31.0.2.41**. Il utilise le filtre de requêtes intégré au moteur Chromium de Samsung. Les règles sont inscrites dans l’APK et leur modification nécessite un nouveau patching.

## Utilisation dans Morphe Manager

1. Actualise **Nesquik’s patches** en version **0.4.14** ou importe le [MPP indépendant](../releases/Samsung-Internet-Local-Website-Blocker-0.1.0.mpp). Utilise une seule de ces sources pour le bloqueur.
2. Sélectionne l’APK **Samsung Internet 31.0.2.41** original. Coche **Samsung Internet - Local Website Blocker** et les patchs **SBrowserX v1.3.9** que tu souhaites appliquer dans la même opération.
3. Ouvre les options du bloqueur. Ajoute tes règles dans **Domaines bloqués** ou utilise **Importer un TXT**. Morphe fusionne les deux listes et élimine les doublons. Lance le patching et installe l’APK obtenu.

| Règle | Effet |
| --- | --- |
| `example.org`, `*.example.org` ou `https://example.org/` | Le domaine et ses sous-domaines sont bloqués en HTTP et HTTPS. |
| `example.org/dossier` | Bloque ce chemin et ses descendants. Autorise `/dossier-bis`. |
| `example.org/page?x=1` | Bloque la valeur exacte et les paramètres suivants. Autorise `?x=12`. |
| `/r/exemple` ou `reddit.com/r/exemple` | Bloque ce subreddit sur Reddit et ses sous-domaines. Autorise `/r/exemple2`. |

Le TXT doit être en UTF-8, avec des noms de domaine ASCII ou Punycode, de préférence une règle par ligne. Les anciens exports comportant plusieurs entrées séparées par des espaces sont acceptés. Un mot commençant par `#` introduit un commentaire. Limites de la liste : 1 Mio et 50 000 règles. Une liste vide conserve le comportement Samsung.

## Fonctionnement

Le moteur demande à `TinUrlFilterBridge.isUrlFilterSupported()` s’il doit activer son filtre. Pour une liste non vide, le patch l’active et mémorise la réponse d’origine. Au départ d’une requête et à chaque redirection interceptée, `TerraceUrlFilter.isBlockedUrl(String)` consulte les règles intégrées. Si l’URL est autorisée, Knox conserve sa décision sur les appareils où il était déjà actif. Le code ne consulte aucun serveur, réglage de navigation ou fichier utilisateur. Il n’ajoute ni menu ni extension désactivable.

Les changements de chemin ou de fragment entièrement internes à une page déjà chargée ne déclenchent pas systématiquement une requête. Ils échappent alors à ce point de filtrage. Le niveau de validation sur appareil est précisé dans le [bilan des tests](TESTS.md).

## Compilation et essais

Depuis la racine du dépôt, avec un JDK 21 et Python 3.11+ :

```sh
python3 tools/fetch-toolchain.py .toolchain morphe.jar r8.jar android.jar annotations.jar kotlin-compiler-embeddable-2.4.jar kotlin-stdlib-2.4.jar kotlin-script-runtime-2.4.jar kotlin-reflect.jar kotlin-coroutines.jar dex-tools.zip
samsung-internet/build.sh .toolchain dist/Samsung-Internet-Local-Website-Blocker-0.1.0.mpp
java -jar .toolchain/morphe.jar patch --exclusive --validate \
  -p dist/Samsung-Internet-Local-Website-Blocker-0.1.0.mpp \
  -OdomainsFile="$PWD/samsung-internet/tests/domains.txt" \
  -e 'Samsung Internet - Local Website Blocker' \
  -o /tmp/samsung-patched.apk /chemin/vers/samsung-31.0.2.41.apk
samsung-internet/tests/check.sh .toolchain /tmp/samsung-patched.apk
```

Code du bloqueur sous [MPL-2.0](https://www.mozilla.org/MPL/2.0/).
