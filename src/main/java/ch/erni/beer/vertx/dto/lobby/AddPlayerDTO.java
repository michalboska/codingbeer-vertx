package ch.erni.beer.vertx.dto.lobby;

import ch.erni.beer.vertx.dto.EntityCreatedDTO;
import ch.erni.beer.vertx.dto.PongDTO;

/**
 * Created by Michal on 6. 12. 2014.
 */
public class AddPlayerDTO extends EntityCreatedDTO {

    public AddPlayerDTO(String guid) {
        super(guid);
        putString("sessionID", "abcdef");
    }
    @Override
    public String getType() {
        return "addPlayer";
    }
}
