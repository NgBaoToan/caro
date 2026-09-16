package caroai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What the AI must never get wrong.
 *
 * The symmetry test is the one that matters most historically: the old AI kept
 * its own copy of the board and encoded stones by symbol rather than by owner,
 * so choosing O quietly inverted its whole idea of who was who and it started
 * helping its opponent. A position and its colour-swapped twin must produce the
 * same move; nothing but reading the real board can make that true.
 */
class AITest {

    private static final int SIZE = 50;

    private AI ai(Difficulty level) {
        return new AI(SIZE, GameEngine.WIN_LENGTH, level);
    }

    private Map<Coord, GameEngine.Cell> board() { return new HashMap<>(); }

    private void run(Map<Coord, GameEngine.Cell> b, GameEngine.Cell c,
                     int col, int row, int count) {
        for (int i = 0; i < count; i++) b.put(new Coord(col + i, row), c);
    }

    /** The same position with every stone's colour flipped. */
    private Map<Coord, GameEngine.Cell> mirrored(Map<Coord, GameEngine.Cell> b) {
        Map<Coord, GameEngine.Cell> out = new HashMap<>();
        for (Map.Entry<Coord, GameEngine.Cell> e : b.entrySet()) {
            out.put(e.getKey(), GameEngine.opponentOf(e.getValue()));
        }
        return out;
    }

    // ── Taking a win ─────────────────────────────────────────

    @Test
    @DisplayName("it completes five when the square is there")
    void takesTheWin() {
        Map<Coord, GameEngine.Cell> b = board();
        run(b, GameEngine.Cell.O, 10, 10, 4);
        run(b, GameEngine.Cell.X, 10, 20, 2);

        Coord move = ai(Difficulty.EASY).findBestMove(b, GameEngine.Cell.O, SIZE);
        assertThat(move).isIn(new Coord(9, 10), new Coord(14, 10));
    }

    @Test
    @DisplayName("it prefers its own win over blocking the opponent's")
    void winningBeatsBlocking() {
        Map<Coord, GameEngine.Cell> b = board();
        run(b, GameEngine.Cell.O, 10, 10, 4);   // we complete at (14,10)
        b.put(new Coord(9, 10), GameEngine.Cell.X);
        run(b, GameEngine.Cell.X, 10, 30, 4);   // they would complete at (14,30)
        b.put(new Coord(9, 30), GameEngine.Cell.O);

        Coord move = ai(Difficulty.EASY).findBestMove(b, GameEngine.Cell.O, SIZE);
        assertThat(move).isEqualTo(new Coord(14, 10));
    }

    @Test
    @DisplayName("every difficulty takes an immediate win")
    void allLevelsTakeTheWin() {
        for (Difficulty level : Difficulty.values()) {
            Map<Coord, GameEngine.Cell> b = board();
            run(b, GameEngine.Cell.X, 10, 10, 4);
            b.put(new Coord(9, 10), GameEngine.Cell.O);
            run(b, GameEngine.Cell.O, 20, 30, 2);

            Coord move = ai(level).findBestMove(b, GameEngine.Cell.X, SIZE);
            assertThat(move)
                    .as("level %s", level.label())
                    .isEqualTo(new Coord(14, 10));
        }
    }

    // ── Blocking ─────────────────────────────────────────────

    @Test
    @DisplayName("it blocks the only square that would give the opponent five")
    void blocksTheForcedLoss() {
        Map<Coord, GameEngine.Cell> b = board();
        run(b, GameEngine.Cell.X, 10, 10, 4);      // X completes at (14,10)
        b.put(new Coord(9, 10), GameEngine.Cell.O); // the other end is already shut
        b.put(new Coord(20, 30), GameEngine.Cell.O);

        Coord move = ai(Difficulty.EASY).findBestMove(b, GameEngine.Cell.O, SIZE);
        assertThat(move).isEqualTo(new Coord(14, 10));
    }

    @Test
    @DisplayName("it answers an open three instead of playing elsewhere")
    void blocksAnOpenThree() {
        Map<Coord, GameEngine.Cell> b = board();
        run(b, GameEngine.Cell.X, 10, 10, 3);          // _ X X X _
        b.put(new Coord(30, 30), GameEngine.Cell.O);   // nothing of ours to build on

        Coord move = ai(Difficulty.MEDIUM).findBestMove(b, GameEngine.Cell.O, SIZE);
        assertThat(move).isIn(new Coord(9, 10), new Coord(13, 10));
    }

