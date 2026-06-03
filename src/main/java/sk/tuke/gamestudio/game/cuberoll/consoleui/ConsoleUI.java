package sk.tuke.gamestudio.game.cuberoll.consoleui;

import sk.tuke.gamestudio.entity.Comment;
import sk.tuke.gamestudio.entity.Rating;
import sk.tuke.gamestudio.entity.Score;
import sk.tuke.gamestudio.game.cuberoll.core.Direction;
import sk.tuke.gamestudio.game.cuberoll.core.Field;
import sk.tuke.gamestudio.game.cuberoll.core.GameState;
import sk.tuke.gamestudio.game.cuberoll.core.Levels;
import sk.tuke.gamestudio.service.CommentException;
import sk.tuke.gamestudio.service.CommentService;
import sk.tuke.gamestudio.service.RatingException;
import sk.tuke.gamestudio.service.RatingService;
import sk.tuke.gamestudio.service.ScoreException;
import sk.tuke.gamestudio.service.ScoreService;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConsoleUI {
    private static final String GAME_NAME = "cuberoll";
    private static final Pattern RATE_PATTERN = Pattern.compile("rate\\s+([1-5])", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMMENT_PATTERN = Pattern.compile("comment\\s+(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLAYER_PATTERN = Pattern.compile("player\\s+([A-Za-z0-9_-]{2,20})", Pattern.CASE_INSENSITIVE);
    private static final Pattern LEVEL_PATTERN = Pattern.compile("level\\s+([1-9][0-9]*)", Pattern.CASE_INSENSITIVE);

    private final Scanner scanner;
    private final ScoreService scoreService;
    private final CommentService commentService;
    private final RatingService ratingService;

    private Field field;
    private int currentLevelIndex;
    private String playerName = "player";

    public ConsoleUI(ScoreService scoreService, CommentService commentService, RatingService ratingService) {
        this(scoreService, commentService, ratingService, new Scanner(System.in));
    }

    public ConsoleUI(ScoreService scoreService, CommentService commentService, RatingService ratingService, Scanner scanner) {
        this.scoreService = scoreService;
        this.commentService = commentService;
        this.ratingService = ratingService;
        this.scanner = scanner;
        this.currentLevelIndex = 0;
    }


    public void play(Field field) {
        this.field = field;
        printWelcome();

        do {
            show();
            handleInput();
        } while (this.field.getState() == GameState.PLAYING);

        show();
        handleEndOfGame();
    }

    public void show() {
        System.out.println();
        System.out.println("=== CubeRoll | Level: " + field.getDefinition().getName() + " | Player: " + playerName + " ===");
        printColumnLegend();

        for (int row = 0; row < field.getRowCount(); row++) {
            System.out.printf("%2s ", (char) ('A' + row));
            for (int column = 0; column < field.getColumnCount(); column++) {
                System.out.printf(" %s", field.getSymbolAt(row, column));
            }
            System.out.println();
        }

        System.out.println("Legend: C=cube, F=finish, .=floor, #=void, r/b/g/y=one-use painters, R/B/G/Y=gates");
        System.out.println("Moves: " + field.getMoveCount());
        if (field.getDefinition().isFinishRequiresAllPainters()) {
            System.out.println("Painters still required for finish: " + field.getRemainingPainters());
        }
        printCubeIndicator();
        System.out.println("Orientation: " + field.getCube().getOrientationSummary());
        System.out.println("Status: " + field.getStatusLine());
        printRatingSummary();
        //System.out.println("Commands: w/a/s/d, new, level N, player NAME, top/score, comments, rate N, comment TEXT, help, x");
    }

    private void printCubeIndicator() {
        System.out.println("Painted faces on cube:");
        System.out.println("        [TOP    " + field.getCube().getTop().getShortCode() + "]");
        System.out.println("        [NORTH  " + field.getCube().getNorth().getShortCode() + "]");
        System.out.println("[WEST   " + field.getCube().getWest().getShortCode() + "] [BOTTOM " + field.getCube().getBottom().getShortCode() + "] [EAST   " + field.getCube().getEast().getShortCode() + "]");
        System.out.println("        [SOUTH  " + field.getCube().getSouth().getShortCode() + "]");
    }

    private void printColumnLegend() {
        System.out.print("   ");
        for (int column = 0; column < field.getColumnCount(); column++) {
            System.out.printf(" %d", column);
        }
        System.out.println();
    }

    public void handleInput() {
        while (field.getState() == GameState.PLAYING) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                System.out.println("Empty command. Type help.");
                continue;
            }

            String normalized = input.toLowerCase(Locale.ROOT);
            switch (normalized) {
                case "w", "up" -> {
                    field.move(Direction.NORTH);
                    return;
                }
                case "s", "down" -> {
                    field.move(Direction.SOUTH);
                    return;
                }
                case "a", "left" -> {
                    field.move(Direction.WEST);
                    return;
                }
                case "d", "right" -> {
                    field.move(Direction.EAST);
                    return;
                }
                case "new", "restart" -> {
                    field.reset();
                    return;
                }
                case "top", "score", "scores" -> printTopScores();
                case "comments" -> printComments();
                case "help" -> printHelp();
                case "x", "q", "quit", "exit" -> {
                    field.exit();
                    return;
                }
                default -> {
                    CommandResult result = handleRegexCommand(input);
                    switch (result) {
                        case REFRESH -> {
                            return;
                        }
                        case HANDLED -> {
                            continue;
                        }
                        case UNKNOWN -> System.out.println("Unknown command. Type help.");
                    }
                }
            }
        }
    }

    private enum CommandResult {
        HANDLED,
        REFRESH,
        UNKNOWN
    }

    private CommandResult handleRegexCommand(String input) {
        Matcher playerMatcher = PLAYER_PATTERN.matcher(input);
        if (playerMatcher.matches()) {
            playerName = playerMatcher.group(1);
            System.out.println("Player set to: " + playerName);
            return CommandResult.HANDLED;
        }

        Matcher levelMatcher = LEVEL_PATTERN.matcher(input);
        if (levelMatcher.matches()) {
            int levelNumber = Integer.parseInt(levelMatcher.group(1));
            if (levelNumber < 1 || levelNumber > Levels.count()) {
                System.out.println("Level must be between 1 and " + Levels.count());
                return CommandResult.HANDLED;
            }
            currentLevelIndex = levelNumber - 1;
            field = new Field(Levels.getLevel(currentLevelIndex));
            System.out.println("Loaded level " + levelNumber + ".");
            return CommandResult.REFRESH;
        }

        Matcher rateMatcher = RATE_PATTERN.matcher(input);
        if (rateMatcher.matches()) {
            int value = Integer.parseInt(rateMatcher.group(1));
            try {
                ratingService.setRating(new Rating(playerName, GAME_NAME, value, new Date()));
                System.out.println("Rating saved: " + value + "/5");
            } catch (RatingException e) {
                System.out.println("Rating service is unavailable: " + e.getMessage());
            }
            return CommandResult.HANDLED;
        }

        Matcher commentMatcher = COMMENT_PATTERN.matcher(input);
        if (commentMatcher.matches()) {
            String text = commentMatcher.group(1).trim();
            try {
                commentService.addComment(new Comment(playerName, GAME_NAME, text, new Date()));
                System.out.println("Comment saved.");
            } catch (CommentException e) {
                System.out.println("Comment service is unavailable: " + e.getMessage());
            }
            return CommandResult.HANDLED;
        }

        return CommandResult.UNKNOWN;
    }

    private void handleEndOfGame() {
        if (field.getState() == GameState.SOLVED) {
            System.out.println("Congratulations, you solved the level.");
            saveScore();
            promptAfterGame();
        } else if (field.getState() == GameState.FAILED) {
            System.out.println("The cube failed the move.");
            promptAfterGame();
        } else if (field.getState() == GameState.EXITED) {
            System.out.println("Game closed by player.");
        }
    }

    private void promptAfterGame() {
        while (true) {
            System.out.print("Choose: [r] replay this level, [n] next level, [x] exit: ");
            String answer = scanner.nextLine().trim().toLowerCase(Locale.ROOT);

            switch (answer) {
                case "r", "replay", "retry" -> {
                    play(new Field(Levels.getLevel(currentLevelIndex)));
                    return;
                }
                case "n", "next" -> {
                    currentLevelIndex = (currentLevelIndex + 1) % Levels.count();
                    play(new Field(Levels.getLevel(currentLevelIndex)));
                    return;
                }
                case "x", "q", "exit", "quit" -> {
                    return;
                }
                default -> System.out.println("Unknown choice. Type r, n or x.");
            }
        }
    }

    private void saveScore() {
        try {
            Score score = new Score(playerName, GAME_NAME, field.calculateScore(), new Date());
            scoreService.addScore(score);
            System.out.println("Score saved: " + score.getPoints());
            printTopScores();
        } catch (ScoreException e) {
            System.out.println("Score service is unavailable: " + e.getMessage());
        }
    }

    private void printTopScores() {
        try {
            List<Score> scores = scoreService.getTopScores(GAME_NAME);
            if (scores.isEmpty()) {
                System.out.println("No scores yet.");
                return;
            }
            System.out.println("Top scores:");
            for (int index = 0; index < scores.size(); index++) {
                Score score = scores.get(index);
                System.out.printf("%2d. %-12s %4d pts%n", index + 1, score.getPlayer(), score.getPoints());
            }
        } catch (ScoreException e) {
            System.out.println("Score service is unavailable: " + e.getMessage());
        }
    }

    private void printComments() {
        try {
            List<Comment> comments = commentService.getComments(GAME_NAME);
            if (comments.isEmpty()) {
                System.out.println("No comments yet.");
                return;
            }
            System.out.println("Comments:");
            for (Comment comment : comments) {
                System.out.println("- " + comment.getPlayer() + ": " + comment.getComment());
            }
        } catch (CommentException e) {
            System.out.println("Comment service is unavailable: " + e.getMessage());
        }
    }

    private void printRatingSummary() {
        try {
            int average = ratingService.getAverageRating(GAME_NAME);
            int ownRating = ratingService.getRating(GAME_NAME, playerName);
            System.out.println("Average rating: " + average + "/5 | Your rating: " + ownRating + "/5");
        } catch (RatingException e) {
            System.out.println("Average rating: unavailable");
        }
    }

    private void printWelcome() {
        System.out.println("Welcome to CubeRoll.");
    }

    private void printHelp() {
        System.out.println("w/a/s/d      - move cube");
        System.out.println("new          - restart current level");
        System.out.println("level N      - load level 1.." + Levels.count());
        System.out.println("player NAME  - change player nickname");
        System.out.println("top / score  - show top scores");
        System.out.println("comments     - show comments");
        System.out.println("rate 1..5    - set rating");
        System.out.println("comment TEXT - save comment");
        System.out.println("Some levels lock F until every lowercase painter tile was used.");
        System.out.println("x            - exit game");
    }
}