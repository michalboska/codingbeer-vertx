package ch.erni.beer.vertx;

import ch.erni.beer.vertx.dto.lobby.PlayerDisconnectDTO;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.sockjs.EventBusBridgeHook;
import org.vertx.java.core.sockjs.SockJSServer;
import org.vertx.java.core.sockjs.SockJSSocket;
import org.vertx.java.platform.Verticle;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Michal Boska on 3. 12. 2014.
 */
public class HTTPServerVerticle extends Verticle implements EventBusBridgeHook {
    public static final String CONFIG_PORT = "port";
    public static final String CONFIG_ADDRESS = "address";
    public static final String CONFIG_ALLOWED_ENDPOINTS_IN = "allowedEndpointsIn";
    public static final String CONFIG_ALLOWED_ENDPOINTS_OUT = "allowedEndpointsOut";
    public static final String TOPIC_SOCKJS_MESSAGES = "ch.erni.beer.vertx.HTTPServerVerticle.topic.sockjs";

    private SockJSServer sockJSServer;
    private Map<String, String> addressToPlayerGuidMap = new HashMap<>();

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
        sockJSServer = vertx.createSockJSServer(httpServer);
        sockJSServer.setHook(this);
        sockJSServer.bridge(sockJsConfig, allowedEndpointsIn, allowedEndpointsOut);
        httpServer.listen(Configuration.getInteger(CONFIG_PORT, container), Configuration.getString(CONFIG_ADDRESS, container));
    }

    @Override
    public boolean handleSocketCreated(SockJSSocket sock) {
        container.logger().info("handleSocketCreated called");
        return true;
    }

    @Override
    public void handleSocketClosed(SockJSSocket sock) {
        String address = sock.remoteAddress().toString();
        String guid = addressToPlayerGuidMap.get(address);
        if (guid == null) {
            //player with such remote address not found, nothing more to do
            return;
        }
        vertx.eventBus().publish(TOPIC_SOCKJS_MESSAGES, new PlayerDisconnectDTO(guid));
    }

    @Override
    public boolean handleSendOrPub(SockJSSocket sock, boolean send, JsonObject msg, String address) {
        //we want to intercept only Lobby messages to listen for Create-Game or Join-Game message
        //These messages contain player GUID, so we can pair player GUID with the socket's remote address
        if (!address.equals(GameLobbyVerticle.QUEUE_LOBBY)) {
            return true;
        }
        JsonObject body = msg.getObject("body");
        if (body == null) {
            return true;
        }
        String type = body.getString("type");
        if (!"addGame".equals(type) && !"joinGame".equals(type)) {
            return true;
        }
        String playerGuid = body.getString("playerGuid");
        if (playerGuid == null) {
            return true;
        }
        addressToPlayerGuidMap.put(sock.remoteAddress().toString(), playerGuid);
        return true;
    }

    @Override
    public boolean handlePreRegister(SockJSSocket sock, String address) {
        container.logger().info("handlePreRegister called");
        return true;
    }

    @Override
    public void handlePostRegister(SockJSSocket sock, String address) {
    }

    @Override
    public boolean handleUnregister(SockJSSocket sock, String address) {
        return true;
    }

    @Override
    public boolean handleAuthorise(JsonObject message, String sessionID, Handler<AsyncResult<Boolean>> handler) {
        container.logger().info("handleAuthorise() called");
        return true;
    }
}
