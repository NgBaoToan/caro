package caroai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The undo rule.
 *
 * How far back an undo goes is computed from the history, not assumed, so these
 * cases cover the ones that used to be hard-coded wrong: a human holding O, a
 * history with a single move in it, and a two-player game where every move
 * belongs to a person.
 */
class MoveHistoryTest {

    private MoveHistory history(int count) {
        MoveHistory h = new MoveHistory();
        for (int i = 0; i < count; i++) h.push(new Coord(i, 0));
        return h;
    }

    // ── Turn derivation ──────────────────────────────────────

    @Test
    @DisplayName("X opens, so an even number of moves means X is to play")
    void turnFollowsFromTheMoveCount() {
        assertThat(history(0).xToMove()).isTrue();
        assertThat(history(1).xToMove()).isFalse();
        assertThat(history(2).xToMove()).isTrue();
        assertThat(history(7).xToMove()).isFalse();
    }

    @Test
    @DisplayName("an empty history has no last move")
    void emptyHistoryHasNoLast() {
        MoveHistory h = new MoveHistory();
        assertThat(h.isEmpty()).isTrue();
        assertThat(h.last()).isNull();
        assertThat(h.lastHumanIndex(true, 0)).isEqualTo(-1);
    }

    // ── Finding the human's last move ────────────────────────

    @Test
    @DisplayName("holding X, the human owns the even-numbered moves")
    void humanHoldingXOwnsEvenMoves() {
        assertThat(history(4).lastHumanIndex(true, 0)).isEqualTo(2);
        assertThat(history(3).lastHumanIndex(true, 0)).isEqualTo(2);
        assertThat(history(1).lastHumanIndex(true, 0)).isZero();
    }

    @Test
    @DisplayName("holding O, the human owns the odd-numbered moves")
    void humanHoldingOOwnsOddMoves() {
        assertThat(history(4).lastHumanIndex(true, 1)).isEqualTo(3);
        assertThat(history(2).lastHumanIndex(true, 1)).isEqualTo(1);
    }

    @Test
    @DisplayName("holding O with only the AI's opening played, there is nothing to undo")
    void nothingToUndoAfterTheAIOpens() {
        assertThat(history(1).lastHumanIndex(true, 1)).isEqualTo(-1);
    }

    @Test
    @DisplayName("in a two-player game every move belongs to a person")
    void twoPlayerGameOwnsEveryMove() {
        assertThat(history(5).lastHumanIndex(false, 0)).isEqualTo(4);
        assertThat(history(1).lastHumanIndex(false, 0)).isZero();
    }

    // ── Rewinding ────────────────────────────────────────────

    @Test
    @DisplayName("an undo after one move empties the history and hands the turn back to X")
    void undoOfASingleMove() {
        MoveHistory h = history(1);

        List<Coord> removed = h.rewindTo(h.lastHumanIndex(true, 0));

        assertThat(removed).containsExactly(new Coord(0, 0));
        assertThat(h.isEmpty()).isTrue();
        assertThat(h.xToMove()).isTrue();
    }

    @Test
    @DisplayName("an undo takes back the human's move and every AI reply after it")
    void undoTakesBackTheAIRepliesToo() {
        MoveHistory h = history(4);   // human, AI, human, AI

        List<Coord> removed = h.rewindTo(h.lastHumanIndex(true, 0));

        // Most recent first: the AI's reply, then the human's move.
        assertThat(removed).containsExactly(new Coord(3, 0), new Coord(2, 0));
        assertThat(h.size()).isEqualTo(2);
        assertThat(h.last()).isEqualTo(new Coord(1, 0));
        assertThat(h.xToMove()).isTrue();
    }

    @Test
    @DisplayName("an undo while the AI is still to reply takes back one move only")
    void undoBeforeTheAIHasReplied() {
        MoveHistory h = history(3);   // human, AI, human

        List<Coord> removed = h.rewindTo(h.lastHumanIndex(true, 0));

        assertThat(removed).containsExactly(new Coord(2, 0));
        assertThat(h.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("holding O, an undo rewinds to just before the human's last move")
    void undoWhenTheHumanHoldsO() {
        MoveHistory h = history(4);   // AI, human, AI, human

        List<Coord> removed = h.rewindTo(h.lastHumanIndex(true, 1));

        assertThat(removed).containsExactly(new Coord(3, 0));
        assertThat(h.size()).isEqualTo(3);
        assertThat(h.xToMove()).isFalse();   // the human holds O, so it is their turn
    }

    @Test
    @DisplayName("in a two-player game an undo takes back exactly one move")
    void undoInTwoPlayerGame() {
        MoveHistory h = history(5);

        List<Coord> removed = h.rewindTo(h.lastHumanIndex(false, 0));

        assertThat(removed).hasSize(1);
        assertThat(h.size()).isEqualTo(4);
    }

    @Test
    @DisplayName("repeated undos walk the history back to empty without going negative")
    void repeatedUndosStopAtEmpty() {
        MoveHistory h = history(4);

        for (int i = 0; i < 10; i++) {
            int target = h.lastHumanIndex(true, 0);
            if (target < 0) break;
            h.rewindTo(target);
        }

        assertThat(h.isEmpty()).isTrue();
        assertThat(h.rewindTo(-5)).isEmpty();
        assertThat(h.size()).isZero();
    }

    @Test
    @DisplayName("the exposed list cannot be edited from outside")
    void movesViewIsReadOnly() {
        MoveHistory h = history(2);
        assertThat(h.moves()).hasSize(2);
        assertThatThrownBy(() -> h.moves().add(new Coord(9, 9)))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
