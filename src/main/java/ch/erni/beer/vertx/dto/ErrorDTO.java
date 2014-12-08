package ch.erni.beer.vertx.dto;

import org.vertx.java.core.json.JsonObject;

/**
 * Created by Michal Boska on 5. 12. 2014.
 */
public class ErrorDTO extends PongDTO {

    public static boolean isError(JsonObject jsonObject) {
        return jsonObject.containsField("error") && jsonObject.getString("error") != null;
    }

    public ErrorDTO(String message) {
        putString("message", message);
    }

    public ErrorDTO(Throwable throwable) {
        putString("message", throwable.getMessage());
    }

    @Override
    public String getType() {
        return "error";
    }
}
