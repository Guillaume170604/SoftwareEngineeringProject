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


## Auteur

**FAURE Guillaume**

## Licence

Projet Université Côte d'Azur - Software Engineering Project 2025

