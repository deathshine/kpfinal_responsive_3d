package sk.tuke.gamestudio.server.webservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import sk.tuke.gamestudio.server.GameStudioServer;
import sk.tuke.gamestudio.service.CommentService;
import sk.tuke.gamestudio.service.RatingService;
import sk.tuke.gamestudio.service.ScoreService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = GameStudioServer.class,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:gamestudio-rest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
    })
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class RestServicesTest {
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
    void scoreEndpointsWork() throws Exception {
        mockMvc.perform(post("/api/score")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"player":"demo","game":"cuberoll","points":111,"playedOn":"2026-03-31T12:00:00.000+00:00"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/score/cuberoll"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("demo")));
    }

    @Test
    void commentEndpointsWork() throws Exception {
        mockMvc.perform(post("/api/comment")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"player":"demo","game":"cuberoll","comment":"hello","commentedOn":"2026-03-31T12:05:00.000+00:00"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/comment/cuberoll"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("hello")));
    }

    @Test
    void ratingEndpointsWork() throws Exception {
        mockMvc.perform(post("/api/rating")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"player":"demo","game":"cuberoll","rating":4,"ratedOn":"2026-03-31T12:10:00.000+00:00"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/rating/cuberoll"))
            .andExpect(status().isOk())
            .andExpect(content().string("4"));

        mockMvc.perform(get("/api/rating/cuberoll/demo"))
            .andExpect(status().isOk())
            .andExpect(content().string("4"));
    }
}
