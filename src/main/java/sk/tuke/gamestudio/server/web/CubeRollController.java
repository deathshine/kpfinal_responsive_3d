package sk.tuke.gamestudio.server.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import sk.tuke.gamestudio.entity.Comment;
import sk.tuke.gamestudio.entity.Rating;
import sk.tuke.gamestudio.entity.Score;
import sk.tuke.gamestudio.game.cuberoll.core.Direction;
import sk.tuke.gamestudio.game.cuberoll.core.Field;
import sk.tuke.gamestudio.game.cuberoll.core.GameState;
import sk.tuke.gamestudio.game.cuberoll.core.LevelDefinition;
import sk.tuke.gamestudio.game.cuberoll.core.Levels;
import sk.tuke.gamestudio.service.CommentService;
import sk.tuke.gamestudio.service.RatingService;
import sk.tuke.gamestudio.service.ScoreService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Controller
public class CubeRollController {
    private static final String GAME = "cuberoll";
    private static final int MAX_COMMENT_LENGTH = 300;
    private static final Pattern PLAYER_PATTERN = Pattern.compile("[A-Za-z0-9_-]{2,20}");

    private final ScoreService scoreService;
    private final CommentService commentService;
    private final RatingService ratingService;

    public CubeRollController(ScoreService scoreService, CommentService commentService, RatingService ratingService) {
        this.scoreService = scoreService;
        this.commentService = commentService;
        this.ratingService = ratingService;
    }

    @GetMapping({"/", "/cuberoll"})
    public String index(Model model, HttpSession httpSession) {
        WebGameSession session = getSession(httpSession);
        fillModel(model, session);
        return "cuberoll";
    }

    @PostMapping("/cuberoll/login")
    public String login(@RequestParam String player, HttpSession httpSession) {
        WebGameSession session = getSession(httpSession);
        String normalized = player == null ? "" : player.trim();
        if (PLAYER_PATTERN.matcher(normalized).matches()) {
            session.setPlayerName(normalized);
        }
        return "redirect:/cuberoll";
    }

    @PostMapping("/cuberoll/logout")
    public String logout(HttpSession httpSession) {
        getSession(httpSession).logout();
        return "redirect:/cuberoll";
    }

    @PostMapping("/cuberoll/move")
    public String move(@RequestParam String direction, HttpSession httpSession) {
        WebGameSession session = getSession(httpSession);
        boolean wasPlaying = session.getField().getState() == GameState.PLAYING;
        parseDirection(direction).ifPresent(session.getField()::move);
        if (wasPlaying) {
            saveScoreAfterSolvedLevel(session);
        }
        return "redirect:/cuberoll";
    }

    @PostMapping("/cuberoll/reset")
    public String reset(HttpSession httpSession) {
        getSession(httpSession).resetLevel();
        return "redirect:/cuberoll";
    }

    @PostMapping("/cuberoll/level")
    public String level(@RequestParam int level, HttpSession httpSession) {
        getSession(httpSession).selectLevel(level);
        return "redirect:/cuberoll";
    }

    @PostMapping("/cuberoll/next")
    public String next(HttpSession httpSession) {
        getSession(httpSession).nextLevel();
        return "redirect:/cuberoll";
    }

    @PostMapping("/cuberoll/comment")
    public String addComment(@RequestParam String comment, HttpSession httpSession) {
        WebGameSession session = getSession(httpSession);
        String trimmed = comment == null ? "" : comment.trim();
        if (session.isLoggedIn() && !trimmed.isBlank()) {
            commentService.addComment(new Comment(
                    session.getPlayerName(),
                    GAME,
                    trimmed.substring(0, Math.min(trimmed.length(), MAX_COMMENT_LENGTH)),
                    new Date()
            ));
        }
        return "redirect:/cuberoll";
    }

    @PostMapping("/cuberoll/rating")
    public String setRating(@RequestParam int value, HttpSession httpSession) {
        WebGameSession session = getSession(httpSession);
        if (session.isLoggedIn() && value >= 1 && value <= 5) {
            ratingService.setRating(new Rating(session.getPlayerName(), GAME, value, new Date()));
        }
        return "redirect:/cuberoll";
    }

