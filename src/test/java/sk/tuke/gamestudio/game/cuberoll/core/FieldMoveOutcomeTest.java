package sk.tuke.gamestudio.game.cuberoll.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldMoveOutcomeTest {
    @Test
    void getCellTypeAtReturnsRealBoardCellWithoutPlayerOverlay() {
        Field field = new Field(new LevelDefinition(
                "Cells",
                "#####",
                "#SrF#",
                "#####"
        ));

        assertEquals(CellType.FLOOR, field.getCellTypeAt(1, 1));
        assertEquals(CellType.PAINTER_RED, field.getCellTypeAt(1, 2));
        assertEquals('C', field.getSymbolAt(1, 1));
    }

    @Test
    void painterMoveReportsPainterEffectAndConsumesPainterCell() {
        Field field = new Field(new LevelDefinition(
                "Painter",
                "#####",
                "#SrF#",
                "#####"
        ));

        MoveOutcome outcome = field.moveWithOutcome(Direction.EAST);

        assertTrue(outcome.hasEffect(MoveEffect.ROLLED));
        assertTrue(outcome.hasEffect(MoveEffect.MOVED));
        assertTrue(outcome.hasEffect(MoveEffect.PAINTER_USED));
        assertEquals(1, outcome.fromRow());
        assertEquals(1, outcome.fromColumn());
        assertEquals(1, outcome.toRow());
        assertEquals(2, outcome.toColumn());
        assertEquals(CellType.PAINTER_RED, outcome.destinationCellType());
        assertEquals(FaceColor.RED, field.getCube().getBottom());
        assertEquals(CellType.FLOOR, field.getCellTypeAt(1, 2));
    }

    @Test
    void lockedFinishReportsNoRollAndLeavesPositionUnchanged() {
        Field field = new Field(new LevelDefinition(
                "Locked finish",
                true,
                "#####",
                "#SF##",
                "#r###",
                "#####"
        ));

        MoveOutcome outcome = field.moveWithOutcome(Direction.EAST);

        assertTrue(outcome.hasEffect(MoveEffect.FINISH_LOCKED));
        assertFalse(outcome.rolled());
        assertFalse(outcome.playerMoved());
        assertEquals(0, field.getMoveCount());
        assertEquals(1, field.getPlayerRow());
        assertEquals(1, field.getPlayerColumn());
        assertEquals(GameState.PLAYING, outcome.stateAfter());
    }

    @Test
    void wrongGateColorReportsGateFailureWithoutMovingPlayer() {
        Field field = new Field(new LevelDefinition(
                "Gate",
                "#####",
                "#SRF#",
                "#####"
        ));

        MoveOutcome outcome = field.moveWithOutcome(Direction.EAST);

        assertTrue(outcome.hasEffect(MoveEffect.ROLLED));
        assertTrue(outcome.hasEffect(MoveEffect.WRONG_GATE_COLOR));
        assertFalse(outcome.playerMoved());
        assertEquals(CellType.GATE_RED, outcome.destinationCellType());
        assertEquals(1, outcome.attemptedRow());
        assertEquals(2, outcome.attemptedColumn());
        assertEquals(1, outcome.toRow());
        assertEquals(1, outcome.toColumn());
        assertEquals(GameState.FAILED, field.getState());
    }
}
