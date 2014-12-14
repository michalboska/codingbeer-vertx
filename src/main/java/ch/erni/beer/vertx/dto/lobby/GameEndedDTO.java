package ch.erni.beer.vertx.dto.lobby;

import ch.erni.beer.vertx.dto.PongDTO;

/**
 * Created by Michal on 14. 12. 2014.
 */
public class GameEndedDTO extends PongDTO {

    public GameEndedDTO(String gameGuid) {
        putString("guid", gameGuid);
    }

    @Override
    public String getType() {
        return "gameEnded";
    }
}
