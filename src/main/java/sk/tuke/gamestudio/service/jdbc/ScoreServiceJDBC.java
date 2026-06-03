package sk.tuke.gamestudio.service.jdbc;

import sk.tuke.gamestudio.entity.Score;
import sk.tuke.gamestudio.service.ScoreException;
import sk.tuke.gamestudio.service.ScoreService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ScoreServiceJDBC extends AbstractJdbcService implements ScoreService {
    private static final String INSERT_SCORE = "INSERT INTO score(player, game, points, playedon) VALUES (?, ?, ?, ?)";
    private static final String SELECT_TOP_SCORES = "SELECT player, game, points, playedon FROM score WHERE game = ? ORDER BY points DESC, playedon ASC LIMIT 10";
    private static final String DELETE_SCORES = "DELETE FROM score";

    public ScoreServiceJDBC() {
        super();
    }

    public ScoreServiceJDBC(JdbcConfig config) {
        super(config);
    }

    @Override
    public void addScore(Score score) throws ScoreException {
        try (var connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SCORE)) {
            statement.setString(1, score.getPlayer());
            statement.setString(2, score.getGame());
            statement.setInt(3, score.getPoints());
            statement.setTimestamp(4, new Timestamp(score.getPlayedOn().getTime()));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new ScoreException("Cannot add score.", e);
        }
    }

    @Override
    public List<Score> getTopScores(String game) throws ScoreException {
        try (var connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_TOP_SCORES)) {
            statement.setString(1, game);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Score> scores = new ArrayList<>();
                while (resultSet.next()) {
                    scores.add(new Score(
                        resultSet.getString("player"),
                        resultSet.getString("game"),
                        resultSet.getInt("points"),
                        resultSet.getTimestamp("playedon")
                    ));
                }
                return scores;
            }
        } catch (SQLException e) {
            throw new ScoreException("Cannot read top scores.", e);
        }
    }

    @Override
    public void reset() throws ScoreException {
        try (var connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_SCORES)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new ScoreException("Cannot reset scores.", e);
        }
    }
}
