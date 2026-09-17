package caroai;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;

class SearchRegressionTest {
    private Map<Coord, GameEngine.Cell> fixture() {
        Map<Coord, GameEngine.Cell> b = new HashMap<>();
        int[][] cells = {{25,25},{26,25},{24,26},{26,26},{23,27},{25,27},{24,24},{27,24}};
        for (int n = 0; n < cells.length; n++)
            b.put(new Coord(cells[n][0],cells[n][1]), n%2 == 0 ? GameEngine.Cell.X : GameEngine.Cell.O);
        return b;
    }

    @Test void cachedAndUncachedSearchAgreeOnMoveAndScore() {
        for (int variant = 0; variant < 4; variant++) {
            Map<Coord, GameEngine.Cell> b = fixture();
            if (variant > 0) b.put(new Coord(22+variant, 23), GameEngine.Cell.X);
            AI cached = new AI(50,5,Difficulty.MEDIUM), plain = new AI(50,5,Difficulty.MEDIUM);
            plain.useTranspositions = false;
            assertThat(cached.findBestMove(b, GameEngine.Cell.O, 50))
                    .isEqualTo(plain.findBestMove(b, GameEngine.Cell.O, 50));
            assertThat(cached.searchStats().completedDepth()).isEqualTo(plain.searchStats().completedDepth());
            assertThat(cached.completedScore).isEqualTo(plain.completedScore);
            assertThat(cached.searchStats().ttHits()).isPositive();
        }
    }

    @Test void threatContinuationProvesOpenFourAtFirstNominalPly() {
        Map<Coord, GameEngine.Cell> b = new HashMap<>();
        for (int x = 5; x <= 7; x++) b.put(new Coord(x,7), GameEngine.Cell.O);
        b.put(new Coord(2,2), GameEngine.Cell.X);
        AI ai = new AI(15,5,Difficulty.EASY);
        assertThat(ai.findBestMove(b, GameEngine.Cell.O, 15)).isIn(new Coord(4,7),new Coord(8,7));
        assertThat(ai.searchStats().completedDepth()).isEqualTo(1);
        assertThat(ai.completedScore).isGreaterThan(AI.WIN_SCORE-1000);
        assertThat(ai.searchStats().threatNodes()).isPositive();
    }

    @Test void cancellationUnwindsAndInstanceCanBeReused() throws Exception {
        AI ai = new AI(50,5,Difficulty.HARD);
        Map<Coord, GameEngine.Cell> board = Map.of(new Coord(25,25),GameEngine.Cell.X,
                new Coord(26,25),GameEngine.Cell.O);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Coord> result = executor.submit(() -> ai.findBestMove(board,GameEngine.Cell.O,50));
            Thread.sleep(50);
            ai.cancelSearch();
            Coord move = result.get(2,TimeUnit.SECONDS);
            assertThat(move.isInside(50)).isTrue();
            assertThat(board).doesNotContainKey(move);
            ai.setDifficulty(Difficulty.MEDIUM);
            AI fresh = new AI(50,5,Difficulty.MEDIUM);
            assertThat(ai.findBestMove(fixture(), GameEngine.Cell.O,50))
                    .isEqualTo(fresh.findBestMove(fixture(),GameEngine.Cell.O,50));
        } finally { executor.shutdownNow(); }
    }
}
