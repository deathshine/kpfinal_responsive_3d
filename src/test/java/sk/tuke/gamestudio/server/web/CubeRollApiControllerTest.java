package sk.tuke.gamestudio.server.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import sk.tuke.gamestudio.entity.Score;
import sk.tuke.gamestudio.server.GameStudioServer;
import sk.tuke.gamestudio.service.ScoreService;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = GameStudioServer.class,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:gamestudio-api;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
    })
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CubeRollApiControllerTest {
    private static final String GAME = "cuberoll";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ScoreService scoreService;

    @BeforeEach
    void resetServices() {
        scoreService.reset();
    }

    @Test
    void stateEndpointReturnsRendererReadyGameFacts() throws Exception {
        mockMvc.perform(get("/api/cuberoll3d/state"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.currentLevelIndex").value(0))
            .andExpect(jsonPath("$.currentLevelNumber").value(1))
            .andExpect(jsonPath("$.levelCount").value(3))
            .andExpect(jsonPath("$.levelName").value("Red Door"))
            .andExpect(jsonPath("$.rows").value(6))
            .andExpect(jsonPath("$.columns").value(8))
            .andExpect(jsonPath("$.cells[1][1]").value("FLOOR"))
            .andExpect(jsonPath("$.cells[3][3]").value("PAINTER_RED"))
            .andExpect(jsonPath("$.playerRow").value(1))
            .andExpect(jsonPath("$.playerColumn").value(1))
            .andExpect(jsonPath("$.cubeFaces.bottom").value("NONE"))
            .andExpect(jsonPath("$.moveCount").value(0))
            .andExpect(jsonPath("$.gameState").value("PLAYING"))
            .andExpect(jsonPath("$.loggedIn").value(false));
    }

    @Test
    void levelsEndpointReturnsLevelMetadata() throws Exception {
        mockMvc.perform(get("/api/cuberoll3d/levels"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].index").value(0))
            .andExpect(jsonPath("$[0].number").value(1))
            .andExpect(jsonPath("$[0].name").value("Red Door"))
            .andExpect(jsonPath("$[0].rows").value(6))
            .andExpect(jsonPath("$[0].columns").value(8))
            .andExpect(jsonPath("$[1].finishRequiresAllPainters").value(true));
    }

    @Test
    void moveEndpointReturnsOutcomePlusUpdatedState() throws Exception {
        mockMvc.perform(post("/api/cuberoll3d/move")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"direction\":\"SOUTH\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.outcome.direction").value("SOUTH"))
            .andExpect(jsonPath("$.outcome.fromRow").value(1))
            .andExpect(jsonPath("$.outcome.fromColumn").value(1))
            .andExpect(jsonPath("$.outcome.attemptedRow").value(2))
            .andExpect(jsonPath("$.outcome.attemptedColumn").value(1))
            .andExpect(jsonPath("$.outcome.toRow").value(2))
            .andExpect(jsonPath("$.outcome.toColumn").value(1))
            .andExpect(jsonPath("$.outcome.destinationCellType").value("FLOOR"))
            .andExpect(jsonPath("$.outcome.rolled").value(true))
            .andExpect(jsonPath("$.outcome.playerMoved").value(true))
            .andExpect(jsonPath("$.outcome.effects", hasItems("ROLLED", "MOVED")))
            .andExpect(jsonPath("$.state.playerRow").value(2))
            .andExpect(jsonPath("$.state.playerColumn").value(1))
            .andExpect(jsonPath("$.state.moveCount").value(1))
            .andExpect(jsonPath("$.scoreSavedThisMove").value(false));
    }

    @Test
    void levelResetAndNextEndpointsReturnFreshState() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/cuberoll3d/level")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"levelIndex\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentLevelIndex").value(1))
            .andExpect(jsonPath("$.levelName").value("Red And Blue"));

        mockMvc.perform(post("/api/cuberoll3d/move")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"direction\":\"SOUTH\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.state.moveCount").value(1));

        mockMvc.perform(post("/api/cuberoll3d/reset").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentLevelIndex").value(1))
            .andExpect(jsonPath("$.moveCount").value(0))
            .andExpect(jsonPath("$.gameState").value("PLAYING"));

        mockMvc.perform(post("/api/cuberoll3d/next").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentLevelIndex").value(2))
            .andExpect(jsonPath("$.levelName").value("Three Colors"));
    }


    @Test
    void invalidApiInputIsRejectedOrSafelyNormalized() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/cuberoll3d/move")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"direction\":\"DIAGONAL\"}"))
            .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/cuberoll3d/level")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"levelIndex\":999}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentLevelIndex").value(2))
            .andExpect(jsonPath("$.gameState").value("PLAYING"));
    }

    @Test
    void apiSavesScoreExactlyOnceForLoggedInSolver() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/cuberoll/login").session(session).param("player", "solver3d"))
            .andExpect(status().is3xxRedirection());

        ResultActions lastMove = null;
        for (char step : "SSEEWWSENEEE".toCharArray()) {
            lastMove = mockMvc.perform(post("/api/cuberoll3d/move")
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"direction\":\"" + toDirection(step) + "\"}"))
                .andExpect(status().isOk());
        }

        lastMove
            .andExpect(jsonPath("$.state.gameState").value("SOLVED"))
            .andExpect(jsonPath("$.state.scoreSaved").value(true))
            .andExpect(jsonPath("$.scoreSavedThisMove").value(true));

        List<Score> topScores = scoreService.getTopScores(GAME);
        assertEquals(1, topScores.size());
        assertEquals("solver3d", topScores.get(0).getPlayer());
        assertEquals(230, topScores.get(0).getPoints());

        mockMvc.perform(post("/api/cuberoll3d/move")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"direction\":\"EAST\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.outcome.effects", hasItems("IGNORED_NOT_PLAYING")))
            .andExpect(jsonPath("$.scoreSavedThisMove").value(false));

        assertEquals(1, scoreService.getTopScores(GAME).size());

        mockMvc.perform(get("/cuberoll").session(session))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Score <strong>230</strong> was saved")));
    }

    @Test
    void invalidMoveDirectionIsRejectedWithoutMutatingState() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/cuberoll3d/move")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"direction\":\"DIAGONAL\"}"))
            .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/cuberoll3d/state").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.playerRow").value(1))
            .andExpect(jsonPath("$.playerColumn").value(1))
            .andExpect(jsonPath("$.moveCount").value(0))
            .andExpect(jsonPath("$.gameState").value("PLAYING"));
    }

    private String toDirection(char step) {
        return switch (step) {
            case 'N' -> "NORTH";
            case 'S' -> "SOUTH";
            case 'W' -> "WEST";
            case 'E' -> "EAST";
            default -> throw new IllegalArgumentException("Unsupported step: " + step);
        };
    }
}
