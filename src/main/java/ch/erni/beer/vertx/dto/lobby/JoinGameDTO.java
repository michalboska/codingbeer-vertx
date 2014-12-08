package ch.erni.beer.vertx.dto.lobby;

import ch.erni.beer.vertx.dto.PongDTO;

/**
 * Created by bol on 8. 12. 2014.
 */
public class JoinGameDTO extends PongDTO {

    public JoinGameDTO() {
        setStatusOk();
    }

    @Override
    public String getType() {
        return "joinGame";
    }
}
