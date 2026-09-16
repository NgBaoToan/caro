package caroai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Core game rules: stones on the board, turn order, win detection.
 *
 * The engine owns the one and only copy of the board. Nothing else keeps a
 * mirror of it — the UI and the AI both read this map. That removes the whole
 * class of bugs where a second copy drifts out of sync with the real board.
 *
 * Coordinate safety lives here too: {@link #makeMove} refuses anything outside
 * the board, so no caller can push an out-of-range coordinate deeper into the
 * program.
 */
public class GameEngine {

    public enum Cell { X, O }

    /** Board width/height in cells when no explicit size is given. */
    public static final int DEFAULT_SIZE = 50;

    /** Stones in a row needed to win. */
    public static final int WIN_LENGTH = 5;

    private final int size;

    private final Map<Coord, Cell> board = new HashMap<>();

    private final Player player1, player2;
    private Player currentPlayer;

    private Coord lastPlayerMove = null;   // last stone placed by the human side

    private Cell playerSymbol = Cell.X;
    private Cell aiSymbol     = Cell.O;

    public GameEngine(Player p1, Player p2) {
        this(p1, p2, DEFAULT_SIZE);
    }

    public GameEngine(Player p1, Player p2, int size) {
        // A board smaller than the win length can never be won, so clamp it.
        this.size    = Math.max(WIN_LENGTH, size);
        this.player1 = p1;
        this.player2 = p2;
        this.currentPlayer = p1;
    }

    // ── Board state ──────────────────────────────────────────

    public int getSize() { return size; }

    /** Read-only view of the board. Mutations go through {@link #makeMove}. */
    public Map<Coord, Cell> getBoard() { return Collections.unmodifiableMap(board); }

    public Cell cellAt(Coord c) { return board.get(c); }

    public boolean isEmpty() { return board.isEmpty(); }

    public boolean isOccupied(Coord c) { return board.containsKey(c); }

    /** True when the coordinate lies on the board. */
    public boolean isInside(Coord c) { return c != null && c.isInside(size); }

    // ── Symbols & players ────────────────────────────────────

    public void setPlayerSymbol(Cell c) { this.playerSymbol = c; }
    public void setAISymbol(Cell c)     { this.aiSymbol = c; }

    public Cell getPlayerSymbol() { return playerSymbol; }
    public Cell getAISymbol()     { return aiSymbol; }

    /** The symbol the opponent of {@code c} plays. */
    public static Cell opponentOf(Cell c) { return c == Cell.X ? Cell.O : Cell.X; }

    public Player getCurrentPlayer() { return currentPlayer; }
    public Coord  getLastMoveOfPlayer() { return lastPlayerMove; }

    public void switchTurn() {
        currentPlayer = (currentPlayer == player1 ? player2 : player1);
    }

    /** Lets the caller decide who opens (e.g. the AI when the human picked O). */
    public void setCurrentPlayer(Player p) { this.currentPlayer = p; }

    // ── Moves ────────────────────────────────────────────────

    /**
     * Places a stone. Returns false — without changing anything — when the
     * coordinate is null, off the board, or already taken.
     *
     * This is the single gate every move passes through, which is why bounds
     * checking belongs here and not scattered across the UI.
     */
    public boolean makeMove(Coord c, Cell symbol) {
        if (!isInside(c)) return false;
        if (board.containsKey(c)) return false;

        board.put(c, symbol);
        if (symbol == playerSymbol) lastPlayerMove = c;
        return true;
    }

    /** Removes a stone (used by undo). Returns true if something was removed. */
    public boolean undoMove(Coord c) {
        if (c == null) return false;
        boolean removed = board.remove(c) != null;
        if (removed && c.equals(lastPlayerMove)) lastPlayerMove = null;
        return removed;
    }

    // ── Win detection ────────────────────────────────────────

    private static final int[][] DIRECTIONS = { {1, 0}, {0, 1}, {1, 1}, {1, -1} };

    public boolean checkWin(Coord c) {
        Cell stone = board.get(c);
        if (stone == null) return false;

        for (int[] d : DIRECTIONS) {
            int count = 1
                    + countDir(c,  d[0],  d[1], stone)
                    + countDir(c, -d[0], -d[1], stone);
            if (count >= WIN_LENGTH) return true;
        }
        return false;
    }

    /** The five stones that won, or an empty list when there is no win. */
    public List<Coord> getWinningLine(Coord c) {
        Cell stone = board.get(c);
        if (stone == null) return new ArrayList<>();

        for (int[] d : DIRECTIONS) {
            List<Coord> line = new ArrayList<>();
            line.add(c);
            line.addAll(collectDir(c,  d[0],  d[1], stone));
            line.addAll(collectDir(c, -d[0], -d[1], stone));
            if (line.size() >= WIN_LENGTH) return line.subList(0, WIN_LENGTH);
        }
        return new ArrayList<>();
    }

    private int countDir(Coord from, int dCol, int dRow, Cell stone) {
        int count = 0;
        Coord c = from.offset(dCol, dRow);
        while (board.get(c) == stone) {
            count++;
            c = c.offset(dCol, dRow);
        }
        return count;
    }

    private List<Coord> collectDir(Coord from, int dCol, int dRow, Cell stone) {
        List<Coord> line = new ArrayList<>();
        Coord c = from.offset(dCol, dRow);
        while (board.get(c) == stone) {
            line.add(c);
            c = c.offset(dCol, dRow);
        }
        return line;
    }
}
