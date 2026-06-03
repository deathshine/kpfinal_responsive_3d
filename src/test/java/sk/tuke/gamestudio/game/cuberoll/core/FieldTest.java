package sk.tuke.gamestudio.game.cuberoll.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldTest {
    @Test
    void fieldStartsInPlayingState() {
        Field field = new Field(Levels.getLevel(0));

        assertEquals(GameState.PLAYING, field.getState());
        assertEquals(0, field.getMoveCount());
    }

    @Test
    void painterColorsBottomFace() {
        Field field = new Field(new LevelDefinition(
                "Painter",
                "#####",
                "#SrF#",
                "#####"
        ));

        field.move(Direction.EAST);

        assertEquals(FaceColor.RED, field.getCube().getBottom());
        assertEquals(GameState.PLAYING, field.getState());
    }

    @Test
    void wrongColorOnGateLeadsToFailure() {
        Field field = new Field(new LevelDefinition(
                "Gate",
                "#####",
                "#SRF#",
                "#####"
        ));

        field.move(Direction.EAST);

        assertEquals(GameState.FAILED, field.getState());
    }

    @Test
    void lockedFinishCannotBeEnteredBeforeAllPaintersAreUsed() {
        Field field = new Field(new LevelDefinition(
                "Locked finish",
                true,
                "#######",
                "#S.F###",
                "#r.b###",
                "#######"
        ));

        field.move(Direction.EAST);
        field.move(Direction.EAST);

        assertEquals(GameState.PLAYING, field.getState());
        assertEquals(1, field.getPlayerRow());
        assertEquals(2, field.getPlayerColumn());
        assertEquals(2, field.getRemainingPainters());
        assertTrue(field.getStatusLine().contains("Finish is locked"));
    }

    @Test
    void levelTwoCannotBeSolvedByOldBypassRoute() {
        Field field = new Field(Levels.getLevel(1));

        play(field, "SSEEWWSENEEESS");

        assertTrue(field.getState() != GameState.SOLVED);
    }


    @Test
    void levelTwoStillSolvesWithSequentialPaintAndGateFlow() {
        Field field = new Field(Levels.getLevel(1));

        play(field, "SSEEWWSENEEEESSEWNNWWNNWWWSSSENEEEESSEEEE");

        assertEquals(GameState.SOLVED, field.getState());
        assertEquals(0, field.getRemainingPainters());
    }


    @Test
    void levelThreeStillSolvesAfterSequentiallyUsingAllPainters() {
        Field field = new Field(Levels.getLevel(2));

        play(field, "SSEEWWSENEEEESSEWNNWWNNWWWSSSENEEEESSEEESESSENEE");

        assertEquals(GameState.SOLVED, field.getState());
        assertEquals(0, field.getRemainingPainters());
    }

    private void play(Field field, String path) {
        for (char step : path.toCharArray()) {
            field.move(toDirection(step));
        }
    }

    private Direction toDirection(char step) {
        return switch (step) {
            case 'N' -> Direction.NORTH;
            case 'S' -> Direction.SOUTH;
            case 'W' -> Direction.WEST;
            case 'E' -> Direction.EAST;
            default -> throw new IllegalArgumentException("Unsupported step: " + step);
        };
    }
}
