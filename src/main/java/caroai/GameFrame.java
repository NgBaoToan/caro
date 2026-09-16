package caroai;

import javax.swing.*;
import java.awt.*;

/**
 * Main window.
 * CardLayout:  "menu" -> StartMenuPanel   |   "game" -> BoardPanel + MenuPanel
 * Overlay:     WinOverlayPanel is added to the layered pane when a game ends.
 */
public class GameFrame extends JFrame {

    private static final String CARD_MENU = "menu";
    private static final String CARD_GAME = "game";

    private static final int BOARD_SIZE = GameEngine.DEFAULT_SIZE;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel     rootPanel  = new JPanel(cardLayout);

    private StartMenuPanel startMenuPanel;
    private JPanel         gameContainer;
    private MenuPanel      menuPanel;
    private GameEngine     engine;
    private BoardPanel     boardPanel;
    private AI             ai;

    private final MoveHistory history = new MoveHistory();

    private GameEngine.Cell playerSymbol = GameEngine.Cell.X;
    private GameEngine.Cell aiSymbol     = GameEngine.Cell.O;
    private boolean         vsAI         = true;
    private Difficulty      difficulty   = Difficulty.MEDIUM;

    private WinOverlayPanel currentOverlay;

    // Who holds X and who holds O this game. Used to recompute whose turn it is
    // from the number of moves played (X ALWAYS opens).
    private Player xPlayer, oPlayer;

