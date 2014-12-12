package ch.erni.beer.vertx.dto.lobby;

import ch.erni.beer.vertx.dto.PongDTO;
import org.vertx.java.core.json.JsonArray;

/**
 * Created by Michal on 6. 12. 2014.
 */
public class ListPlayersDTO extends PongDTO {

    public ListPlayersDTO(String[] playerNames) {
        putArray("players", new JsonArray(playerNames));
        setStatusOk();
    }

    @Override
    public String getType() {
        return "listPlayers";
    }
}
