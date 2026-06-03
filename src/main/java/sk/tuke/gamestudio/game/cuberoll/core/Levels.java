package sk.tuke.gamestudio.game.cuberoll.core;

import java.util.List;

public final class Levels {
    private static final List<LevelDefinition> LEVELS = List.of(
            new LevelDefinition(
                    "Red Door",
                    "########",
                    "#S...###",
                    "#.##.###",
                    "#..rRF##",
                    "#..#####",
                    "########"
            ),
            new LevelDefinition(
                    "Red And Blue",
                    true,
                    "############",
                    "#S...#######",
                    "#.##.#######",
                    "#..rR..#####",
                    "#..###.#####",
                    "######.bB.F#",
                    "######.##.##",
                    "######...###",
                    "############"
            ),
            new LevelDefinition(
                    "Three Colors",
                    true,
                    "################",
                    "#S...###########",
                    "#.##.###########",
                    "#..rR..#########",
                    "#..###.#########",
                    "######.bB..#####",
                    "######.##..#####",
                    "######...#gG.F##",
                    "##########..####",
                    "################"
            )
    );

    private Levels() {
    }

    public static int count() {
        return LEVELS.size();
    }

    public static LevelDefinition getLevel(int index) {
        if (index < 0 || index >= LEVELS.size()) {
            throw new IllegalArgumentException("Unknown level index: " + index);
        }
        return LEVELS.get(index);
    }
}