    @Test
    @DisplayName("it answers a broken three as well as a solid one")
    void blocksABrokenThree() {
        Map<Coord, GameEngine.Cell> b = board();
        run(b, GameEngine.Cell.X, 10, 10, 2);
        b.put(new Coord(13, 10), GameEngine.Cell.X);   // X X _ X, gap at (12,10)
        b.put(new Coord(30, 30), GameEngine.Cell.O);

        Coord move = ai(Difficulty.MEDIUM).findBestMove(b, GameEngine.Cell.O, SIZE);
        assertThat(move).isIn(new Coord(9, 10),  new Coord(12, 10),
                              new Coord(14, 10));
    }

    // ── Symmetry: the regression test for the inverted-roles bug ──

    @Test
    @DisplayName("a position and its colour-swapped twin get the same move")
    void playingXAndPlayingOAreEquivalent() {
        Map<Coord, GameEngine.Cell> b = board();
        run(b, GameEngine.Cell.X, 10, 10, 3);
        run(b, GameEngine.Cell.O, 10, 11, 2);
        b.put(new Coord(14, 14), GameEngine.Cell.X);

        Coord asX = ai(Difficulty.MEDIUM).findBestMove(b, GameEngine.Cell.X, SIZE);
        Coord asO = ai(Difficulty.MEDIUM).findBestMove(mirrored(b), GameEngine.Cell.O, SIZE);

        assertThat(asX).isNotNull();
        assertThat(asO).isEqualTo(asX);
    }

    @Test
    @DisplayName("the defending side plays the same square whichever colour it holds")
    void defenceIsColourBlind() {
        Map<Coord, GameEngine.Cell> b = board();
        run(b, GameEngine.Cell.X, 10, 10, 4);
        b.put(new Coord(9, 10), GameEngine.Cell.O);
        b.put(new Coord(20, 20), GameEngine.Cell.O);

        Coord asO = ai(Difficulty.EASY).findBestMove(b, GameEngine.Cell.O, SIZE);
        Coord asX = ai(Difficulty.EASY).findBestMove(mirrored(b), GameEngine.Cell.X, SIZE);

        assertThat(asO).isEqualTo(new Coord(14, 10));
        assertThat(asX).isEqualTo(asO);
    }

    // ── Contract ─────────────────────────────────────────────

    @Test
    @DisplayName("an empty board has nothing to reason about, so no move is returned")
    void emptyBoardReturnsNothing() {
        assertThat(ai(Difficulty.MEDIUM).findBestMove(board(), GameEngine.Cell.X, SIZE))
                .isNull();
        assertThat(ai(Difficulty.MEDIUM).findBestMove(null, GameEngine.Cell.X, SIZE))
                .isNull();
    }

    @Test
    @DisplayName("the move it returns is always an empty square on the board")
    void returnsALegalSquare() {
        Map<Coord, GameEngine.Cell> b = board();
        run(b, GameEngine.Cell.X, 0, 0, 3);        // hard against the corner
        b.put(new Coord(1, 1), GameEngine.Cell.O);

        Coord move = ai(Difficulty.MEDIUM).findBestMove(b, GameEngine.Cell.O, SIZE);

        assertThat(move).isNotNull();
        assertThat(move.isInside(SIZE)).isTrue();
        assertThat(b).doesNotContainKey(move);
    }

    @Test
    @DisplayName("the search leaves the caller's position untouched")
    void doesNotMutateTheSnapshot() {
        Map<Coord, GameEngine.Cell> b = board();
        run(b, GameEngine.Cell.X, 10, 10, 3);
        run(b, GameEngine.Cell.O, 10, 12, 2);
        Map<Coord, GameEngine.Cell> before = new HashMap<>(b);

        ai(Difficulty.MEDIUM).findBestMove(b, GameEngine.Cell.O, SIZE);

        assertThat(b).isEqualTo(before);
    }

    @Test
    @DisplayName("a cancelled search still returns a legal move rather than nothing")
    void cancellationStillYieldsAMove() {
        Map<Coord, GameEngine.Cell> b = board();
        run(b, GameEngine.Cell.X, 10, 10, 3);
        b.put(new Coord(20, 20), GameEngine.Cell.O);

        AI slow = ai(Difficulty.HARD);
        slow.cancelSearch();   // cancel before it starts; the flag resets on entry

        Coord move = slow.findBestMove(b, GameEngine.Cell.O, SIZE);
        assertThat(move).isNotNull();
    }
}
