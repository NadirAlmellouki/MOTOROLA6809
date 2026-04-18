# Motorola 6809 Simulator (Java Swing)

Ce projet est un simulateur simple du processeur **Motorola 6809** avec une interface graphique Java Swing.

Il permet de:
- ecrire un petit programme assembleur 6809,
- compiler ce programme vers la ROM simulee,
- executer pas a pas (`Step`) ou en continu (`Run`),
- visualiser les registres, la RAM, la ROM et la pile.

## Structure du projet

- `src/Nadiros/Nadir.java`: interface graphique (fenetre principale, boutons, affichage).
- `src/Nadiros/CPU.java`: coeur du simulateur (assembleur minimal, memoire, execution CPU).

## Prerequis

- Java JDK 8+ (recommande: JDK 17 ou plus).
- Systeme Windows/Linux/macOS.

Verifier:

```bash
javac -version
java -version
```

## Compilation

Depuis la racine du projet:

```bash
javac -d out src/Nadiros/*.java
```

## Execution

```bash
java -cp out Nadiros.Nadir
```

## Utilisation de l'interface

- **Compile**: assemble le texte du panneau "Programme" et charge le binaire en ROM (`0x8000`).
- **Step**: execute une instruction.
- **Run**: execute en boucle (timer) jusqu'a `END` ou `Stop`.
- **Reset**: reinitialise CPU + memoire.

Le panneau "Programme" garde maintenant ton code assembleur intact et surligne seulement la ligne courante (sans ajouter de lignes/espaces repetes).

## Format attendu du programme assembleur

- Une instruction par ligne.
- Les lignes vides et les lignes commencant par `;` sont ignorees.
- Le programme doit se terminer par `END`.

Exemple:

```asm
LDD #$1234
STD >$1000
LDX #$2000
INCA
STA <$80
END
```

## Modes d'adressage pris en charge (selon les instructions implementees)

- Immediat: `#$nn` / `#$nnnn`
- Direct: `<$nn`
- Etendu: `>$nnnn`
- Indexe X: `$nn,X`
- Inherent: sans operande (ex: `CLRA`, `INCA`, `PULS`)

## Instructions deja implementees (extrait)

- Chargement/stockage: `LDA`, `LDB`, `LDX`, `LDD`, `LDY`, `LDS`, `LDU`, `STA`, `STB`, `STX`, `STD`, `STY`, `STU`
- Arithmetique/compare: `ADDA`, `ADDB`, `ADDD`, `CMPA`, `CMPB`
- Inherentes: `CLRA`, `CLRB`, `DECA`, `DECB`, `INCA`, `INCB`
- Pile: `PSHS`, `PSHU`, `PULS`, `PULU`
- Fin de programme: `END`

> Note: ce simulateur est volontairement pedagogique et ne couvre pas encore l'integralite du jeu d'instructions 6809.

## Depannage

- **Erreur "Veuillez d'abord compiler le programme"**  
  Clique d'abord sur `Compile`.

- **Erreur de compilation assembleur**  
  Verifie le format des operandes (`#$`, `<$`, `>$`, `,X`) et la presence de `END`.

- **Erreur d'ecriture en ROM**  
  La ROM est en `0x8000-0xFFFF` et est protegee en ecriture pendant l'execution.

## Ameliorations possibles

- Ajouter plus d'instructions 6809 (branches, logique, interruptions).
- Ajouter un desassembleur plus riche.
- Ajouter des tests unitaires JUnit pour l'assembleur et le coeur CPU.
- Ajouter le support labels/symboles complet dans l'assembleur.

