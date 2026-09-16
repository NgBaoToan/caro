package caroai;

/**
 * One move: where the stone went, and whether the AI played it.
 */
public record Move(Coord coord, boolean byAI) {
}
