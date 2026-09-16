package caroai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The list of stones played this game, in order.
 *
 * This used to be a bare {@code Stack<Coord>} inside {@link GameFrame}, which
 * meant the undo rule could only be exercised by opening a window. Pulled out
 * here it is plain data with no Swing in sight, so the rule can be tested
 * directly.
 *
 * X always opens, so everything else follows from the move count: whose turn it
 * is, and which entries belong to the human.
 */
public class MoveHistory {

    private final List<Coord> moves = new ArrayList<>();

    public void push(Coord c) {
        if (c != null) moves.add(c);
    }

    public int     size()    { return moves.size(); }
    public boolean isEmpty() { return moves.isEmpty(); }
    public void    clear()   { moves.clear(); }

    /** The most recent stone, or null when nothing has been played. */
    public Coord last() {
        return moves.isEmpty() ? null : moves.get(moves.size() - 1);
    }

    public List<Coord> moves() { return Collections.unmodifiableList(moves); }

    /** X always opens, so an even count means X is to play. */
    public boolean xToMove() { return moves.size() % 2 == 0; }

    /**
     * Index of the human's last stone, or -1 when they have not played yet.
     *
     * @param vsAI            false in two-player games, where every move is a
     *                        human move
     * @param firstHumanIndex 0 when the human holds X, 1 when they hold O
     */
    public int lastHumanIndex(boolean vsAI, int firstHumanIndex) {
        for (int i = moves.size() - 1; i >= 0; i--) {
            if (!vsAI || i % 2 == firstHumanIndex) return i;
        }
        return -1;
    }

    /**
     * Drops every move from the end until the history holds {@code target}
     * entries, and returns what was removed, most recent first.
     *
     * The number of stones taken back is therefore computed, not assumed. An
     * undo after a single opening move rewinds exactly that one move; an undo
     * that has to unwind several AI replies unwinds all of them.
     */
    public List<Coord> rewindTo(int target) {
        List<Coord> removed = new ArrayList<>();
        if (target < 0) target = 0;
        while (moves.size() > target) {
            removed.add(moves.remove(moves.size() - 1));
        }
        return removed;
    }
}
