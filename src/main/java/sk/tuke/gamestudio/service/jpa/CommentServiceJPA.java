package sk.tuke.gamestudio.service.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import sk.tuke.gamestudio.entity.Comment;
import sk.tuke.gamestudio.service.CommentException;
import sk.tuke.gamestudio.service.CommentService;

import java.util.List;

@Transactional
public class CommentServiceJPA implements CommentService {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void addComment(Comment comment) throws CommentException {
        try {
            entityManager.persist(comment);
        } catch (Exception e) {
            throw new CommentException("Cannot add comment via JPA.", e);
        }
    }

    @Override
    public List<Comment> getComments(String game) throws CommentException {
        try {
            return entityManager.createNamedQuery("Comment.getComments", Comment.class)
                .setParameter("game", game)
                .getResultList();
        } catch (Exception e) {
            throw new CommentException("Cannot read comments via JPA.", e);
        }
    }

    @Override
    public void reset() throws CommentException {
        try {
            entityManager.createNamedQuery("Comment.resetComments").executeUpdate();
        } catch (Exception e) {
            throw new CommentException("Cannot reset comments via JPA.", e);
        }
    }
}
