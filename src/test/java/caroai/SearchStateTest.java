package caroai;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

class SearchStateTest {
    private void verify(SearchState state) {
        assertThat(state.score()).isEqualTo(Evaluator.evaluate(state.board, state.size, state.me, state.winLength));
        long expected = 0;
        for (var e : state.board.entrySet()) expected ^= SearchState.stoneKey(e.getKey(), e.getValue());
        assertThat(state.hash).isEqualTo(expected);
        List<SearchState.Candidate> all = state.candidates(() -> {});
        Set<Coord> expectedNeighbours = new HashSet<>();
        for (Coord c : state.board.keySet()) for (int x = -2; x <= 2; x++) for (int y = -2; y <= 2; y++) {
            Coord neighbour = c.offset(x,y);
            if (neighbour.isInside(state.size) && !state.board.containsKey(neighbour)) expectedNeighbours.add(neighbour);
        }
        assertThat(all.stream().map(SearchState.Candidate::move).toList()).containsExactlyInAnyOrderElementsOf(expectedNeighbours);
        Set<Coord> potential = new HashSet<>();
        state.candidates(() -> {}, true).forEach(c -> potential.add(c.move()));
        for (var c : all) {
            assertThat(c.attack()).isEqualTo(Evaluator.scoreAt(state.board, state.size, c.move(), state.me, state.winLength));
            assertThat(c.defence()).isEqualTo(Evaluator.scoreAt(state.board, state.size, c.move(), state.them, state.winLength));
            if (state.winLength == 5 && Math.max(c.attack(), c.defence()) >= Pattern.FOUR.score())
                assertThat(potential).contains(c.move());
        }
    }

    @Test void randomizedMakeUndoMatchesFullRecomputeAndCandidateCache() {
        Random random = new Random(73291);
        for (var side : List.of(GameEngine.Cell.X, GameEngine.Cell.O)) {
            SearchState s = new SearchState(Map.of(), 15, 5, side);
            int count = 0;
            for (int n = 0; n < 800; n++) {
                if (count > 0 && (count > 110 || random.nextInt(3) == 0)) { s.undo(); count--; }
                else {
                    Coord c;
                    do { c = new Coord(random.nextInt(15), random.nextInt(15)); } while (s.board.containsKey(c));
                    s.place(c, random.nextBoolean() ? GameEngine.Cell.X : GameEngine.Cell.O); count++;
                }
                verify(s);
            }
            while (count-- > 0) { s.undo(); verify(s); }
            assertThat(s.hash).isZero();
        }
    }

    @Test void longRunsEdgesAndGapsMatchFullScore() {
        for (int win : new int[]{3,5,7}) for (int hole = 0; hole < 18; hole++) {
            Map<Coord, GameEngine.Cell> board = new HashMap<>();
            for (int x = 0; x < 18; x++) if (x != hole) board.put(new Coord(x,0), GameEngine.Cell.X);
            SearchState s = new SearchState(board, 20, win, GameEngine.Cell.O);
            verify(s);
            s.place(new Coord(hole,0), GameEngine.Cell.X); verify(s);
            s.undo(); verify(s);
            s.place(new Coord(hole,0), GameEngine.Cell.O); verify(s);
            s.undo(); verify(s);
        }
    }

    @Test void hashDependsOnPositionColourAndSideNotMoveOrder() {
        SearchState a = new SearchState(Map.of(), 15, 5, GameEngine.Cell.X);
        SearchState b = new SearchState(Map.of(), 15, 5, GameEngine.Cell.X);
        Coord c = new Coord(4,5), d = new Coord(7,8);
        a.place(c, GameEngine.Cell.X); a.place(d, GameEngine.Cell.O);
        b.place(d, GameEngine.Cell.O); b.place(c, GameEngine.Cell.X);
        assertThat(a.hash).isEqualTo(b.hash);
        assertThat(a.key(true)).isNotEqualTo(a.key(false));
        assertThat(SearchState.stoneKey(c, GameEngine.Cell.X)).isNotEqualTo(SearchState.stoneKey(c, GameEngine.Cell.O));
    }
}
