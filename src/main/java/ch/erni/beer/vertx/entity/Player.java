package ch.erni.beer.vertx.entity;

/**
 * Created by bol on 8. 12. 2014.
 */
public class Player extends Entity {
    private int position; //player cursor position
    private int score;
    private String inputQueueAddress;

    public Player(String name, String guid) {
        super(name, guid);
        position = 270;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getInputQueueAddress() {
        return inputQueueAddress;
    }

    public void setInputQueueAddress(String gameGuid) {
        this.inputQueueAddress = gameGuid + "-" + guid;
    }
}
