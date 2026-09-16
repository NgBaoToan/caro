package caroai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pattern recognition and the score ladder.
 *
 * Each shape gets its own test so the ladder is pinned down by name rather than
 * by a number someone can quietly retune. If a change to {@link Evaluator}
 * makes an open three score like a closed one, exactly one test fails and it
 * says which shape moved.
 */
class EvaluatorTest {

    private static final int SIZE = 50;
    private static final int WIN  = 5;

    private static final GameEngine.Cell ME   = GameEngine.Cell.X;
    private static final GameEngine.Cell THEM = GameEngine.Cell.O;

    private Map<Coord, GameEngine.Cell> board() { return new HashMap<>(); }

    private void run(Map<Coord, GameEngine.Cell> b, GameEngine.Cell c,
                     int col, int row, int count) {
        for (int i = 0; i < count; i++) b.put(new Coord(col + i, row), c);
    }

    private int score(Map<Coord, GameEngine.Cell> b) {
        return Evaluator.rawScore(b, SIZE, ME, WIN);
    }

    // ── Twos ─────────────────────────────────────────────────

    @Test
    @DisplayName("two stones with both ends free score as an open two")
    void openTwo() {
        Map<Coord, GameEngine.Cell> b = board();
        run(b, ME, 10, 10, 2);
        assertThat(score(b)).isEqualTo(Pattern.OPEN_TWO.score());
    }

    @Test
    @DisplayName("two stones with one end blocked score as a closed two")
    void closedTwo() {
        Map<Coord, GameEngine.Cell> b = board();
        run(b, ME, 10, 10, 2);
        b.put(new Coord(9, 10), THEM);
        assertThat(score(b)).isEqualTo(Pattern.CLOSED_TWO.score());
    }

    @Test
    @DisplayName("the board edge blocks a run exactly like an opponent stone")
    void edgeBlocksLikeAStone() {
        Map<Coord, GameEngine.Cell> b = board();
        run(b, ME, 0, 0, 2);   // nothing to the left of column 0
        assertThat(score(b)).isEqualTo(Pattern.CLOSED_TWO.score());
    }

    // ── Threes ───────────────────────────────────────────────

    @Test
    @DisplayName("three stones with both ends free score as an open three")
    void openThree() {
        Map<Coord, GameEngine.Cell> b = board();
        run(b, ME, 10, 10, 3);
        assertThat(score(b)).isEqualTo(Pattern.OPEN_THREE.score());
    }

    @Test
    @DisplayName("three stones with one end blocked score as a closed three")
    void closedThree() {
        Map<Coord, GameEngine.Cell> b = board();
        run(b, ME, 10, 10, 3);
        b.put(new Coord(9, 10), THEM);
        assertThat(score(b)).isEqualTo(Pattern.CLOSED_THREE.score());
    }

    @Test
    @DisplayName("a three sealed at both ends is worth nothing")
    void deadThreeIsWorthless() {
        Map<Coord, GameEngine.Cell> b = board();
        run(b, ME, 10, 10, 3);
        b.put(new Coord(9,  10), THEM);
        b.put(new Coord(13, 10), THEM);
        assertThat(score(b)).isZero();
    }

    @Test
    @DisplayName("X X _ X scores as a broken three")
    void brokenThree() {
        Map<Coord, GameEngine.Cell> b = board();
        run(b, ME, 10, 10, 2);
        b.put(new Coord(13, 10), ME);   // gap at (12,10)
        assertThat(score(b)).isEqualTo(Pattern.BROKEN_THREE.score());
    }

    // ── Fours ────────────────────────────────────────────────

    @Test
    @DisplayName("four stones with both ends free score as an open four")
    void openFour() {
        Map<Coord, GameEngine.Cell> b = board();
        run(b, ME, 10, 10, 4);
        assertThat(score(b)).isEqualTo(Pattern.OPEN_FOUR.score());
    }

    @Test
    @DisplayName("four stones with one end blocked score as a four")
    void closedFour() {
        Map<Coord, GameEngine.Cell> b = board();
        run(b, ME, 10, 10, 4);
        b.put(new Coord(9, 10), THEM);
        assertThat(score(b)).isEqualTo(Pattern.FOUR.score());
    }

