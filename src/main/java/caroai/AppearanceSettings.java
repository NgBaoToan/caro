package caroai;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Theme picker shared by the title screen and the game rail. */
final class AppearanceSettings {
    private AppearanceSettings() {}

    static void show(GameFrame frame) {
        JDialog dialog = new JDialog(frame, "Appearance", true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout(0, 20));
        content.setBackground(Theme.SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(28, 30, 28, 30));

        JPanel header = new JPanel(new GridLayout(2, 1, 0, 4));
        header.setOpaque(false);
        JLabel title = new JLabel("Choose your look");
        title.setFont(Theme.display(Font.BOLD, 25));
        title.setForeground(Theme.TEXT);
        JLabel subtitle = new JLabel("Changes apply to your current game immediately.");
        subtitle.setFont(Theme.ui(Font.PLAIN, 12));
        subtitle.setForeground(Theme.TEXT_DIM);
        header.add(title);
        header.add(subtitle);
        content.add(header, BorderLayout.NORTH);

        JPanel choices = new JPanel(new GridLayout(1, 2, 14, 0));
        choices.setOpaque(false);
        choices.add(new ThemeChoice(Theme.Mode.LIGHT, dialog, frame));
        choices.add(new ThemeChoice(Theme.Mode.DARK, dialog, frame));
        content.add(choices, BorderLayout.CENTER);

        dialog.setContentPane(content);
        dialog.setSize(470, 265);
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(frame);
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.setVisible(true);
    }

    private static final class ThemeChoice extends JComponent {
        private final Theme.Mode mode;
        private boolean hovered;

        ThemeChoice(Theme.Mode mode, JDialog dialog, GameFrame frame) {
            this.mode = mode;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                @Override public void mouseClicked(MouseEvent e) {
                    frame.setAppearance(mode);
                    dialog.dispose();
                }
            });
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            int w = getWidth(), h = getHeight();
            boolean dark = mode == Theme.Mode.DARK;
            Color tile = dark ? new Color(0x111B2B) : new Color(0xFFFCF5);
            Color board = dark ? new Color(0x1B293A) : new Color(0xEDE8DD);
            Color ink = dark ? new Color(0xF1F5FA) : new Color(0x242A25);
            Color accent = dark ? new Color(0x40D4B0) : new Color(0x3B7438);
            g2.setColor(tile);
            g2.fill(Theme.round(0, 0, w, h, 12));
            g2.setColor(mode == Theme.mode() || hovered ? Theme.ACCENT : Theme.BORDER);
            g2.setStroke(new BasicStroke(mode == Theme.mode() ? 2f : 1f));
            g2.draw(Theme.round(1, 1, w - 2, h - 2, 12));

            g2.setColor(board);
            g2.fill(Theme.round(16, 16, w - 32, 60, 8));
            g2.setColor(accent);
            g2.fill(Theme.round(28, 30, 54, 9, 5));
            g2.setColor(ink);
            g2.fillOval(w - 60, 32, 19, 19);
            g2.setFont(Theme.ui(Font.BOLD, 14));
            g2.setColor(ink);
            g2.drawString(dark ? "Dark" : "Light", 18, h - 18);
            if (mode == Theme.mode()) {
                g2.setColor(accent);
                g2.fillOval(w - 34, h - 31, 12, 12);
            }
            g2.dispose();
        }
    }
}
