# The Island

Projet Java/Swing du jeu **The Island**.

## Description

Le jeu se joue à 4 joueurs.  
Le but est de sauver le plus d’explorateurs possible avant l’apparition du volcan.

## Fonctionnalités

- plateau hexagonal ;
- explorateurs ;
- bateaux ;
- nageurs ;
- tuiles terrain ;
- tuiles spéciales ;
- créatures marines ;
- dé de créature ;
- volcan ;
- scores finaux.

## Étapes du jeu

Au début de la partie, les joueurs placent leurs explorateurs et leurs bateaux.

Ensuite, chaque tour se déroule comme suit :

1. Le joueur peut jouer une tuile spéciale ou passer.
2. Le joueur effectue jusqu’à 3 déplacements.
3. Le joueur retire une tuile de terrain.
4. Le joueur lance le dé de créature.
5. Le joueur déplace la créature indiquée par le dé.
6. Le tour passe au joueur suivant.

La partie se termine quand le volcan apparaît.

## Règles principales

Les créatures ont des effets différents :

- le requin élimine les nageurs ;
- la baleine chavire les bateaux occupés ;
- le serpent élimine les nageurs et les bateaux occupés.

## Lancer le jeu

Ouvrir un terminal dans le dossier qui contient `pom.xml`.

Pour compiler le projet, utiliser la commande `mvn clean package`.

Après la compilation, lancer le jeu avec la commande `java -jar target/the-island-0.3.0-SNAPSHOT.jar`.

## Classe principale

```text
fr.iatic.theisland.App