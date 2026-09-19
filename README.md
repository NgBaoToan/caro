# CaroAI

**Language:** **English** · [Tiếng Việt](README.vi.md)

A desktop Gomoku game built with Java 21 and Swing. Play locally against another person or against an AI with three difficulty levels. The interface is custom drawn and supports Light and Dark themes.

## At a glance

| Area | What is implemented |
|---|---|
| Gameplay | 50 × 50 board, five in a row, local two player mode, undo, restart and win highlighting |
| AI | Minimax with alpha-beta pruning, iterative deepening, tactical win/block checks and bounded threat search |
| Search performance | Move ordering, nearby candidate selection, incremental evaluation and a Zobrist transposition table |
| Interface | Custom Swing/Graphics2D components, board panning, hover preview, result overlay and saved theme preference |
| Quality | JUnit 5 and AssertJ tests, JaCoCo configuration and reproducible benchmark tools |

The default AI level is **Medium**. **X always moves first**; when you choose O, the computer opens.

## Requirements

- JDK **21** or newer (`java -version` and `javac -version`)
- Maven **3.8+** for the standard build and test workflow
- A desktop environment for the Swing window

Maven downloads test dependencies on the first build. The game itself does not require a network connection.

## Run the game

From the repository root:

```bash
mvn clean package
java -jar target/caroai.jar
```

`mvn clean package` compiles the current sources, runs the tests and creates the runnable JAR. Rebuild after changing code; an older JAR may not contain your latest edits.

Without Maven, compile the application directly:

```bash
mkdir -p out
javac -d out src/main/java/caroai/*.java
java -cp out caroai.Main
```

On Windows PowerShell:

```powershell
New-Item -ItemType Directory -Force out | Out-Null
javac -d out (Get-ChildItem src/main/java/caroai -Filter *.java | ForEach-Object FullName)
java -cp out caroai.Main
```

The `javac` route starts the game but does not run the JUnit test suite.

## Play

1. Select **Play vs Computer** or **Two Players**.
2. Against the computer, choose X or O and select Easy, Medium or Hard.
3. Select **START**. Click an empty cell to place a stone.

| Action | Control |
|---|---|
| Pan the board | W, A, S, D after clicking the board |
| Undo | **Undo** in the left rail |
| Start a new game | **New Game** in the left rail |
| Return to title screen | **Main Menu** in the left rail |
| Change appearance | **Theme: DARK/LIGHT** on the title screen or in the left rail |
| Play again after a result | **PLAY AGAIN** or R |
| Return to the menu after a result | **Main Menu** or Esc |

The theme picker shows Light and Dark previews. Switching themes keeps the current game and saves the choice for the next launch through Java Preferences.

## AI and response time

All three levels use the same search algorithm; depth, candidate count and time budget change by level.

| Level | Maximum depth | Candidate limit | Search budget | Minimum response time |
|---|---:|---:|---:|---:|
| Easy | 2 | 8 | 250 ms | 70 ms |
| Medium | 4 | 12 | 700 ms | 40 ms |
| Hard | 7 | 16 | 850 ms | None |

The time budget is a search deadline, not a promise that every move takes that long or that the maximum depth is always reached. The minimum response time is a short interface pause for fast searches. Hard can return the best move from its last **completed** depth when the budget expires. Searches run in a `SwingWorker` so the window remains responsive.

The AI first checks for an immediate win, then a mandatory block. It searches nearby empty cells, orders candidates by a pattern based score, and uses alpha-beta pruning. Zobrist hashing and a transposition table reuse positions reached by different move orders. Threat search at the horizon is bounded; it does not solve every open-three combination.

## Project structure

| Path | Purpose |
|---|---|
| `src/main/java/caroai/GameEngine.java` | Board state, legal moves and win detection |
| `src/main/java/caroai/AI.java` | Iterative deepening and alpha-beta search |
| `src/main/java/caroai/SearchState.java` | Incremental search state and candidate cache |
| `src/main/java/caroai/Theme.java` | Light/Dark palettes, fonts and stone drawing |
| `src/main/java/caroai/GameFrame.java` | Screen flow and game lifecycle |
| `src/test/java/caroai/` | Rules, AI and search regression tests |
| `bench/` | Benchmark programs and historical measurements |

`GameEngine` owns the board. The UI reads it directly; the AI receives a snapshot before searching on a background thread. Undo, restart and returning to the menu cancel pending AI work.

## Test and benchmark

```bash
mvn test
mvn clean package
```

The tests cover game rules, undo, tactical choices and search state consistency. Maven is configured to generate a JaCoCo report during the test phase. No coverage percentage is claimed here without a current report.

[The benchmark report](bench/REPORT.md) documents the earlier optimization study, including its machine, fixtures and limitations. **Those numbers were collected with a 1,350 ms Hard budget**; the current game uses 850 ms. Treat the report as historical evidence, not a current latency or playing strength claim.

## Current limits

- This is a local desktop game; there is no online multiplayer or save/load feature.
- The bounded threat search is not a complete Gomoku solver.
- Search deadlines can be exceeded by long GC or operating system pauses.
- Platform specific UI behavior and the current release package still need broader manual verification.

## Author

Created by **nbaotoan**. Contributions are welcome; run `mvn clean package` before submitting changes.
