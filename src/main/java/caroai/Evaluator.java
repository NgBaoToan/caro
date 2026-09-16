package caroai;

import java.util.Map;

/**
 * Static evaluation: reads a position and returns a number.
 *
 * Everything the AI knows about "good shape" lives here. The search on top of
 * it only decides how far ahead to look, so improving play starts with this
 * file.
 *
 * Nothing here is Swing-aware and nothing mutates the caller's map beyond
 * putting a stone down and taking it straight back, so the whole class is
 * testable without a display.
 */
public final class Evaluator {

    private Evaluator() {}

    /** Directions to scan: horizontal, vertical, and both diagonals. */
    static final int[][] DIRECTIONS = { {1, 0}, {0, 1}, {1, 1}, {1, -1} };

    /**
     * How much an opponent shape counts compared with the same shape of our
     * own. Kept at or slightly above 1 so that, at equal run length, blocking
     * always beats building. Below 1 the AI races the opponent and loses by a
     * tempo — that was the old scoring bug.
     */
    public static final double DEFENCE_WEIGHT = 1.1;

    // ── Position scoring ─────────────────────────────────────

    /** Score of the whole position from {@code me}'s point of view. */
    public static int evaluate(Map<Coord, GameEngine.Cell> board, int size,
                               GameEngine.Cell me, int winLength) {
        GameEngine.Cell them = GameEngine.opponentOf(me);
        int mine  = rawScore(board, size, me,   winLength);
        int yours = rawScore(board, size, them, winLength);
        return mine - (int) (DEFENCE_WEIGHT * yours);
    }

    /** Sum of every shape {@code player} owns in the position. */
    public static int rawScore(Map<Coord, GameEngine.Cell> board, int size,
                               GameEngine.Cell player, int winLength) {
        int total = 0;
        for (Map.Entry<Coord, GameEngine.Cell> entry : board.entrySet()) {
            if (entry.getValue() != player) continue;
            Coord c = entry.getKey();

            for (int[] d : DIRECTIONS) {
                // Only score a run from its first stone, so each run counts once.
                if (board.get(c.offset(-d[0], -d[1])) == player) continue;
                total += runFrom(board, size, c, d, player, winLength).score();
            }
        }
        return total;
    }

    /**
     * Score of every shape that would pass through {@code c} if {@code player}
     * had a stone there. Used to order and shortlist candidate moves.
     *
     * The map must be mutable: the stone is placed and removed again.
     */
    public static int scoreAt(Map<Coord, GameEngine.Cell> board, int size,
                              Coord c, GameEngine.Cell player, int winLength) {
        if (board.containsKey(c)) return 0;

        board.put(c, player);
        int total = 0;
        for (int[] d : DIRECTIONS) {
            total += shapeThrough(board, size, c, d, player, winLength).score();
        }
        board.remove(c);
        return total;
    }

    /**
     * True when placing {@code player} at {@code c} completes a winning run.
     * The map must be mutable: the stone is placed and removed again.
     */
    public static boolean makesFive(Map<Coord, GameEngine.Cell> board, int size,
                                    Coord c, GameEngine.Cell player, int winLength) {
        if (board.containsKey(c)) return false;

        board.put(c, player);
        boolean win = false;
        for (int[] d : DIRECTIONS) {
            if (solidLength(board, c, d, player) >= winLength) { win = true; break; }
        }
        board.remove(c);
        return win;
    }

    // ── Shape recognition ────────────────────────────────────

