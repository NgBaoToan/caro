package caroai;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * Shown when a game ends: a dimmed backdrop and a single result card.
 * It fades in and rises slightly. No confetti.
 */
public class WinOverlayPanel extends JPanel {

    private static final int CARD_W = 400;
    private static final int CARD_H = 306;

    private final String  winnerName;
    private final String  symbol;
    private final boolean humanWon;
    private final boolean vsAI;

    private final Runnable onPlayAgain;
    private final Runnable onMainMenu;

    private final ClassicButton btnPlayAgain, btnMainMenu;
    private boolean buttonsPlaced = false;

    private float progress = 0f;   // 0 -> 1
    private final Timer entrance;

    public WinOverlayPanel(String winnerName, String symbol,
                           boolean humanWon, boolean vsAI,
                           Runnable onPlayAgain, Runnable onMainMenu) {
        this.winnerName  = winnerName;
        this.symbol      = symbol;
        this.humanWon    = humanWon;
        this.vsAI        = vsAI;
        this.onPlayAgain = onPlayAgain;
        this.onMainMenu  = onMainMenu;

        setLayout(null);
        setOpaque(false);

        btnPlayAgain = new ClassicButton("PLAY AGAIN", ClassicButton.Kind.PRIMARY);
        btnPlayAgain.addActionListener(e -> { stop(); onPlayAgain.run(); });

        btnMainMenu = new ClassicButton("Main Menu", ClassicButton.Kind.SECONDARY);
        btnMainMenu.addActionListener(e -> { stop(); onMainMenu.run(); });

        add(btnPlayAgain);
        add(btnMainMenu);
        btnPlayAgain.setVisible(false);
        btnMainMenu.setVisible(false);

        entrance = new Timer(14, e -> {
            progress = Math.min(1f, progress + 0.055f);
            if (progress >= 1f) ((Timer) e.getSource()).stop();
            repaint();
        });
        entrance.start();

        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { placeButtons(); }
        });
    }

    private void stop() {
        entrance.stop();
    }

    private void placeButtons() {
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) return;

        int cardX = (w - CARD_W) / 2;
        int cardY = (h - CARD_H) / 2;

        int pad  = 28;
        int btnW = CARD_W - pad * 2;

        btnPlayAgain.setBounds(cardX + pad, cardY + CARD_H - 112, btnW, 50);
        btnMainMenu.setBounds(cardX + pad, cardY + CARD_H - 54,  btnW, 42);

        btnPlayAgain.setVisible(true);
        btnMainMenu.setVisible(true);
        buttonsPlaced = true;

        revalidate();
        repaint();
    }

    /** Shortcuts: R plays again, Esc returns to the main menu. */
    public void attachKeyBindings(JRootPane rootPane) {
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("R"), "playAgain");
        rootPane.getActionMap().put("playAgain", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { stop(); onPlayAgain.run(); }
        });

        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ESCAPE"), "mainMenu");
        rootPane.getActionMap().put("mainMenu", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { stop(); onMainMenu.run(); }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);

        int w = getWidth(), h = getHeight();

        // Dimmed backdrop
        g2.setColor(new Color(0, 0, 0, (int) (165 * progress)));
        g2.fillRect(0, 0, w, h);

        // Result card, rising into place
        int cardX = (w - CARD_W) / 2;
        int cardY = (h - CARD_H) / 2 + (int) ((1f - ease(progress)) * 26);

        g2.setColor(new Color(0, 0, 0, (int) (70 * progress)));
        g2.fill(Theme.round(cardX, cardY + 8, CARD_W, CARD_H, 14));

        g2.setColor(Theme.SURFACE);
        g2.fill(Theme.round(cardX, cardY, CARD_W, CARD_H, 14));

        g2.setColor(Theme.BORDER);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(Theme.round(cardX + 0.5, cardY + 0.5, CARD_W - 1, CARD_H - 1, 14));

        // Top band: green on a win, bronze on a loss
        boolean celebrate = humanWon || !vsAI;
        g2.setColor(celebrate ? Theme.ACCENT : Theme.GOLD);
        g2.fill(Theme.round(cardX, cardY, CARD_W, 4, 3));

        int cx = cardX + CARD_W / 2;

        // The winning stone
        int tile = 58;
        Theme.drawStoneTile(g2, symbol.equals("X"), cx - tile / 2, cardY + 30, tile);

        String headline;
        if (!vsAI)         headline = "VICTORY";
        else if (humanWon) headline = "YOU WIN";
        else               headline = "YOU LOSE";

        g2.setFont(Theme.display(Font.BOLD, 30));
        FontMetrics fm = g2.getFontMetrics();
        float tracking = 3f;
        float tw = Theme.trackedWidth(fm, headline, tracking);
        g2.setColor(Theme.TEXT);
        Theme.drawTracked(g2, headline, cx - tw / 2f, cardY + 30 + tile + 42, tracking);

        g2.setFont(Theme.ui(Font.PLAIN, 12));
        g2.setColor(Theme.TEXT_FAINT);
        String sub = vsAI ? (winnerName + " won this round")
                          : (winnerName + " connected five");
        Theme.drawCentered(g2, sub, cx, cardY + 30 + tile + 66);

        g2.dispose();

        if (!buttonsPlaced) placeButtons();
    }

    private float ease(float t) {
        return 1f - (float) Math.pow(1f - t, 3);
    }
}
