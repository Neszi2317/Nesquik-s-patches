# Points vérifiés dans YouTube 21.16.256

## Hooks utilisés

Les noms ci-dessous sont des **résultats de résolution**, pas des noms codés en dur dans le patch.

| Fonction | Méthode / champ résolu |
|---|---|
| Création de la vue BrowseFragment | `Llne;->O(LayoutInflater,ViewGroup,Bundle)View` |
| Endpoint du fragment | `Llne;->ap:Layrv;` |
| Affectation d’endpoint | `Llne;->cj(Layrv;)V` |
| Conversion endpoint → browseId | `Lhvf;->s(Layrv;)String` |
| Extension browse du protobuf endpoint | `Laybm;->a:Lawnp;` |
| Construction Cronet | `Lacgs;->R(Lacuj;Map;[B;Lacry;UrlRequest$Callback;Lacwv;Lacrg;)UrlRequest$Builder` |
| Compte / page YouTube | `Laade;->c:String` |
| Navigation privée | `Laade;->d:Z` |

Fingerprint principal : chaîne exacte `Browse Fragment was given a navigation endpoint without browse data.` et signature `View(LayoutInflater,ViewGroup,Bundle)`. Le champ endpoint et la méthode browseId sont dérivés des instructions entourant le contrôle de l’extension browse. Les retours de la création de vue reçoivent un appel statique sans changement de registre local.

La méthode bridge ajoutée à notre propre extension est remplacée par un accès typé au champ du fragment. Les méthodes natives ne sont pas remplacées. La loupe est ajoutée à un FrameLayout existant dans la vue du fragment ; la recherche globale reste intacte.

`Ljjy;->bc(View)View` appelle `bF(...)` qui peut retourner la vue d’origine ou un ViewGroup contenant cette vue dans un FrameLayout. D’où la recherche d’un FrameLayout descendant si la racine n’est pas elle-même un FrameLayout.

Le hook de session recherche les appels `newUrlRequestBuilder`, `Map.entrySet` et `setPriority`. Il lit les en-têtes sans modifier la map. Les champs de compte sont dérivés des libellés `, getPageId=` et `, isIncognito=` de `toString`, puis quatre getters / contrôles courts sont instrumentés.

## Pistes natives étudiées, non utilisées dans cette version

- `ahvq` : service browse, `f()` crée un `ahvs`, `l(ahvs,Executor)` produit un futur de résultat.
- `ahvs.Q(String)` affecte le browseId ; le résultat est enveloppé dans `ahmq.a:bbhh`.
- Le chaînage natif des continuations passe notamment par `aoyy`, `aozb`, `belx` et `ahvq.b(aoyy)`.
- Une adaptation complète de ces modèles protobuf et des différentes variantes de rendu aurait été plus fragile pour cette première livraison. La version livrée reprend la route JSON déjà utilisée par `GetPlaylistItemsRequest` / `PlaylistRoutes` de Morphe, avec pagination ajoutée.
- `SEARCH_PLAYLIST_ID` intervient notamment dans `Lnqr;->c()` (construction d’intent de recherche vocale). Ce nom seul ne démontre pas l’existence d’une recherche serveur utilisable et limitée à une playlist.
- `playlist_video` apparaît également dans des tables de contenu hors ligne. Ce n’est pas un index universel des playlists privées du compte.

## Tests reproductibles

`./test.sh` vérifie sur fixtures synthétiques : titre OU chaîne, accents, casse, requête vide, zéro résultat, ordre, exclusion d’éléments extérieurs, refus des formats inconnus, pagination de 1 100 vidéos et changement d’identité.

Les essais HTTP publics ont employé la même route `browse`, le même client Android et le même parseur Java que l’extension :

- `PLzH6n4zXuckpfMu_4Ff8E7Z1behQks5ba` : 11 vidéos ; les 11 correspondent à la recherche `Computerphile`.
- `UU8l9frL61Yl5KFOl87nIm2w` : 19 réponses, 1 100 vidéos ; toutes les pages décodées, correspondance de chaîne validée. Essai arrêté à 1 100, alors qu’une continuation était encore disponible : **pas un parcours complet de cette grande playlist**.

Ces identifiants publics proviennent des cas de test du projet yt-dlp (`youtube/_tab.py`). Ils ne nécessitent pas de compte. Les en-têtes de connexion du téléphone, les playlists privées et le rendu Android n’ont pas été testés dans cet environnement.

## Format et outillage

JDK Temurin 21.0.12.1, Kotlin 2.4.21 (language/API 2.2), D8/R8 8.13.17, Morphe Desktop 1.18.1 / Patcher 1.15.1. La sortie de métadonnées Kotlin 2.2 évite les avertissements de métadonnées non comprises par cette version de D8. Un avertissement de dépréciation de `compatibleWith(Pair...)` reste sans échec de compilation.

Le `.mpp` contient les classes JVM, `classes.dex`, `playlist-extension.dex`, le manifeste et les mentions de licence. Les classes du moteur Morphe, d’Android et d’org.json ne sont pas embarquées.
