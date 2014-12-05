package ch.erni.beer.vertx.dto.lobby;

import ch.erni.beer.vertx.dto.PongDTO;

/**
 * Created by Michal Boska on 5. 12. 2014.
 */
public class ListGamesCommandDTO extends PongDTO {



    @Override
    public String getType() {
        return "listGames";
    }
}
