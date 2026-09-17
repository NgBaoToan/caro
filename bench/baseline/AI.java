package caroai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The computer opponent: minimax with alpha-beta pruning, driven by
 * {@link Evaluator} and bounded by {@link Difficulty}.
 *
 * Three things keep the search usable on a 50x50 board:
 *
 *   Shortlisting — only empty squares within two cells of a stone are
 *   considered, and only the best few of those survive at each node. Anywhere
 *   else on the board is irrelevant to the position.
 *
 *   Move ordering — candidates are sorted by static score before they are
 *   searched, so alpha-beta cuts off early and often. Ordering is where most
 *   of the pruning comes from.
 *
 *   Iterative deepening — the search runs to depth 1, then 2, and so on until
 *   the time budget runs out. The move from the last depth that finished is
 *   the one played, so the AI always has an answer ready and never returns a
 *   half-searched one.
 *
 * The class holds no board of its own. {@link #findBestMove} takes a snapshot,
 * copies it, and works on the copy, which is what makes it safe to call from a
 * background thread while the interface repaints.
 */
public final class AI {

    /** Bigger than any score {@link Evaluator} can produce. */
    static final int WIN_SCORE = 90_000_000;

    private static final int INFINITY  = Integer.MAX_VALUE / 2;
    private static final int NEIGHBOUR_RADIUS = 2;

    /** Thrown internally when the budget runs out or the caller cancels. */
    private static final class SearchStopped extends RuntimeException {
        SearchStopped() { super(null, null, false, false); }   // no stack trace
    }
    private static final SearchStopped STOP = new SearchStopped();

    private final int size;
    private final int winLength;

    private volatile Difficulty difficulty;
    private volatile boolean    cancelled = false;

    private long deadlineNanos;
    private long nodes;

    public AI(int size, int winLength) {
        this(size, winLength, Difficulty.MEDIUM);
    }

    public AI(int size, int winLength, Difficulty difficulty) {
        this.size       = Math.max(1, size);
        this.winLength  = Math.max(2, winLength);
        this.difficulty = difficulty == null ? Difficulty.MEDIUM : difficulty;
    }

    public int        getSize()       { return size; }
    public int        getWinLength()  { return winLength; }
    public Difficulty getDifficulty() { return difficulty; }

    public void setDifficulty(Difficulty d) {
        if (d != null) this.difficulty = d;
    }

    /** Asks a running search to give up. Safe to call from another thread. */
    public void cancelSearch() { cancelled = true; }

    // ── Entry points ─────────────────────────────────────────

    /** Convenience wrapper that snapshots the engine and searches. */
    public Coord getBestMove(GameEngine engine) {
        if (engine == null) return null;
        return findBestMove(engine.getBoard(), engine.getAISymbol(), engine.getSize());
    }

    /**
     * Best move for {@code me}, or null when there is nothing to reason about
     * (an empty or a full board). The caller decides what to do in that case —
     * the AI does not invent an opening square.
     */
    public Coord findBestMove(Map<Coord, GameEngine.Cell> snapshot,
                              GameEngine.Cell me, int boardSize) {

        cancelled = false;
        nodes     = 0;

        if (snapshot == null || snapshot.isEmpty() || me == null) return null;

        int limit = Math.min(size, boardSize);
        Map<Coord, GameEngine.Cell> board = new HashMap<>(snapshot);
        GameEngine.Cell them = GameEngine.opponentOf(me);

        Difficulty level = difficulty;

        // Tactics first. These two are not an optimisation — they are the
        // guarantee that the AI never misses a win it can take now, and never
        // ignores a loss it must stop now, whatever the search does afterwards.
        Coord winning = immediateWin(board, limit, me);
        if (winning != null) return winning;

        Coord blocking = immediateWin(board, limit, them);
        if (blocking != null) return blocking;

        List<Coord> roots = shortlist(board, limit, me, them, level.branchLimit());
        if (roots.isEmpty()) return null;

        Coord best = roots.get(0);
        deadlineNanos = System.nanoTime() + level.budgetMillis() * 1_000_000L;

        try {
            for (int depth = 1; depth <= level.maxDepth(); depth++) {
                Coord bestAtDepth  = null;
                int   bestScore    = -INFINITY;
                int   alpha        = -INFINITY;

                for (Coord c : roots) {
                    int score = scoreRootMove(board, c, depth, me, them, limit, alpha);
                    if (score > bestScore) {
                        bestScore   = score;
                        bestAtDepth = c;
                    }
                    if (score > alpha) alpha = score;
                }

                // Only commit a depth that finished. If the budget expires part
                // way through, SearchStopped skips this line and the previous
                // depth's move stands.
                if (bestAtDepth != null) best = bestAtDepth;

                // A forced win is found; searching deeper cannot improve on it.
                if (bestScore >= WIN_SCORE - 1000) break;
            }
        } catch (SearchStopped stopped) {
            // Out of time or cancelled — keep the last completed depth.
        }

        return best;
    }

    // ── Search ───────────────────────────────────────────────

    private int scoreRootMove(Map<Coord, GameEngine.Cell> board, Coord c, int depth,
                              GameEngine.Cell me, GameEngine.Cell them,
                              int limit, int alpha) {
        board.put(c, me);
        int score;
        if (winsAt(board, c, me)) {
            score = WIN_SCORE;
        } else {
            score = minimax(board, depth - 1, 1, alpha, INFINITY, false, me, them, limit);
        }
        board.remove(c);
        return score;
    }

    /**
     * Plain minimax with alpha-beta. Scores are always from {@code me}'s point
     * of view, so a single evaluator serves both sides.
     *
     * {@code ply} shifts the value of a forced win so the AI prefers winning in
     * two moves over winning in four, and losing later over losing sooner.
     */
    private int minimax(Map<Coord, GameEngine.Cell> board, int depth, int ply,
                        int alpha, int beta, boolean maximizing,
                        GameEngine.Cell me, GameEngine.Cell them, int limit) {

        checkBudget();

        if (depth <= 0) {
            return Evaluator.evaluate(board, limit, me, winLength);
        }

        GameEngine.Cell mover = maximizing ? me : them;
        GameEngine.Cell other = maximizing ? them : me;

        List<Coord> moves = shortlist(board, limit, mover, other,
                                      difficulty.branchLimit());
        if (moves.isEmpty()) {
            return Evaluator.evaluate(board, limit, me, winLength);
        }

        int best = maximizing ? -INFINITY : INFINITY;

        for (Coord c : moves) {
            board.put(c, mover);

            int score;
            if (winsAt(board, c, mover)) {
                score = maximizing ? (WIN_SCORE - ply) : -(WIN_SCORE - ply);
            } else {
                score = minimax(board, depth - 1, ply + 1, alpha, beta,
                                !maximizing, me, them, limit);
            }

            board.remove(c);

            if (maximizing) {
                if (score > best) best = score;
                if (best > alpha) alpha = best;
            } else {
                if (score < best) best = score;
                if (best < beta)  beta = best;
            }
            if (beta <= alpha) break;   // this branch cannot influence the result
        }
        return best;
    }

    /** Aborts the search once the budget is spent or a cancel has come in. */
    private void checkBudget() {
        if ((++nodes & 0x3FF) != 0) return;   // check every 1024 nodes
        if (cancelled || System.nanoTime() > deadlineNanos) throw STOP;
    }

    // ── Candidate moves ──────────────────────────────────────

    /**
     * Empty squares worth considering, best first, capped at {@code limit}.
     *
     * A square only qualifies if a stone sits within {@link #NEIGHBOUR_RADIUS}
     * of it. On an otherwise empty board that leaves nothing, which is why an
     * empty position returns no candidates at all.
     */
    List<Coord> shortlist(Map<Coord, GameEngine.Cell> board, int boardLimit,
                          GameEngine.Cell mover, GameEngine.Cell other, int limit) {

        List<Coord> raw = new ArrayList<>(neighbourhood(board, boardLimit));
        if (raw.isEmpty()) return raw;

        Map<Coord, Integer> ranking = new HashMap<>();
        for (Coord c : raw) {
            int attack = Evaluator.scoreAt(board, boardLimit, c, mover, winLength);
            int defend = Evaluator.scoreAt(board, boardLimit, c, other, winLength);
            ranking.put(c, attack + (int) (Evaluator.DEFENCE_WEIGHT * defend));
        }

        // Sort by score, then by coordinate, so the result never depends on
        // hash order — an unstable search makes tests unreproducible.
        raw.sort(Comparator
                .comparingInt((Coord c) -> -ranking.get(c))
                .thenComparingInt(Coord::col)
                .thenComparingInt(Coord::row));

        return raw.size() > limit ? new ArrayList<>(raw.subList(0, limit)) : raw;
    }

    /**
     * Every empty square within the neighbourhood radius of some stone.
     *
     * Deduplication goes through a hash set rather than a list scan — this runs
     * at every node of the search, and a linear scan here would dominate the
     * whole cost.
     */
    private List<Coord> neighbourhood(Map<Coord, GameEngine.Cell> board, int boardLimit) {
        Set<Coord> out = new LinkedHashSet<>();

        for (Coord stone : board.keySet()) {
            for (int dCol = -NEIGHBOUR_RADIUS; dCol <= NEIGHBOUR_RADIUS; dCol++) {
                for (int dRow = -NEIGHBOUR_RADIUS; dRow <= NEIGHBOUR_RADIUS; dRow++) {
                    if (dCol == 0 && dRow == 0) continue;
                    Coord c = stone.offset(dCol, dRow);
                    if (!c.isInside(boardLimit)) continue;
                    if (board.containsKey(c))    continue;
                    out.add(c);
                }
            }
        }
        return new ArrayList<>(out);
    }

    /** The square that completes five for {@code player}, or null. */
    Coord immediateWin(Map<Coord, GameEngine.Cell> board, int boardLimit,
                       GameEngine.Cell player) {
        List<Coord> best = null;
        for (Coord c : neighbourhood(board, boardLimit)) {
            if (Evaluator.makesFive(board, boardLimit, c, player, winLength)) {
                if (best == null) best = new ArrayList<>();
                best.add(c);
            }
        }
        if (best == null) return null;

        // Deterministic pick when several squares win.
        best.sort(Comparator.comparingInt(Coord::col).thenComparingInt(Coord::row));
        return best.get(0);
    }

    private boolean winsAt(Map<Coord, GameEngine.Cell> board, Coord c,
                           GameEngine.Cell player) {
        for (int[] d : Evaluator.DIRECTIONS) {
            if (Evaluator.solidLength(board, c, d, player) >= winLength) return true;
        }
        return false;
    }
}
