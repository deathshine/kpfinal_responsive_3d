package sk.tuke.gamestudio.service.jdbc;

import sk.tuke.gamestudio.entity.Rating;
import sk.tuke.gamestudio.service.RatingException;
import sk.tuke.gamestudio.service.RatingService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class RatingServiceJDBC extends AbstractJdbcService implements RatingService {
    private static final String INSERT_RATING = "INSERT INTO rating(player, game, rating, ratedon) VALUES (?, ?, ?, ?)";
    private static final String UPDATE_RATING = "UPDATE rating SET rating = ?, ratedon = ? WHERE player = ? AND game = ?";
    private static final String SELECT_AVERAGE = "SELECT AVG(rating) AS avg_rating FROM rating WHERE game = ?";
    private static final String SELECT_PLAYER_RATING = "SELECT rating FROM rating WHERE game = ? AND player = ?";
    private static final String DELETE_RATINGS = "DELETE FROM rating";

    public RatingServiceJDBC() {
        super();
    }

    public RatingServiceJDBC(JdbcConfig config) {
        super(config);
    }

    @Override
    public void setRating(Rating rating) throws RatingException {
        try (var connection = getConnection();
             PreparedStatement updateStatement = connection.prepareStatement(UPDATE_RATING)) {
            updateStatement.setInt(1, rating.getRating());
            updateStatement.setTimestamp(2, new Timestamp(rating.getRatedOn().getTime()));
            updateStatement.setString(3, rating.getPlayer());
            updateStatement.setString(4, rating.getGame());
            int updatedRows = updateStatement.executeUpdate();
            if (updatedRows == 0) {
                try (PreparedStatement insertStatement = connection.prepareStatement(INSERT_RATING)) {
                    insertStatement.setString(1, rating.getPlayer());
                    insertStatement.setString(2, rating.getGame());
                    insertStatement.setInt(3, rating.getRating());
                    insertStatement.setTimestamp(4, new Timestamp(rating.getRatedOn().getTime()));
                    insertStatement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RatingException("Cannot set rating.", e);
        }
    }

    @Override
    public int getAverageRating(String game) throws RatingException {
        try (var connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_AVERAGE)) {
            statement.setString(1, game);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    double average = resultSet.getDouble("avg_rating");
                    if (resultSet.wasNull()) {
                        return 0;
                    }
                    return (int) Math.round(average);
                }
                return 0;
            }
        } catch (SQLException e) {
            throw new RatingException("Cannot read average rating.", e);
        }
    }

    @Override
    public int getRating(String game, String player) throws RatingException {
        try (var connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_PLAYER_RATING)) {
            statement.setString(1, game);
            statement.setString(2, player);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("rating");
                }
                return 0;
            }
        } catch (SQLException e) {
            throw new RatingException("Cannot read player rating.", e);
        }
    }

    @Override
    public void reset() throws RatingException {
        try (var connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_RATINGS)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RatingException("Cannot reset ratings.", e);
        }
    }
}
