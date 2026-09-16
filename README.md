# CaroAI

<div align="center">

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Maven](https://img.shields.io/badge/Build-Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
![Swing](https://img.shields.io/badge/Java%20Swing-GUI-5C5CFF?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)
![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20macOS%20%7C%20Linux-lightgrey?style=for-the-badge)

**A desktop Caro (Gomoku) game written in Java Swing.**
Play against the computer or against a friend on one screen. The board pans freely and every pixel of the interface is hand-painted.

*by nbaotoan*

</div>

---

## Contents

- [About](#about)
- [Screenshots](#screenshots)
- [Features](#features)
- [Requirements](#requirements)
- [Installing the toolchain](#installing-the-toolchain)
  - [Windows](#windows)
  - [macOS](#macos)
  - [Linux](#linux)
- [Running the game](#running-the-game)
- [How to play](#how-to-play)
- [Project structure](#project-structure)
- [Design system](#design-system)
- [How the AI works](#how-the-ai-works)
- [Architecture notes](#architecture-notes)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

---

## About

Caro (Gomoku) is a two-player strategy game. Players take turns placing **X** and **O** stones on a square grid; the first to line up **five in a row** — horizontally, vertically or diagonally — wins.

CaroAI implements that on the desktop with Java Swing. The goal was not only a game that works but one that feels considered, so the interface is custom-painted rather than left to Swing's default look-and-feel:

- Every component is drawn by hand; no stock Swing look-and-feel
- A parchment board in a dark wooden frame, with go-style star points
- A dark control rail showing whose turn it is, live
- A quiet result card when a game ends — fade in, slight rise, no confetti
- A 50x50 board that pans under a smaller window, driven by WASD

**Built with:** Java 21 · Swing · AWT · Maven · a greedy heuristic AI · CardLayout · JLayeredPane

Maven drives the build (see [Installing the toolchain](#installing-the-toolchain)) — one `mvn clean package` compiles, tests and packages a runnable jar, with no manual IDE setup.

---

## Screenshots

> The images below are placeholders. Run the game, take screenshots and drop them into `docs/screenshots/` under the filenames used here.

| Title screen | In play | Result card |
|---|---|---|
| ![Title screen](docs/screenshots/start-menu.png) | ![In play](docs/screenshots/gameplay.png) | ![Result card](docs/screenshots/win-overlay.png) |
| Choose a mode (vs computer / two players) and a side | Panning board with a hover preview of the next stone | Win or loss, with Play Again and Main Menu |

To capture them: run the game, screenshot each state (`Win + Shift + S` on Windows, `Cmd + Shift + 4` on macOS, your desktop's screenshot tool on Linux), save into `docs/screenshots/` with the filenames above, then commit.

---

## Features

| Feature | What it does |
|---|---|
| **Title screen** | Pick a mode; when playing the computer, pick X or O |
| **Play vs computer** | A greedy heuristic opponent; you may open (X) or move second (O) |
| **Two players** | Hotseat on one machine, X and O alternating correctly |
| **Large board** | 50x50 grid; pan the camera with WASD |
| **Hover preview** | A ghost stone shows where the next move lands |
| **Last-move marker** | The most recent stone is tinted and outlined |
| **Win highlight** | The five winning stones flash, joined by a drawn line |
| **Result card** | Win or loss, with Play Again (`R`) and Main Menu (`Esc`) |
| **Undo** | Takes back your last move plus every reply the computer made after it |
| **Restart / Main menu** | Start over or return to the title screen at any time |

---

## Requirements

| Tool | Minimum | Why |
|---|---|---|
| **JDK** | 21 | Compiling and running (the code uses records) |
| **Maven** | 3.8+ | Build, dependencies, packaging |
| **Git** | any | Cloning the repository |

> Maven downloads JUnit 5 and AssertJ (test scope only) on the first build, so that build needs a network connection.

---

## Installing the toolchain

Check first — open a terminal and run:

```bash
java -version
mvn -version
```

If `java` reports 21 or newer and `mvn` runs, skip ahead to [Running the game](#running-the-game).

### Windows

Install JDK 21 with winget (bundled with Windows 10/11):

```powershell
winget install Microsoft.OpenJDK.21
```

For Maven, the most reliable route is the official zip plus a PATH entry:

1. Download the *Binary zip archive* from https://maven.apache.org/download.cgi
2. Unzip it, for example to `C:\Program Files\Apache\maven`
3. Set the environment variables (PowerShell as Administrator):
   ```powershell
   setx JAVA_HOME "C:\Program Files\Microsoft\jdk-21"
   setx MAVEN_HOME "C:\Program Files\Apache\maven"
   setx PATH "%PATH%;%MAVEN_HOME%\bin"
   ```
4. Open a new terminal and check: `mvn -version`

> With **Chocolatey** installed, `choco install maven` replaces the Maven steps.

### macOS

Using **Homebrew**:

```bash
brew install openjdk@21

sudo ln -sfn $(brew --prefix openjdk@21)/libexec/openjdk.jdk \
  /Library/Java/JavaVirtualMachines/openjdk-21.jdk

brew install maven

java -version
mvn -version
```

If you do not have Homebrew yet, install it from https://brew.sh

### Linux

**Ubuntu / Debian:**

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk maven
```

**Fedora:**

```bash
sudo dnf install -y java-21-openjdk-devel maven
```

**Arch Linux:**

```bash
sudo pacman -S jdk21-openjdk maven
```

> If your distribution has no JDK 21 package, [SDKMAN!](https://sdkman.io/) works everywhere:
> ```bash
> curl -s "https://get.sdkman.io" | bash
> source "$HOME/.sdkman/bin/sdkman-init.sh"
> sdk install java 21-open
> sdk install maven
> ```

---

## Running the game

### Clone

```bash
git clone https://github.com/ToanNB1204/CaroAI.git
cd CaroAI
```

### Option 1 — Maven (recommended)

```bash
mvn clean package
java -jar target/caroai.jar
```

`mvn clean package` compiles, runs the tests and produces a single runnable jar at `target/caroai.jar`. The same two commands work on Windows, macOS and Linux.

### Option 2 — javac, no Maven

```bash
javac -d out $(find src/main/java -name "*.java")
java -cp out caroai.Main
```

> **PowerShell (Windows):**
> ```powershell
> javac -d out (Get-ChildItem -Recurse src\main\java -Filter *.java | % { $_.FullName })
> java -cp out caroai.Main
> ```

---

## How to play

### Title screen

1. Choose a mode:
   - **Play vs Computer**
   - **Two Players** — hotseat on one machine
2. Against the computer, choose your side:
   - **X** — you open
   - **O** — the computer opens
3. Press **START**

The mode defaults to *Play vs Computer*, so pressing START straight away starts a valid game.

### In-game controls

| Action | Input |
|---|---|
| Place a stone | Left-click an empty square |
| Pan the board | `W` `A` `S` `D` |
| Undo | **Undo** in the side rail |
| Restart | **New Game** in the side rail |
| Back to the title screen | **Main Menu** in the side rail |
| Quit | **Exit** in the side rail |

> Click once on the board after entering a game so it takes keyboard focus; WASD does nothing until it does.

### Result card

| Action | Input |
|---|---|
| Play again | **PLAY AGAIN** or `R` |
| Main menu | **Main Menu** or `Esc` |

---

## Project structure

```
CaroAI/
├── pom.xml                          # Maven config (Java 21, JUnit 5, AssertJ, JaCoCo)
├── .gitignore
├── src/
│   ├── main/java/caroai/
│   │   ├── Main.java                # Entry point — opens GameFrame
│   │   ├── GameFrame.java           # Main window — CardLayout, move history, undo
│   │   ├── StartMenuPanel.java      # Title screen: mode and side selection
│   │   ├── MenuPanel.java           # In-game side rail: turn card, actions, credit
│   │   ├── BoardPanel.java          # Draws the board, handles mouse and keys
│   │   ├── WinOverlayPanel.java     # Result card over a dimmed backdrop
│   │   ├── GameEngine.java          # Rules: stones, bounds, turns, win detection
│   │   ├── AI.java                  # Greedy heuristic opponent
│   │   ├── Coord.java               # Board coordinate (record)
│   │   ├── Player.java              # One side: name, symbol, human or AI
│   │   ├── Move.java                # A move: coordinate + who played it
│   │   ├── Theme.java               # Design system: palette, fonts, stone drawing
│   │   ├── ClassicButton.java       # The one button style, three variants
│   │   ├── Board.java               # (Legacy) first JButton-grid experiment
│   │   └── Menu.java                # (Legacy) first JMenuBar experiment
│   └── test/java/caroai/            # Unit tests (JUnit 5 + AssertJ)
├── docs/screenshots/                # Screenshots for this README
└── README.md
```

---

## Design system

Everything visual is centralised in `Theme.java`: the palette, the font selection, and the routine that draws a stone. The board, the side rail and the result card all call the same `Theme.drawStone()`, so an X looks identical everywhere. Changing a constant in `Theme` re-tones the whole game.

- **Board** — warm parchment with a faint checker, a light grid, go-style star points every six cells, and a dark wooden frame
- **Stones** — X is black ink, O is vermilion; a classic pair that reads clearly on parchment
- **Chrome** — dark wood-toned surfaces with a single green accent reserved for the primary action, and a muted gold for rules and dividers
- **Type** — a serif display face for headings (Georgia → Cambria → Noto Serif, whichever the machine has), a sans face for everything else
- **Buttons** — `ClassicButton`, one style in three variants, each with a solid bottom lip that the face sinks into when pressed

---

## How the AI works

The AI is a **greedy heuristic** with no look-ahead. Each turn it scores every candidate square and plays the highest.

### Candidate squares

Only empty squares with **at least one stone within one cell** are considered. This drops isolated squares and keeps the search fast on a large board.

### Scoring

Each candidate is evaluated along **four axes**: horizontal, vertical, and both diagonals. The AI counts its own run and the opponent's run through that square:

| Run length | Attack (its own) | Defence (blocking) |
|---|---|---|
| 5 in a row (immediate win) | 100,000 | 90,000 |
| 4 in a row | 10,000 | 9,000 |
| 3 in a row | 1,000 | 900 |
| 2 in a row | 100 | 90 |

So it **always takes a win** when one exists (100,000), then **always blocks** an opponent about to win (90,000), and only then extends its own shapes.

### Opening move

On an empty board every candidate is isolated, so `getBestMove` returns `null`. `BoardPanel` handles that case by playing near the centre of the **visible window** rather than the centre of the 50x50 board — a stone at (25,25) would be off-screen and look like a frozen game.

---

## Architecture notes

A few decisions that are worth knowing before changing the code.

**One board, no copies.** `GameEngine` owns the position as a `Map<Coord, Cell>` and hands out a read-only view. The AI reads that map directly on every call. It used to keep its own `int[][]` mirror that the UI updated by hand after each move; a single missed or mis-ordered update silently flipped the meaning of a cell, and the AI would start playing for the other side. There is now nothing to synchronise.

**Coordinates are a record.** `Coord(col, row)` replaces `java.awt.Point`. Point is mutable — a poor map key — and its `x`/`y` names never said which was the column. `Coord` is immutable, gets `equals`/`hashCode` from its components for free, and names its fields after what they are. There are no array indices left in the move path, so there is no out-of-bounds to hit.

**Bounds live in the engine.** `GameEngine.makeMove()` rejects a null, off-board or occupied coordinate and returns `false` without changing anything. It is the single gate every move passes through, so no caller can push a bad coordinate deeper in.

**Turn order is derived, not tracked.** X always opens, so whose turn it is follows from the number of moves played. `GameFrame` recomputes it from the history rather than assigning it case by case, which is also what makes undo work for any number of stones instead of a hard-coded two.

**Pending AI moves are cancellable.** The AI plays on a one-shot `Timer`. Undo, restart and returning to the menu all cancel it first, so a move from the previous position can never land on the new one.

---

## Troubleshooting

**The window does not open**
- Check `java` and `mvn` are on PATH: `java -version`, `mvn -version`
- With Maven, make sure `mvn clean package` succeeded before `java -jar target/caroai.jar`

**`error: package caroai does not exist`**
- Compiling by hand, compile the whole tree at once as shown in [Option 2](#option-2--javac-no-maven)

**`javac: command not found`**
- You have a JRE, not a full JDK — reinstall per [Installing the toolchain](#installing-the-toolchain)

**`mvn: command not found`**
- Maven is not on PATH — recheck the install steps for your OS

**WASD does nothing**
- Click once on the board so it takes keyboard focus

**Blurry interface on a high-DPI display (Windows)**
- Add a JVM flag:
  ```bash
  java -Dsun.java2d.uiScale=1.0 -jar target/caroai.jar
  ```

---

## Contributing

Issues and pull requests are welcome. Run `mvn clean package` before opening a PR to confirm the build and tests still pass.

## License

MIT.
