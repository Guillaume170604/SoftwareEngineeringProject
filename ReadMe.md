# Mode d'emploi du programme

## Prérequis
- Java 11 ou version supérieure (testé avec OpenJDK 25)
- Git (pour cloner le repository)

## Installation

```bash
git clone <url-du-repository>
cd <nom-du-dossier>
```

## Structure du projet

```
src/
├── compression/
│   ├── BitPacking.java                 # Interface principale
│   ├── ConsecutiveBitPacking.java      # Version avec chevauchement
│   ├── NonConsecutiveBitPacking.java   # Version sans chevauchement
│   └── OverflowBitPacking.java         # Version avec zone d'overflow
├── factory/
│   └── CompressionFactory.java         # Factory pour créer les compresseurs
└── Main.java                           # Programme de benchmark
```

## Compilation

```bash
javac -d bin src/**/*.java src/*.java
```

## Exécution

```bash
java -cp bin Main
```

## Personnalisation des tests

Le programme inclut 6 tests prédéfinis. Le **TEST 6** est entièrement personnalisable pour tester vos propres données.

### Modifier le TEST 6

Dans le fichier `src/Main.java`, localisez cette ligne (environ ligne 45) :

```java
test(new int[]{4, 8, 15, 16, 23, 42}, "Custom");
```

Remplacez le tableau par vos propres valeurs. Exemples :

**Tableau simple :**
```java
test(new int[]{10, 20, 30, 40, 50}, "Custom");
```

**Utiliser les fonctions de génération :**
```java
// 1000 éléments aléatoires entre 0 et 65535
test(generateArray(1000, 65535), "Custom");

// 200 éléments avec 10 outliers de valeur 100000
test(generateWithOutliers(200, 255, 10, 100000), "Custom");
```

### Fonctions utiles disponibles

- `generateArray(taille, valeurMax)` : génère un tableau aléatoire
- `generateWithOutliers(taille, valeurMax, nbOutliers, valeurOutlier)` : génère un tableau avec quelques valeurs extrêmes

Après modification, recompilez et relancez le programme pour voir les résultats avec vos données.


## Auteur

**FAURE Guillaume**

## Licence

Projet Université Côte d'Azur - Software Engineering Project 2025

