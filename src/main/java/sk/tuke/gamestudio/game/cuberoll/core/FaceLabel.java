package sk.tuke.gamestudio.game.cuberoll.core;

public enum FaceLabel {
    COLORED("RED"),
    WHITE_1("W1"),
    WHITE_2("W2"),
    WHITE_3("W3"),
    WHITE_4("W4"),
    WHITE_5("W5");

    private final String code;

    FaceLabel(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