    public GameFrame() {
        setTitle("Caro - Five in a Row");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 600));
        setSize(1200, 900);
        setLocationRelativeTo(null);

        getContentPane().setBackground(Theme.BG);
        rootPanel.setBackground(Theme.BG);

        startMenuPanel = new StartMenuPanel(this::onStartGame);
        gameContainer  = new JPanel(new BorderLayout());
        gameContainer.setBackground(Theme.BG);

        rootPanel.add(startMenuPanel, CARD_MENU);
        rootPanel.add(gameContainer,  CARD_GAME);
        add(rootPanel);

        cardLayout.show(rootPanel, CARD_MENU);
        setVisible(true);
    }

    // ── Callback from StartMenuPanel ─────────────────────────
    private void onStartGame() {
        vsAI = startMenuPanel.isVsAI();
        difficulty = startMenuPanel.getDifficulty();
        if (vsAI) {
            playerSymbol = startMenuPanel.playerChoseX() ? GameEngine.Cell.X : GameEngine.Cell.O;
            aiSymbol     = GameEngine.opponentOf(playerSymbol);
        }
        startGame();
        cardLayout.show(rootPanel, CARD_GAME);
        if (boardPanel != null) boardPanel.requestFocusInWindow();
    }

    // ── New game ─────────────────────────────────────────────
    public void startGame() {
        removeOverlay();
        // Drop any AI move still pending from the previous game (for instance
        // when Play Again is pressed while the AI is thinking).
        if (boardPanel != null) boardPanel.cancelPendingAI();

        Player human, opponent;
        if (vsAI) {
            human    = new Player("You",      playerSymbol.toString(), false);
            opponent = new Player("Computer", aiSymbol.toString(),     true);
            ai       = new AI(BOARD_SIZE, GameEngine.WIN_LENGTH, difficulty);
        } else {
            playerSymbol = GameEngine.Cell.X;
            aiSymbol     = GameEngine.Cell.O;
            human        = new Player("Player 1", "X", false);
            opponent     = new Player("Player 2", "O", false);
            ai           = null;
        }

        engine = new GameEngine(human, opponent, BOARD_SIZE);
        engine.setPlayerSymbol(playerSymbol);
        engine.setAISymbol(aiSymbol);

        if (playerSymbol == GameEngine.Cell.X) { xPlayer = human;    oPlayer = opponent; }
        else                                   { xPlayer = opponent; oPlayer = human;    }

        history.clear();
        // Rule: X always opens. Derive the turn from history instead of setting
        // it by hand for each case.
        syncTurnFromHistory();

        menuPanel  = new MenuPanel(this);
        boardPanel = new BoardPanel(engine, ai) {
            @Override
            public void repaint() {
                super.repaint();
                if (menuPanel != null && engine != null)
                    menuPanel.updateCurrentPlayer(engine.getCurrentPlayer().getSymbol());
            }
        };

        gameContainer.removeAll();
        gameContainer.add(menuPanel,  BorderLayout.WEST);
        gameContainer.add(boardPanel, BorderLayout.CENTER);
        gameContainer.revalidate();
        gameContainer.repaint();
        boardPanel.requestFocusInWindow();

        // The AI opens when the human picked O — same path as any other AI move.
        boardPanel.maybeAIMove();
    }

    // ── Result overlay ───────────────────────────────────────
    public void showWinOverlay(Player winner) {
        boolean humanWon = !winner.isAI();

        currentOverlay = new WinOverlayPanel(
            winner.getName(),
            winner.getSymbol(),
            humanWon,
            vsAI,
            this::startGame,    // Play again
            this::goToMainMenu  // Main menu
        );

        JLayeredPane layered = getLayeredPane();
        currentOverlay.setBounds(0, 0, layered.getWidth(), layered.getHeight());
        layered.add(currentOverlay, JLayeredPane.POPUP_LAYER);
        layered.revalidate();
        layered.repaint();

        currentOverlay.attachKeyBindings(getRootPane());
    }

    private void removeOverlay() {
        if (currentOverlay != null) {
            getLayeredPane().remove(currentOverlay);
            getLayeredPane().repaint();
            currentOverlay = null;
        }
    }

    // ── Back to the main menu ────────────────────────────────
    public void goToMainMenu() {
        removeOverlay();
        if (boardPanel != null) boardPanel.cancelPendingAI();
        rootPanel.remove(startMenuPanel);
        startMenuPanel = new StartMenuPanel(this::onStartGame);
        rootPanel.add(startMenuPanel, CARD_MENU);
        cardLayout.show(rootPanel, CARD_MENU);
    }

    // ── Undo ─────────────────────────────────────────────────
    /**
     * Takes back the human's LAST move together with every AI reply that came
     * after it.
     *
     * Because X always opens, move number i belongs to the human exactly when
     * i % 2 == firstHumanIndex (0 if the human holds X, 1 if O). Find the index
     * of the human's last move and rewind to it — so the number of stones to
     * remove is computed, not hard-coded to two.
     */
    public void undoMove() {
        if (engine == null || boardPanel == null) return;

        // Cancel the AI's pending search, or its move would land on the rewound board.
        boardPanel.cancelPendingAI();

        int firstHumanIndex = (vsAI && playerSymbol == GameEngine.Cell.O) ? 1 : 0;
        int target = history.lastHumanIndex(vsAI, firstHumanIndex);

        // The human has not played yet (picked O, AI just opened) — nothing to undo.
        if (target < 0) { syncTurnFromHistory(); boardPanel.repaint(); return; }

        for (Coord c : history.rewindTo(target)) {
            engine.undoMove(c);
        }

        removeOverlay();
        boardPanel.clearGameOver();
        boardPanel.setLastMove(history.last());
        syncTurnFromHistory();
        boardPanel.repaint();

        // Safety net: if the turn lands on the AI after rewinding, wake it up
        // instead of leaving the board frozen.
        boardPanel.maybeAIMove();
    }

    /** X always opens: an even number of moves means X is to play. */
    private void syncTurnFromHistory() {
        if (engine == null || xPlayer == null) return;
        engine.setCurrentPlayer(history.xToMove() ? xPlayer : oPlayer);
    }

    public void restartGame()        { startGame(); }
    public void recordMove(Coord c)  { history.push(c); }

    /** Difficulty of the game in progress, shown in the side rail. */
    public Difficulty getDifficulty() { return vsAI ? difficulty : null; }

    /** Called by the board while a background search is running. */
    public void setThinking(boolean thinking) {
        if (menuPanel != null) menuPanel.setThinking(thinking);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameFrame::new);
    }
}
