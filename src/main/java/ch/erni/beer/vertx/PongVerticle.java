package ch.erni.beer.vertx;

import ch.erni.beer.vertx.dto.AsyncHandlerDTO;
import ch.erni.beer.vertx.dto.ErrorDTO;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import java.util.function.Function;

/**
 * Created by Michal on 6. 12. 2014.
 */
public abstract class PongVerticle extends Verticle {

    protected Handler<Message<JsonObject>> createHandler(Function<Message<JsonObject>, JsonObject> handlerFunction) {
        return msg -> {
            JsonObject result = handlerFunction.apply(msg);
            if (result == null) {
                unknownHandlerError(msg);
            } else if (!(result instanceof AsyncHandlerDTO)) {
                msg.reply(result);
            }
            //else, if the result is an instance of AsyncHandlerDTO, do nothing, as the handlerFunction will handle
            //the response itself
        };
    }

    protected void unknownHandlerError(Message msg) {
        msg.reply(new ErrorDTO("No handler registered for this message type"));
    }

}
