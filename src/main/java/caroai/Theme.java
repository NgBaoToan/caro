package caroai;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.HashSet;
import java.util.Set;
import java.util.prefs.Preferences;

/**
 * The shared design system for the whole interface.
 *
 * Shared colors for the light parchment and dark slate appearances.
 */
public final class Theme {

    private Theme() {}

    public enum Mode { DARK, LIGHT }
    public enum StoneStyle {
        ROUND("Bo tròn"), SHARP("Góc cạnh"), NEON("Neon"), BADGE("Huy hiệu");
        private final String label;
        StoneStyle(String label) { this.label = label; }
        public String label() { return label; }
    }
    private static final Preferences PREFS = Preferences.userNodeForPackage(Theme.class);
    private static Mode mode = "LIGHT".equals(PREFS.get("appearance", "DARK")) ? Mode.LIGHT : Mode.DARK;
    private static StoneStyle stoneStyle = readStoneStyle();

    private static StoneStyle readStoneStyle() {
        try { return StoneStyle.valueOf(PREFS.get("stoneStyle", "ROUND")); }
        catch (IllegalArgumentException e) { return StoneStyle.ROUND; }
    }

    public static StoneStyle stoneStyle() { return stoneStyle; }

    public static void setStoneStyle(StoneStyle value) {
        stoneStyle = java.util.Objects.requireNonNull(value);
        PREFS.put("stoneStyle", value.name());
    }

    public static Mode mode() { return mode; }

    public static void setMode(Mode value) {
        mode = java.util.Objects.requireNonNull(value);
        applyPalette();
        PREFS.put("appearance", value.name());
    }

    // ── Backgrounds & surfaces ───────────────────────────────
    public static Color BG         = new Color(0x26, 0x24, 0x21);
    public static Color SURFACE    = new Color(0x30, 0x2E, 0x2B);
    public static Color SURFACE_2  = new Color(0x3A, 0x38, 0x35);
    public static Color SURFACE_3  = new Color(0x46, 0x43, 0x3F);
    public static Color BORDER     = new Color(0x4A, 0x47, 0x43);
    public static Color BORDER_LT  = new Color(0x5C, 0x58, 0x53);

    // ── Text ─────────────────────────────────────────────────
    public static Color TEXT       = new Color(0xED, 0xEC, 0xEA);
    public static Color TEXT_DIM   = new Color(0xA8, 0xA3, 0x9C);
    public static Color TEXT_FAINT = new Color(0x7A, 0x75, 0x6E);

    // ── Accents ──────────────────────────────────────────────
    public static Color ACCENT       = new Color(0x81, 0xB6, 0x4C);
    public static Color ACCENT_HOVER = new Color(0x95, 0xC9, 0x5E);
    public static Color ACCENT_DARK  = new Color(0x5D, 0x87, 0x34);
    public static Color GOLD         = new Color(0xC8, 0xA2, 0x4A);

    // ── Board ────────────────────────────────────────────────
    public static Color BOARD_BG     = new Color(0xEB, 0xE6, 0xD3);
    public static Color BOARD_BG_ALT = new Color(0xE7, 0xE2, 0xCD);
    public static Color BOARD_LINE   = new Color(0xB4, 0xA8, 0x8C);
    public static Color BOARD_STAR   = new Color(0x8C, 0x7E, 0x62);
    public static Color BOARD_FRAME  = new Color(0x3B, 0x2F, 0x25);

    // ── Stones ───────────────────────────────────────────────
    public static Color STONE_X      = new Color(0x2E, 0x2B, 0x27);
    public static Color STONE_O      = new Color(0x8C, 0x3A, 0x32);

    // ── Board markers ────────────────────────────────────────
    public static Color LAST_MOVE    = new Color(0xF2, 0xDE, 0x76, 130);
    public static Color WIN_CELL     = new Color(0x81, 0xB6, 0x4C, 110);
    public static Color WIN_LINE     = new Color(0x5D, 0x87, 0x34);

    static { applyPalette(); }

