package ch.erni.beer.vertx;

import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

/**
 * Created by Michal Boska on 3. 12. 2014.
 */
public class HTTPServerVerticle extends Verticle {
    public static final String CONFIG_PORT = "port";
    public static final String CONFIG_ADDRESS = "address";
    public static final String CONFIG_ALLOWED_ENDPOINTS_IN = "allowedEndpointsIn";
    public static final String CONFIG_ALLOWED_ENDPOINTS_OUT = "allowedEndpointsOut";

    @Override
    public void start() {
        HttpServer httpServer = vertx.createHttpServer();
        httpServer.requestHandler(r -> {
            String file = "";
            if (r.path().equals("/")) {
                file = "index.html";
            } else if (!r.path().contains("..")) {
                file = r.path();
            }
            r.response().sendFile("www/" + file, "www/404.html");
        });

        JsonObject sockJsConfig = new JsonObject().putString("prefix", "/eventbus");
        JsonArray allowedEndpointsIn = Configuration.getArray(CONFIG_ALLOWED_ENDPOINTS_IN, container);
        JsonArray allowedEndpointsOut = Configuration.getArray(CONFIG_ALLOWED_ENDPOINTS_OUT, container);
        vertx.createSockJSServer(httpServer).bridge(sockJsConfig, allowedEndpointsIn, allowedEndpointsOut);
        httpServer.listen(Configuration.getInteger(CONFIG_PORT, container), Configuration.getString(CONFIG_ADDRESS, container));
    }
}
