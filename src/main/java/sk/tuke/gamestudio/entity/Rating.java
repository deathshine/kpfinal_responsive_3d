package sk.tuke.gamestudio.entity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.UniqueConstraint;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@Entity
@Table(name = "rating", uniqueConstraints = @UniqueConstraint(columnNames = {"player", "game"}))
@NamedQueries({
    @NamedQuery(name = "Rating.getAverageRating",
        query = "SELECT AVG(r.rating) FROM Rating r WHERE r.game = :game"),
    @NamedQuery(name = "Rating.getPlayerRating",
        query = "SELECT r.rating FROM Rating r WHERE r.game = :game AND r.player = :player"),
    @NamedQuery(name = "Rating.resetRatings", query = "DELETE FROM Rating r")
})
public class Rating implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int ident;

    @Column(nullable = false, length = 128)
    private String player;

    @Column(nullable = false, length = 128)
    private String game;

    @Column(nullable = false)
    private int rating;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "ratedon", nullable = false)
    private Date ratedOn;

    public Rating() {
    }

    public Rating(String player, String game, int rating, Date ratedOn) {
        this.player = Objects.requireNonNull(player, "player");
        this.game = Objects.requireNonNull(game, "game");
        setRating(rating);
        this.ratedOn = new Date(Objects.requireNonNull(ratedOn, "ratedOn").getTime());
    }

    public int getIdent() {
        return ident;
    }

    public void setIdent(int ident) {
        this.ident = ident;
    }

    public String getPlayer() {
        return player;
    }

    public void setPlayer(String player) {
        this.player = Objects.requireNonNull(player, "player");
    }

    public String getGame() {
        return game;
    }

    public void setGame(String game) {
        this.game = Objects.requireNonNull(game, "game");
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }
        this.rating = rating;
    }

    public Date getRatedOn() {
        return ratedOn == null ? null : new Date(ratedOn.getTime());
    }

    public void setRatedOn(Date ratedOn) {
        this.ratedOn = new Date(Objects.requireNonNull(ratedOn, "ratedOn").getTime());
    }
}
