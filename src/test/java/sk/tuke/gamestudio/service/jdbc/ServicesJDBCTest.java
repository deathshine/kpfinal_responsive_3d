package sk.tuke.gamestudio.service.jdbc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sk.tuke.gamestudio.entity.Comment;
import sk.tuke.gamestudio.entity.Rating;
import sk.tuke.gamestudio.entity.Score;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServicesJDBCTest {
    private static final JdbcConfig TEST_CONFIG = new JdbcConfig(
        "jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "sa",
        ""
    );

    @BeforeEach
    void prepareDatabase() throws Exception {
        try (Connection connection = DriverManager.getConnection(TEST_CONFIG.url(), TEST_CONFIG.user(), TEST_CONFIG.password());
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS score");
            statement.execute("DROP TABLE IF EXISTS comment");
            statement.execute("DROP TABLE IF EXISTS rating");
            statement.execute("CREATE TABLE score (player VARCHAR(128), game VARCHAR(128), points INTEGER, playedOn TIMESTAMP)");
            statement.execute("CREATE TABLE comment (player VARCHAR(128), game VARCHAR(128), comment TEXT, commentedOn TIMESTAMP)");
            statement.execute("CREATE TABLE rating (player VARCHAR(128), game VARCHAR(128), rating INTEGER, ratedOn TIMESTAMP, PRIMARY KEY (player, game))");
        }
    }

    @Test
    void scoreServiceStoresAndReturnsScore() {
        ScoreServiceJDBC service = new ScoreServiceJDBC(TEST_CONFIG);
        service.addScore(new Score("daniil", "cuberoll", 250, new Date()));

        assertEquals(1, service.getTopScores("cuberoll").size());
        assertEquals("daniil", service.getTopScores("cuberoll").get(0).getPlayer());
    }

    @Test
    void commentServiceStoresComment() {
        CommentServiceJDBC service = new CommentServiceJDBC(TEST_CONFIG);
        service.addComment(new Comment("daniil", "cuberoll", "nice game", new Date()));

        assertEquals(1, service.getComments("cuberoll").size());
        assertEquals("nice game", service.getComments("cuberoll").get(0).getComment());
    }

    @Test
    void ratingServiceUpdatesExistingRating() {
        RatingServiceJDBC service = new RatingServiceJDBC(TEST_CONFIG);
        service.setRating(new Rating("daniil", "cuberoll", 3, new Date()));
        service.setRating(new Rating("daniil", "cuberoll", 5, new Date()));

        assertEquals(5, service.getRating("cuberoll", "daniil"));
        assertEquals(5, service.getAverageRating("cuberoll"));
    }
}
