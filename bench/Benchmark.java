package caroai;

import java.util.*;
import java.lang.reflect.Field;

/** Fixed fixtures, warmup, repeated wall-clock and node measurements. */
public final class Benchmark {
    public static Map<Coord, GameEngine.Cell> position(int fixture) {
        Map<Coord, GameEngine.Cell> b = new HashMap<>();
        int[][][] fixtures = {
            {{25,25},{26,25}},
            {{25,25},{26,25},{24,26},{26,26},{23,27},{25,27},{24,24},{27,24}},
            {{23,23},{24,23},{25,23},{26,24},{24,24},{23,25},{25,25},{26,25},{27,26},{24,26},{25,27},{23,27},{27,23},{22,24},{22,26},{26,28}},
            {{20,20},{22,21},{21,20},{23,21},{23,20},{25,23},{24,22},{25,24},{26,26},{24,25}}
        };
        int i=0;
        for (int[] p : fixtures[fixture]) b.put(new Coord(p[0],p[1]), i++%2==0 ? GameEngine.Cell.X : GameEngine.Cell.O);
        return b;
    }
    public static void main(String[] args) throws Exception {
        int repeats = args.length == 0 ? 5 : Integer.parseInt(args[0]);
        if (args.length > 1 && Integer.parseInt(args[1]) > 0) {
            Field maxDepth = Difficulty.class.getDeclaredField("maxDepth");
            maxDepth.setAccessible(true);
            maxDepth.setInt(Difficulty.HARD, Integer.parseInt(args[1]));
        }
        Field nodes = AI.class.getDeclaredField("nodes"); nodes.setAccessible(true);
        Field depth = null;
        try { depth=AI.class.getDeclaredField("completedDepth"); depth.setAccessible(true); } catch (NoSuchFieldException ignored) {}
        System.out.println("fixture,level,iteration,millis,nodes,completedDepth,col,row,ttHits,ttCutoffs,threatNodes");
        for (Difficulty d : Difficulty.values()) for (int f=0;f<4;f++) {
            if (args.length > 2 && !d.name().equals(args[2])) continue;
            AI ai = new AI(50,5,d);
            for(int w=0;w<2;w++) ai.findBestMove(position(f),GameEngine.Cell.O,50);
            for(int r=0;r<repeats;r++) {
                long start=System.nanoTime();
                Coord c=ai.findBestMove(position(f),GameEngine.Cell.O,50);
                double ms=(System.nanoTime()-start)/1e6;
                System.out.printf(Locale.ROOT,"%d,%s,%d,%.3f,%d,%d,%d,%d,%d,%d,%d%n",f,d,r,ms,nodes.getLong(ai),depth==null?-1:depth.getInt(ai),c.col(),c.row(), metric(ai,"ttHits"),metric(ai,"ttCutoffs"),metric(ai,"threatNodes"));
            }
        }
    }
    private static long metric(AI ai, String name) throws Exception {
        try { Field f = AI.class.getDeclaredField(name); f.setAccessible(true); return f.getLong(ai); }
        catch (NoSuchFieldException ignored) { return -1; }
    }
}
