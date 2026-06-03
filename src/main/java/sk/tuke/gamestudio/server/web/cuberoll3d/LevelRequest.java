package sk.tuke.gamestudio.server.web.cuberoll3d;

public record LevelRequest(Integer levelIndex, Integer level, Integer index) {
    public Integer requestedIndex() {
        if (levelIndex != null) {
            return levelIndex;
        }
        if (level != null) {
            return level;
        }
        return index;
    }
}
