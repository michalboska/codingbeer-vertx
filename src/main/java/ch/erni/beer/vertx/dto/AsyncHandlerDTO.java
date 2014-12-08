package ch.erni.beer.vertx.dto;

import org.vertx.java.core.json.JsonObject;

/**
 * Created by bol on 8. 12. 2014.
 */
public class AsyncHandlerDTO extends JsonObject {

    public static AsyncHandlerDTO getInstance() {
        return _instance;
    }

    private static final AsyncHandlerDTO _instance = new AsyncHandlerDTO();
    private AsyncHandlerDTO(){}
}