    @Test
    @DisplayName("X X X _ X scores as a four, because one square completes five")
    void brokenFour() {
        Map<Coord, GameEngine.Cell> b = board();
        run(b, ME, 10, 10, 3);
        b.put(new Coord(14, 10), ME);   // gap at (13,10)
        assertThat(score(b)).isEqualTo(Pattern.FOUR.score());
    }

    // ── Five ─────────────────────────────────────────────────

    @Test
    @DisplayName("five in a row scores as a five even when both ends are blocked")
    void fiveOutranksEverything() {
        Map<Coord, GameEngine.Cell> b = board();
        run(b, ME, 10, 10, 5);
        b.put(new Coord(9,  10), THEM);
        b.put(new Coord(15, 10), THEM);
        assertThat(score(b)).isEqualTo(Pattern.FIVE.score());
    }

    @Test
    @DisplayName("shapes on the diagonals are recognised too")
    void diagonalsAreScored() {
        Map<Coord, GameEngine.Cell> b = board();
        for (int i = 0; i < 3; i++) b.put(new Coord(10 + i, 10 + i), ME);
        assertThat(score(b)).isEqualTo(Pattern.OPEN_THREE.score());

        Map<Coord, GameEngine.Cell> up = board();
        for (int i = 0; i < 3; i++) up.put(new Coord(10 + i, 20 - i), ME);
        assertThat(Evaluator.rawScore(up, SIZE, ME, WIN))
                .isEqualTo(Pattern.OPEN_THREE.score());
    }

    // ── The ladder itself ────────────────────────────────────

    @Test
    @DisplayName("every tier clearly outranks the one below it")
    void ladderIsStrictlyIncreasing() {
        assertThat(Pattern.CLOSED_TWO.score()).isLessThan(Pattern.OPEN_TWO.score());
        assertThat(Pattern.OPEN_TWO.score()).isLessThan(Pattern.CLOSED_THREE.score());
        assertThat(Pattern.CLOSED_THREE.score()).isLessThan(Pattern.BROKEN_THREE.score());
        assertThat(Pattern.BROKEN_THREE.score()).isLessThan(Pattern.OPEN_THREE.score());
        assertThat(Pattern.OPEN_THREE.score()).isLessThan(Pattern.FOUR.score());
        assertThat(Pattern.FOUR.score()).isLessThan(Pattern.OPEN_FOUR.score());
        assertThat(Pattern.OPEN_FOUR.score()).isLessThan(Pattern.FIVE.score());
    }

    @Test
    @DisplayName("one four outweighs any realistic pile of threes")
    void tiersDoNotAccumulateAcrossTheGap() {
        assertThat(Pattern.FOUR.score()).isGreaterThan(8 * Pattern.OPEN_THREE.score());
    }

    // ── Defence weight ───────────────────────────────────────

    @Test
    @DisplayName("defence counts at least as much as attack at the same run length")
    void defenceIsNotCheaperThanAttack() {
        assertThat(Evaluator.DEFENCE_WEIGHT).isGreaterThanOrEqualTo(1.0);
    }

    @Test
    @DisplayName("identical shapes on both sides do not score in our favour")
    void mirroredPositionIsNotFavourable() {
        Map<Coord, GameEngine.Cell> b = board();
        run(b, ME,   10, 10, 3);
        run(b, THEM, 10, 20, 3);
        assertThat(Evaluator.evaluate(b, SIZE, ME, WIN)).isLessThan(0);
    }

    @Test
    @DisplayName("an opponent three outweighs a two of our own")
    void opponentThreatDominatesOurSmallShape() {
        Map<Coord, GameEngine.Cell> b = board();
        run(b, ME,   10, 10, 2);
        run(b, THEM, 10, 20, 3);
        assertThat(Evaluator.evaluate(b, SIZE, ME, WIN)).isLessThan(0);
    }

    @Test
    @DisplayName("a winning square is spotted from an empty square")
    void makesFiveDetectsTheWinningSquare() {
        Map<Coord, GameEngine.Cell> b = board();
        run(b, ME, 10, 10, 4);

        assertThat(Evaluator.makesFive(b, SIZE, new Coord(14, 10), ME, WIN)).isTrue();
        assertThat(Evaluator.makesFive(b, SIZE, new Coord(9,  10), ME, WIN)).isTrue();
        assertThat(Evaluator.makesFive(b, SIZE, new Coord(20, 20), ME, WIN)).isFalse();

        // The probe must leave the board exactly as it found it.
        assertThat(b).hasSize(4);
    }
}
