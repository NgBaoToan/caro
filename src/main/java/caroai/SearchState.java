package caroai;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Mutable search-only position. Every make has an exact, constant-time score undo. */
final class SearchState {
    final Map<Coord, GameEngine.Cell> board;
    final int size, winLength;
    final GameEngine.Cell me, them;
    private int mine, yours;
    long hash;
    private final int[] neighbours, attack, defence;
    private final Coord[] squares;
    private final GameEngine.Cell[] cells;
    private final int[][] potentialCounts;
    private final BitSet potential = new BitSet();
    private final BitSet candidates = new BitSet(), dirty = new BitSet();
    private final ArrayDeque<Undo> history = new ArrayDeque<>();
    private record Undo(Coord move, int mine, int yours, long hash) {}
    record Candidate(Coord move, int attack, int defence) {
        int rank(boolean maximizing) {
            return maximizing ? attack + (int)(Evaluator.DEFENCE_WEIGHT * defence)
                    : defence + (int)(Evaluator.DEFENCE_WEIGHT * attack);
        }
    }

    SearchState(Map<Coord, GameEngine.Cell> source, int size, int winLength, GameEngine.Cell me) {
        this.board = new HashMap<>(source);
        this.size = size;
        this.winLength = winLength;
        this.me = me;
        this.them = GameEngine.opponentOf(me);
        mine = Evaluator.rawScore(board, size, me, winLength);
        yours = Evaluator.rawScore(board, size, them, winLength);
        neighbours = new int[size * size];
        attack = new int[size * size];
        defence = new int[size * size];
        squares = new Coord[size * size];
        cells = new GameEngine.Cell[size * size];
        potentialCounts = new int[2][size * size];
        for (int x = 0; x < size; x++) for (int y = 0; y < size; y++)
            squares[x * size + y] = new Coord(x, y);
        for (var e : board.entrySet()) {
            cells[index(e.getKey())] = e.getValue();
            hash ^= stoneKey(e.getKey(), e.getValue());
            updateNeighbours(e.getKey(), 1);
        }
        for (int[] d : Evaluator.DIRECTIONS) for (Coord c : squares)
            updateWindow(c.col(), c.row(), d, 1);
        dirty.set(0, squares.length);
    }

    int score() { return mine - (int)(Evaluator.DEFENCE_WEIGHT * yours); }

    // SplitMix64: deterministic pseudorandom Zobrist values for (square, colour).
    static long stoneKey(Coord c, GameEngine.Cell side) {
        long key = ((long)c.col() << 32) ^ (c.row() & 0xffffffffL);
        return mix(key ^ (side == GameEngine.Cell.X ? 0x243f6a8885a308d3L : 0x13198a2e03707344L));
    }
    private static long mix(long x) {
        x = (x ^ (x >>> 30)) * 0xbf58476d1ce4e5b9L;
        x = (x ^ (x >>> 27)) * 0x94d049bb133111ebL;
        return x ^ (x >>> 31);
    }
    long key(boolean maximizing) { return hash ^ (maximizing ? 0xa4093822299f31d0L : 0x082efa98ec4e6c89L); }

    void place(Coord c, GameEngine.Cell side) {
        if (!c.isInside(size) || board.containsKey(c)) throw new IllegalArgumentException("Occupied/outside square");
        history.push(new Undo(c, mine, yours, hash));
        int oldMine = localScore(c, me), oldYours = localScore(c, them);
        updateWindows(c, -1);
        board.put(c, side);
        cells[index(c)] = side;
        updateWindows(c, 1);
        mine += localScore(c, me) - oldMine;
        yours += localScore(c, them) - oldYours;
        hash ^= stoneKey(c, side);
        candidates.clear(index(c));
        updateNeighbours(c, 1);
        invalidate(c);
    }

    void undo() {
        Undo u = history.pop();
        updateWindows(u.move, -1);
        board.remove(u.move);
        cells[index(u.move)] = null;
        updateWindows(u.move, 1);
        mine = u.mine;
        yours = u.yours;
        hash = u.hash;
        updateNeighbours(u.move, -1);
        if (neighbours[index(u.move)] > 0) candidates.set(index(u.move));
        invalidate(u.move);
    }

