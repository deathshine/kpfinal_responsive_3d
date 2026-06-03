package sk.tuke.gamestudio.game.cuberoll.core;

public class Field {
    private final LevelDefinition definition;
    private final CellType[][] board;
    private final int rowCount;
    private final int columnCount;
    private final int startRow;
    private final int startColumn;

    private Cube cube;
    private int playerRow;
    private int playerColumn;
    private int moveCount;
    private int remainingPainters;
    private GameState state;
    private String lastMessage;
    private FaceColor activeColor;

    public Field(LevelDefinition definition) {
        this.definition = definition;
        String[] rows = definition.getRows();
        this.rowCount = rows.length;
        this.columnCount = rows[0].length();
        this.board = new CellType[rowCount][columnCount];

        int discoveredStartRow = -1;
        int discoveredStartColumn = -1;
        boolean finishFound = false;

        for (int row = 0; row < rowCount; row++) {
            if (rows[row].length() != columnCount) {
                throw new IllegalArgumentException("All level rows must have same length.");
            }
            for (int column = 0; column < columnCount; column++) {
                char symbol = rows[row].charAt(column);
                switch (symbol) {
                    case 'S' -> {
                        board[row][column] = CellType.FLOOR;
                        discoveredStartRow = row;
                        discoveredStartColumn = column;
                    }
                    case 'T', 'F' -> {
                        board[row][column] = CellType.FINISH;
                        finishFound = true;
                    }
                    case '.' -> board[row][column] = CellType.FLOOR;
                    case '#' -> board[row][column] = CellType.VOID;
                    case 'r' -> board[row][column] = CellType.PAINTER_RED;
                    case 'R' -> board[row][column] = CellType.GATE_RED;
                    case 'b' -> board[row][column] = CellType.PAINTER_BLUE;
                    case 'B' -> board[row][column] = CellType.GATE_BLUE;
                    case 'g' -> board[row][column] = CellType.PAINTER_GREEN;
                    case 'G' -> board[row][column] = CellType.GATE_GREEN;
                    case 'y' -> board[row][column] = CellType.PAINTER_YELLOW;
                    case 'Y' -> board[row][column] = CellType.GATE_YELLOW;
                    default -> throw new IllegalArgumentException("Unsupported map symbol: " + symbol);
                }
            }
        }

        if (discoveredStartRow < 0 || !finishFound) {
            throw new IllegalArgumentException("Level must contain both start S and finish F/T.");
        }

        this.startRow = discoveredStartRow;
        this.startColumn = discoveredStartColumn;
        reset();
    }

    public void move(Direction direction) {
        moveWithOutcome(direction);
    }

    public MoveOutcome moveWithOutcome(Direction direction) {
        if (direction == null) {
            throw new IllegalArgumentException("Direction must not be null.");
        }

        int fromRow = playerRow;
        int fromColumn = playerColumn;
        int attemptedRow = playerRow + direction.getRowDelta();
        int attemptedColumn = playerColumn + direction.getColumnDelta();
        GameState stateBefore = state;

        if (state != GameState.PLAYING) {
            return new MoveOutcome(
                    direction,
                    fromRow,
                    fromColumn,
                    attemptedRow,
                    attemptedColumn,
                    playerRow,
                    playerColumn,
                    getCellTypeAt(playerRow, playerColumn),
                    cube.getBottom(),
                    false,
                    false,
                    stateBefore,
                    state,
                    lastMessage,
                    java.util.List.of(MoveEffect.IGNORED_NOT_PLAYING)
            );
        }

        CellType destination = isInside(attemptedRow, attemptedColumn)
                ? board[attemptedRow][attemptedColumn]
                : CellType.VOID;

        if (destination == CellType.VOID) {
            cube.roll(direction);
            moveCount++;
            state = GameState.FAILED;
            lastMessage = "Failed: the cube fell into void or left the board.";
            return new MoveOutcome(
                    direction,
                    fromRow,
                    fromColumn,
                    attemptedRow,
                    attemptedColumn,
                    playerRow,
                    playerColumn,
                    destination,
                    cube.getBottom(),
                    true,
                    false,
                    stateBefore,
                    state,
                    lastMessage,
                    java.util.List.of(MoveEffect.ROLLED, MoveEffect.FELL)
            );
        }

        if (destination == CellType.FINISH
                && definition.isFinishRequiresAllPainters()
                && remainingPainters > 0) {
            lastMessage = "Finish is locked: use all painter blocks first (remaining: " + remainingPainters + ").";
            return new MoveOutcome(
                    direction,
                    fromRow,
                    fromColumn,
                    attemptedRow,
                    attemptedColumn,
                    playerRow,
                    playerColumn,
                    destination,
                    cube.getBottom(),
                    false,
                    false,
                    stateBefore,
                    state,
                    lastMessage,
                    java.util.List.of(MoveEffect.FINISH_LOCKED)
            );
        }

        cube.roll(direction);
        FaceColor bottomFaceAfterRoll = cube.getBottom();
        moveCount++;

        // Gate must be checked against the REAL bottom face after the roll,
        // not against some stored active color.
        if (destination.isGate() && bottomFaceAfterRoll != destination.getColor()) {
            state = GameState.FAILED;
            lastMessage = "Failed: gate " + destination.getColor().getDisplayName()
                    + " requires the painted bottom face.";
            return new MoveOutcome(
                    direction,
                    fromRow,
                    fromColumn,
                    attemptedRow,
                    attemptedColumn,
                    playerRow,
                    playerColumn,
                    destination,
                    bottomFaceAfterRoll,
                    true,
                    false,
                    stateBefore,
                    state,
                    lastMessage,
                    java.util.List.of(MoveEffect.ROLLED, MoveEffect.WRONG_GATE_COLOR)
            );
        }

        playerRow = attemptedRow;
        playerColumn = attemptedColumn;

        java.util.ArrayList<MoveEffect> effects = new java.util.ArrayList<>();
        effects.add(MoveEffect.ROLLED);
        effects.add(MoveEffect.MOVED);

        if (destination.isPainter()) {
            cube.paintBottom(destination.getColor());
            activeColor = destination.getColor(); // can stay for status/debug
            board[playerRow][playerColumn] = CellType.FLOOR;
            remainingPainters--;
            lastMessage = "Painter used once: bottom face is now "
                    + destination.getColor().getDisplayName() + ".";
            effects.add(MoveEffect.PAINTER_USED);
        } else if (destination.isGate()) {
            activeColor = FaceColor.NONE;
            cube.clearColors();
            board[playerRow][playerColumn] = CellType.FLOOR;
            lastMessage = "Gate opened permanently: color was consumed, repaint before the next gate.";
            effects.add(MoveEffect.GATE_OPENED);
        } else if (destination == CellType.FINISH) {
            state = GameState.SOLVED;
            lastMessage = "Solved: you passed the needed gates and reached the finish.";
            effects.add(MoveEffect.SOLVED);
        } else {
            lastMessage = "Move the painted face to the bottom when stepping onto a matching gate.";
        }

        return new MoveOutcome(
                direction,
                fromRow,
                fromColumn,
                attemptedRow,
                attemptedColumn,
                playerRow,
                playerColumn,
                destination,
                bottomFaceAfterRoll,
                true,
                true,
                stateBefore,
                state,
                lastMessage,
                effects
        );
    }

