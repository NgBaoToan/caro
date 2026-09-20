package caroai;

import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AdvantagePanelTest {
    @Test void desktopEvaluationFollowsWinUndoAndReset() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var engine = new GameEngine(new Player("X","X",false),new Player("O","O",false));
            var panel = new AdvantagePanel();
            panel.updatePosition(engine);
            assertThat(panel.getAccessibleContext().getAccessibleDescription()).contains("X 50%, O 50%");
            for (int i=0;i<5;i++) engine.makeMove(new Coord(i,0),GameEngine.Cell.X);
            panel.updatePosition(engine);
            assertThat(panel.getAccessibleContext().getAccessibleDescription()).contains("X đã thắng", "X 100%");
            engine.undoMove(new Coord(4,0));panel.updatePosition(engine);
            assertThat(panel.getAccessibleContext().getAccessibleDescription()).doesNotContain("đã thắng", "100%");
            for(int i=0;i<4;i++)engine.undoMove(new Coord(i,0));panel.updatePosition(engine);
            assertThat(panel.getAccessibleContext().getAccessibleDescription()).contains("X 50%, O 50%");
        });
    }
}
