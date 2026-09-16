package caroai;

import javax.swing.*;
import java.awt.*;

/**
 * The control rail to the left of the board: wordmark, turn indicator,
 * action buttons, keyboard hints and the author credit.
 */
public class MenuPanel extends JPanel {

    private static final int WIDTH = 248;

    private final TurnCard turnCard;

    public MenuPanel(GameFrame frame) {
        setLayout(null);
        setOpaque(true);
        setBackground(Theme.SURFACE);
        setPreferredSize(new Dimension(WIDTH, 0));
        setMinimumSize(new Dimension(WIDTH, 0));

        int pad      = 22;
        int contentW = WIDTH - pad * 2;
        int y        = 34;

        // The wordmark is painted in paintComponent; just reserve the space.
        y += 62;

        turnCard = new TurnCard();
        turnCard.setBounds(pad, y, contentW, 78);
        add(turnCard);
        y += 78 + 28;

        String[][] buttons = {
            { "\u21B6", "Undo",     "undo"    },
            { "\u21BB", "New Game", "restart" },
        };
        for (String[] b : buttons) {
            ClassicButton btn = new ClassicButton(b[0], b[1], ClassicButton.Kind.SECONDARY);
            btn.setBounds(pad, y, contentW, 46);
            String action = b[2];
            btn.addActionListener(e -> {
                if (action.equals("undo")) frame.undoMove();
                else                       frame.restartGame();
            });
            add(btn);
            y += 46 + 10;
        }

        y += 6;
        ClassicButton menuBtn = new ClassicButton("\u2190", "Main Menu",
                                                  ClassicButton.Kind.QUIET);
        menuBtn.setBounds(pad, y, contentW, 42);
        menuBtn.addActionListener(e -> frame.goToMainMenu());
        add(menuBtn);
        y += 42 + 8;

        ClassicButton exitBtn = new ClassicButton("\u2715", "Exit",
                                                  ClassicButton.Kind.QUIET);
        exitBtn.setBounds(pad, y, contentW, 42);
        exitBtn.addActionListener(e -> System.exit(0));
        add(exitBtn);
    }

    /** Called by GameFrame every time the board repaints. */
    public void updateCurrentPlayer(String symbol) {
        turnCard.setSymbol(symbol);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);

        int w = getWidth(), h = getHeight();

        g2.setColor(Theme.SURFACE);
        g2.fillRect(0, 0, w, h);

        // Divider against the board
        g2.setColor(new Color(0, 0, 0, 90));
        g2.fillRect(w - 2, 0, 2, h);

        int pad = 22;

        // ── Wordmark ─────────────────────────────────────────
        g2.setFont(Theme.display(Font.BOLD, 30));
        g2.setColor(Theme.TEXT);
        Theme.drawTracked(g2, "CARO", pad, 64, 3.5f);

        g2.setFont(Theme.ui(Font.PLAIN, 10));
        g2.setColor(Theme.TEXT_FAINT);
        Theme.drawTracked(g2, "CLASSIC GOMOKU", pad, 82, 1.6f);

        g2.setColor(Theme.GOLD);
        g2.fillRect(pad, 92, 34, 2);

        // ── Hints and credit at the bottom ───────────────────
        g2.setFont(Theme.ui(Font.PLAIN, 10));
        g2.setColor(Theme.TEXT_FAINT);
        g2.drawString("W A S D  \u2014  pan the board", pad, h - 52);
        g2.drawString("X always moves first",           pad, h - 36);

        g2.setColor(Theme.alpha(Theme.GOLD, 150));
        g2.fillRect(pad, h - 26, 18, 1);

        g2.setFont(Theme.ui(Font.PLAIN, 10));
        g2.setColor(Theme.TEXT_DIM);
        Theme.drawTracked(g2, "BY NBAOTOAN", pad, h - 12, 1.2f);

        g2.dispose();
    }

    // ── Turn indicator ───────────────────────────────────────
    static class TurnCard extends JComponent {

        private String symbol = "X";
        private float  glow   = 0f;

        TurnCard() {
            Timer t = new Timer(40, e -> {
                glow = (glow + 0.045f) % (2f * (float) Math.PI);
                repaint();
            });
            t.start();
        }

        void setSymbol(String s) {
            if (s != null && !s.equals(symbol)) {
                symbol = s;
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);

            int w = getWidth(), h = getHeight();
            boolean isX = symbol.equals("X");

            g2.setColor(Theme.SURFACE_2);
            g2.fill(Theme.round(0, 0, w, h, 10));

            g2.setColor(Theme.BORDER);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(Theme.round(0.5, 0.5, w - 1, h - 1, 10));

            // Accent bar that breathes, so the game reads as live
            float pulse = 0.55f + 0.45f * (float) Math.sin(glow);
            g2.setColor(Theme.alpha(Theme.ACCENT, (int) (120 + 110 * pulse)));
            g2.fill(Theme.round(0, 12, 3, h - 24, 2));

            int tile = 46;
            Theme.drawStoneTile(g2, isX, 18, (h - tile) / 2, tile);

            int textX = 18 + tile + 16;

            g2.setFont(Theme.ui(Font.PLAIN, 10));
            g2.setColor(Theme.TEXT_FAINT);
            Theme.drawTracked(g2, "NOW PLAYING", textX, h / 2f - 8, 1.4f);

            g2.setFont(Theme.display(Font.BOLD, 21));
            g2.setColor(Theme.TEXT);
            g2.drawString(symbol + " to move", textX, h / 2f + 16);

            g2.dispose();
        }
    }
}
