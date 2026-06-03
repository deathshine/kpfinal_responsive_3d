package sk.tuke.gamestudio.server.web.cuberoll3d;

public record MoveResponseDto(
        MoveOutcomeDto outcome,
        CubeRollStateDto state,
        boolean scoreSavedThisMove
) {
}
