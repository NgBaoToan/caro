package caroai;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class HintTest {
    @Test void suggestsWinningMoveForCurrentOWithoutPlayingIt() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<BoardPanel> panelRef = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            Player x = new Player("X","X",false), o = new Player("O","O",false);
            var engine = new GameEngine(x,o);
            for (int i=0;i<4;i++) engine.makeMove(new Coord(i,0),GameEngine.Cell.O);
            engine.setCurrentPlayer(o);
            var panel = new BoardPanel(engine,null,()->{});panel.setSize(800,800);
            panelRef.set(panel);
            panel.addPropertyChangeListener("hintStatus", e -> {
                if (panel.getHintMove()!=null) done.countDown();
            });
            panel.requestHint();
            assertThat(engine.getBoard()).hasSize(4);
            assertThat(engine.getCurrentPlayer()).isSameAs(o);
        });
        assertThat(done.await(5,TimeUnit.SECONDS)).isTrue();
        SwingUtilities.invokeAndWait(() -> {
            assertThat(panelRef.get().getHintMove()).isEqualTo(new Coord(4,0));
            panelRef.get().cancelPendingAI();
            assertThat(panelRef.get().getHintMove()).isNull();
        });
    }

    @Test void cancelBeforeWorkerDeliveryDiscardsHint() throws Exception {
        AtomicReference<BoardPanel> ref = new AtomicReference<>();
        CountDownLatch workerDone = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> {
            var engine = new GameEngine(new Player("X","X",false),new Player("O","O",false));
            var panel = new BoardPanel(engine,null,()->{});ref.set(panel);
            panel.requestHint();panel.cancelPendingAI();
            // Allow the SwingWorker completion queue to drain without depending on a search duration.
            var timer = new javax.swing.Timer(150, e -> workerDone.countDown());
            timer.setRepeats(false);timer.start();
        });
        assertThat(workerDone.await(3,TimeUnit.SECONDS)).isTrue();
        SwingUtilities.invokeAndWait(() -> {
            assertThat(ref.get().getHintMove()).isNull();assertThat(ref.get().getHintStatus()).isEmpty();
        });
    }

    @Test void refusesToSuggestDuringComputerTurn() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var engine = new GameEngine(new Player("AI","X",true),new Player("O","O",false));
            var panel = new BoardPanel(engine,null,()->{});panel.requestHint();
            assertThat(panel.getHintMove()).isNull();assertThat(panel.getHintStatus()).contains("Chờ máy");
            assertThat(engine.getBoard()).isEmpty();
        });
    }
}
