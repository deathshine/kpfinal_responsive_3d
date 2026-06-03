package sk.tuke.gamestudio.server.web;

import sk.tuke.gamestudio.game.cuberoll.core.Field;
import sk.tuke.gamestudio.game.cuberoll.core.Levels;

public class WebGameSession {
    public static final String SESSION_ATTRIBUTE = "cuberollSession";

    private int levelIndex;
    private Field field;
    private String playerName;
    private boolean scoreSaved;
    private int lastSavedScore;
    private String lastScorePlayer;

    public WebGameSession() {
        selectLevel(0);
    }

    public int getLevelIndex() {
        return levelIndex;
    }

    public int getLevelNumber() {
        return levelIndex + 1;
    }

    public boolean isLastLevel() {
        return levelIndex == Levels.count() - 1;
    }

    public Field getField() {
        return field;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        String normalized = playerName == null ? null : playerName.trim();
        this.playerName = normalized == null || normalized.isBlank() ? null : normalized;
    }

    public void logout() {
        this.playerName = null;
    }

    public boolean isLoggedIn() {
        return playerName != null && !playerName.isBlank();
    }

    public boolean isScoreSaved() {
        return scoreSaved;
    }

    public int getLastSavedScore() {
        return lastSavedScore;
    }

    public String getLastScorePlayer() {
        return lastScorePlayer;
    }

    public void markScoreSaved(int score) {
        this.scoreSaved = true;
        this.lastSavedScore = score;
        this.lastScorePlayer = playerName;
    }

    public void selectLevel(int levelIndex) {
        this.levelIndex = Math.max(0, Math.min(levelIndex, Levels.count() - 1));
        this.field = new Field(Levels.getLevel(this.levelIndex));
        clearSavedScoreInfo();
    }

    public void resetLevel() {
        this.field.reset();
        clearSavedScoreInfo();
    }

    public void nextLevel() {
        selectLevel(Math.min(levelIndex + 1, Levels.count() - 1));
    }

    private void clearSavedScoreInfo() {
        this.scoreSaved = false;
        this.lastSavedScore = 0;
        this.lastScorePlayer = null;
    }
}
