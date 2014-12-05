package ch.erni.beer.vertx;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

/**
 * Created by Michal Boska on 2. 12. 2014.
 */
public class ConfigVerticle extends Verticle {

    @Override
    public void start() {

        String httpAddress = Configuration.getString("http.address", container);
        Integer httpPort = Configuration.getInteger("http.port", container);
        Integer numInstances = Runtime.getRuntime().availableProcessors();

        JsonObject object = new JsonObject();
        object.putString(HTTPServerVerticle.CONFIG_ADDRESS, httpAddress);
        object.putNumber(HTTPServerVerticle.CONFIG_PORT, httpPort);

        container.deployVerticle("ch.erni.beer.vertx.HTTPServerVerticle", object, numInstances, r -> {
            if (r.succeeded()) {
                container.logger().info("Starting " + numInstances + " instances of Pong HTTP server at address " + httpAddress + " port:" + httpPort);
                container.deployVerticle("ch.erni.beer.vertx.GameLobbyVerticle", rr -> {
                    if (rr.succeeded()) {
                        container.logger().info("Pong server successfully started");
                    } else {
                        onError(rr.cause());
                    }
                });
            } else {
                onError(r.cause());
            }
        });
    }

    private void onError(Throwable t) {
        container.logger().error("An error has occured while starting Pong server", t);
        container.exit();
    }

}
