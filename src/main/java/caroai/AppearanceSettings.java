package caroai;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.function.BiConsumer;

/** Staged appearance editor: previewing or canceling never changes a running game. */
final class AppearanceSettings {
    private AppearanceSettings() {}

    static void show(GameFrame frame) {
        JDialog dialog = new JDialog(frame, "Giao diện và quân cờ", true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        JPanel content = createContent((mode, style) -> {
            frame.setAppearance(mode, style);
            dialog.dispose();
        }, dialog::dispose);
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        dialog.setContentPane(scroll);
        Rectangle screen = frame.getGraphicsConfiguration().getBounds();
        Insets inset = Toolkit.getDefaultToolkit().getScreenInsets(frame.getGraphicsConfiguration());
        dialog.setSize(Math.min(650, screen.width-inset.left-inset.right-32),
                Math.min(720, screen.height-inset.top-inset.bottom-48));
        dialog.setLocationRelativeTo(frame);
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.setVisible(true);
    }

    static JPanel createContent(BiConsumer<Theme.Mode, Theme.StoneStyle> apply, Runnable cancel) {
        Theme.Mode[] mode = {Theme.mode()};
        Theme.StoneStyle[] style = {Theme.stoneStyle()};
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(26,28,26,28));
        root.setBackground(Theme.SURFACE);
        root.add(label("Bàn cờ theo cách của bạn", 25, Font.BOLD));
        root.add(Box.createVerticalStrut(8));
        root.add(label("Chọn giao diện và kiểu quân. Ván đang chơi được giữ nguyên.", 12, Font.PLAIN));
        root.add(Box.createVerticalStrut(24));
        Preview preview = new Preview(mode, style);
        JPanel modes = row(1,2);
        ButtonGroup themeGroup = new ButtonGroup();
        for (Theme.Mode option : Theme.Mode.values()) {
            JToggleButton button = choice(option == Theme.Mode.DARK ? "Tối · Slate" : "Sáng · Ivory");
            button.setSelected(mode[0] == option);
            themeGroup.add(button); modes.add(button);
            button.addActionListener(e -> { mode[0]=option; preview.repaint(); });
        }
        root.add(label("GIAO DIỆN",11,Font.BOLD)); root.add(Box.createVerticalStrut(9));
        modes.setMaximumSize(new Dimension(Integer.MAX_VALUE,44)); root.add(modes);
        root.add(Box.createVerticalStrut(20));
        root.add(label("HÌNH DẠNG QUÂN X / O",11,Font.BOLD)); root.add(Box.createVerticalStrut(9));
        JPanel styles = row(2,2);
        ButtonGroup styleGroup = new ButtonGroup();
        for (Theme.StoneStyle option : Theme.StoneStyle.values()) {
            JToggleButton button = choice(option.label());
            button.setIcon(new Icon() {
                public int getIconWidth() { return 66; }
                public int getIconHeight() { return 40; }
                public void paintIcon(Component c, Graphics graphics, int x, int y) {
                    Theme.drawStone((Graphics2D) graphics,true,x+16,y+20,13,1,option,Theme.TEXT);
                    Theme.drawStone((Graphics2D) graphics,false,x+48,y+20,13,1,option,Theme.STONE_O);
                }
            });
            button.setSelected(style[0] == option); styleGroup.add(button); styles.add(button);
            button.addActionListener(e -> { style[0]=option; preview.repaint(); });
        }
        styles.setMaximumSize(new Dimension(Integer.MAX_VALUE,112)); root.add(styles);
        root.add(Box.createVerticalStrut(20));
        root.add(label("XEM TRƯỚC",11,Font.BOLD)); root.add(Box.createVerticalStrut(9));
        preview.setAlignmentX(Component.LEFT_ALIGNMENT);
        preview.setPreferredSize(new Dimension(540,170));
        preview.setMinimumSize(new Dimension(250,170));
        preview.setMaximumSize(new Dimension(Integer.MAX_VALUE,170)); root.add(preview);
        root.add(Box.createVerticalStrut(12));
        root.add(label("Lựa chọn được ghi nhớ cho lần mở game tiếp theo.",12,Font.PLAIN));
        root.add(Box.createVerticalStrut(22));
        JPanel actions = row(1,2);
        JButton back = action("Hủy", false); JButton save = action("Áp dụng", true);
        for (JButton button : new JButton[]{back,save}) {
            button.setFont(Theme.ui(Font.BOLD,14)); button.setFocusPainted(true);
            button.setBackground(Theme.SURFACE_2); button.setForeground(Theme.TEXT);
        }
        back.addActionListener(e -> cancel.run()); save.addActionListener(e -> apply.accept(mode[0],style[0]));
        actions.add(back);actions.add(save);actions.setMaximumSize(new Dimension(Integer.MAX_VALUE,44));root.add(actions);
        return root;
    }

