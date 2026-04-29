# EDT

HeRVé, un moteur de Recherche Vectorielle.

## Résumé

### Principe

HeRVé permet d'effectuer une recherche de mots-clés sur un corpus de textes (pages web par exemple), à partir de fichiers d'index préalablement générés.

Cette recherche pourra être effectuée :

- soit en ligne de commande, en lançant une recherche ponctuelle ou en basculant en mode interactif.
- soit via une interface web.

### Remarques

- L'application est écrite en Java : on n'utilise que les paquets Java standards (OpenJDK 17), sans faire appel à aucune librairie externe.
- Elle tourne sous Linux (Debian 12 ou 13).
- L'encodage utilisé est toujours l'UTF8 (fichiers de configuration, fichiers d'index, affichage...).
- Vous pouvez rajouter des options supplémentaires par rapport à celles spécifiées ici, mais vous ne pouvez pas en enlever ni en renommer.

### Utilisation

L'exécutable s'appelle `herve`.

C'est en fait un fichier bash qui appelle le programme java lui-même.

Celui-ci possède obligatoirement les sous-commandes suivantes :

- `herve ask` : affichage dans le terminal des résultats d'une requête ponctuelle.
- `herve run` : lancement du mode interactif dans le terminal. Le mode interactif se quitte en tapant `exit`.
- `herve web` : lance un serveur permettant d'effectuer des recherches via une page web interactive.

Si vous vous faites le choix d'implémenter également la phase d'indexation, il possèdera alors la commande supplémentaire suivante :

- `herve index` : génère les fichiers d'index, typiquement à partir d'un site web.

Ces commandes sont détaillées ci-dessous.

## Requête ponctuelle

La sous-commande `ask` affiche les résultats d'une recherche ponctuelle dans le terminal.

### Syntaxe

Sa syntaxe est la suivante :

```
herve ask [--index <index-directory>] [--max <max-results>] [--seuil <minimal-result-score>] <mots clés recherchés>
```

Cette commande génère une liste de résultats, du plus pertinent au moins pertinent.

### Paramètres

Voici le détail des paramètres de la commande `herve ask` :

- `--index <index-directory>` permet de préciser l'emplacement des fichiers d'index.

  Ici, `<index-directory>` est un dossier contenant l'ensemble des fichiers d'index `<NUM>.txt`.

  Par défaut, si ce paramètre n'est pas fourni, les fichiers d'index sont lus depuis le dossier `.config/herve/INDEX` du dossier personnel de l'utilisateur.

  Exemple : `/home/nicolas/.config/herve` si l'utilisateur s'appelle `nicolas`.
  Si le dossier n'existe pas ou est invalide, un message d'erreur précisant
  clairement la nature du problème sera affichée.

- `--max <max-results>` indique le nombre maximal de résultats renvoyé par la requête.

  Par défaut, tous les résultats sont renvoyés, à condition que leur score dépasse _strictement_ le seuil
  (déterminé par le paramètre `--seuil`).

- `--seuil <minimal-result-score>` indique le score à partir duquel les résultats n'apparaissent plus.

  Par défaut, le seuil est de `0`, ce qui signifie que tous les documents qui contiennent au moins un mot-clé de la requête sont renvoyés
  (sous réserve de ne pas dépasser le nombre maximal de résultats précisé par le paramètre `--max`).

### Exemple

Exemple de résultats, avec les fichiers d'index fournis par défaut :

```
$ herve ask --index doc/exemples-fichiers/INDEX/ --max 5 tomate oignon
18 résultats pour la requête "tomate oignon".
SearchResult [url=https://fr.vikidia.org/wiki/Ratatouille, score=28.284271247461902]
SearchResult [url=https://fr.vikidia.org/wiki/Ketchup, score=27.386127875258303]
SearchResult [url=https://fr.vikidia.org/wiki/Taboulé, score=24.753688574416856]
SearchResult [url=https://fr.vikidia.org/wiki/Baklava, score=17.556172079419582]
SearchResult [url=https://fr.vikidia.org/wiki/Mjadra, score=16.222142113076252]
13 résultats supplémentaires non affichés.
```

## Lancement du mode interactif dans le terminal

La commande `herve run` démarre le mode interactif, qui permet d'enchaîner plusieurs requêtes dans le terminal.

### Syntaxe

Sa syntaxe est la suivante :

```
herve run [--index <index-directory>] [--max <max-results>] [--seuil <minimal-result-score>]
```

### Paramètres

Voir la description des paramètres de la commande `ask` (ils sont identiques).

### Affichage

Voici un exemple de session interactive :

```
$ herve run --index doc/exemples-fichiers/INDEX/ --max 3
index path : /home/nicolas/DocumentsFamille/nicolas/Travail/enseignements/s2/SAE/25-26/prototype/herve/doc/exemples-fichiers/INDEX
Bienvenue sur Hervé, moteur de Recherche Vectoriel. Taper `exit` pour quitter.
Recherche :
tomate
11 résultats pour la requête "tomate".
SearchResult [url=https://fr.vikidia.org/wiki/Ketchup, score=38.72983346207417]
SearchResult [url=https://fr.vikidia.org/wiki/Taboulé, score=21.004201260420146]
SearchResult [url=https://fr.vikidia.org/wiki/Ratatouille, score=20.0]
8 résultats supplémentaires non affichés.
Recherche :
aubergine poivron
4 résultats pour la requête "aubergine poivron".
SearchResult [url=https://fr.vikidia.org/wiki/Ratatouille, score=20.0]
SearchResult [url=https://fr.vikidia.org/wiki/Moussaka, score=16.222142113076256]
SearchResult [url=https://fr.vikidia.org/wiki/Sauce_wiimam, score=14.586499149789455]
1 résultats supplémentaires non affichés.
Recherche :
exit
À bientôt !
```

## Interface web

La sous-commande `herve web` permet de lancer un serveur permettant d'effectuer des recherches sur une page web.

### Syntaxe

Sa syntaxe est la suivante :

```
herve web [--index <index-directory>] [--max <max-results>] [--seuil <minimal-result-score>] [--port <port>]
```

### Paramètres

Concernant les options `--index`, `--max` et `--seuil`, se rapporter à la documentation de la commande `herve ask` ci-dessus.
Leur fonctionnement est identique pour `herve web`.

Le port peut être spécifié via l'option `--port`. Par défaut, il vaut `2026`. L'adresse IP est toujours `127.0.0.1` (localhost).

La page web située à l'adresse `127.0.0.1:<port>` contient un formulaire permettant d'effectuer une recherche, en choisissant notamment le nombre de résultats affichés.
En cliquant sur les résultats, on peut accéder aux pages web correspondantes.
D'autres paramètres peuvent être ajoutés pour plus de praticité.

Le site web affiché est fluide et léger, réactif (c'est-à-dire adaptée à un smartphone comme à un écran 34"), ergonomique et agréable. Il n'utilise que HTML + CSS (pas de JS, pas de framework...)

## Configuration

Des fichiers de configuration peuvent être utilisés pour mémoriser des paramètres d'une fois sur l'autre.
Si c'est le cas, ils doivent être enregistrés dans le dossier `~/.config/herve`.

Attention, ces fichiers de configuration doivent être optionnels (l'application doit pouvoir se lancer sans).
