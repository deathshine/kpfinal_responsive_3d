package sk.tuke.gamestudio.service.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import sk.tuke.gamestudio.entity.Rating;
import sk.tuke.gamestudio.service.RatingException;
import sk.tuke.gamestudio.service.RatingService;

@Transactional
public class RatingServiceJPA implements RatingService {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void setRating(Rating rating) throws RatingException {
        try {
            var existingRatings = entityManager.createQuery(
                    "SELECT r FROM Rating r WHERE r.game = :game AND r.player = :player", Rating.class)
                .setParameter("game", rating.getGame())
                .setParameter("player", rating.getPlayer())
                .getResultList();

            if (existingRatings.isEmpty()) {
                entityManager.persist(rating);
            } else {
                Rating existing = existingRatings.get(0);
                existing.setRating(rating.getRating());
                existing.setRatedOn(rating.getRatedOn());
                entityManager.merge(existing);
            }
        } catch (Exception e) {
            throw new RatingException("Cannot set rating via JPA.", e);
        }
    }

    @Override
    public int getAverageRating(String game) throws RatingException {
        try {
            Double result = entityManager.createNamedQuery("Rating.getAverageRating", Double.class)
                .setParameter("game", game)
                .getSingleResult();
            return result == null ? 0 : (int) Math.round(result);
        } catch (NoResultException e) {
            return 0;
        } catch (Exception e) {
            throw new RatingException("Cannot read average rating via JPA.", e);
        }
    }

    @Override
    public int getRating(String game, String player) throws RatingException {
        try {
            Integer result = entityManager.createNamedQuery("Rating.getPlayerRating", Integer.class)
                .setParameter("game", game)
                .setParameter("player", player)
                .getSingleResult();
            return result == null ? 0 : result;
        } catch (NoResultException e) {
            return 0;
        } catch (Exception e) {
            throw new RatingException("Cannot read player rating via JPA.", e);
        }
    }

    @Override
    public void reset() throws RatingException {
        try {
            entityManager.createNamedQuery("Rating.resetRatings").executeUpdate();
        } catch (Exception e) {
            throw new RatingException("Cannot reset ratings via JPA.", e);
        }
    }
}
