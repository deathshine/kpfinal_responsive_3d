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

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@Entity
@Table(name = "score")
@NamedQueries({
    @NamedQuery(name = "Score.getTopScores",
        query = "SELECT s FROM Score s WHERE s.game = :game ORDER BY s.points DESC, s.playedOn ASC"),
    @NamedQuery(name = "Score.resetScores", query = "DELETE FROM Score s")
})
public class Score implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int ident;

    @Column(nullable = false, length = 128)
    private String player;

    @Column(nullable = false, length = 128)
    private String game;

    @Column(nullable = false)
    private int points;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "playedon", nullable = false)
    private Date playedOn;

    public Score() {
    }

    public Score(String player, String game, int points, Date playedOn) {
        this.player = Objects.requireNonNull(player, "player");
        this.game = Objects.requireNonNull(game, "game");
        this.points = points;
        this.playedOn = new Date(Objects.requireNonNull(playedOn, "playedOn").getTime());
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

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public Date getPlayedOn() {
        return playedOn == null ? null : new Date(playedOn.getTime());
    }

    public void setPlayedOn(Date playedOn) {
        this.playedOn = new Date(Objects.requireNonNull(playedOn, "playedOn").getTime());
    }
}
