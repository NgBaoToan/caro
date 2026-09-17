package caroai;

/**
 * The three strengths offered on the title screen.
 *
 * Each level is a search budget, not a different algorithm: the same evaluator
 * and the same search run at every level, with more depth, a wider shortlist
 * and more time as the level rises. That keeps the AI's style consistent and
 * means an improvement to {@link Evaluator} lifts all three at once.
 *
 * {@code minThinkMillis} is purely cosmetic: without it, EASY answers before
 * the player's hand has left the mouse, which reads as a bug rather than as a
 * fast opponent.
 */
public enum Difficulty {

    EASY  ("Easy",   2,  8,   250,  220),
    MEDIUM("Medium", 4, 12,   700,  120),
    HARD  ("Hard",   6, 16,  2000,    0);

    private final String label;
    private final int    maxDepth;
    private final int    branchLimit;
    private final long   budgetMillis;
    private final long   minThinkMillis;

    Difficulty(String label, int maxDepth, int branchLimit,
               long budgetMillis, long minThinkMillis) {
        this.label          = label;
        this.maxDepth       = maxDepth;
        this.branchLimit    = branchLimit;
        this.budgetMillis   = budgetMillis;
        this.minThinkMillis = minThinkMillis;
    }

    public String label()          { return label; }
    /** Deepest ply iterative deepening will attempt. */
    public int    maxDepth()       { return maxDepth; }
    /** How many candidate moves survive the shortlist at each node. */
    public int    branchLimit()    { return branchLimit; }
    /** Wall-clock ceiling for one move. */
    public long   budgetMillis()   { return budgetMillis; }
    /** Floor on how quickly a move may be played back, for feel. */
    public long   minThinkMillis() { return minThinkMillis; }
}
