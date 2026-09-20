package caroai;

import java.awt.*;
import java.util.Map;
import javax.swing.*;

/** Desktop view of the same evaluation used by the web edition. */
final class AdvantagePanel extends JComponent {
    private Map<Coord, GameEngine.Cell> previous;
    private PositionEvaluation evaluation = new PositionEvaluation(50, "shapes");
    private String status = "Thế cờ cân bằng";

    AdvantagePanel() {
        setToolTipText("Lợi thế ước tính từ thế cờ, không phải xác suất thắng chính xác.");
    }

    void updatePosition(GameEngine engine) {
        Map<Coord, GameEngine.Cell> board = engine.getBoard();
        if (board.equals(previous)) return;
        previous = Map.copyOf(board);
        GameEngine.Cell winner = null;
        for (var entry : board.entrySet()) {
            if (engine.checkWin(entry.getKey())) { winner = entry.getValue(); break; }
        }
        boolean draw = winner == null && board.size() == engine.getSize() * engine.getSize();
        evaluation = PositionEvaluation.assess(previous, engine.getSize(),
                board.size() % 2 == 0 ? GameEngine.Cell.X : GameEngine.Cell.O, winner, draw);
        int x = evaluation.xPercent();
        status = winner != null ? winner + " đã thắng" : draw ? "Hòa cờ"
                : Math.abs(x - 50) < 5 ? "Thế cờ cân bằng"
                : (x > 50 ? "O" : "X") + " đang bất lợi";
        getAccessibleContext().setAccessibleDescription(status + ", X " + x + "%, O " + (100-x) + "%");
        repaint();
    }

    @Override public javax.accessibility.AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) accessibleContext = new AccessibleJComponent() {};
        return accessibleContext;
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create(); Theme.antialias(g);
        int w = getWidth(), xShare = evaluation.xPercent();
        g.setColor(Theme.SURFACE_2); g.fill(Theme.round(0,0,w,getHeight(),10));
        g.setFont(Theme.ui(Font.BOLD,12)); g.setColor(Theme.TEXT);
        g.drawString("X  " + xShare + "%",10,20);
        String o = "O  " + (100-xShare) + "%";
        g.drawString(o,w-10-g.getFontMetrics().stringWidth(o),20);
        Shape bar = Theme.round(10,29,w-20,17,5);
        g.setColor(new Color(0xC78343));g.fill(bar);
        g.setClip(bar);g.setColor(new Color(0xEEF3FA));
        g.fillRect(10,29,Math.round((w-20)*xShare/100f),17);g.setClip(null);
        g.setColor(Theme.BORDER_LT);g.draw(bar);
        g.setColor(Theme.TEXT);g.setFont(Theme.ui(Font.BOLD,11));
        Theme.drawCentered(g,status,w/2,62);
        g.setColor(Theme.TEXT_DIM);g.setFont(Theme.ui(Font.PLAIN,9));
        Theme.drawCentered(g,"Lợi thế ước tính · Không phải xác suất",w/2,81);
        g.dispose();
    }
}