    public void reset() {
        loadBoardFromDefinition();
        this.cube = new Cube();
        this.playerRow = startRow;
        this.playerColumn = startColumn;
        this.moveCount = 0;
        this.state = GameState.PLAYING;
        this.activeColor = FaceColor.NONE;
        this.lastMessage = definition.isFinishRequiresAllPainters()
                ? "Use every painter block before the finish unlocks. Each gate consumes the current color and stays open."
                : "Lowercase tiles paint the current color once and then disappear. Uppercase tiles consume the color and stay open.";
    }

    private void loadBoardFromDefinition() {
        String[] rows = definition.getRows();
        int painterCount = 0;

        for (int row = 0; row < rowCount; row++) {
            for (int column = 0; column < columnCount; column++) {
                char symbol = rows[row].charAt(column);
                switch (symbol) {
                    case 'S' -> board[row][column] = CellType.FLOOR;
                    case 'T', 'F' -> board[row][column] = CellType.FINISH;
                    case '.' -> board[row][column] = CellType.FLOOR;
                    case '#' -> board[row][column] = CellType.VOID;
                    case 'r' -> {
                        board[row][column] = CellType.PAINTER_RED;
                        painterCount++;
                    }
                    case 'R' -> board[row][column] = CellType.GATE_RED;
                    case 'b' -> {
                        board[row][column] = CellType.PAINTER_BLUE;
                        painterCount++;
                    }
                    case 'B' -> board[row][column] = CellType.GATE_BLUE;
                    case 'g' -> {
                        board[row][column] = CellType.PAINTER_GREEN;
                        painterCount++;
                    }
                    case 'G' -> board[row][column] = CellType.GATE_GREEN;
                    case 'y' -> {
                        board[row][column] = CellType.PAINTER_YELLOW;
                        painterCount++;
                    }
                    case 'Y' -> board[row][column] = CellType.GATE_YELLOW;
                    default -> throw new IllegalArgumentException("Unsupported map symbol: " + symbol);
                }
            }
        }

        remainingPainters = painterCount;
    }

    public void exit() {
        this.state = GameState.EXITED;
        this.lastMessage = "Game ended by player.";
    }

    public boolean isInside(int row, int column) {
        return row >= 0 && row < rowCount && column >= 0 && column < columnCount;
    }

    public CellType getCellTypeAt(int row, int column) {
        if (!isInside(row, column)) {
            throw new IndexOutOfBoundsException("Cell is outside the board: row=" + row + ", column=" + column);
        }
        return board[row][column];
    }

    public char getSymbolAt(int row, int column) {
        if (row == playerRow && column == playerColumn && state != GameState.FAILED) {
            return 'C';
        }
        return board[row][column].getSymbol();
    }

    public String getStatusLine() {
        return lastMessage;
    }

    public int calculateScore() {
        int score = 250 - moveCount * 10;
        if (state == GameState.SOLVED) {
            score += 100;
        }
        return Math.max(score, 10);
    }

    public LevelDefinition getDefinition() {
        return definition;
    }

    public Cube getCube() {
        return cube;
    }

    public int getPlayerRow() {
        return playerRow;
    }

    public int getPlayerColumn() {
        return playerColumn;
    }

    public int getMoveCount() {
        return moveCount;
    }

    public int getRemainingPainters() {
        return remainingPainters;
    }

    public GameState getState() {
        return state;
    }

    public int getRowCount() {
        return rowCount;
    }

    public int getColumnCount() {
        return columnCount;
    }
}
