package sk.tuke.gamestudio.game.cuberoll.core;

import java.util.List;
import java.util.Objects;

/**
 * Immutable description of what happened during one attempted move.
 */
public record MoveOutcome(
        Direction direction,
        int fromRow,
        int fromColumn,
        int attemptedRow,
        int attemptedColumn,
        int toRow,
        int toColumn,
        CellType destinationCellType,
        FaceColor bottomFaceAfterRoll,
        boolean rolled,
        boolean playerMoved,
        GameState stateBefore,
        GameState stateAfter,
        String message,
        List<MoveEffect> effects
) {
    public MoveOutcome {
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(bottomFaceAfterRoll, "bottomFaceAfterRoll");
        Objects.requireNonNull(stateBefore, "stateBefore");
        Objects.requireNonNull(stateAfter, "stateAfter");
        message = message == null ? "" : message;
        effects = List.copyOf(Objects.requireNonNull(effects, "effects"));
    }

    public boolean hasEffect(MoveEffect effect) {
        return effects.contains(effect);
    }
}
