package caroai;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TranspositionTableTest {
    @Test void collisionsNeverReturnAnUnrelatedPosition() {
        TranspositionTable t = new TranspositionTable(0);
        t.put(42, 3, 123, TranspositionTable.Bound.EXACT, new Coord(1,2));
        assertThat(t.get(43)).isNull();
        t.put(43, 4, -456, TranspositionTable.Bound.LOWER, new Coord(2,3));
        assertThat(t.get(42)).isNull();
        assertThat(t.get(43).score()).isEqualTo(-456);
    }
    @Test void preservesDepthFlagsAndPreferredMove() {
        TranspositionTable t = new TranspositionTable(4);
        for (var bound : TranspositionTable.Bound.values()) {
            t.put(42, 6, 77, bound, new Coord(1,2));
            t.put(42, 2, 99, TranspositionTable.Bound.EXACT, null);
            assertThat(t.get(42).depth()).isEqualTo(6);
            assertThat(t.get(42).bound()).isEqualTo(bound);
            assertThat(t.get(42).move()).isEqualTo(new Coord(1,2));
        }
    }
}
