package sk.tuke.gamestudio.game.cuberoll.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CubeTest {
    @Test
    void paintedBottomMovesToWestAfterRollingEast() {
        Cube cube = new Cube();
        cube.paintBottom(FaceColor.RED);

        cube.roll(Direction.EAST);

        assertEquals(FaceColor.NONE, cube.getBottom());
        assertEquals(FaceColor.RED, cube.getWest());
    }

    @Test
    void oppositeMovesRestoreOriginalOrientation() {
        Cube cube = new Cube();
        cube.paintBottom(FaceColor.BLUE);

        cube.roll(Direction.NORTH);
        cube.roll(Direction.SOUTH);

        assertEquals(FaceColor.BLUE, cube.getBottom());
        assertEquals(FaceColor.NONE, cube.getTop());
    }
    @Test
    void clearColorsRemovesPaintFromAllFaces() {
        Cube cube = new Cube();
        cube.paintBottom(FaceColor.GREEN);
        cube.roll(Direction.EAST);
        cube.clearColors();

        assertEquals(FaceColor.NONE, cube.getTop());
        assertEquals(FaceColor.NONE, cube.getBottom());
        assertEquals(FaceColor.NONE, cube.getNorth());
        assertEquals(FaceColor.NONE, cube.getSouth());
        assertEquals(FaceColor.NONE, cube.getWest());
        assertEquals(FaceColor.NONE, cube.getEast());
    }

}


// sads