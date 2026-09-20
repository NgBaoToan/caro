package caroai.web;

import caroai.AI;
import caroai.Coord;
import caroai.Difficulty;
import caroai.GameEngine;
import caroai.Player;
import caroai.PositionEvaluation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Request-local game: replay and validate history before asking the existing AI. */
final class WebGame {
    private final GameEngine engine = new GameEngine(
            new Player("X", "X", false), new Player("O", "O", false));
    private final List<Coord> moves = new ArrayList<>();
    private GameEngine.Cell winner;
    private List<Coord> line = List.of();

    static String respond(Map<String, String> form) {
        String mode = form.getOrDefault("mode", "ai");
        if (!mode.equals("ai") && !mode.equals("local")) throw new IllegalArgumentException();
        String action = form.getOrDefault("action", "reply");
        if (!List.of("reply", "state", "hint").contains(action)) throw new IllegalArgumentException();
        GameEngine.Cell human = GameEngine.Cell.valueOf(form.getOrDefault("human", "X"));
        Difficulty difficulty = Difficulty.valueOf(form.getOrDefault("difficulty", "MEDIUM"));
        WebGame game = new WebGame();
        String history = form.getOrDefault("moves", "");
        if (!history.isEmpty()) {
            String[] entries = history.split(";", -1);
            if (entries.length > GameEngine.DEFAULT_SIZE * GameEngine.DEFAULT_SIZE) {
                throw new IllegalArgumentException();
            }
            for (String entry : entries) {
                String[] xy = entry.split(",", -1);
                if (xy.length != 2) throw new IllegalArgumentException();
                game.place(new Coord(Integer.parseInt(xy[0]), Integer.parseInt(xy[1])));
            }
        }
        long elapsed = 0;
        Coord hint = null;
        boolean wantsHint = action.equals("hint");
        if (wantsHint && mode.equals("ai") && !game.finished() && game.turn() != human) {
            throw new IllegalArgumentException("Wait for the computer before requesting a hint");
        }
        if (!game.finished() && (wantsHint || (action.equals("reply") && mode.equals("ai") && game.turn() != human))) {
            long started = System.nanoTime();
            AI ai = new AI(game.engine.getSize(), GameEngine.WIN_LENGTH, difficulty);
            Coord move = game.moves.isEmpty()
                    ? new Coord(game.engine.getSize() / 2, game.engine.getSize() / 2)
                    : ai.findBestMove(game.engine.getBoard(), game.turn(), game.engine.getSize());
            if (move == null) throw new IllegalStateException("AI did not return a move");
            if (wantsHint) hint = move;
            else game.place(move);
            elapsed = (System.nanoTime() - started) / 1_000_000;
        }
        return game.json(elapsed, hint);
    }

    private GameEngine.Cell turn() {
        return moves.size() % 2 == 0 ? GameEngine.Cell.X : GameEngine.Cell.O;
    }

    private boolean finished() {
        return winner != null || moves.size() == engine.getSize() * engine.getSize();
    }

    private void place(Coord c) {
        GameEngine.Cell symbol = turn();
        if (finished() || !engine.makeMove(c, symbol)) throw new IllegalArgumentException();
        moves.add(c);
        if (engine.checkWin(c)) {
            winner = symbol;
            line = engine.getWinningLine(c);
        }
    }

    private String json(long elapsed, Coord hint) {
        var evaluation = PositionEvaluation.assess(engine.getBoard(), engine.getSize(), turn(), winner, winner == null && finished());
        return "{\"size\":" + engine.getSize() + ",\"moves\":" + coordinates(moves)
                + ",\"turn\":\"" + turn() + "\",\"winner\":"
                + (winner == null ? "null" : "\"" + winner + "\"")
                + ",\"draw\":" + (winner == null && finished())
                + ",\"winningLine\":" + coordinates(line) + ",\"aiMillis\":" + elapsed
                + ",\"hint\":" + (hint == null ? "null" : "[" + hint.col() + "," + hint.row() + "]")
                + ",\"evaluation\":{\"xPercent\":" + evaluation.xPercent() + ",\"reason\":\"" + evaluation.reason() + "\"}}";
    }

    private static String coordinates(List<Coord> list) {
        return list.stream().map(c -> "[" + c.col() + "," + c.row() + "]")
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }
}
