package ch.erni.beer.vertx.dto;

/**
 * Created by Michal Boska on 5. 12. 2014.
 */
public class ErrorDTO extends PongDTO {

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
