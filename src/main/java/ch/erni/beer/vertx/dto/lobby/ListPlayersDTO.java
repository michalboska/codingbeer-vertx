package ch.erni.beer.vertx.dto.lobby;

import ch.erni.beer.vertx.dto.PongDTO;
import org.vertx.java.core.json.JsonArray;

import java.util.Set;

/**
 * Created by Michal on 6. 12. 2014.
 */
public class ListPlayersDTO extends PongDTO {

    public ListPlayersDTO(Set<String> playerNames) {
        putArray("players", new JsonArray(playerNames.toArray()));
        setStatusOk();
    }

    @Override
    public String getType() {
        return "listPlayers";
    }
}
