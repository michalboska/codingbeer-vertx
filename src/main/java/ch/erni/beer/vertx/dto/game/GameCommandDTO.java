package ch.erni.beer.vertx.dto.game;

/**
 * Created by bol on 12. 12. 2014.
 */
public class GameCommandDTO extends GameDTO {

    public GameCommandDTO() {
        setCommand("");
    }

    public void setCommand(String command) {
        this.putString("command", command);
    }

    @Override
    public String getType() {
        return "command";
    }
}
