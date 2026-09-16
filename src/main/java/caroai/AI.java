package caroai;

import java.util.Map;

/**
 * Greedy heuristic opponent. No look-ahead: every empty cell next to an
 * existing stone is scored, and the highest score wins.
 *
 * The AI holds no board of its own. It reads {@link GameEngine} on every call,
 * so there is nothing to keep in sync and nothing that can drift. The earlier
 * version mirrored the board into an {@code int[][]} that the UI had to update
 * by hand after every move; a single missed or mis-ordered update flipped the
 * meaning of a cell and the AI started playing for the other side.
 */
public class AI {

    private final int size;
    private final int countToWin;

    public AI(int size, int countToWin) {
        this.size       = Math.max(1, size);
        this.countToWin = Math.max(2, countToWin);
    }

    public int getSize()       { return size; }
    public int getCountToWin() { return countToWin; }

    /**
     * Best move for the side the engine says the AI is playing, or null when
     * no candidate exists (empty board, or board full). The caller decides what
     * to do in that case — the AI does not invent a fallback square.
     */
    public Coord getBestMove(GameEngine engine) {
        if (engine == null) return null;

        Map<Coord, GameEngine.Cell> board = engine.getBoard();
        GameEngine.Cell mine  = engine.getAISymbol();
        GameEngine.Cell yours = GameEngine.opponentOf(mine);

        int limit = Math.min(size, engine.getSize());

        int   bestScore = -1;
        Coord best      = null;

        for (int col = 0; col < limit; col++) {
            for (int row = 0; row < limit; row++) {
                Coord c = new Coord(col, row);
                if (board.containsKey(c)) continue;

                int score = evaluate(board, c, mine, yours, limit);
                if (score > bestScore) {
                    bestScore = score;
                    best      = c;
                }
            }
        }

        // -1 means every candidate was isolated, i.e. the board is still empty.
        return bestScore < 0 ? null : best;
    }

    // ── Scoring ──────────────────────────────────────────────

    private static final int[][] DIRECTIONS = { {1, 0}, {0, 1}, {1, 1}, {1, -1} };

    private int evaluate(Map<Coord, GameEngine.Cell> board, Coord c,
                         GameEngine.Cell mine, GameEngine.Cell yours, int limit) {

        if (!hasNeighbour(board, c, limit)) return -1;

        int score = 0;

        for (int[] d : DIRECTIONS) {
            int attack = 1
                    + countRun(board, c,  d[0],  d[1], mine, limit)
                    + countRun(board, c, -d[0], -d[1], mine, limit);
            int defend = 1
                    + countRun(board, c,  d[0],  d[1], yours, limit)
                    + countRun(board, c, -d[0], -d[1], yours, limit);

            if (attack >= countToWin) return 100_000;   // win now
            if (attack == 4)      score += 10_000;
            else if (attack == 3) score += 1_000;
            else if (attack == 2) score += 100;

            if (defend >= countToWin) return 90_000;    // block an immediate loss
            if (defend == 4)      score += 9_000;
            else if (defend == 3) score += 900;
            else if (defend == 2) score += 90;
        }
        return score;
    }

    /** True when at least one of the eight surrounding cells holds a stone. */
    private boolean hasNeighbour(Map<Coord, GameEngine.Cell> board, Coord c, int limit) {
        for (int dCol = -1; dCol <= 1; dCol++) {
            for (int dRow = -1; dRow <= 1; dRow++) {
                if (dCol == 0 && dRow == 0) continue;
                Coord n = c.offset(dCol, dRow);
                if (!n.isInside(limit)) continue;
                if (board.containsKey(n)) return true;
            }
        }
        return false;
    }

    /** Consecutive stones of {@code want} starting one step away from {@code c}. */
    private int countRun(Map<Coord, GameEngine.Cell> board, Coord c,
                         int dCol, int dRow, GameEngine.Cell want, int limit) {
        int count = 0;
        Coord n = c.offset(dCol, dRow);
        while (n.isInside(limit) && board.get(n) == want) {
            count++;
            n = n.offset(dCol, dRow);
        }
        return count;
    }
}
