package caroai.web;

import caroai.Coord;
import caroai.PositionEvaluation;
import caroai.GameEngine.Cell;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PositionEvaluationTest {
    private PositionEvaluation assess(Map<Coord, Cell> board, Cell turn) {
        return PositionEvaluation.assess(board, 50, turn, null, false);
    }
    @Test void emptyAndTerminalStates() {
        assertThat(assess(Map.of(), Cell.X).xPercent()).isEqualTo(50);
        assertThat(PositionEvaluation.assess(Map.of(),50,Cell.O,Cell.X,false).xPercent()).isEqualTo(100);
        assertThat(PositionEvaluation.assess(Map.of(),50,Cell.X,Cell.O,false).xPercent()).isZero();
        assertThat(PositionEvaluation.assess(Map.of(),50,Cell.X,null,true).reason()).isEqualTo("draw");
    }
    @Test void swappingColoursMirrorsAdvantageWithoutMutatingBoard() {
        var board = Map.of(new Coord(5,5),Cell.X,new Coord(6,5),Cell.X,new Coord(7,5),Cell.X,new Coord(15,15),Cell.O);
        Map<Coord,Cell> swapped = new HashMap<>();
        board.forEach((c,s)->swapped.put(c,s==Cell.X?Cell.O:Cell.X));
        assertThat(assess(board,Cell.O).xPercent()).isGreaterThan(50);
        assertThat(assess(board,Cell.O).xPercent()+assess(swapped,Cell.X).xPercent()).isEqualTo(100);
        assertThat(board).hasSize(4);
    }
    @Test void immediateWinTakesPriorityOverOpponentsThreat() {
        Map<Coord,Cell> board = new HashMap<>();
        for(int i=0;i<4;i++){board.put(new Coord(i,0),Cell.X);board.put(new Coord(i,2),Cell.O);}
        assertThat(assess(board,Cell.X).reason()).isEqualTo("x-can-win");
        assertThat(assess(board,Cell.O).reason()).isEqualTo("o-can-win");
    }
    @Test void distinguishesSingleBlockFromUnblockableDoubleThreat() {
        Map<Coord,Cell> board = new HashMap<>();
        for(int i=0;i<4;i++)board.put(new Coord(i,0),Cell.X);
        assertThat(assess(board,Cell.O).reason()).isEqualTo("o-must-block");
        board.clear();for(int i=1;i<5;i++)board.put(new Coord(i,2),Cell.X);
        assertThat(assess(board,Cell.O).reason()).isEqualTo("x-double-threat");
    }
    @Test void stateReplyAndHintAllIncludeEvaluation() {
        for(String action:new String[]{"state","reply","hint"}) {
            assertThat(WebGame.respond(Map.of("action",action,"mode","local","moves","0,0;5,5;1,0")))
                    .contains("\"evaluation\":{\"xPercent\":", "\"reason\":");
        }
    }
}
