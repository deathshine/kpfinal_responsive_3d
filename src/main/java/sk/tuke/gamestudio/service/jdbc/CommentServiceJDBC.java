package sk.tuke.gamestudio.service.jdbc;

import sk.tuke.gamestudio.entity.Comment;
import sk.tuke.gamestudio.service.CommentException;
import sk.tuke.gamestudio.service.CommentService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class CommentServiceJDBC extends AbstractJdbcService implements CommentService {
    private static final String INSERT_COMMENT = "INSERT INTO comment(player, game, comment, commentedon) VALUES (?, ?, ?, ?)";
    private static final String SELECT_COMMENTS = "SELECT player, game, comment, commentedon FROM comment WHERE game = ? ORDER BY commentedon DESC";
    private static final String DELETE_COMMENTS = "DELETE FROM comment";

    public CommentServiceJDBC() {
        super();
    }

    public CommentServiceJDBC(JdbcConfig config) {
        super(config);
    }

    @Override
    public void addComment(Comment comment) throws CommentException {
        try (var connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_COMMENT)) {
            statement.setString(1, comment.getPlayer());
            statement.setString(2, comment.getGame());
            statement.setString(3, comment.getComment());
            statement.setTimestamp(4, new Timestamp(comment.getCommentedOn().getTime()));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new CommentException("Cannot add comment.", e);
        }
    }

    @Override
    public List<Comment> getComments(String game) throws CommentException {
        try (var connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_COMMENTS)) {
            statement.setString(1, game);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Comment> comments = new ArrayList<>();
                while (resultSet.next()) {
                    comments.add(new Comment(
                        resultSet.getString("player"),
                        resultSet.getString("game"),
                        resultSet.getString("comment"),
                        resultSet.getTimestamp("commentedon")
                    ));
                }
                return comments;
            }
        } catch (SQLException e) {
            throw new CommentException("Cannot read comments.", e);
        }
    }

    @Override
    public void reset() throws CommentException {
        try (var connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_COMMENTS)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new CommentException("Cannot reset comments.", e);
        }
    }
}
