package sk.tuke.gamestudio.game.cuberoll;

import sk.tuke.gamestudio.game.cuberoll.consoleui.ConsoleUI;
import sk.tuke.gamestudio.game.cuberoll.core.Field;
import sk.tuke.gamestudio.game.cuberoll.core.Levels;
import sk.tuke.gamestudio.service.jdbc.CommentServiceJDBC;
import sk.tuke.gamestudio.service.jdbc.RatingServiceJDBC;
import sk.tuke.gamestudio.service.jdbc.ScoreServiceJDBC;

public class CubeRoll {
    public static void main(String[] args) {
        ConsoleUI consoleUI = new ConsoleUI(
            new ScoreServiceJDBC(),
            new CommentServiceJDBC(),
            new RatingServiceJDBC()
        );
        consoleUI.play(new Field(Levels.getLevel(0)));
    }
}

// lvl 1 = s s d d a a s d w d d d
// lvl 2 = ssdd aasd wddd dssd aaww aaww aa ss sdwd ddds sddd d
//lvl 3 = ssdd aasd wddd dssd aww aaww aaa ss sdwd ddds sddd sdss dwdd