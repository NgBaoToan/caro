package caroai;

import java.util.*;

/** One cold JVM call per invocation, followed by deterministic non-winning stress positions. */
public final class LatencyProbe {
    public static void main(String[] args) {
        System.out.println("case,stones,millis,completedDepth,legal");
        measure("cold-open", Benchmark.position(0));
        Random random = new Random(55197);
        for (int test = 0; test < 12; test++) {
            Map<Coord, GameEngine.Cell> board = new HashMap<>();
            int target = 12 + test*4;
            while (board.size() < target) {
                Coord c = new Coord(15+random.nextInt(20),15+random.nextInt(20));
                GameEngine.Cell side = board.size()%2 == 0 ? GameEngine.Cell.X : GameEngine.Cell.O;
                if (!board.containsKey(c) && !Evaluator.makesFive(board,50,c,side,5)) board.put(c,side);
            }
            measure("stress-"+test,board);
        }
    }
    private static void measure(String label, Map<Coord, GameEngine.Cell> board) {
        AI ai = new AI(50,5,Difficulty.HARD);
        long start = System.nanoTime();
        Coord c = ai.findBestMove(board,GameEngine.Cell.O,50);
        double ms = (System.nanoTime()-start)/1e6;
        boolean legal = c != null && c.isInside(50) && !board.containsKey(c);
        System.out.printf(Locale.ROOT,"%s,%d,%.3f,%d,%s%n",label,board.size(),ms,ai.searchStats().completedDepth(),legal);
        if (!legal || ms >= 1500) throw new AssertionError("Latency/legality target failed: "+label+" "+ms);
    }
}
