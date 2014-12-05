package ch.erni.beer.vertx.dto;

import org.vertx.java.core.json.JsonObject;

/**
 * Created by Michal Boska on 5. 12. 2014.
 */
public abstract class PongDTO extends JsonObject {

    public PongDTO() {
        putString("type", getType());
    }

    public abstract String getType();
}
