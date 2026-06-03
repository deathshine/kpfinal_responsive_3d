package sk.tuke.gamestudio.server.web.cuberoll3d;

public record LevelOptionDto(
        int index,
        int number,
        String name,
        int rows,
        int columns,
        boolean finishRequiresAllPainters
) {
}
