package caroai;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Title screen: pick a mode and a side.
 *
 * The layout is built once in the constructor; resizing only repositions the
 * components instead of recreating them, so nothing flickers and no selection
 * is lost.
 */
public class StartMenuPanel extends JPanel {

    /** Credit shown in the bottom-right corner. */
    private static final String AUTHOR = "nbaotoan";

    // 0 = vs computer, 1 = two players
    private int selectedMode   = 0;
    // 0 = X, 1 = O
    private int selectedSymbol = 0;
    // Index into Difficulty.values(); Medium is the default.
    private int selectedLevel  = 1;

    private final Runnable onStart;

    private final ModeCard      cardAI, cardHuman;
    private final SymbolTile    tileX, tileO;
    private final LevelTile[]   levelTiles;
    private final JLabel        labelMode, labelSymbol, labelLevel, labelHint;
    private final ClassicButton startBtn, exitBtn;

    private int titleBaseline = 160;   // recomputed in layoutUI()

    public StartMenuPanel(Runnable onStart) {
        this.onStart = onStart;
        setLayout(null);
        setBackground(Theme.BG);

        labelMode   = sectionLabel("SELECT MODE");
        labelSymbol = sectionLabel("CHOOSE YOUR SIDE");
        labelLevel  = sectionLabel("DIFFICULTY");

        labelHint = new JLabel("X always moves first", SwingConstants.CENTER);
        labelHint.setFont(Theme.ui(Font.PLAIN, 12));
        labelHint.setForeground(Theme.TEXT_FAINT);

        cardAI    = new ModeCard(0, "Play vs Computer", "Test your skill against the AI");
        cardHuman = new ModeCard(1, "Two Players",      "Share one screen");

        tileX = new SymbolTile(0);
        tileO = new SymbolTile(1);

        Difficulty[] levels = Difficulty.values();
        levelTiles = new LevelTile[levels.length];
        for (int i = 0; i < levels.length; i++) levelTiles[i] = new LevelTile(i, levels[i]);

        startBtn = new ClassicButton("START", ClassicButton.Kind.PRIMARY);
        startBtn.setFontSize(15);
        startBtn.addActionListener(e -> onStart.run());

        exitBtn = new ClassicButton("Exit", ClassicButton.Kind.QUIET);
        exitBtn.addActionListener(e -> System.exit(0));

        add(labelMode);
        add(labelSymbol);
        add(labelHint);
        add(cardAI);
        add(cardHuman);
        add(tileX);
        add(tileO);
        add(labelLevel);
        for (LevelTile lt : levelTiles) add(lt);
        add(startBtn);
        add(exitBtn);

        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { layoutUI(); }
        });

        updateSymbolVisibility();
    }

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(Theme.ui(Font.BOLD, 10));
        l.setForeground(Theme.TEXT_FAINT);
        return l;
    }

    // ── Layout ───────────────────────────────────────────────
    private void layoutUI() {
        int W = getWidth(), H = getHeight();
        if (W == 0 || H == 0) return;

        int cx = W / 2;

        int cardW = 212, cardH = 118, gap = 20;
        int blockH = 96 + 22 + cardH + 34 + 18 + 72 + 22 + 18 + 40 + 22 + 52 + 16 + 40;
        int top = Math.max(40, (H - blockH) / 2);

        titleBaseline = top + 64;

        int y = top + 96;

        labelMode.setBounds(cx - 160, y, 320, 16);
        y += 22;

        int totalW = cardW * 2 + gap;
        cardAI.setBounds(cx - totalW / 2, y, cardW, cardH);
        cardHuman.setBounds(cx - totalW / 2 + cardW + gap, y, cardW, cardH);
        y += cardH + 34;

        labelSymbol.setBounds(cx - 160, y, 320, 16);
        y += 18;

        int tile = 72, tgap = 16;
        tileX.setBounds(cx - tile - tgap / 2, y, tile, tile);
        tileO.setBounds(cx + tgap / 2,        y, tile, tile);
        y += tile + 22;

        labelLevel.setBounds(cx - 160, y, 320, 16);
        y += 18;

        int levelW = 92, levelH = 40, lgap = 10;
        int levelRow = levelTiles.length * levelW + (levelTiles.length - 1) * lgap;
        for (int i = 0; i < levelTiles.length; i++) {
            levelTiles[i].setBounds(cx - levelRow / 2 + i * (levelW + lgap),
                                    y, levelW, levelH);
        }
        y += levelH + 12;

        labelHint.setBounds(cx - 160, y, 320, 16);
        y += 22;

        startBtn.setBounds(cx - 110, y, 220, 52);
        y += 52 + 16;

        exitBtn.setBounds(cx - 60, y, 120, 40);

        revalidate();
        repaint();
    }

    private void updateSymbolVisibility() {
        boolean vsAI = (selectedMode == 0);
        labelSymbol.setVisible(vsAI);
        labelHint.setVisible(vsAI);
        tileX.setVisible(vsAI);
        tileO.setVisible(vsAI);
        labelLevel.setVisible(vsAI);
        for (LevelTile lt : levelTiles) lt.setVisible(vsAI);
    }

    // ── Read by GameFrame ────────────────────────────────────
    public int     getSelectedMode() { return selectedMode; }
    public boolean isVsAI()          { return selectedMode == 0; }
    public boolean playerChoseX()    { return selectedSymbol == 0; }

    /** The level chosen on this screen. Two-player games ignore it. */
    public Difficulty getDifficulty() {
        Difficulty[] levels = Difficulty.values();
        int i = Math.max(0, Math.min(selectedLevel, levels.length - 1));
        return levels[i];
    }

    // ── Background ───────────────────────────────────────────
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);

        int W = getWidth(), H = getHeight();
        if (W <= 0 || H <= 0) { g2.dispose(); return; }   // gradient radius must be > 0

        g2.setColor(Theme.BG);
        g2.fillRect(0, 0, W, H);

        // Very faint board grid as wallpaper
        g2.setColor(new Color(255, 255, 255, 6));
        for (int x = 0; x < W; x += 44) g2.drawLine(x, 0, x, H);
        for (int y = 0; y < H; y += 44) g2.drawLine(0, y, W, y);

        // Darken towards the edges to pull the eye to the centre
        g2.setPaint(new RadialGradientPaint(
            new Point(W / 2, H / 2),
            Math.max(W, H) * 0.72f,
            new float[] { 0f, 1f },
            new Color[] { new Color(0, 0, 0, 0), new Color(0, 0, 0, 150) }
        ));
        g2.fillRect(0, 0, W, H);

        // ── Title ────────────────────────────────────────────
        g2.setFont(Theme.display(Font.BOLD, 52));
        FontMetrics fm = g2.getFontMetrics();
        String title = "CARO";
        float tracking = 8f;
        float tw = Theme.trackedWidth(fm, title, tracking);

        g2.setColor(Theme.TEXT);
        Theme.drawTracked(g2, title, W / 2f - tw / 2f, titleBaseline, tracking);

        // Gold rules flanking the subtitle
        g2.setFont(Theme.ui(Font.PLAIN, 11));
        FontMetrics sfm = g2.getFontMetrics();
        String sub = "FIVE IN A ROW";
        float stracking = 2.6f;
        float sw = Theme.trackedWidth(sfm, sub, stracking);
        float subY = titleBaseline + 26;

        g2.setColor(Theme.TEXT_FAINT);
        Theme.drawTracked(g2, sub, W / 2f - sw / 2f, subY, stracking);

        g2.setColor(Theme.GOLD);
        int lineY = (int) subY - 4;
        int lineW = 40;
        g2.fillRect((int) (W / 2f - sw / 2f) - lineW - 16, lineY, lineW, 1);
        g2.fillRect((int) (W / 2f + sw / 2f) + 16,         lineY, lineW, 1);

        // ── Author credit, bottom-right corner ───────────────
        String credit = "by " + AUTHOR;
        g2.setFont(Theme.ui(Font.PLAIN, 11));
        FontMetrics cfm = g2.getFontMetrics();
        float ctracking = 1.2f;
        float cw = Theme.trackedWidth(cfm, credit, ctracking);

        g2.setColor(Theme.TEXT_DIM);
        Theme.drawTracked(g2, credit, W - 28 - cw, H - 24, ctracking);

        g2.setColor(Theme.alpha(Theme.GOLD, 150));
        g2.fillRect((int) (W - 28 - cw), H - 18, (int) cw, 1);

        g2.dispose();
    }

    // ── Mode card ────────────────────────────────────────────
    private class ModeCard extends JComponent {
        private final int    index;
        private final String title, desc;
        private boolean hovered = false;

        ModeCard(int index, String title, String desc) {
            this.index = index;
            this.title = title;
            this.desc  = desc;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                @Override public void mousePressed(MouseEvent e) {
                    selectedMode = ModeCard.this.index;
                    updateSymbolVisibility();
                    cardAI.repaint();
                    cardHuman.repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);

            int w = getWidth(), h = getHeight();
            boolean sel = (selectedMode == index);

            g2.setColor(sel ? Theme.SURFACE_2
                            : (hovered ? Theme.SURFACE_2 : Theme.SURFACE));
            g2.fill(Theme.round(0, 0, w, h, 10));

            g2.setStroke(new BasicStroke(sel ? 2f : 1f));
            g2.setColor(sel ? Theme.ACCENT
                            : (hovered ? Theme.BORDER_LT : Theme.BORDER));
            g2.draw(Theme.round(1, 1, w - 2, h - 2, 10));

            // Illustrate the mode with the game's own stones
            int tile = 34;
            int ix = 18, iy = 18;
            if (index == 0) {
                Theme.drawStoneTile(g2, true, ix, iy, tile);
            } else {
                Theme.drawStoneTile(g2, true,  ix,             iy, tile);
                Theme.drawStoneTile(g2, false, ix + tile + 8,  iy, tile);
            }

            g2.setFont(Theme.display(Font.BOLD, 17));
            g2.setColor(sel ? Theme.TEXT : Theme.TEXT_DIM);
            g2.drawString(title, 18, iy + tile + 28);

            g2.setFont(Theme.ui(Font.PLAIN, 11));
            g2.setColor(Theme.TEXT_FAINT);
            g2.drawString(desc, 18, iy + tile + 46);

            g2.dispose();
        }
    }

    // ── X / O picker ─────────────────────────────────────────
    private class SymbolTile extends JComponent {
        private final int index;
        private boolean hovered = false;

        SymbolTile(int index) {
            this.index = index;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                @Override public void mousePressed(MouseEvent e) {
                    selectedSymbol = SymbolTile.this.index;
                    tileX.repaint();
                    tileO.repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);

            int w = getWidth(), h = getHeight();
            boolean sel = (selectedSymbol == index);

            g2.setColor(hovered ? Theme.SURFACE_2 : Theme.SURFACE);
            g2.fill(Theme.round(0, 0, w, h, 10));

            g2.setStroke(new BasicStroke(sel ? 2f : 1f));
            g2.setColor(sel ? Theme.ACCENT : Theme.BORDER);
            g2.draw(Theme.round(1, 1, w - 2, h - 2, 10));

            int tile = w - 22;
            Theme.drawStoneTile(g2, index == 0, 11, (h - tile) / 2, tile);

            g2.dispose();
        }
    }

    // ── Difficulty picker ────────────────────────────────────
    private class LevelTile extends JComponent {
        private final int        index;
        private final Difficulty level;
        private boolean hovered = false;

        LevelTile(int index, Difficulty level) {
            this.index = index;
            this.level = level;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                @Override public void mousePressed(MouseEvent e) {
                    selectedLevel = LevelTile.this.index;
                    for (LevelTile lt : levelTiles) lt.repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);

            int w = getWidth(), h = getHeight();
            boolean sel = (selectedLevel == index);

            g2.setColor(hovered ? Theme.SURFACE_2 : Theme.SURFACE);
            g2.fill(Theme.round(0, 0, w, h, 8));

            g2.setStroke(new BasicStroke(sel ? 2f : 1f));
            g2.setColor(sel ? Theme.ACCENT : Theme.BORDER);
            g2.draw(Theme.round(1, 1, w - 2, h - 2, 8));

            g2.setFont(Theme.ui(sel ? Font.BOLD : Font.PLAIN, 12));
            g2.setColor(sel ? Theme.TEXT : Theme.TEXT_DIM);
            Theme.drawCentered(g2, level.label(), w / 2, h / 2);

            g2.dispose();
        }
    }
}
