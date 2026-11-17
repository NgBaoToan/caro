package caroai;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The one button style used across the game.
 *
 * A rounded block with a solid lip along its bottom edge; pressing it sinks the
 * face by exactly the height of that lip, so it reads as a physical press
 * rather than a colour change.
 */
public class ClassicButton extends JComponent {

    public enum Kind {
        PRIMARY,    // primary action — green
        SECONDARY,  // secondary action — grey face
        QUIET       // outline only, used for Exit
    }

    private static final int ARC      = 8;
    private static final int EDGE     = 7;   // height of the bottom lip

    private String label;
    private final String icon;        // may be null
    private final Kind   kind;

    private boolean hovered = false;
    private boolean armed   = false;
    private float   hoverAnim = 0f;
    private final Timer anim;

    private int fontSize = 14;

    private final List<ActionListener> listeners = new ArrayList<>();

    public ClassicButton(String label, Kind kind) {
        this(null, label, kind);
    }

    public ClassicButton(String icon, String label, Kind kind) {
        this.icon  = icon;
        this.label = label;
        this.kind  = kind;

        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        anim = new Timer(16, e -> {
            float target = hovered ? 1f : 0f;
            float next = hoverAnim + (target - hoverAnim) * 0.22f;
            if (Math.abs(target - next) < 0.005f) {
                hoverAnim = target;
                ((Timer) e.getSource()).stop();
            } else {
                hoverAnim = next;
            }
            repaint();
        });

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hovered = isEnabled(); anim.start(); }
            @Override public void mouseExited(MouseEvent e)  { hovered = false; armed = false; anim.start(); }
            @Override public void mousePressed(MouseEvent e) {
                armed = isEnabled() && SwingUtilities.isLeftMouseButton(e); repaint();
            }
            @Override public void mouseReleased(MouseEvent e) {
                boolean fire = isEnabled() && armed && contains(e.getPoint()) && SwingUtilities.isLeftMouseButton(e);
                armed = false;
                repaint();
                if (fire) fire();
            }
        });
    }

    @Override public void removeNotify() {
        anim.stop(); hovered = false; armed = false; hoverAnim = 0;
        super.removeNotify();
    }

    private void fire() {
        ActionEvent ev = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, label);
        for (ActionListener l : new ArrayList<>(listeners)) l.actionPerformed(ev);
    }

    public void addActionListener(ActionListener l) { listeners.add(l); }

    public void setFontSize(int size) { this.fontSize = size; repaint(); }

    public void setLabel(String label) { this.label = label; repaint(); }

    public void setFixedSize(int w, int h) {
        Dimension d = new Dimension(w, h);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);

        int w = getWidth(), h = getHeight();
        int sink = armed ? EDGE : Math.round(hoverAnim * 4);

        Color face, edge, text;
        switch (kind) {
            case PRIMARY:
                face = Theme.mix(Theme.ACCENT, Theme.ACCENT_HOVER, hoverAnim);
                edge = Theme.ACCENT_DARK;
                text = Theme.mode() == Theme.Mode.DARK ? Theme.BG : Color.WHITE;
                break;
            case SECONDARY:
                face = Theme.mix(Theme.SURFACE_2, Theme.SURFACE_3, hoverAnim);
                edge = Theme.BORDER;
                text = Theme.TEXT;
                break;
            default: // QUIET
                face = Theme.mix(Theme.SURFACE_2, Theme.SURFACE_3, hoverAnim);
                edge = Theme.mix(Theme.SURFACE_2, Color.BLACK, .35f);
                text = Theme.TEXT_DIM;
                break;
        }

        // Inset glow, cast shadow and a deeper base make the face visibly raised.
        g2.setColor(Theme.alpha(Theme.ACCENT, (int) (75 * hoverAnim)));
        g2.fill(Theme.round(0, 0, w, h, ARC + 4));
        g2.setColor(Theme.alpha(Color.BLACK, 90));
        g2.fill(Theme.round(2, 4, w - 4, h - 4, ARC));
        if (edge != null) {
            g2.setColor(edge);
            g2.fill(Theme.round(2, 2, w - 4, h - 4, ARC));
        }

        // Button face
        g2.setPaint(new GradientPaint(0, sink, Theme.mix(face, Color.WHITE, .18f),
                0, h, Theme.mix(face, Color.BLACK, .12f)));
        g2.fill(Theme.round(2, 1 + sink, w - 4, h - EDGE - 3, ARC));
        g2.setColor(Theme.mix(Theme.BORDER_LT, Theme.ACCENT, hoverAnim));
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(Theme.round(2.5, 1.5 + sink, w - 5, h - EDGE - 4, ARC));
        g2.setColor(Theme.alpha(Color.WHITE, armed ? 35 : 95));
        g2.drawLine(9, 3 + sink, w - 10, 3 + sink);

        // Content
        int centerY = sink + (h - EDGE) / 2;
        g2.setColor(text);

        if (icon == null) {
            g2.setFont(Theme.ui(Font.BOLD, fontSize));
            Theme.drawCentered(g2, label, w / 2, centerY);
        } else {
            // Icon on the left, label right after it
            // Logical fonts provide platform glyph fallback for arrows and symbols.
            g2.setFont(new Font(Font.DIALOG, Font.PLAIN, fontSize + 3));
            FontMetrics ifm = g2.getFontMetrics();
            int iconX = 20;
            g2.drawString(icon, iconX,
                          centerY + (ifm.getAscent() - ifm.getDescent()) / 2f);

            g2.setFont(Theme.ui(Font.BOLD, fontSize));
            FontMetrics lfm = g2.getFontMetrics();
            g2.drawString(label, iconX + ifm.stringWidth(icon) + 14,
                          centerY + (lfm.getAscent() - lfm.getDescent()) / 2f);
        }

        g2.dispose();
    }
}
