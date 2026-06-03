package sk.tuke.gamestudio.server.web.cuberoll3d;

import java.util.List;

public record CubeRollStateDto(
        int currentLevelIndex,
        int currentLevelNumber,
        int levelCount,
        String levelName,
        boolean lastLevel,
        boolean finishRequiresAllPainters,
        int rows,
        int columns,
        List<List<String>> cells,
        int playerRow,
        int playerColumn,
        CubeFacesDto cubeFaces,
        int moveCount,
        int remainingPainters,
        int score,
        String gameState,
        String message,
        boolean loggedIn,
        String playerName,
        boolean scoreSaved,
        int lastSavedScore,
        String lastScorePlayer
) {
}
