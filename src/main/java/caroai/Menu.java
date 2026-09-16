package caroai;

import javax.swing.*;
import java.awt.*;

/**
 * Legacy prototype: the first JMenuBar experiment.
 * Kept for reference only — the game starts from {@link Main}.
 */
public class Menu {

    public static void main(String[] args) {
        int size = 15;
        JFrame frame = new JFrame("Caro");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 600);

        JMenuBar menuBar  = new JMenuBar();
        JMenu    menu     = new JMenu("Menu");
        JMenuItem settingItem = new JMenuItem("Settings");
        JMenuItem exitItem    = new JMenuItem("Exit");

        menu.add(settingItem);
        menu.add(exitItem);
        menuBar.add(menu);
        frame.setJMenuBar(menuBar);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(size, size));

        ImageIcon xIcon = new ImageIcon("x.png");
        ImageIcon oIcon = new ImageIcon("o.png");

        JButton[][] buttons = new JButton[size][size];
        boolean[] isXTurn = {true};

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                buttons[i][j] = new JButton();
                int row = i, col = j;
                buttons[i][j].addActionListener(e -> {
                    if (buttons[row][col].getIcon() == null) {
                        buttons[row][col].setIcon(isXTurn[0] ? xIcon : oIcon);
                        isXTurn[0] = !isXTurn[0];
                    }
                });
                panel.add(buttons[i][j]);
            }
        }

        settingItem.addActionListener(e -> {
            String[] options = { "Two players", "Play vs computer" };
            int choice = JOptionPane.showOptionDialog(
                frame,
                "Choose a game mode:",
                "Settings",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
            );
            if (choice == 0) {
                JOptionPane.showMessageDialog(frame, "Two-player mode selected.");
            } else if (choice == 1) {
                JOptionPane.showMessageDialog(frame, "Play-vs-computer mode selected.");
            }
        });

        exitItem.addActionListener(e -> System.exit(0));

        frame.add(panel);
        frame.setVisible(true);
    }
}