    private static JLabel label(String text,int size,int weight) {
        JLabel label = new JLabel(text);label.setFont(Theme.ui(weight,size));
        label.setForeground(weight==Font.BOLD ? Theme.TEXT : Theme.TEXT_DIM);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);return label;
    }

    private static JPanel row(int rows,int cols) {
        JPanel panel = new JPanel(new GridLayout(rows,cols,10,8));panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);return panel;
    }

    private static JButton action(String text, boolean primary) {
        JButton button = new JButton(text) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                Theme.antialias(g);
                int sink = ControlSurface.choice(g, getWidth(), getHeight(), primary || hasFocus(),
                        getModel().isRollover() || getModel().isPressed());
                g.translate(0, sink);
                super.paintComponent(g);
                g.dispose();
            }
        };
        button.setContentAreaFilled(false); button.setBorderPainted(false); button.setFocusPainted(false);
        button.setRolloverEnabled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private static JToggleButton choice(String text) {
        JToggleButton button = new JToggleButton(text) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();Theme.antialias(g);
                int sink = ControlSurface.choice(g, getWidth(), getHeight(),
                        isSelected() || hasFocus(), getModel().isRollover() || getModel().isPressed());
                g.translate(0, sink);
                super.paintComponent(g);
                g.dispose();
            }
        };
        button.setFont(Theme.ui(Font.BOLD,13));button.setForeground(Theme.TEXT);
        button.setRolloverEnabled(true);
        button.setContentAreaFilled(false);button.setBorderPainted(false);button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));return button;
    }

    private static final class Preview extends JPanel {
        private final Theme.Mode[] mode;
        private final Theme.StoneStyle[] style;
        Preview(Theme.Mode[] mode,Theme.StoneStyle[] style) {this.mode=mode;this.style=style;}
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();Theme.antialias(g);
            boolean dark = mode[0] == Theme.Mode.DARK;
            Color base = new Color(dark ? 0x343F4E : 0xFFF9E9);
            Color alternate = new Color(dark ? 0x48566A : 0xF7EFDA);
            Color xInk = new Color(dark ? 0xF1F5FA : 0x242A25);
            Color oInk = new Color(dark ? 0xF4BB75 : 0x9A3E32);
            int cell=42;
            g.setColor(base);g.fillRect(0,0,getWidth(),getHeight());
            for(int x=0;x<getWidth();x+=cell)for(int y=0;y<getHeight();y+=cell){
                if((x/cell+y/cell)%2==0){g.setColor(alternate);g.fillRect(x,y,cell,cell);}
            }
            g.setColor(new Color(dark ? 0x66778B : 0xC4B9A7));
            for(int x=0;x<getWidth();x+=cell)g.drawLine(x,0,x,getHeight());
            for(int y=0;y<getHeight();y+=cell)g.drawLine(0,y,getWidth(),y);
            int start=Math.max(0,(getWidth()/cell-5)/2)*cell;
            for(int i=0;i<5;i++) Theme.drawStone(g,i%2==0,start+i*cell+21,63,16,1,style[0],i%2==0?xInk:oInk);
            Theme.drawStone(g,false,start+2*cell+21,105,16,1,style[0],oInk);
            Theme.drawStone(g,true,start+3*cell+21,105,16,1,style[0],xInk);
            g.dispose();
        }
    }
}