    private static void applyPalette() {
        if (mode == Mode.DARK) {
            BG = new Color(0x1C222C); SURFACE = new Color(0x252D39);
            SURFACE_2 = new Color(0x343F4E); SURFACE_3 = new Color(0x435163);
            BORDER = new Color(0x31445A); BORDER_LT = new Color(0x4B6279);
            TEXT = new Color(0xF1F5FA); TEXT_DIM = new Color(0xB1C0CF);
            TEXT_FAINT = new Color(0x8EA2B8);
            ACCENT = new Color(0x81B64C); ACCENT_HOVER = new Color(0x96C966);
            ACCENT_DARK = new Color(0x547C30); GOLD = new Color(0xD9B873);
            BOARD_BG = new Color(0x343F4E); BOARD_BG_ALT = new Color(0x48566A);
            BOARD_LINE = new Color(0x66778B); BOARD_STAR = new Color(0x8DA0B4);
            BOARD_FRAME = new Color(0x0D1725); STONE_X = new Color(0xF1F5FA);
            STONE_O = new Color(0xF4BB75); LAST_MOVE = new Color(0x81, 0xB6, 0x4C, 100);
            WIN_CELL = new Color(0xD9, 0xB8, 0x73, 85); WIN_LINE = new Color(0xD9B873);
            return;
        }
        BG = new Color(0xF3F0E8); SURFACE = new Color(0xFFFCF5);
        SURFACE_2 = new Color(0xEDE8DD); SURFACE_3 = new Color(0xE2DBCD);
        BORDER = new Color(0xC4B9A7); BORDER_LT = new Color(0xA99B86);
        TEXT = new Color(0x242A25); TEXT_DIM = new Color(0x4D574D);
        TEXT_FAINT = new Color(0x5D685D);
        ACCENT = new Color(0x3B7438); ACCENT_HOVER = new Color(0x4A8746);
        ACCENT_DARK = new Color(0x285B29); GOLD = new Color(0x8A641F);
        BOARD_BG = new Color(0xFFF9E9); BOARD_BG_ALT = new Color(0xF7EFDA);
        BOARD_LINE = new Color(0xA99A7F); BOARD_STAR = new Color(0x76684F);
        BOARD_FRAME = new Color(0x665B4C); STONE_X = new Color(0x242A25);
        STONE_O = new Color(0x9A3E32); LAST_MOVE = new Color(0xDD, 0xAF, 0x38, 130);
        WIN_CELL = new Color(0x6A, 0xB6, 0x48, 110); WIN_LINE = new Color(0x3B7438);
    }

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
    /** Regular interface font (buttons, labels). */
    public static Font ui(int style, int size) {
        return pick(style, size, UI_FONTS, Font.SANS_SERIF);
    }

    /** Strong sans-serif headings for the game dashboard. */
    public static Font display(int style, int size) {
        return pick(style, size, UI_FONTS, Font.SANS_SERIF);
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
        drawStone(g2, isX, cx, cy, r, alpha, stoneStyle, isX ? STONE_X : STONE_O);
    }

    /** Explicit style and ink let settings preview changes without altering preferences. */
    public static void drawStone(Graphics2D graphics, boolean isX,
                                 double cx, double cy, double r, float opacity,
                                 StoneStyle style, Color ink) {
        if (r <= 0 || opacity <= 0) return;
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            antialias(g);
            g.setComposite(AlphaComposite.SrcOver.derive(Math.min(1f, opacity)));
            if (style == StoneStyle.BADGE) {
                g.setColor(new Color(0, 0, 0, 65));
                g.fill(new Ellipse2D.Double(cx-r, cy-r+r*.12, r*2, r*2));
                g.setPaint(new GradientPaint((float)cx, (float)(cy-r), mix(ink, Color.WHITE, .3f),
                        (float)cx, (float)(cy+r), ink.darker()));
                g.fill(new Ellipse2D.Double(cx-r, cy-r, r*2, r*2));
                g.setColor(alpha(Color.WHITE, 100));
                g.setStroke(new BasicStroke((float)Math.max(1, r*.07)));
                g.draw(new Ellipse2D.Double(cx-r*.84, cy-r*.84, r*1.68, r*1.68));
                ink = (ink.getRed()*.299 + ink.getGreen()*.587 + ink.getBlue()*.114) < 145
                        ? new Color(0xFFF9EE) : new Color(0x1B2430);
                r *= .7;
            }
            double k = r*.62;
            java.awt.Shape glyph;
            if (isX) {
                var cross = new java.awt.geom.Path2D.Double();
                cross.moveTo(cx-k,cy-k); cross.lineTo(cx+k,cy+k);
                cross.moveTo(cx+k,cy-k); cross.lineTo(cx-k,cy+k);
                glyph = cross;
            } else glyph = new Ellipse2D.Double(cx-r*.68,cy-r*.68,r*1.36,r*1.36);
            if (style == StoneStyle.NEON) {
                for (int i=3;i>=1;i--) {
                    g.setColor(alpha(ink, 25));
                    g.setStroke(new BasicStroke((float)(r*(.2+i*.2)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.draw(glyph);
                }
            }
            g.setColor(ink);
            int cap = style == StoneStyle.SHARP ? BasicStroke.CAP_SQUARE : BasicStroke.CAP_ROUND;
            float stroke = (float)(r*(style == StoneStyle.NEON ? .19 : style == StoneStyle.SHARP ? .26 : .38));
            g.setStroke(new BasicStroke(Math.max(1f, stroke), cap, BasicStroke.JOIN_ROUND));
            g.draw(glyph);
        } finally { g.dispose(); }
    }

    /**
     * Draws a stone on a board-colored tile in the side rail and result card.
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
