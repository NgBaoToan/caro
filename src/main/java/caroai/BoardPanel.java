package caroai;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The board: draws the grid and the stones, and turns mouse and keyboard input
 * into moves.
 *
 * The panel never stores a copy of the position — it reads {@link GameEngine}
 * every time it paints, and every move goes through {@code engine.makeMove()},
 * which rejects anything off the board.
 */
public class BoardPanel extends JPanel {

    private final GameEngine engine;
    private final AI         ai;

    private final int cellSize = 40;
    private int offsetX = 0, offsetY = 0;   // top-left cell of the visible window

    private Coord       lastMove    = null;
    private Coord       hoverCell   = null;
    private List<Coord> winningLine = new ArrayList<>();
    private boolean     gameOver    = false;   // blocks clicks once the game ends

    /** Pending AI move; kept so it can be cancelled on undo or restart. */
    private Timer aiTimer = null;

    public BoardPanel(GameEngine engine, AI ai) {
        this.engine = engine;
        this.ai     = ai;
        setPreferredSize(new Dimension(800, 800));

        // Hover preview
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (gameOver) return;
                Coord c = cellAt(e.getX(), e.getY());
                if (!c.equals(hoverCell)) { hoverCell = c; repaint(); }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!gameOver) handleClick(e.getX(), e.getY());
                requestFocusInWindow();
            }
            @Override
            public void mouseExited(MouseEvent e) { hoverCell = null; repaint(); }
        });

        // WASD pans the camera
        setFocusable(true);
        requestFocusInWindow();
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W: offsetY--; break;
                    case KeyEvent.VK_S: offsetY++; break;
                    case KeyEvent.VK_A: offsetX--; break;
                    case KeyEvent.VK_D: offsetX++; break;
                    default: return;
                }
                // Keep the camera over the board so the player cannot scroll off
                // the edge and stare at an area where no move is legal.
                int size = engine.getSize();
                int maxX = Math.max(0, size - getWidth()  / cellSize);
                int maxY = Math.max(0, size - getHeight() / cellSize);
                offsetX = Math.max(0, Math.min(offsetX, maxX));
                offsetY = Math.max(0, Math.min(offsetY, maxY));
                repaint();
            }
        });
    }

    // ── Input ────────────────────────────────────────────────

    /** Screen pixel to board cell. */
    private Coord cellAt(int mouseX, int mouseY) {
        return new Coord(mouseX / cellSize + offsetX,
                         mouseY / cellSize + offsetY);
    }

    private void handleClick(int mouseX, int mouseY) {
        // Ignore clicks that are not the human's turn — for instance while the
        // AI is "thinking" — otherwise one side could play twice in a row.
        if (engine.getCurrentPlayer().isAI()) return;

        Coord c = cellAt(mouseX, mouseY);

        // Whoever is to move plays their own symbol. Reading it from the current
        // player (rather than a fixed field) is what makes two-player mode
        // alternate X and O correctly.
        GameEngine.Cell symbol = engine.getCurrentPlayer().cell();

        // makeMove() rejects off-board and occupied cells, so an out-of-range
        // click simply does nothing instead of throwing mid-turn.
        if (!engine.makeMove(c, symbol)) return;

        lastMove = c;
        GameFrame frame = (GameFrame) SwingUtilities.getWindowAncestor(this);
        if (frame != null) frame.recordMove(c);

        repaint();

        if (engine.checkWin(c)) {
            winningLine = engine.getWinningLine(c);
            triggerWin();
            return;
        }

        engine.switchTurn();
        maybeAIMove();
    }

    // ── AI turn ──────────────────────────────────────────────

    /** Lets the AI play when it is its turn, including the opening move. */
    public void maybeAIMove() {
        if (ai == null || gameOver || !engine.getCurrentPlayer().isAI()) return;

        cancelPendingAI();   // never let two AI moves wait at once

        aiTimer = new Timer(300, ev -> {
            aiTimer = null;
            if (gameOver || !engine.getCurrentPlayer().isAI()) return;

            Coord move = ai.getBestMove(engine);

            // Opening move: with an empty board the AI has nothing to build on
            // and returns null. Play inside the visible window — the centre of a
            // 50x50 board is off-screen, and a stone the player cannot see looks
            // like a frozen game.
            if (engine.isEmpty()) move = visibleCenterCell();

            // The AI must hand back a legal square. If it does not, find the
            // nearest empty one; otherwise makeMove() would fail while the turn
            // still flipped, leaving the game stuck.
            if (move == null || engine.isOccupied(move) || !engine.isInside(move)) {
                move = findFallbackMove();
            }
            if (move == null) return;   // board is full

            if (!engine.makeMove(move, engine.getAISymbol())) return;

            lastMove = move;
            GameFrame frame = (GameFrame) SwingUtilities.getWindowAncestor(this);
            if (frame != null) frame.recordMove(move);
            repaint();

            if (engine.checkWin(move)) {
                winningLine = engine.getWinningLine(move);
                triggerWin();
                return;
            }
            engine.switchTurn();
            repaint();
        });
        aiTimer.setRepeats(false);
        aiTimer.start();
    }

    /** Cancels a pending AI move (undo, restart, back to menu). */
    public void cancelPendingAI() {
        if (aiTimer != null) { aiTimer.stop(); aiTimer = null; }
    }

    /** Empty cell nearest the middle of the view — the AI never skips a turn. */
    private Coord findFallbackMove() {
        int size = engine.getSize();
        int w = getWidth()  > 0 ? getWidth()  : getPreferredSize().width;
        int h = getHeight() > 0 ? getHeight() : getPreferredSize().height;
        int cx = offsetX + Math.max(1, w / cellSize) / 2;
        int cy = offsetY + Math.max(1, h / cellSize) / 2;

        for (int r = 0; r < size; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    Coord c = new Coord(cx + dx, cy + dy);
                    if (!engine.isInside(c)) continue;
                    if (!engine.isOccupied(c)) return c;
                }
            }
        }
        return null;
    }

    /**
     * The cell at the centre of the VISIBLE WINDOW, not the centre of the whole
     * board. The board is far wider than the window, so cell (25,25) is normally
     * out of sight.
     */
    private Coord visibleCenterCell() {
        int size = engine.getSize();
        int w = getWidth()  > 0 ? getWidth()  : getPreferredSize().width;
        int h = getHeight() > 0 ? getHeight() : getPreferredSize().height;
        int cols = Math.max(1, w / cellSize);
        int rows = Math.max(1, h / cellSize);

        int cx = offsetX + cols / 2;
        int cy = offsetY + rows / 2;

        // Jitter a couple of cells so openings differ between games, while
        // staying inside the view with a one-cell margin.
        int jitter = 2;
        cx += (int) (Math.random() * (2 * jitter + 1)) - jitter;
        cy += (int) (Math.random() * (2 * jitter + 1)) - jitter;

        cx = Math.max(offsetX + 1, Math.min(cx, offsetX + cols - 2));
        cy = Math.max(offsetY + 1, Math.min(cy, offsetY + rows - 2));
        cx = Math.max(0, Math.min(cx, size - 1));
        cy = Math.max(0, Math.min(cy, size - 1));
        return new Coord(cx, cy);
    }

    // ── State hooks used by GameFrame ────────────────────────

    /** Moves the highlight after an undo. */
    public void setLastMove(Coord c) { lastMove = c; }

    /** Allows play to continue after undoing a finished game. */
    public void clearGameOver() {
        gameOver    = false;
        winningLine = new ArrayList<>();
    }

    public void resetState() {
        gameOver    = false;
        lastMove    = null;
        winningLine = new ArrayList<>();
        hoverCell   = null;
        offsetX     = 0;
        offsetY     = 0;
    }

    /** Flash the winning line, then hand over to the result overlay. */
    private void triggerWin() {
        gameOver  = true;
        hoverCell = null;

        Timer blink = new Timer(300, null);
        final int[] count = {0};
        blink.addActionListener(e -> {
            count[0]++;
            repaint();
            if (count[0] >= 6) {
                blink.stop();
                GameFrame frame = (GameFrame) SwingUtilities.getWindowAncestor(this);
                if (frame != null) frame.showWinOverlay(engine.getCurrentPlayer());
            }
        });
        blink.start();
    }

    // ── Painting ─────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);

        int w = getWidth(), h = getHeight();
        int size = engine.getSize();

        // ── Parchment surface ────────────────────────────────
        g2.setColor(Theme.BOARD_BG);
        g2.fillRect(0, 0, w, h);

        // Very faint checker so the surface is not dead flat
        g2.setColor(Theme.BOARD_BG_ALT);
        for (int sx = 0; sx <= w / cellSize + 1; sx++) {
            for (int sy = 0; sy <= h / cellSize + 1; sy++) {
                if (((offsetX + sx) + (offsetY + sy)) % 2 == 0) {
                    g2.fillRect(sx * cellSize, sy * cellSize, cellSize, cellSize);
                }
            }
        }

        // ── Grid ─────────────────────────────────────────────
        g2.setColor(Theme.BOARD_LINE);
        g2.setStroke(new BasicStroke(1f));
        for (int i = 0; i <= w / cellSize + 1; i++)
            g2.drawLine(i * cellSize, 0, i * cellSize, h);
        for (int i = 0; i <= h / cellSize + 1; i++)
            g2.drawLine(0, i * cellSize, w, i * cellSize);

        // ── Star points every six cells, as on a go board ─────
        g2.setColor(Theme.BOARD_STAR);
        for (int col = 0; col <= size; col += 6) {
            for (int row = 0; row <= size; row += 6) {
                if (col == 0 || row == 0) continue;
                int sx = (col - offsetX) * cellSize;
                int sy = (row - offsetY) * cellSize;
                if (sx < -8 || sy < -8 || sx > w + 8 || sy > h + 8) continue;
                g2.fillOval(sx - 3, sy - 3, 6, 6);
            }
        }

        // ── Last move ────────────────────────────────────────
        if (lastMove != null && onScreen(lastMove, w, h)) {
            g2.setColor(Theme.LAST_MOVE);
            g2.fillRect(screenX(lastMove) + 1, screenY(lastMove) + 1,
                        cellSize - 1, cellSize - 1);
        }

        // ── Winning line ─────────────────────────────────────
        if (!winningLine.isEmpty()) {
            g2.setColor(Theme.WIN_CELL);
            for (Coord c : winningLine) {
                if (!onScreen(c, w, h)) continue;
                g2.fillRect(screenX(c) + 1, screenY(c) + 1, cellSize - 1, cellSize - 1);
            }
            if (winningLine.size() >= 2) {
                Coord first = winningLine.get(0);
                Coord last  = winningLine.get(winningLine.size() - 1);
                g2.setColor(Theme.alpha(Theme.WIN_LINE, 170));
                g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND,
                                             BasicStroke.JOIN_ROUND));
                g2.drawLine(screenX(first) + cellSize / 2, screenY(first) + cellSize / 2,
                            screenX(last)  + cellSize / 2, screenY(last)  + cellSize / 2);
            }
        }

        // ── Hover preview ────────────────────────────────────
        if (hoverCell != null && !gameOver
                && engine.isInside(hoverCell)
                && !engine.isOccupied(hoverCell)
                && !engine.getCurrentPlayer().isAI()
                && onScreen(hoverCell, w, h)) {
            boolean isX = engine.getCurrentPlayer().getSymbol().equals("X");
            Theme.drawStone(g2, isX,
                            screenX(hoverCell) + cellSize / 2.0,
                            screenY(hoverCell) + cellSize / 2.0,
                            cellSize * 0.38, 0.28f);
        }

        // ── Stones ───────────────────────────────────────────
        for (var entry : engine.getBoard().entrySet()) {
            Coord c = entry.getKey();
            if (!onScreen(c, w, h)) continue;
            boolean isX = entry.getValue() == GameEngine.Cell.X;
            Theme.drawStone(g2, isX,
                            screenX(c) + cellSize / 2.0,
                            screenY(c) + cellSize / 2.0,
                            cellSize * 0.38, 1f);
        }

        // ── Outline of the last move, drawn on top so it stays visible ──
        if (lastMove != null && onScreen(lastMove, w, h)) {
            g2.setColor(Theme.alpha(Theme.BOARD_FRAME, 90));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRect(screenX(lastMove) + 1, screenY(lastMove) + 1,
                        cellSize - 2, cellSize - 2);
        }

        // ── Dark wooden frame around the surface ─────────────
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(Theme.alpha(Theme.BOARD_FRAME, 120));
        g2.drawRect(0, 0, w - 1, h - 1);
        g2.setColor(Theme.alpha(Theme.BOARD_FRAME, 45));
        g2.drawRect(1, 1, w - 3, h - 3);

        g2.dispose();
    }

    private int screenX(Coord c) { return (c.col() - offsetX) * cellSize; }
    private int screenY(Coord c) { return (c.row() - offsetY) * cellSize; }

    private boolean onScreen(Coord c, int w, int h) {
        int sx = screenX(c), sy = screenY(c);
        return sx > -cellSize && sy > -cellSize && sx < w && sy < h;
    }
}
