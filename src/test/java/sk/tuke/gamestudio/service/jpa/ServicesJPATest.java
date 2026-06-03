package sk.tuke.gamestudio.service.jpa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import sk.tuke.gamestudio.server.GameStudioServer;
import sk.tuke.gamestudio.entity.Comment;
import sk.tuke.gamestudio.entity.Rating;
import sk.tuke.gamestudio.entity.Score;
import sk.tuke.gamestudio.service.CommentService;
import sk.tuke.gamestudio.service.RatingService;
import sk.tuke.gamestudio.service.ScoreService;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = GameStudioServer.class, webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:gamestudio-jpa;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
    })
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ServicesJPATest {
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
    void scoreServiceStoresAndReturnsScore() {
        scoreService.addScore(new Score("daniil", "cuberoll", 420, new Date()));

        assertEquals(1, scoreService.getTopScores("cuberoll").size());
        assertEquals("daniil", scoreService.getTopScores("cuberoll").get(0).getPlayer());
    }

    @Test
    void commentServiceStoresComment() {
        commentService.addComment(new Comment("daniil", "cuberoll", "jpa works", new Date()));

        assertEquals(1, commentService.getComments("cuberoll").size());
        assertEquals("jpa works", commentService.getComments("cuberoll").get(0).getComment());
    }

    @Test
    void ratingServiceUpdatesExistingRating() {
        ratingService.setRating(new Rating("daniil", "cuberoll", 2, new Date()));
        ratingService.setRating(new Rating("daniil", "cuberoll", 5, new Date()));

        assertEquals(5, ratingService.getRating("cuberoll", "daniil"));
        assertEquals(5, ratingService.getAverageRating("cuberoll"));
    }
}
