# Firefox - Local Website Blocker

Le patch **0.1.3** applique des règles locales aux URL HTTP(S) de Firefox Android (`org.mozilla.firefox`). Il est inclus dans **Nesquik’s patches 0.4.0** et prend en charge **159.0**, **160.0** et **160.0.1**.

Dans Morphe, sélectionne **Firefox - Local Website Blocker** et configure ses options avant de générer l’APK. Les entrées manuelles sont fusionnées avec le fichier TXT UTF-8, qui accepte une règle par ligne.

| Exemple | Blocage |
| --- | --- |
| `example.org` ou `https://example.org/` | Le domaine et ses sous-domaines |
| `example.org/dossier` | Ce chemin et ses sous-chemins sur le domaine |
| `reddit.com/r/exemple` | Ce subreddit, y compris sur les sous-domaines de Reddit |

Pour Firefox **160.0.1**, active les préversions de la source Heval et sélectionne **[3.1.0-dev.1](https://github.com/heval99/Heval-Morphe-Patches/releases/tag/v3.1.0-dev.1)**. La version stable Heval **3.0.0** cible Firefox **160.0**. Le filtre DNS de Heval a ses propres réglages, indépendants de la liste de blocage locale.

Le blocage est disponible dans le bundle de la collection ou en [MPP indépendant](../releases/Firefox-Local-Website-Blocker-0.1.3.mpp). Sélectionne une seule de ces deux sources pour le bloqueur.

## Construction et tests des règles

Depuis la racine du dépôt, avec Java 21 et Node.js pour les tests :

```sh
python3 tools/fetch-toolchain.py .toolchain
bash firefox/build.sh .toolchain dist/Firefox-Local-Website-Blocker-0.1.3.mpp
node firefox/tests/rules.mjs
```

Voir les [résultats de test](TESTS.md).

Code sous [MPL-2.0](https://www.mozilla.org/MPL/2.0/).
