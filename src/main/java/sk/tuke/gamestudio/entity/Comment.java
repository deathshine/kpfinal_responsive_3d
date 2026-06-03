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
@Table(name = "comment")
@NamedQueries({
    @NamedQuery(name = "Comment.getComments",
        query = "SELECT c FROM Comment c WHERE c.game = :game ORDER BY c.commentedOn DESC"),
    @NamedQuery(name = "Comment.resetComments", query = "DELETE FROM Comment c")
})
public class Comment implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int ident;

    @Column(nullable = false, length = 128)
    private String player;

    @Column(nullable = false, length = 128)
    private String game;

    @Column(nullable = false, length = 300)
    private String comment;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "commentedon", nullable = false)
    private Date commentedOn;

    public Comment() {
    }

    public Comment(String player, String game, String comment, Date commentedOn) {
        this.player = Objects.requireNonNull(player, "player");
        this.game = Objects.requireNonNull(game, "game");
        this.comment = Objects.requireNonNull(comment, "comment");
        this.commentedOn = new Date(Objects.requireNonNull(commentedOn, "commentedOn").getTime());
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = Objects.requireNonNull(comment, "comment");
    }

    public Date getCommentedOn() {
        return commentedOn == null ? null : new Date(commentedOn.getTime());
    }

    public void setCommentedOn(Date commentedOn) {
        this.commentedOn = new Date(Objects.requireNonNull(commentedOn, "commentedOn").getTime());
    }
}