    private void fillModel(Model model, WebGameSession session) {
        Field field = session.getField();
        int playerRating = session.isLoggedIn() ? ratingService.getRating(GAME, session.getPlayerName()) : 0;

        model.addAttribute("gameSession", session);
        model.addAttribute("field", field);
        model.addAttribute("board", toBoard(field));
        model.addAttribute("levels", buildLevels());
        model.addAttribute("topScores", scoreService.getTopScores(GAME));
        model.addAttribute("comments", commentService.getComments(GAME));
        model.addAttribute("averageRating", ratingService.getAverageRating(GAME));
        model.addAttribute("playerRating", playerRating);
        model.addAttribute("ratingValues", List.of(1, 2, 3, 4, 5));
        model.addAttribute("anonymousSolved", field.getState() == GameState.SOLVED && !session.isLoggedIn());
    }

    private void saveScoreAfterSolvedLevel(WebGameSession session) {
        Field field = session.getField();
        if (field.getState() == GameState.SOLVED && session.isLoggedIn() && !session.isScoreSaved()) {
            int points = field.calculateScore();
            scoreService.addScore(new Score(session.getPlayerName(), GAME, points, new Date()));
            session.markScoreSaved(points);
        }
    }

    private WebGameSession getSession(HttpSession httpSession) {
        WebGameSession session = (WebGameSession) httpSession.getAttribute(WebGameSession.SESSION_ATTRIBUTE);
        if (session == null) {
            session = new WebGameSession();
            httpSession.setAttribute(WebGameSession.SESSION_ATTRIBUTE, session);
        }
        return session;
    }

    private Optional<Direction> parseDirection(String direction) {
        try {
            return Optional.of(Direction.valueOf(direction.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }

    private List<List<BoardCell>> toBoard(Field field) {
        List<List<BoardCell>> rows = new ArrayList<>();
        for (int row = 0; row < field.getRowCount(); row++) {
            List<BoardCell> cells = new ArrayList<>();
            for (int column = 0; column < field.getColumnCount(); column++) {
                cells.add(boardCellFor(field, row, column));
            }
            rows.add(cells);
        }
        return rows;
    }

    private BoardCell boardCellFor(Field field, int row, int column) {
        char symbol = field.getSymbolAt(row, column);
        return switch (symbol) {
            case 'C' -> new BoardCell("cell player", "C", "cube");
            case '#' -> new BoardCell("cell void", "", "void");
            case '.' -> new BoardCell("cell floor", "", "floor");
            case 'F', 'T' -> new BoardCell("cell finish", "F", "finish");
            case 'r' -> new BoardCell("cell painter red", "r", "red painter");
            case 'R' -> new BoardCell("cell gate red", "R", "red gate");
            case 'b' -> new BoardCell("cell painter blue", "b", "blue painter");
            case 'B' -> new BoardCell("cell gate blue", "B", "blue gate");
            case 'g' -> new BoardCell("cell painter green", "g", "green painter");
            case 'G' -> new BoardCell("cell gate green", "G", "green gate");
            case 'y' -> new BoardCell("cell painter yellow", "y", "yellow painter");
            case 'Y' -> new BoardCell("cell gate yellow", "Y", "yellow gate");
            default -> new BoardCell("cell floor", String.valueOf(symbol), "floor");
        };
    }

    private List<LevelOption> buildLevels() {
        List<LevelOption> levels = new ArrayList<>();
        for (int index = 0; index < Levels.count(); index++) {
            LevelDefinition level = Levels.getLevel(index);
            levels.add(new LevelOption(index, index + 1, level.getName()));
        }
        return levels;
    }

    public static final class BoardCell {
        private final String cssClass;
        private final String label;
        private final String title;

        public BoardCell(String cssClass, String label, String title) {
            this.cssClass = cssClass;
            this.label = label;
            this.title = title;
        }

        public String getCssClass() {
            return cssClass;
        }

        public String getLabel() {
            return label;
        }

        public String getTitle() {
            return title;
        }
    }

    public static final class LevelOption {
        private final int index;
        private final int number;
        private final String name;

        public LevelOption(int index, int number, String name) {
            this.index = index;
            this.number = number;
            this.name = name;
        }

        public int getIndex() {
            return index;
        }

        public int getNumber() {
            return number;
        }

        public String getName() {
            return name;
        }
    }
}
