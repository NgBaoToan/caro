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
    private static final int EDGE     = 4;   // height of the bottom lip

    private final String label;
    private final String icon;        // may be null
    private final Kind   kind;

    private boolean hovered = false;
    private boolean armed   = false;
    private float   hoverAnim = 0f;

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

        Timer anim = new Timer(16, e -> {
            float target = hovered ? 1f : 0f;
            float next = hoverAnim + (target - hoverAnim) * 0.22f;
            if (Math.abs(next - hoverAnim) > 0.001f) {
                hoverAnim = next;
                repaint();
            }
        });
        anim.start();

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
            @Override public void mouseExited(MouseEvent e)  { hovered = false; armed = false; repaint(); }
            @Override public void mousePressed(MouseEvent e) { armed = true;    repaint(); }
            @Override public void mouseReleased(MouseEvent e) {
                boolean fire = armed && hovered;
                armed = false;
                repaint();
                if (fire) fire();
            }
        });
    }

    private void fire() {
        ActionEvent ev = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, label);
        for (ActionListener l : new ArrayList<>(listeners)) l.actionPerformed(ev);
    }

    public void addActionListener(ActionListener l) { listeners.add(l); }

    public void setFontSize(int size) { this.fontSize = size; repaint(); }

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
        int sink = armed ? EDGE : 0;          // how far the face sinks

        Color face, edge, text;
        switch (kind) {
            case PRIMARY:
                face = Theme.mix(Theme.ACCENT, Theme.ACCENT_HOVER, hoverAnim);
                edge = Theme.ACCENT_DARK;
                text = new Color(0xF6, 0xFA, 0xF0);
                break;
            case SECONDARY:
                face = Theme.mix(Theme.SURFACE_2, Theme.SURFACE_3, hoverAnim);
                edge = new Color(0x26, 0x24, 0x21);
                text = Theme.mix(Theme.TEXT_DIM, Theme.TEXT, hoverAnim);
                break;
            default: // QUIET
                face = Theme.alpha(Theme.SURFACE_2, (int) (90 * hoverAnim));
                edge = null;
                text = Theme.mix(Theme.TEXT_FAINT, Theme.TEXT_DIM, hoverAnim);
                break;
        }

        // Bottom lip
        if (edge != null) {
            g2.setColor(edge);
            g2.fill(Theme.round(0, 0, w, h, ARC));
        }

        // Button face
        g2.setColor(face);
        g2.fill(Theme.round(0, sink, w, h - EDGE, ARC));

        if (kind == Kind.QUIET) {
            g2.setColor(Theme.alpha(Theme.BORDER, 160));
            g2.setStroke(new BasicStroke(1f));
            g2.draw(Theme.round(0.5, sink + 0.5, w - 1, h - EDGE - 1, ARC));
        }

        // Content
        int centerY = sink + (h - EDGE) / 2;
        g2.setColor(text);

        if (icon == null) {
            g2.setFont(Theme.ui(Font.BOLD, fontSize));
            Theme.drawCentered(g2, label, w / 2, centerY);
        } else {
            // Icon on the left, label right after it
            g2.setFont(Theme.ui(Font.PLAIN, fontSize + 3));
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