    /** Only starts whose run/gap/end can change. Longer runs already score FIVE. */
    private int localScore(Coord changed, GameEngine.Cell side) {
        int score = 0;
        int radius = Math.max(5, winLength) + 1;
        for (int[] d : Evaluator.DIRECTIONS) {
            for (int n = -radius; n <= 1; n++) {
                Coord c = changed.offset(n * d[0], n * d[1]);
                if (board.get(c) == side && board.get(c.offset(-d[0], -d[1])) != side)
                    score += Evaluator.runFrom(board, size, c, d, side, winLength).score();
            }
        }
        return score;
    }

    List<Candidate> candidates(Runnable budgetCheck) { return candidates(budgetCheck, false); }
    List<Candidate> candidates(Runnable budgetCheck, boolean forcingOnly) {
        List<Candidate> out = new ArrayList<>();
        BitSet selected = candidates;
        if (forcingOnly) {
            selected = (BitSet) potential.clone();
            selected.and(candidates);
        }
        for (int i = selected.nextSetBit(0); i >= 0; i = selected.nextSetBit(i + 1)) {
            budgetCheck.run();
            if (dirty.get(i)) {
                attack[i] = Evaluator.scoreAt(board, size, squares[i], me, winLength);
                defence[i] = Evaluator.scoreAt(board, size, squares[i], them, winLength);
                dirty.clear(i);
            }
            out.add(new Candidate(squares[i], attack[i], defence[i]));
        }
        return out;
    }

    // Only windows crossing the changed square can gain/lose a forcing move.
    // A length-k window with k-2 stones and no enemy can create a four-equivalent.
    private void updateWindows(Coord c, int delta) {
        for (int[] d : Evaluator.DIRECTIONS) for (int n = 0; n < winLength; n++)
            updateWindow(c.col()-n*d[0], c.row()-n*d[1], d, delta);
    }
    private void updateWindow(int x, int y, int[] d, int delta) {
        int endX = x+(winLength-1)*d[0], endY = y+(winLength-1)*d[1];
        if (x < 0 || x >= size || y < 0 || y >= size || endX < 0 || endX >= size || endY < 0 || endY >= size) return;
        int xs = 0, os = 0, start = x*size+y, step = d[0]*size+d[1];
        for (int n = 0, i = start; n < winLength; n++, i += step) {
            if (cells[i] == GameEngine.Cell.X) xs++;
            else if (cells[i] == GameEngine.Cell.O) os++;
        }
        boolean px = os == 0 && xs >= winLength-2;
        boolean po = xs == 0 && os >= winLength-2;
        if (!px && !po) return;
        for (int n = 0, i = start; n < winLength; n++, i += step) if (cells[i] == null) {
            if (px) potentialCounts[0][i] += delta;
            if (po) potentialCounts[1][i] += delta;
            potential.set(i, potentialCounts[0][i] > 0 || potentialCounts[1][i] > 0);
        }
    }

    private int index(Coord c) { return c.col() * size + c.row(); }
    private void updateNeighbours(Coord c, int delta) {
        for (int x = Math.max(0, c.col()-2); x <= Math.min(size-1, c.col()+2); x++)
            for (int y = Math.max(0, c.row()-2); y <= Math.min(size-1, c.row()+2); y++) {
                int i = x * size + y;
                neighbours[i] += delta;
                candidates.set(i, neighbours[i] > 0 && !board.containsKey(squares[i]));
            }
    }
    private void invalidate(Coord c) {
        // Full affected lines keep ordering exact even for long runs and unusual win lengths.
        for (int[] d : Evaluator.DIRECTIONS) for (int n = -size; n <= size; n++) {
            int x = c.col() + n*d[0], y = c.row() + n*d[1];
            if (x >= 0 && x < size && y >= 0 && y < size) dirty.set(x*size+y);
        }
    }
}
