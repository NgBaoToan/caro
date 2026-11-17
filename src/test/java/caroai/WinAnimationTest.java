package caroai;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class WinAnimationTest {
    @Test void undoStopsPendingWinnerAnimation() throws Exception {
        verifyCancellation(BoardPanel::clearGameOver);
    }

    @Test void resettingBoardStopsPendingWinnerAnimation() throws Exception {
        verifyCancellation(BoardPanel::resetState);
    }

    @Test void leavingGameStopsPendingWinnerAnimation() throws Exception {
        verifyCancellation(BoardPanel::cancelPendingAI);
    }

    @Test void removingPanelStopsPendingWinnerAnimation() throws Exception {
        verifyCancellation(BoardPanel::removeNotify);
    }

    private static void verifyCancellation(Consumer<BoardPanel> cancel) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameEngine engine = new GameEngine(new Player("X", "X", false), new Player("O", "O", false));
            for (int i = 0; i < 4; i++) engine.makeMove(new Coord(i, 0), GameEngine.Cell.X);
            BoardPanel panel = new BoardPanel(engine, null, () -> {});
            panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_CLICKED, 0, 0, 180, 20, 1, false));
            assertThat(engine.checkWin(new Coord(4, 0))).isTrue();
            Timer previous = timer(panel);
            try {
                assertThat(previous).isNotNull();
                assertThat(previous.isRunning()).isTrue();
                cancel.accept(panel);
                assertThat(previous.isRunning()).isFalse();
                assertThat(timer(panel)).isNull();
                // Simulate a callback already queued before the cancellation.
                for (int tick = 0; tick < 6; tick++) {
                    for (var listener : previous.getActionListeners()) {
                        listener.actionPerformed(new ActionEvent(previous, ActionEvent.ACTION_PERFORMED, "queued"));
                    }
                }
                assertThat(previous.isRunning()).isFalse();
                assertThat(timer(panel)).isNull();
            } finally {
                if (previous != null) previous.stop();
                panel.cancelPendingAI();
            }
        });
    }

    private static Timer timer(BoardPanel panel) {
        try {
            Field field = BoardPanel.class.getDeclaredField("winBlink");
            field.setAccessible(true);
            return (Timer) field.get(panel);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
}
