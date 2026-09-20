package caroai;

import caroai.Coord;
import caroai.Evaluator;
import caroai.GameEngine;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Symmetric static advantage estimate, not a calibrated win probability. */
public record PositionEvaluation(int xPercent, String reason) {
    public static PositionEvaluation assess(Map<Coord, GameEngine.Cell> position, int size,
                                     GameEngine.Cell turn, GameEngine.Cell winner, boolean draw) {
        if (winner != null) return new PositionEvaluation(winner == GameEngine.Cell.X ? 100 : 0, "finished");
        if (draw) return new PositionEvaluation(50, "draw");
        Map<Coord, GameEngine.Cell> board = new HashMap<>(position);
        Set<Coord> candidates = new HashSet<>();
        for (Coord c : position.keySet()) for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) {
            Coord n = c.offset(dx, dy);
            if (n.col() >= 0 && n.row() >= 0 && n.col() < size && n.row() < size && !board.containsKey(n)) candidates.add(n);
        }
        int xWins = 0, oWins = 0;
        for (Coord c : candidates) {
            if (Evaluator.makesFive(board, size, c, GameEngine.Cell.X, GameEngine.WIN_LENGTH)) xWins++;
            if (Evaluator.makesFive(board, size, c, GameEngine.Cell.O, GameEngine.WIN_LENGTH)) oWins++;
        }
        if (turn == GameEngine.Cell.X && xWins > 0) return new PositionEvaluation(97, "x-can-win");
        if (turn == GameEngine.Cell.O && oWins > 0) return new PositionEvaluation(3, "o-can-win");
        if (xWins > 1) return new PositionEvaluation(97, "x-double-threat");
        if (oWins > 1) return new PositionEvaluation(3, "o-double-threat");
        if (xWins > 0) return new PositionEvaluation(70, "o-must-block");
        if (oWins > 0) return new PositionEvaluation(30, "x-must-block");
        double x = Evaluator.rawScore(board, size, GameEngine.Cell.X, GameEngine.WIN_LENGTH);
        double o = Evaluator.rawScore(board, size, GameEngine.Cell.O, GameEngine.WIN_LENGTH);
        int share = (int) Math.round(50 + 40 * (x - o) / (x + o + 1000));
        return new PositionEvaluation(share, "shapes");
    }
}
