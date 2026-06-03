package sk.tuke.gamestudio.server.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import sk.tuke.gamestudio.entity.Score;
import sk.tuke.gamestudio.server.GameStudioServer;
import sk.tuke.gamestudio.service.CommentService;
import sk.tuke.gamestudio.service.RatingService;
import sk.tuke.gamestudio.service.ScoreService;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = GameStudioServer.class,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:gamestudio-web;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
    })
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CubeRollWebControllerTest {
    private static final String GAME = "cuberoll";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ScoreService scoreService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private RatingService ratingService;

    @BeforeEach
    void resetServices() {
        scoreService.reset();
        commentService.reset();
        ratingService.reset();
    }

    @Test
    void anonymousUserCanOpenGameAndViewServiceSections() throws Exception {
        mockMvc.perform(get("/cuberoll"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Login")))
            .andExpect(content().string(containsString("Top scores")))
            .andExpect(content().string(containsString("Comments")))
            .andExpect(content().string(containsString("Open 3D PlayCanvas")))
            .andExpect(content().string(containsString("cuberoll-playcanvas.html")))
            .andExpect(content().string(containsString("Average: 0 / 5")));
    }

    @Test
    void playCanvasStaticPrototypeIsServedBySpring() throws Exception {
        mockMvc.perform(get("/cuberoll-playcanvas.html"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("application-canvas")))
            .andExpect(content().string(containsString("webgl-fallback")))
            .andExpect(content().string(containsString("cuberoll-playcanvas.js")))
            .andExpect(content().string(containsString("Classic UI")));

        mockMvc.perform(get("/cuberoll-playcanvas.js"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("/api/cuberoll3d")))
            .andExpect(content().string(containsString("supportsWebGL")))
            .andExpect(content().string(containsString("class ApiClient")))
            .andExpect(content().string(containsString("class MaterialFactory")))
            .andExpect(content().string(containsString("class BoardRenderer")))
            .andExpect(content().string(containsString("class CubeRenderer")))
            .andExpect(content().string(containsString("class AnimationController")))
            .andExpect(content().string(containsString("class HUDController")))
            .andExpect(content().string(containsString("class InputController")))
            .andExpect(content().string(containsString("submitMove(direction)")))
            .andExpect(content().string(containsString("animateOutcome")))
            .andExpect(content().string(containsString("POST")))
            .andExpect(content().string(containsString("response.outcome")));

        mockMvc.perform(get("/cuberoll-playcanvas.css"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("drift-stars")))
            .andExpect(content().string(containsString("status-orb")))
            .andExpect(content().string(containsString("is-failed")))
            .andExpect(content().string(containsString("is-solved")))
            .andExpect(content().string(containsString("webgl-fallback")));
    }

    @Test
    void loggedUserCanAddCommentAndUpdateRating() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/cuberoll/login").session(session).param("player", "tester"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/cuberoll"));

        mockMvc.perform(post("/cuberoll/comment").session(session).param("comment", "hello from web"))
            .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/cuberoll/rating").session(session).param("value", "2"))
            .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/cuberoll/rating").session(session).param("value", "5"))
            .andExpect(status().is3xxRedirection());

        assertEquals(1, commentService.getComments(GAME).size());
        assertEquals("hello from web", commentService.getComments(GAME).get(0).getComment());
        assertEquals(5, ratingService.getRating(GAME, "tester"));
        assertEquals(5, ratingService.getAverageRating(GAME));

        mockMvc.perform(get("/cuberoll").session(session))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("hello from web")))
            .andExpect(content().string(containsString("Your rating: 5 / 5")));
    }

    @Test
    void scoreIsSavedAutomaticallyAfterSolvedLevel() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/cuberoll/login").session(session).param("player", "solver"))
            .andExpect(status().is3xxRedirection());

        for (char step : "SSEEWWSENEEE".toCharArray()) {
            mockMvc.perform(post("/cuberoll/move").session(session).param("direction", toDirection(step)))
                .andExpect(status().is3xxRedirection());
        }

        List<Score> topScores = scoreService.getTopScores(GAME);
        assertEquals(1, topScores.size());
        assertEquals("solver", topScores.get(0).getPlayer());
        assertEquals(230, topScores.get(0).getPoints());

        mockMvc.perform(get("/cuberoll").session(session))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Score <strong>230</strong> was saved")));
    }

    @Test
    void anonymousSolvedLevelIsNotSavedAfterLaterLogin() throws Exception {
        MockHttpSession session = new MockHttpSession();

        for (char step : "SSEEWWSENEEE".toCharArray()) {
            mockMvc.perform(post("/cuberoll/move").session(session).param("direction", toDirection(step)))
                .andExpect(status().is3xxRedirection());
        }

        assertEquals(0, scoreService.getTopScores(GAME).size());

        mockMvc.perform(post("/cuberoll/login").session(session).param("player", "lateuser"))
            .andExpect(status().is3xxRedirection());

        assertEquals(0, scoreService.getTopScores(GAME).size());
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
