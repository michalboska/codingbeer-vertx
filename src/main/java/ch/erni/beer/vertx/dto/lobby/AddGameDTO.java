package ch.erni.beer.vertx.dto.lobby;

import ch.erni.beer.vertx.dto.EntityCreatedDTO;

/**
 * Created by bol on 8. 12. 2014.
 */
public class AddGameDTO extends EntityCreatedDTO {

    public AddGameDTO(String guid) {
        super(guid);
    }

    @Override
    public String getType() {
        return "addGame";
    }
}
