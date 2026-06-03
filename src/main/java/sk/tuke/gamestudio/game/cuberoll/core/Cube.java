package sk.tuke.gamestudio.game.cuberoll.core;

public class Cube {
    private FaceColor top = FaceColor.NONE;
    private FaceColor bottom = FaceColor.NONE;
    private FaceColor north = FaceColor.NONE;
    private FaceColor south = FaceColor.NONE;
    private FaceColor west = FaceColor.NONE;
    private FaceColor east = FaceColor.NONE;

    public void roll(Direction direction) {
        switch (direction) {
            case NORTH -> rollNorth();
            case SOUTH -> rollSouth();
            case WEST -> rollWest();
            case EAST -> rollEast();
        }
    }

    private void rollNorth() {
        FaceColor originalTop = top;
        top = south;
        south = bottom;
        bottom = north;
        north = originalTop;
    }

    private void rollSouth() {
        FaceColor originalTop = top;
        top = north;
        north = bottom;
        bottom = south;
        south = originalTop;
    }

    private void rollWest() {
        FaceColor originalTop = top;
        top = east;
        east = bottom;
        bottom = west;
        west = originalTop;
    }

    private void rollEast() {
        FaceColor originalTop = top;
        top = west;
        west = bottom;
        bottom = east;
        east = originalTop;
    }

    public void paintBottom(FaceColor color) {
        bottom = color;
    }

    public void clearColors() {
        top = FaceColor.NONE;
        bottom = FaceColor.NONE;
        north = FaceColor.NONE;
        south = FaceColor.NONE;
        west = FaceColor.NONE;
        east = FaceColor.NONE;
    }

    public FaceColor getTop() {
        return top;
    }

    public FaceColor getBottom() {
        return bottom;
    }

    public FaceColor getNorth() {
        return north;
    }

    public FaceColor getSouth() {
        return south;
    }

    public FaceColor getWest() {
        return west;
    }

    public FaceColor getEast() {
        return east;
    }

    public String getOrientationSummary() {
        return String.format(
                "Top=%s Bottom=%s North=%s South=%s West=%s East=%s",
                top.getShortCode(),
                bottom.getShortCode(),
                north.getShortCode(),
                south.getShortCode(),
                west.getShortCode(),
                east.getShortCode()
        );
    }
}