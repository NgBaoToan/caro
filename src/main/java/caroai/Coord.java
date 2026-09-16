package caroai;

/**
 * A board coordinate.
 *
 * This replaces {@link java.awt.Point}, which was borrowed from the graphics
 * library and carried two problems:
 *
 *   1. Its {@code x}/{@code y} names do not say which one is the column and
 *      which one is the row, so call sites kept swapping them.
 *   2. It is mutable, yet it was used as a {@code HashMap} key. Mutating a key
 *      after insertion silently corrupts the map.
 *
 * A record is immutable and generates {@code equals}/{@code hashCode} from its
 * components, which is exactly what a map key needs. The field names
 * {@code col}/{@code row} make the order impossible to get wrong.
 */
public record Coord(int col, int row) {

    /** A coordinate offset by the given amount. */
    public Coord offset(int dCol, int dRow) {
        return new Coord(col + dCol, row + dRow);
    }

    /** True when this coordinate lies inside a square board of {@code size}. */
    public boolean isInside(int size) {
        return col >= 0 && row >= 0 && col < size && row < size;
    }

    @Override
    public String toString() {
        return "(" + col + "," + row + ")";
    }
}
