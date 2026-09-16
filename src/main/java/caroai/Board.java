package caroai;

import javax.swing.*;
import java.awt.*;

/**
 * Legacy prototype: the very first grid-of-JButtons experiment.
 * Kept for reference only — the game itself uses {@link BoardPanel}.
 */
public class Board {

    int size = 50;
    JFrame frame = new JFrame("Caro");
    JPanel panel = new JPanel();
    ImageIcon iconX = new ImageIcon("assets/x.png");
    ImageIcon iconO = new ImageIcon("assets/o.png");
    JButton[][] buttons = new JButton[size][size];
    boolean[] isXTurn = {true};

    public Board() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 800);

        panel.setLayout(new GridLayout(size, size));

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                buttons[i][j] = new JButton();
                int row = i, col = j;
                buttons[i][j].addActionListener(e -> {
                    if (buttons[row][col].getIcon() == null) {   // empty square
                        buttons[row][col].setIcon(isXTurn[0] ? iconX : iconO);
                        isXTurn[0] = !isXTurn[0];                // swap turns
                    }
                });
                panel.add(buttons[i][j]);
            }
        }
        frame.add(panel);
        frame.setVisible(true);
    }
}
