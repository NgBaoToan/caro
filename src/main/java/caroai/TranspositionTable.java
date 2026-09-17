package caroai;

/** Fixed-size, full-key-checked table; cleared for each root/perspective. */
final class TranspositionTable {
    enum Bound { EXACT, LOWER, UPPER }
    record Entry(long key, int depth, int score, Bound bound, Coord move) {}
    private final Entry[] entries;
    TranspositionTable(int bits) { entries = new Entry[1 << bits]; }
    private int index(long key) { return (int)(key ^ (key >>> 32)) & (entries.length-1); }
    Entry get(long key) {
        Entry e = entries[index(key)];
        return e != null && e.key == key ? e : null;
    }
    void put(long key, int depth, int score, Bound bound, Coord move) {
        int i = index(key);
        Entry old = entries[i];
        if (old == null || old.key != key || depth >= old.depth)
            entries[i] = new Entry(key, depth, score, bound, move);
    }
}
