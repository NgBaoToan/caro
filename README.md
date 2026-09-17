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
- [Tests](#tests)
- [Architecture notes](#architecture-notes)
- [Technology & Features](#technology--features)
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

**Built with:** Java 21 · Swing · AWT · Maven · minimax with alpha-beta pruning · SwingWorker · CardLayout · JLayeredPane

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
| **Play vs computer** | Minimax opponent with three strengths; you may open (X) or move second (O) |
| **Difficulty** | Easy, Medium and Hard — depth, shortlist width and time budget |
| **Search off the event thread** | The window stays responsive while Hard thinks |
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
3. Choose a difficulty: **Easy**, **Medium** or **Hard** (Medium by default)
4. Press **START**

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
│   │   ├── AI.java                  # Minimax search: pruning, deepening, budgets
│   │   ├── Evaluator.java           # Static evaluation and pattern recognition
│   │   ├── Pattern.java             # The named shapes and their score ladder
│   │   ├── Difficulty.java          # Easy / Medium / Hard search budgets
│   │   ├── MoveHistory.java         # The move list and the undo rule
│   │   ├── Coord.java               # Board coordinate (record)
│   │   ├── Player.java              # One side: name, symbol, human or AI
│   │   ├── Move.java                # A move: coordinate + who played it
│   │   ├── Theme.java               # Design system: palette, fonts, stone drawing
│   │   ├── ClassicButton.java       # The one button style, three variants
│   │   ├── Board.java               # (Legacy) first JButton-grid experiment
│   │   └── Menu.java                # (Legacy) first JMenuBar experiment
│   └── test/java/caroai/            # Unit tests (JUnit 5 + AssertJ)
│       ├── GameEngineTest.java      # Win rule, board edges, bounds, undo
│       ├── EvaluatorTest.java       # One test per named pattern, plus the ladder
│       ├── AITest.java              # Forced wins, forced blocks, colour symmetry
│       └── MoveHistoryTest.java     # Undo across every side and history length
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

Three layers, each replaceable on its own: a set of named shapes, a function that scores a position from them, and a search that looks ahead.

### Shapes

`Pattern` names what the evaluator can see, with a deliberately steep score ladder:

| Shape | What it looks like | Score |
|---|---|---|
| Closed two | `O X X _` | 100 |
| Open two | `_ X X _` | 500 |
| Closed three | `O X X X _` | 2,000 |
| Broken three | `_ X X _ X _` | 12,000 |
| Open three | `_ X X X _` | 20,000 |
| Four | `O X X X X _` or `X X X _ X` | 200,000 |
| Open four | `_ X X X X _` | 1,000,000 |
| Five | `X X X X X` | 10,000,000 |

Each tier outranks any realistic number of shapes from the tier below, so a real four is never traded away for a handful of twos. The board edge counts as a block, exactly like an opponent stone.

`Four` covers the closed four and the broken four together, because each has exactly one square that completes five and each forces an immediate reply.

### Scoring a position

`Evaluator.evaluate()` sums our shapes, sums theirs, and returns the difference — with the opponent's total multiplied by `DEFENCE_WEIGHT`, which is **1.1**:

```
score = ourShapes - 1.1 x theirShapes
```

Keeping that multiplier at or just above 1 is what makes blocking beat building at equal run length. Below 1 the AI races the opponent and loses by a tempo, which was the old scoring bug. Our own five is worth ten million while their open four scaled is worth 1.1 million, so taking a win still beats blocking — the ladder keeps the priorities in order without special cases.

### Searching

On top of that sits minimax with alpha-beta pruning. Three things keep it usable on a 50x50 board:

- **Shortlisting** — only empty squares within two cells of a stone are candidates, and only the best few of those survive at each node. The rest of the board is irrelevant to the position.
- **Move ordering** — candidates are sorted by static score before being searched, so alpha-beta cuts off early and often. Most of the pruning comes from the ordering, not from the pruning rule itself.
- **Iterative deepening** — the search runs to depth 1, then 2, and so on until the time budget expires. The move from the last depth that *finished* is the one played, so the AI always has an answer ready and never plays a half-searched one.

Two tactical checks run before the search starts: take a win if one exists, otherwise block a loss if one is threatened. They are not an optimisation — they are the guarantee that no time budget, however small, can make the AI miss a win it could take now.

### Difficulty

Each level is a budget rather than a different algorithm, so an improvement to the evaluator lifts all three at once.

| Level | Max depth | Shortlist | Time budget |
|---|---|---|---|
| Easy | 2 | 8 | 250 ms |
| Medium | 4 | 12 | 700 ms |
| Hard | 7 | 16 | 1350 ms |

Easy also holds its answer back by about 200 ms. Without that it replies before your hand has left the mouse, which reads as a glitch rather than as a fast opponent.

### Off the event thread

The search runs in a `SwingWorker`. `BoardPanel` snapshots the position on the event thread, hands that copy to the worker, and applies the result when it returns — so the window never freezes while Hard thinks, and the search never reads a board the interface is changing underneath it. Undo, restart and returning to the menu all cancel the running search first.

### Opening move

An empty board has no candidate squares at all, so `findBestMove` returns `null`. `BoardPanel` handles that by playing near the centre of the **visible window** rather than the centre of the 50x50 board — a stone at (25,25) would be off-screen and look like a frozen game.

---

## Tests

```bash
mvn test
```

The suite is about rules and tactics rather than pixels; nothing in it needs a display.

| File | What it pins down |
|---|---|
| `GameEngineTest` | Five in a row in all four directions; wins hard against the board edges; negative and oversized coordinates refused without changing state; undo frees the square and cancels a win |
| `EvaluatorTest` | One test per named shape, so the ladder is fixed by name rather than by a number someone can quietly retune; defence weight at or above 1 |
| `AITest` | Always takes an immediate win; always blocks a forced loss; answers an open three and a broken three; **a position and its colour-swapped twin produce the same move** |
| `MoveHistoryTest` | Undo returns the board and the turn to the previous state — holding X, holding O, in a two-player game, and with a single move in the history |

The colour-symmetry test is the important one. The old AI kept its own copy of the board and encoded stones by symbol instead of by owner, so choosing O inverted its idea of who was who and it started helping its opponent. That bug cannot pass this test, and no amount of tuning elsewhere would have found it.

---

## Architecture notes

A few decisions that are worth knowing before changing the code.

**One board, no copies.** `GameEngine` owns the position as a `Map<Coord, Cell>` and hands out a read-only view. The AI reads that map directly on every call. It used to keep its own `int[][]` mirror that the UI updated by hand after each move; a single missed or mis-ordered update silently flipped the meaning of a cell, and the AI would start playing for the other side. There is now nothing to synchronise.

**Coordinates are a record.** `Coord(col, row)` replaces `java.awt.Point`. Point is mutable — a poor map key — and its `x`/`y` names never said which was the column. `Coord` is immutable, gets `equals`/`hashCode` from its components for free, and names its fields after what they are. There are no array indices left in the move path, so there is no out-of-bounds to hit.

**Bounds live in the engine.** `GameEngine.makeMove()` rejects a null, off-board or occupied coordinate and returns `false` without changing anything. It is the single gate every move passes through, so no caller can push a bad coordinate deeper in.

**Turn order is derived, not tracked.** X always opens, so whose turn it is follows from the number of moves played. `GameFrame` recomputes it from the history rather than assigning it case by case, which is also what makes undo work for any number of stones instead of a hard-coded two.

**Pending searches are cancellable.** The AI runs in a `SwingWorker`. Undo, restart and returning to the menu all cancel it first, so a move computed for the previous position can never land on the new one.

**Undo lives outside the window.** `MoveHistory` holds the move list and the undo rule as plain data, with no Swing in it, which is why the rule can be tested directly rather than by opening a window and clicking.

---

## Technology & Features

Everything the project is actually built from, and where to find it in the source.

### Language & platform

| Technology | Used for |
|---|---|
| **Java 21** | The whole codebase. `Coord` and `Move` are records; pattern matching on `switch` is used where it reads more clearly than a chain of `if` |
| **Java Swing** | Every window, panel and control — `JFrame`, `JPanel`, `JComponent`, `JLayeredPane`, `CardLayout` |
| **Java AWT / Graphics2D** | All drawing: the board, the stones, the buttons, the title screen background |
| **Maven** | Build, dependency management, testing, packaging — see [Running the game](#running-the-game) |
| **JUnit 5 + AssertJ** | The test suite — see [Tests](#tests) |
| **JaCoCo** | Test coverage reporting, wired into `mvn test` via `pom.xml` |

### Swing techniques in use

| Technique | Where | Why |
|---|---|---|
| **`CardLayout`** | `GameFrame` | Switches between the title screen and the game without separate windows |
| **`JLayeredPane` + `POPUP_LAYER`** | `GameFrame`, `WinOverlayPanel` | Floats the result card above the board and side rail without disturbing their layout |
| **Custom painting (`paintComponent` + `Graphics2D`)** | `BoardPanel`, `MenuPanel`, `StartMenuPanel`, `WinOverlayPanel`, `ClassicButton` | Every visual in the game is hand-drawn; nothing uses the default Swing look-and-feel |
| **`SwingWorker`** | `BoardPanel` | Runs the AI search off the event thread so the window never freezes while it thinks |
| **Swing `Timer`** | `MenuPanel.TurnCard` (pulse animation), `ClassicButton` (hover animation), `WinOverlayPanel` (entrance animation), `BoardPanel` (win-line blink) | Every animation in the interface is a repeating or one-shot `Timer`, not a separate animation library |
| **`KeyListener` / `MouseListener` / `MouseMotionListener`** | `BoardPanel` (WASD panning, click-to-place, hover preview), `StartMenuPanel`, `ClassicButton` (press/hover feedback) | All input handling |
| **`InputMap` / `ActionMap` key bindings** | `WinOverlayPanel` | `R` to play again, `Esc` to return to the menu, bound at the window level rather than to one component |
| **`RadialGradientPaint`** | `StartMenuPanel` | Darkens the title screen toward its edges |
| **Dynamic font fallback** | `Theme.pick()` | Probes `GraphicsEnvironment` for Georgia → Cambria → Noto Serif (and a sans equivalent) so the interface looks right on Windows, macOS and Linux without bundling font files |

### Game logic

| Technique | Where | Why |
|---|---|---|
| **Minimax with alpha-beta pruning** | `AI.minimax()` | The search algorithm — see [How the AI works](#how-the-ai-works) |
| **Iterative deepening** | `AI.findBestMove()` | Runs depth 1, 2, 3… under a time budget, so a move is always ready |
| **Move-ordering shortlist** | `AI.shortlist()` | Candidates are scored and sorted before being searched, which is where most of the pruning comes from |
| **Static position evaluation** | `Evaluator` | Named shapes with a fixed score ladder — see [How the AI works](#how-the-ai-works) |
| **Immutable coordinate record** | `Coord` | Replaces the mutable `java.awt.Point` that used to double as a `HashMap` key |
| **Single source of truth for board state** | `GameEngine` | The AI reads the engine's map directly instead of keeping its own mirror |
| **Derived turn order** | `MoveHistory.xToMove()` | Whose turn it is follows from the move count rather than being tracked separately |

### Testing

| Technology | Used for |
|---|---|
| **JUnit 5** (`@Test`, `@DisplayName`) | Test structure — every test names the rule it checks |
| **AssertJ** fluent assertions | Readable failures (`assertThat(...).isEqualTo(...)`, `.containsExactly(...)`, `.isIn(...)`) |
| **Deterministic AI testing** | Fixed positions and colour-swapped mirrors, rather than random play, so a failure always points at the same rule |

See [Tests](#tests) for what each test file actually checks.

---

## Contributing

Issues and pull requests are welcome. Run `mvn clean package` before opening a PR to confirm the build and tests still pass.

## License

MIT.


## AI optimization update

Zobrist transposition caching, incremental evaluation and bounded four-threat search are enabled. HARD attempts seven plies with a 1350 ms budget. See [benchmark report](bench/REPORT.md).
