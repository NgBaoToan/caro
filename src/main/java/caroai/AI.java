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
 * Search features that keep the search usable on a 50x50 board:
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
 *   Incremental score, neighbourhood and threat-window updates live in SearchState.
 *   Zobrist keys include the side to move; a bounded transposition table stores
 *   exact/lower/upper bounds. At the horizon a maximum of six forcing plies
 *   resolves fours and mandatory replies without extending ordinary threes.
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
    private int completedDepth;
    int completedScore;
    boolean useTranspositions = true; // package scope for exact cached/uncached regression checks
    private long ttHits, ttCutoffs, threatNodes;
    private TranspositionTable table;
    private int branchLimit;
    private static final int THREAT_PLIES = 6;

    public record SearchStats(long nodes, int completedDepth, long ttHits,
                              long ttCutoffs, long threatNodes) {}
    public SearchStats searchStats() {
        return new SearchStats(nodes, completedDepth, ttHits, ttCutoffs, threatNodes);
    }

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
        completedDepth = 0;
        completedScore = 0;
        ttHits = ttCutoffs = threatNodes = 0;
        Difficulty level = difficulty;
        branchLimit = level.branchLimit();
        deadlineNanos = System.nanoTime() + level.budgetMillis() * 1_000_000L;
        table = new TranspositionTable(17);

        if (snapshot == null || snapshot.isEmpty() || me == null) return null;

        int limit = Math.min(size, boardSize);
        if (limit <= 0) return null;
        Map<Coord, GameEngine.Cell> board = new HashMap<>(snapshot);
        GameEngine.Cell them = GameEngine.opponentOf(me);

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
        SearchState state = new SearchState(board, limit, winLength, me);

        try {
            for (int depth = 1; depth <= level.maxDepth(); depth++) {
                Coord bestAtDepth  = null;
                int   bestScore    = -INFINITY;
                int   alpha        = -INFINITY;

                for (Coord c : roots) {
                    checkTime();
                    int score = scoreRootMove(state, c, depth, alpha);
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
                completedDepth = depth;
                completedScore = bestScore;
                roots.remove(best);
                roots.add(0, best);

                // A forced win is found; searching deeper cannot improve on it.
                if (bestScore >= WIN_SCORE - 1000) break;
            }
        } catch (SearchStopped stopped) {
            // Out of time or cancelled — keep the last completed depth.
        }

        return best;
    }

    // ── Search ───────────────────────────────────────────────

    private int scoreRootMove(SearchState state, Coord c, int depth, int alpha) {
        state.place(c, state.me);
        try {
            return winsAt(state.board, c, state.me) ? WIN_SCORE
                    : minimax(state, depth - 1, 1, alpha, INFINITY, false);
        } finally { state.undo(); }
    }

    private int minimax(SearchState state, int depth, int ply,
                        int alpha, int beta, boolean maximizing) {
        nodes++;
        checkTime();
        int originalAlpha = alpha, originalBeta = beta;
        long key = state.key(maximizing);
        TranspositionTable.Entry cached = useTranspositions ? table.get(key) : null;
        if (cached != null) {
            ttHits++;
            if (cached.depth() >= depth) {
                int score = fromTable(cached.score(), ply);
                if (cached.bound() == TranspositionTable.Bound.EXACT) { ttCutoffs++; return score; }
                if (cached.bound() == TranspositionTable.Bound.LOWER) alpha = Math.max(alpha, score);
                else beta = Math.min(beta, score);
                if (alpha >= beta) { ttCutoffs++; return score; }
            }
        }
        if (depth <= 0) {
            int score = threats(state, THREAT_PLIES, ply, alpha, beta, maximizing);
            TranspositionTable.Bound bound = score <= originalAlpha ? TranspositionTable.Bound.UPPER
                    : score >= originalBeta ? TranspositionTable.Bound.LOWER : TranspositionTable.Bound.EXACT;
            if (useTranspositions) table.put(key, 0, toTable(score, ply), bound, null);
            return score;
        }
        List<SearchState.Candidate> moves = ordered(state, maximizing, false);
        if (moves.isEmpty()) return state.score();
        if (cached != null && cached.move() != null) {
            for (int i = 0; i < moves.size(); i++) if (moves.get(i).move().equals(cached.move())) {
                moves.add(0, moves.remove(i)); break;
            }
        }
        int best = maximizing ? -INFINITY : INFINITY;
        Coord bestMove = null;
        for (SearchState.Candidate candidate : moves) {
            checkTime();
            Coord c = candidate.move();
            GameEngine.Cell mover = maximizing ? state.me : state.them;
            state.place(c, mover);
            int score;
            try {
                score = winsAt(state.board, c, mover)
                        ? (maximizing ? WIN_SCORE - ply : -WIN_SCORE + ply)
                        : minimax(state, depth - 1, ply + 1, alpha, beta, !maximizing);
            } finally { state.undo(); }
            if (bestMove == null || (maximizing ? score > best : score < best)) { best = score; bestMove = c; }
            if (maximizing) alpha = Math.max(alpha, best); else beta = Math.min(beta, best);
            if (alpha >= beta) break;
        }
        TranspositionTable.Bound bound = best <= originalAlpha ? TranspositionTable.Bound.UPPER
                : best >= originalBeta ? TranspositionTable.Bound.LOWER : TranspositionTable.Bound.EXACT;
        if (useTranspositions) table.put(key, depth, toTable(best, ply), bound, bestMove);
        return best;
    }

    /** Bounded four-threat quiescence: wins, mandatory blocks, or moves creating a four.
     * Stand-pat is allowed only when the opponent has no immediate winning square.
     * Quiet full-width search still happens above the horizon; this is not a complete
     * all-threat proof solver (open-three combinations are deliberately excluded).
     */
    private int threats(SearchState state, int remaining, int ply,
                        int alpha, int beta, boolean maximizing) {
        checkTime();
        threatNodes++;
        if (remaining == 0) return state.score();
        List<SearchState.Candidate> moves = ordered(state, maximizing, true);
        if (moves.isEmpty()) return state.score();
        boolean mustReply = moves.stream().anyMatch(c ->
                (maximizing ? c.defence() : c.attack()) >= Pattern.FIVE.score());
        int best = mustReply ? (maximizing ? -INFINITY : INFINITY) : state.score();
        if (!mustReply) {
            if (maximizing) alpha = Math.max(alpha, best); else beta = Math.min(beta, best);
            if (alpha >= beta) return best;
        }
        for (SearchState.Candidate candidate : moves) {
            checkTime();
            Coord c = candidate.move();
            GameEngine.Cell mover = maximizing ? state.me : state.them;
            state.place(c, mover);
            int score;
            try {
                score = winsAt(state.board, c, mover)
                        ? (maximizing ? WIN_SCORE - ply : -WIN_SCORE + ply)
                        : threats(state, remaining - 1, ply + 1, alpha, beta, !maximizing);
            } finally { state.undo(); }
            best = maximizing ? Math.max(best, score) : Math.min(best, score);
            if (maximizing) alpha = Math.max(alpha, best); else beta = Math.min(beta, best);
            if (alpha >= beta) break;
        }
        return best;
    }

    private List<SearchState.Candidate> ordered(SearchState state, boolean maximizing, boolean forcingOnly) {
        List<SearchState.Candidate> all = state.candidates(this::checkTime, forcingOnly && winLength == 5);
        boolean hasWin = all.stream().anyMatch(c -> (maximizing ? c.attack() : c.defence()) >= Pattern.FIVE.score());
        boolean hasBlock = all.stream().anyMatch(c -> (maximizing ? c.defence() : c.attack()) >= Pattern.FIVE.score());
        all.removeIf(c -> {
            int own = maximizing ? c.attack() : c.defence();
            int enemy = maximizing ? c.defence() : c.attack();
            if (hasWin) return own < Pattern.FIVE.score();
            if (hasBlock) return enemy < Pattern.FIVE.score();
            return forcingOnly && own < Pattern.FOUR.score();
        });
        all.sort(Comparator.comparingInt((SearchState.Candidate c) -> -c.rank(maximizing))
                .thenComparingInt(c -> c.move().col()).thenComparingInt(c -> c.move().row()));
        // Never discard a mandatory reply or a forcing move just because of the beam width.
        return !forcingOnly && !hasWin && !hasBlock && all.size() > branchLimit
                ? new ArrayList<>(all.subList(0, branchLimit)) : all;
    }

    private static int toTable(int score, int ply) {
        return score >= WIN_SCORE - 1000 ? score + ply : score <= -WIN_SCORE + 1000 ? score - ply : score;
    }
    private static int fromTable(int score, int ply) {
        return score >= WIN_SCORE - 1000 ? score - ply : score <= -WIN_SCORE + 1000 ? score + ply : score;
    }
    private void checkTime() {
        if (cancelled || Thread.currentThread().isInterrupted() || System.nanoTime() >= deadlineNanos) throw STOP;
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
