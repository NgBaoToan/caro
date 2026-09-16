package caroai;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.HashSet;
import java.util.Set;

/**
 * The shared design system for the whole interface.
 *
 * The idea: a classic parchment board in a dark wooden frame, a dark control
 * rail, and one green accent reserved for the primary action.
 * Changing the constants in this file re-tones the entire game.
 */
public final class Theme {

    private Theme() {}

    // ── Backgrounds & surfaces (dark wood) ───────────────────
    public static final Color BG         = new Color(0x26, 0x24, 0x21);
    public static final Color SURFACE    = new Color(0x30, 0x2E, 0x2B);
    public static final Color SURFACE_2  = new Color(0x3A, 0x38, 0x35);
    public static final Color SURFACE_3  = new Color(0x46, 0x43, 0x3F);
    public static final Color BORDER     = new Color(0x4A, 0x47, 0x43);
    public static final Color BORDER_LT  = new Color(0x5C, 0x58, 0x53);

    // ── Text ─────────────────────────────────────────────────
    public static final Color TEXT       = new Color(0xED, 0xEC, 0xEA);
    public static final Color TEXT_DIM   = new Color(0xA8, 0xA3, 0x9C);
    public static final Color TEXT_FAINT = new Color(0x7A, 0x75, 0x6E);

    // ── Accents ──────────────────────────────────────────────
    public static final Color ACCENT       = new Color(0x81, 0xB6, 0x4C);
    public static final Color ACCENT_HOVER = new Color(0x95, 0xC9, 0x5E);
    public static final Color ACCENT_DARK  = new Color(0x5D, 0x87, 0x34);
    public static final Color GOLD         = new Color(0xC8, 0xA2, 0x4A);

    // ── Board (parchment + ink) ──────────────────────────────
    public static final Color BOARD_BG     = new Color(0xEB, 0xE6, 0xD3);
    public static final Color BOARD_BG_ALT = new Color(0xE7, 0xE2, 0xCD);
    public static final Color BOARD_LINE   = new Color(0xB4, 0xA8, 0x8C);
    public static final Color BOARD_STAR   = new Color(0x8C, 0x7E, 0x62);
    public static final Color BOARD_FRAME  = new Color(0x3B, 0x2F, 0x25);

    // ── Stones: classic black ink vs vermilion ───────────────
    public static final Color STONE_X      = new Color(0x2E, 0x2B, 0x27);
    public static final Color STONE_O      = new Color(0x8C, 0x3A, 0x32);

    // ── Board markers ────────────────────────────────────────
    public static final Color LAST_MOVE    = new Color(0xF2, 0xDE, 0x76, 130);
    public static final Color WIN_CELL     = new Color(0x81, 0xB6, 0x4C, 110);
    public static final Color WIN_LINE     = new Color(0x5D, 0x87, 0x34);

    // ── Font ─────────────────────────────────────────────────
    private static Set<String> families;

    private static synchronized boolean hasFamily(String name) {
        if (families == null) {
            families = new HashSet<>();
            for (String f : GraphicsEnvironment.getLocalGraphicsEnvironment()
                                               .getAvailableFontFamilyNames()) {
                families.add(f);
            }
        }
        return families.contains(name);
    }

    private static Font pick(int style, int size, String[] preferred, String fallback) {
        for (String name : preferred) {
            if (hasFamily(name)) return new Font(name, style, size);
        }
        return new Font(fallback, style, size);
    }

    private static final String[] UI_FONTS = {
        "Segoe UI", "Helvetica Neue", "Roboto", "Noto Sans", "DejaVu Sans", "Arial"
    };
    private static final String[] DISPLAY_FONTS = {
        "Georgia", "Palatino Linotype", "Book Antiqua", "Cambria",
        "Noto Serif", "DejaVu Serif", "Times New Roman"
    };

    /** Regular interface font (buttons, labels). */
    public static Font ui(int style, int size) {
        return pick(style, size, UI_FONTS, Font.SANS_SERIF);
    }

    /** Serif display font for headings — the classic note. */
    public static Font display(int style, int size) {
        return pick(style, size, DISPLAY_FONTS, Font.SERIF);
    }

    // ── Drawing helpers ──────────────────────────────────────

