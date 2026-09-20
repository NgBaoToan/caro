package caroai;

import java.awt.*;

/** Shared depth treatment; all drawing stays inside the control's hit area. */
final class ControlSurface {
    private ControlSurface() {}

    static int choice(Graphics2D g, int w, int h, boolean selected, boolean hovered) {
        int sink = hovered ? 4 : 0;
        Color base = selected ? Theme.mix(Theme.SURFACE_2, Theme.ACCENT, .16f) : Theme.SURFACE_2;
        g.setColor(Theme.alpha(Color.BLACK, hovered ? 100 : 60));
        g.fill(Theme.round(1, 2, w - 2, h - 2, 12));
        g.setPaint(new GradientPaint(0, sink, Theme.mix(base, Color.WHITE, hovered ? .02f : .10f),
                0, h, Theme.mix(base, Color.BLACK, hovered ? .20f : .06f)));
        g.fill(Theme.round(1, 1 + sink, w - 2, h - 6, 12));
        g.setStroke(new BasicStroke(selected || hovered ? 2f : 1f));
        g.setColor(selected ? Theme.ACCENT : hovered ? Theme.BORDER_LT : Theme.BORDER);
        g.draw(Theme.round(2, 2 + sink, w - 4, h - 8, 10));
        g.setColor(Theme.alpha(hovered ? Color.BLACK : Color.WHITE, hovered ? 65 : 45));
        g.drawLine(9, 3 + sink, w - 10, 3 + sink);
        return sink - 2;
    }
}
