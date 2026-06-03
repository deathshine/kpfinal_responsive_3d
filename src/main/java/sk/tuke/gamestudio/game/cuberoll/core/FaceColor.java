package sk.tuke.gamestudio.game.cuberoll.core;

public enum FaceColor {
    NONE("none", '-'),
    RED("red", 'R'),
    BLUE("blue", 'B'),
    GREEN("green", 'G'),
    YELLOW("yellow", 'Y');

    private final String displayName;
    private final char shortCode;

    FaceColor(String displayName, char shortCode) {
        this.displayName = displayName;
        this.shortCode = shortCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public char getShortCode() {
        return shortCode;
    }
}