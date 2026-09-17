import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

/** Isolated class loaders, one JVM, alternating baseline/new runs on identical fixtures. */
public final class PairedBenchmark {
    private record Engine(ClassLoader loader, Class<?> ai, Class<?> difficulty, Class<?> cell, Method position) {
        static Engine open(String path) throws Exception {
            ClassLoader l = new URLClassLoader(new URL[]{Path.of(path).toUri().toURL()}, ClassLoader.getPlatformClassLoader());
            return new Engine(l,l.loadClass("caroai.AI"),l.loadClass("caroai.Difficulty"),l.loadClass("caroai.GameEngine$Cell"),
                    l.loadClass("caroai.Benchmark").getMethod("position",int.class));
        }
        Object instance(String level, int depth) throws Exception {
            Object d = difficulty.getField(level).get(null);
            if (level.equals("HARD") && depth > 0) {
                Field field = difficulty.getDeclaredField("maxDepth"); field.setAccessible(true); field.setInt(d,depth);
            }
            return ai.getConstructor(int.class,int.class,difficulty).newInstance(50,5,d);
        }
        void run(Object player, String variant, String level, int fixture, int iteration, boolean print) throws Exception {
            Object board = position.invoke(null,fixture), side = cell.getField("O").get(null);
            Method search = ai.getMethod("findBestMove",Map.class,cell,int.class);
            long start = System.nanoTime();
            Object move = search.invoke(player,board,side,50);
            double ms = (System.nanoTime()-start)/1e6;
            if (print) System.out.printf(Locale.ROOT,"%s,%d,%s,%d,%.3f,%d,%d,%d,%d,%d%n",variant,fixture,level,iteration,ms,
                    metric(player,"nodes"),metric(player,"completedDepth"),metric(player,"ttHits"),metric(player,"ttCutoffs"),metric(player,"threatNodes"));
            if (move == null || ((Map<?,?>)board).containsKey(move)) throw new AssertionError("Illegal result");
        }
        long metric(Object instance,String name) throws Exception {
            try { Field field = ai.getDeclaredField(name); field.setAccessible(true); return ((Number)field.get(instance)).longValue(); }
            catch (NoSuchFieldException e) { return -1; }
        }
    }
    public static void main(String[] args) throws Exception {
        Engine baseline = Engine.open(args[0]), revised = Engine.open(args[1]);
        int repeats = Integer.parseInt(args[2]);
        System.out.println("variant,fixture,level,iteration,millis,nodes,completedDepth,ttHits,ttCutoffs,threatNodes");
        for (String level : new String[]{"EASY","MEDIUM","HARD"}) for (int fixture = 0; fixture < 4; fixture++) {
            Object original = baseline.instance(level,6), sameDepth = revised.instance(level,6);
            for (int warm = 0; warm < 3; warm++) {
                baseline.run(original,"baseline",level,fixture,warm,false);
                revised.run(sameDepth,"depth6",level,fixture,warm,false);
            }
            for (int n = 0; n < repeats; n++) {
                if (n%2 == 0) baseline.run(original,"baseline",level,fixture,n,true);
                revised.run(sameDepth,"depth6",level,fixture,n,true);
                if (n%2 != 0) baseline.run(original,"baseline",level,fixture,n,true);
            }
        }
    }
}
