package caroai;

/**
 * The shapes the evaluator recognises, with the score each is worth.
 *
 * The ladder is deliberately steep: every tier is worth more than any
 * realistic number of shapes from the tier below it, so a single four is
 * never outweighed by a pile of twos. That is what stops the AI from
 * wandering off to build small shapes while the opponent completes a real
 * threat.
 *
 *   OPEN   — both ends of the run are empty and on the board
 *   CLOSED — exactly one end is playable; the other is blocked or off-board
 *   BROKEN — one gap inside the run, e.g. X X _ X
 *
 * FOUR covers both the closed four and the broken four: each has exactly one
 * square that completes five, so both force an immediate reply.
 */
public enum Pattern {

    NONE         (          0),
    CLOSED_TWO   (        100),
    OPEN_TWO     (        500),
    CLOSED_THREE (      2_000),
    BROKEN_THREE (     12_000),
    OPEN_THREE   (     20_000),
    FOUR         (    200_000),
    OPEN_FOUR    (  1_000_000),
    FIVE         ( 10_000_000);

    private final int score;

    Pattern(int score) { this.score = score; }

    public int score() { return score; }

    /** The higher-scoring of two shapes. */
    public static Pattern best(Pattern a, Pattern b) {
        return a.score >= b.score ? a : b;
    }
}
