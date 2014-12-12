package ch.erni.beer.vertx.dto.game;

/**
 * Created by bol on 12. 12. 2014.
 */
public class GameStateDTO extends GameDTO{

    public GameStateDTO() {
        setBallPosition(0, 0);
        setPlayer1position(0);
        setPlayer2position(0);
        setPlayer1score(0);
        setPlayer2score(0);
    }

    @Override
    public String getType() {
        return "state";
    }

    public void setPlayer1position(int y) {
        putNumber("player1pos", y);
    }

    public void setPlayer2position(int y) {
        putNumber("player2pos", y);
    }

    public void setPlayer1score(int score) {
        putNumber("player1score", score);
    }

    public void setPlayer2score(int score) {
        putNumber("player2score", score);
    }

    public void setBallPosition(int x, int y) {
        putNumber("ballx", x);
        putNumber("bally", y);
    }
}
