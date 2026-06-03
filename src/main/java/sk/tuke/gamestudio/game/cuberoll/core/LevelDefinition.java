package sk.tuke.gamestudio.game.cuberoll.core;

import java.util.Objects;

public class LevelDefinition {
    private final String name;
    private final String[] rows;
    private final boolean finishRequiresAllPainters;

    public LevelDefinition(String name, String... rows) {
        this(name, false, rows);
    }

    public LevelDefinition(String name, boolean finishRequiresAllPainters, String... rows) {
        this.name = Objects.requireNonNull(name, "name");
        this.rows = Objects.requireNonNull(rows, "rows");
        this.finishRequiresAllPainters = finishRequiresAllPainters;
    }

    public String getName() {
        return name;
    }

    public String[] getRows() {
        return rows.clone();
    }

    public boolean isFinishRequiresAllPainters() {
        return finishRequiresAllPainters;
    }
}
