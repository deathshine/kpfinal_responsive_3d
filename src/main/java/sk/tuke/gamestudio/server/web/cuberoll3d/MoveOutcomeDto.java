package sk.tuke.gamestudio.server.web.cuberoll3d;

import java.util.List;

public record MoveOutcomeDto(
        String direction,
        int fromRow,
        int fromColumn,
        int attemptedRow,
        int attemptedColumn,
        int toRow,
        int toColumn,
        String destinationCellType,
        String bottomFaceAfterRoll,
        boolean rolled,
        boolean playerMoved,
        String stateBefore,
        String stateAfter,
        String message,
        List<String> effects
) {
}