    public static void antialias(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                            RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    public static RoundRectangle2D.Float round(double x, double y,
                                               double w, double h, double arc) {
        return new RoundRectangle2D.Float((float) x, (float) y,
                                          (float) w, (float) h,
                                          (float) arc, (float) arc);
    }

    public static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    public static Color mix(Color a, Color b, float t) {
        if (t < 0f) t = 0f;
        if (t > 1f) t = 1f;
        return new Color(
            Math.round(a.getRed()   + (b.getRed()   - a.getRed())   * t),
            Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            Math.round(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t),
            Math.round(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t)
        );
    }

    /** Draws text with letter spacing (tracking) — used for caps headings. */
    public static void drawTracked(Graphics2D g2, String text,
                                   float x, float y, float tracking) {
        FontMetrics fm = g2.getFontMetrics();
        float cx = x;
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            g2.drawString(ch, cx, y);
            cx += fm.stringWidth(ch) + tracking;
        }
    }

    public static float trackedWidth(FontMetrics fm, String text, float tracking) {
        if (text.isEmpty()) return 0f;
        return fm.stringWidth(text) + tracking * (text.length() - 1);
    }

    public static void drawCentered(Graphics2D g2, String text, int cx, int cy) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, cx - fm.stringWidth(text) / 2f,
                            cy + (fm.getAscent() - fm.getDescent()) / 2f);
    }

    // ── Stones ───────────────────────────────────────────────

    /**
     * Draws one stone. Shared by the board, the side rail and the result card
     * so the strokes always match.
     *
     * @param isX   true = an X stone (black ink), false = an O stone (vermilion)
     * @param cx,cy centre of the stone
     * @param r     radius of the stone
     * @param alpha opacity 0..1 (used for the hover preview)
     */
    public static void drawStone(Graphics2D g2, boolean isX,
                                 double cx, double cy, double r, float alpha) {
        Composite old = g2.getComposite();
        Stroke oldStroke = g2.getStroke();
        if (alpha < 1f) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        }

        Color color = isX ? STONE_X : STONE_O;

        // A very light drop shadow gives the stone some depth
        if (alpha >= 1f) {
            g2.setColor(new Color(0, 0, 0, 28));
            if (isX) {
                double k = r * 0.60, sw = r * 0.40, off = r * 0.09;
                g2.setStroke(new BasicStroke((float) sw, BasicStroke.CAP_ROUND,
                                             BasicStroke.JOIN_ROUND));
                g2.draw(new java.awt.geom.Line2D.Double(cx - k, cy - k + off,
                                                        cx + k, cy + k + off));
                g2.draw(new java.awt.geom.Line2D.Double(cx + k, cy - k + off,
                                                        cx - k, cy + k + off));
            } else {
                double rr = r * 0.66, sw = r * 0.38, off = r * 0.09;
                g2.setStroke(new BasicStroke((float) sw));
                g2.draw(new Ellipse2D.Double(cx - rr, cy - rr + off, rr * 2, rr * 2));
            }
        }

        g2.setColor(color);
        if (isX) {
            double k = r * 0.60, sw = r * 0.40;
            g2.setStroke(new BasicStroke((float) sw, BasicStroke.CAP_ROUND,
                                         BasicStroke.JOIN_ROUND));
            g2.draw(new java.awt.geom.Line2D.Double(cx - k, cy - k, cx + k, cy + k));
            g2.draw(new java.awt.geom.Line2D.Double(cx + k, cy - k, cx - k, cy + k));
        } else {
            double rr = r * 0.66, sw = r * 0.38;
            g2.setStroke(new BasicStroke((float) sw));
            g2.draw(new Ellipse2D.Double(cx - rr, cy - rr, rr * 2, rr * 2));
        }

        g2.setStroke(oldStroke);
        g2.setComposite(old);
    }

    /**
     * Draws a stone on a small parchment tile — used in the side rail and the
     * result card, where a dark background would swallow the black X.
     */
    public static void drawStoneTile(Graphics2D g2, boolean isX,
                                     int x, int y, int size) {
        g2.setColor(new Color(0, 0, 0, 40));
        g2.fill(round(x + 1, y + 3, size, size, size * 0.24));

        g2.setColor(BOARD_BG);
        g2.fill(round(x, y, size, size, size * 0.24));

        g2.setColor(alpha(BOARD_FRAME, 70));
        g2.setStroke(new BasicStroke(1f));
        g2.draw(round(x + 0.5, y + 0.5, size - 1, size - 1, size * 0.24));

        drawStone(g2, isX, x + size / 2.0, y + size / 2.0, size * 0.30, 1f);
    }
}