    /**
     * Classifies the run that starts at {@code start} and goes along {@code d}.
     * The caller guarantees {@code start} is the first stone of the run.
     */
    static Pattern runFrom(Map<Coord, GameEngine.Cell> board, int size, Coord start,
                           int[] d, GameEngine.Cell player, int winLength) {

        int len = 1;
        Coord tail = start.offset(d[0], d[1]);
        while (board.get(tail) == player) {
            len++;
            tail = tail.offset(d[0], d[1]);
        }
        Coord head = start.offset(-d[0], -d[1]);

        boolean openHead = isFree(board, size, head);
        boolean openTail = isFree(board, size, tail);

        Pattern solid = classify(len, (openHead ? 1 : 0) + (openTail ? 1 : 0), winLength);

        // A gap of exactly one empty square, with more of our stones beyond it,
        // is still a threat: X X _ X wins the same square a solid three would.
        Pattern gapped = Pattern.NONE;
        if (openTail) {
            Coord beyond = tail.offset(d[0], d[1]);
            if (board.get(beyond) == player) {
                int second = 0;
                Coord n = beyond;
                // Keep the whole shape inside one winning span.
                while (board.get(n) == player && len + 1 + second < winLength) {
                    second++;
                    n = n.offset(d[0], d[1]);
                }
                gapped = classifyGapped(len + second, openHead, isFree(board, size, n), winLength);
            }
        }

        return Pattern.best(solid, gapped);
    }

    /**
     * Classifies the best shape passing through {@code c}, in direction
     * {@code d}, for a stone already placed at {@code c}.
     */
    static Pattern shapeThrough(Map<Coord, GameEngine.Cell> board, int size, Coord c,
                                int[] d, GameEngine.Cell player, int winLength) {
        // Walk back to the first stone of the run, then classify from there.
        Coord start = c;
        while (board.get(start.offset(-d[0], -d[1])) == player) {
            start = start.offset(-d[0], -d[1]);
        }
        Pattern forward = runFrom(board, size, start, d, player, winLength);

        // The gap may also lie behind us, so scan the mirrored direction too.
        int[] back = { -d[0], -d[1] };
        Coord startBack = c;
        while (board.get(startBack.offset(-back[0], -back[1])) == player) {
            startBack = startBack.offset(-back[0], -back[1]);
        }
        Pattern backward = runFrom(board, size, startBack, back, player, winLength);

        return Pattern.best(forward, backward);
    }

    /** Length of the unbroken run through {@code c} along {@code d}. */
    static int solidLength(Map<Coord, GameEngine.Cell> board, Coord c,
                           int[] d, GameEngine.Cell player) {
        int len = 1;
        Coord n = c.offset(d[0], d[1]);
        while (board.get(n) == player) { len++; n = n.offset(d[0], d[1]); }
        n = c.offset(-d[0], -d[1]);
        while (board.get(n) == player) { len++; n = n.offset(-d[0], -d[1]); }
        return len;
    }

    /** An unbroken run of {@code len} stones with {@code openEnds} free ends. */
    static Pattern classify(int len, int openEnds, int winLength) {
        if (len >= winLength) return Pattern.FIVE;
        if (openEnds == 0)    return Pattern.NONE;   // sealed at both ends, dead

        switch (len) {
            case 4:  return openEnds == 2 ? Pattern.OPEN_FOUR  : Pattern.FOUR;
            case 3:  return openEnds == 2 ? Pattern.OPEN_THREE : Pattern.CLOSED_THREE;
            case 2:  return openEnds == 2 ? Pattern.OPEN_TWO   : Pattern.CLOSED_TWO;
            default: return Pattern.NONE;            // a lone stone is worth nothing
        }
    }

    /**
     * A run split by one gap, holding {@code stones} stones in total.
     * A gapped shape spans one square more than a solid one, so it can never
     * hold a full five — the best it can be is a four threat.
     */
    static Pattern classifyGapped(int stones, boolean openHead, boolean openTail,
                                  int winLength) {
        if (stones >= winLength - 1) return Pattern.FOUR;   // filling the gap wins
        if (stones == winLength - 2) {
            return (openHead && openTail) ? Pattern.BROKEN_THREE : Pattern.CLOSED_THREE;
        }
        return Pattern.NONE;
    }

    /** True when the square is on the board and empty. */
    static boolean isFree(Map<Coord, GameEngine.Cell> board, int size, Coord c) {
        return c.isInside(size) && !board.containsKey(c);
    }
}
