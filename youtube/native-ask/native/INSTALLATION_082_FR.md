# Ask Native Action Bar 0.8.2 — téléphone Minimaliste

1. Remplace la source Ask Native 0.8.1 par 0.8.2 dans Morphe Manager. Garde « Ask flyout menu », « Hide video action buttons » et « ZZ Ask native action button » sélectionnés, puis patche l'APK original 21.30.216. Installe la mise à jour sans effacer les données de YouTube.
2. Laisse la disposition Morphe sur « Minimaliste ». Active le VPN où le vrai bouton Gemini apparaît, puis ouvre une nouvelle vidéo. Une capture native de la barre affiche « bouton minimaliste et position mémorisés ».
3. Appuie aussi une fois sur Gemini : si la présentation du panneau n'était pas encore capturée, le message « ouverture native du panneau mémorisée au clic » le confirme.
4. Désactive le VPN, ferme puis relance YouTube et ouvre une autre vidéo. Vérifie Ask après Partager, Enregistrer dans ⋯, puis le clic et la vidéo concernée. Vérifie aussi le retour en disposition Classique.

La restauration affiche « bouton minimaliste restauré ». Ce message indique que les données du composant ont été modifiées ; l'affichage réel doit encore être constaté sur l'appareil.

Les captures d'écran montraient le bouton natif avec VPN, mais la 0.8.1 ignorait le template quand son identifiant de propriétés était absent, ne synchronisait pas les listes internes du mode Minimaliste et intervenait avant la conversion locale de disposition. Ces chemins sont corrigés.

Tests réussis : compilation cibleJava 17, neuf suites JVM, test des méthodes exactes du convertisseur officiel avec ses classes protobuf, application/reconstruction/validation DEX sur l'APK original 21.30.216. Bundles : Morphe Patches 2.1.0-dev.8 et Universal native caption translations 0.4.0. Quatre patchs sélectionnés + dépendances, pas les 109 options personnelles.

Aucun test Android réel. La disponibilité serveur du panneau sans VPN n'est pas garantie. Le plein écran paysage n'est pas corrigé par cette version. Aucun nouveau test d'application à l'APK tablette 21.44.528 ; ses chemins sont couverts par les tests de non-régression JVM.
