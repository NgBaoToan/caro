package caroai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The rules layer: what counts as a win, what counts as a legal square, and
 * whether a move can be taken back cleanly.
 */
class GameEngineTest {

    private static final int SIZE = 50;

    private GameEngine engine() {
        return new GameEngine(new Player("You", "X", false),
                              new Player("Computer", "O", true),
                              SIZE);
    }

    private void place(GameEngine e, GameEngine.Cell c, int col, int row, int count,
                       int dCol, int dRow) {
        for (int i = 0; i < count; i++) {
            assertThat(e.makeMove(new Coord(col + i * dCol, row + i * dRow), c)).isTrue();
        }
    }

    // ── Win detection, all four directions ───────────────────

    @Test
    @DisplayName("five in a row wins horizontally")
    void winsHorizontally() {
        GameEngine e = engine();
        place(e, GameEngine.Cell.X, 10, 10, 5, 1, 0);
        assertThat(e.checkWin(new Coord(14, 10))).isTrue();
        assertThat(e.getWinningLine(new Coord(14, 10))).hasSize(5);
    }

    @Test
    @DisplayName("five in a row wins vertically")
    void winsVertically() {
        GameEngine e = engine();
        place(e, GameEngine.Cell.O, 10, 10, 5, 0, 1);
        assertThat(e.checkWin(new Coord(10, 14))).isTrue();
    }

    @Test
    @DisplayName("five in a row wins on the down-right diagonal")
    void winsDiagonally() {
        GameEngine e = engine();
        place(e, GameEngine.Cell.X, 10, 10, 5, 1, 1);
        assertThat(e.checkWin(new Coord(12, 12))).isTrue();   // detected from the middle too
    }

    @Test
    @DisplayName("five in a row wins on the up-right diagonal")
    void winsAntiDiagonally() {
        GameEngine e = engine();
        place(e, GameEngine.Cell.O, 10, 20, 5, 1, -1);
        assertThat(e.checkWin(new Coord(14, 16))).isTrue();
    }

    @Test
    @DisplayName("four in a row is not a win")
    void fourIsNotAWin() {
        GameEngine e = engine();
        place(e, GameEngine.Cell.X, 10, 10, 4, 1, 0);
        assertThat(e.checkWin(new Coord(13, 10))).isFalse();
        assertThat(e.getWinningLine(new Coord(13, 10))).isEmpty();
    }

    @Test
    @DisplayName("a run broken by the opponent is not a win")
    void brokenRunIsNotAWin() {
        GameEngine e = engine();
        place(e, GameEngine.Cell.X, 10, 10, 2, 1, 0);
        e.makeMove(new Coord(12, 10), GameEngine.Cell.O);
        place(e, GameEngine.Cell.X, 13, 10, 3, 1, 0);
        assertThat(e.checkWin(new Coord(15, 10))).isFalse();
    }

    // ── Edges: the scan must not run off the board ───────────

    @Test
    @DisplayName("a win against the origin corner is detected and scans no negative squares")
    void winsAtTheOriginCorner() {
        GameEngine e = engine();
        place(e, GameEngine.Cell.X, 0, 0, 5, 1, 0);
        assertThat(e.checkWin(new Coord(0, 0))).isTrue();

        List<Coord> line = e.getWinningLine(new Coord(0, 0));
        assertThat(line).hasSize(5);
        assertThat(line).allMatch(c -> c.isInside(SIZE));
    }

    @Test
    @DisplayName("a win against the far corner is detected")
    void winsAtTheFarCorner() {
        GameEngine e = engine();
        place(e, GameEngine.Cell.O, SIZE - 5, SIZE - 1, 5, 1, 0);
        assertThat(e.checkWin(new Coord(SIZE - 1, SIZE - 1))).isTrue();
    }

    @Test
    @DisplayName("a vertical win in the last column is detected")
    void winsInTheLastColumn() {
        GameEngine e = engine();
        place(e, GameEngine.Cell.X, SIZE - 1, 0, 5, 0, 1);
        assertThat(e.checkWin(new Coord(SIZE - 1, 4))).isTrue();
    }

    // ── Coordinate bounds ────────────────────────────────────

    @Test
    @DisplayName("negative coordinates are refused and change nothing")
    void refusesNegativeCoordinates() {
        GameEngine e = engine();
        assertThat(e.makeMove(new Coord(-1, 5),   GameEngine.Cell.X)).isFalse();
        assertThat(e.makeMove(new Coord(5, -1),   GameEngine.Cell.X)).isFalse();
        assertThat(e.makeMove(new Coord(-3, -7),  GameEngine.Cell.X)).isFalse();
        assertThat(e.getBoard()).isEmpty();
    }

    @Test
    @DisplayName("coordinates past the far edge are refused and change nothing")
    void refusesOversizedCoordinates() {
        GameEngine e = engine();
        assertThat(e.makeMove(new Coord(SIZE, 5),          GameEngine.Cell.X)).isFalse();
        assertThat(e.makeMove(new Coord(5, SIZE),          GameEngine.Cell.X)).isFalse();
        assertThat(e.makeMove(new Coord(9999, 9999),       GameEngine.Cell.X)).isFalse();
        assertThat(e.makeMove(new Coord(SIZE - 1, SIZE - 1), GameEngine.Cell.X)).isTrue();
        assertThat(e.getBoard()).hasSize(1);
    }

    @Test
    @DisplayName("a null coordinate is refused")
    void refusesNull() {
        assertThat(engine().makeMove(null, GameEngine.Cell.X)).isFalse();
    }

    @Test
    @DisplayName("an occupied square is refused and keeps its original stone")
    void refusesOccupiedSquare() {
        GameEngine e = engine();
        Coord c = new Coord(7, 7);
        assertThat(e.makeMove(c, GameEngine.Cell.X)).isTrue();
        assertThat(e.makeMove(c, GameEngine.Cell.O)).isFalse();
        assertThat(e.cellAt(c)).isEqualTo(GameEngine.Cell.X);
    }

    // ── Undo at the engine level ─────────────────────────────

    @Test
    @DisplayName("undo removes the stone and frees the square again")
    void undoRestoresTheSquare() {
        GameEngine e = engine();
        Coord c = new Coord(4, 9);

        e.makeMove(c, GameEngine.Cell.X);
        assertThat(e.undoMove(c)).isTrue();
        assertThat(e.getBoard()).isEmpty();
        assertThat(e.isOccupied(c)).isFalse();
        assertThat(e.makeMove(c, GameEngine.Cell.O)).isTrue();
    }

    @Test
    @DisplayName("undoing an empty square reports that nothing was removed")
    void undoOfEmptySquareIsNoop() {
        GameEngine e = engine();
        assertThat(e.undoMove(new Coord(1, 1))).isFalse();
        assertThat(e.undoMove(null)).isFalse();
    }

    @Test
    @DisplayName("undoing the fifth stone takes the win away again")
    void undoCancelsAWin() {
        GameEngine e = engine();
        place(e, GameEngine.Cell.X, 10, 10, 5, 1, 0);
        assertThat(e.checkWin(new Coord(14, 10))).isTrue();

        e.undoMove(new Coord(14, 10));
        assertThat(e.checkWin(new Coord(13, 10))).isFalse();
    }
}
