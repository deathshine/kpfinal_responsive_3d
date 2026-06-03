package sk.tuke.gamestudio.server.web.cuberoll3d;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import sk.tuke.gamestudio.entity.Score;
import sk.tuke.gamestudio.game.cuberoll.core.CellType;
import sk.tuke.gamestudio.game.cuberoll.core.Cube;
import sk.tuke.gamestudio.game.cuberoll.core.Direction;
import sk.tuke.gamestudio.game.cuberoll.core.Field;
import sk.tuke.gamestudio.game.cuberoll.core.GameState;
import sk.tuke.gamestudio.game.cuberoll.core.LevelDefinition;
import sk.tuke.gamestudio.game.cuberoll.core.Levels;
import sk.tuke.gamestudio.game.cuberoll.core.MoveEffect;
import sk.tuke.gamestudio.game.cuberoll.core.MoveOutcome;
import sk.tuke.gamestudio.server.web.WebGameSession;
import sk.tuke.gamestudio.service.ScoreService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping(value = "/api/cuberoll3d", produces = MediaType.APPLICATION_JSON_VALUE)
public class CubeRollApiController {
    private static final String GAME = "cuberoll";

    private final ScoreService scoreService;

    public CubeRollApiController(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    @GetMapping("/state")
    public CubeRollStateDto state(HttpSession httpSession) {
        return toStateDto(getSession(httpSession));
    }

    @PostMapping(value = "/move", consumes = MediaType.APPLICATION_JSON_VALUE)
    public MoveResponseDto move(@RequestBody MoveRequest request, HttpSession httpSession) {
        Direction direction = parseDirection(request == null ? null : request.direction());
        WebGameSession session = getSession(httpSession);
        boolean wasPlaying = session.getField().getState() == GameState.PLAYING;

        MoveOutcome outcome = session.getField().moveWithOutcome(direction);
        boolean scoreSavedThisMove = wasPlaying && saveScoreAfterSolvedLevel(session);

        return new MoveResponseDto(
                toMoveOutcomeDto(outcome),
                toStateDto(session),
                scoreSavedThisMove
        );
    }

    @PostMapping("/reset")
    public CubeRollStateDto reset(HttpSession httpSession) {
        WebGameSession session = getSession(httpSession);
        session.resetLevel();
        return toStateDto(session);
    }

    @PostMapping("/level")
    public CubeRollStateDto level(
            @RequestParam(required = false) Integer level,
            @RequestBody(required = false) LevelRequest request,
            HttpSession httpSession
    ) {
        Integer requestedIndex = request == null ? level : request.requestedIndex();
        if (requestedIndex == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing level index.");
        }

        WebGameSession session = getSession(httpSession);
        session.selectLevel(requestedIndex);
        return toStateDto(session);
    }

    @PostMapping("/next")
    public CubeRollStateDto next(HttpSession httpSession) {
        WebGameSession session = getSession(httpSession);
        session.nextLevel();
        return toStateDto(session);
    }

    @GetMapping("/levels")
    public List<LevelOptionDto> levels() {
        List<LevelOptionDto> result = new ArrayList<>();
        for (int index = 0; index < Levels.count(); index++) {
            result.add(toLevelOptionDto(index, Levels.getLevel(index)));
        }
        return result;
    }

    private WebGameSession getSession(HttpSession httpSession) {
        WebGameSession session = (WebGameSession) httpSession.getAttribute(WebGameSession.SESSION_ATTRIBUTE);
        if (session == null) {
            session = new WebGameSession();
            httpSession.setAttribute(WebGameSession.SESSION_ATTRIBUTE, session);
        }
        return session;
    }

    private boolean saveScoreAfterSolvedLevel(WebGameSession session) {
        Field field = session.getField();
        if (field.getState() == GameState.SOLVED && session.isLoggedIn() && !session.isScoreSaved()) {
            int points = field.calculateScore();
            scoreService.addScore(new Score(session.getPlayerName(), GAME, points, new Date()));
            session.markScoreSaved(points);
            return true;
        }
        return false;
    }

    private Direction parseDirection(String direction) {
        if (direction == null || direction.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing move direction.");
        }
        try {
            return Direction.valueOf(direction.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported move direction: " + direction, e);
        }
    }

    private CubeRollStateDto toStateDto(WebGameSession session) {
        Field field = session.getField();
        LevelDefinition level = field.getDefinition();

        return new CubeRollStateDto(
                session.getLevelIndex(),
                session.getLevelNumber(),
                Levels.count(),
                level.getName(),
                session.isLastLevel(),
                level.isFinishRequiresAllPainters(),
                field.getRowCount(),
                field.getColumnCount(),
                toCellTypes(field),
                field.getPlayerRow(),
                field.getPlayerColumn(),
                toCubeFacesDto(field.getCube()),
                field.getMoveCount(),
                field.getRemainingPainters(),
                field.calculateScore(),
                field.getState().name(),
                field.getStatusLine(),
                session.isLoggedIn(),
                session.getPlayerName(),
                session.isScoreSaved(),
                session.getLastSavedScore(),
                session.getLastScorePlayer()
        );
    }

    private List<List<String>> toCellTypes(Field field) {
        List<List<String>> rows = new ArrayList<>();
        for (int row = 0; row < field.getRowCount(); row++) {
            List<String> cells = new ArrayList<>();
            for (int column = 0; column < field.getColumnCount(); column++) {
                CellType cellType = field.getCellTypeAt(row, column);
                cells.add(cellType.name());
            }
            rows.add(cells);
        }
        return rows;
    }

    private CubeFacesDto toCubeFacesDto(Cube cube) {
        return new CubeFacesDto(
                cube.getTop().name(),
                cube.getBottom().name(),
                cube.getNorth().name(),
                cube.getSouth().name(),
                cube.getWest().name(),
                cube.getEast().name()
        );
    }

    private MoveOutcomeDto toMoveOutcomeDto(MoveOutcome outcome) {
        return new MoveOutcomeDto(
                outcome.direction().name(),
                outcome.fromRow(),
                outcome.fromColumn(),
                outcome.attemptedRow(),
                outcome.attemptedColumn(),
                outcome.toRow(),
                outcome.toColumn(),
                outcome.destinationCellType() == null ? null : outcome.destinationCellType().name(),
                outcome.bottomFaceAfterRoll().name(),
                outcome.rolled(),
                outcome.playerMoved(),
                outcome.stateBefore().name(),
                outcome.stateAfter().name(),
                outcome.message(),
                outcome.effects().stream().map(MoveEffect::name).toList()
        );
    }

    private LevelOptionDto toLevelOptionDto(int index, LevelDefinition level) {
        String[] rows = level.getRows();
        int rowCount = rows.length;
        int columnCount = rowCount == 0 ? 0 : rows[0].length();
        return new LevelOptionDto(
                index,
                index + 1,
                level.getName(),
                rowCount,
                columnCount,
                level.isFinishRequiresAllPainters()
        );
    }
}
