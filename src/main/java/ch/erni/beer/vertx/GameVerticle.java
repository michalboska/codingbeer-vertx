package ch.erni.beer.vertx;

import ch.erni.beer.vertx.dto.ErrorDTO;
import ch.erni.beer.vertx.dto.lobby.AddPlayerDTO;
import ch.erni.beer.vertx.entity.Player;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Michal Boska on 3. 12. 2014.
 */
public class GameVerticle extends PongVerticle {

    private List<Player> players = new ArrayList<>(2);
    private String guid;

    @Override
    public void start() {
        guid = Configuration.getString(Constants.CONFIG_GAME_GUID, container);
        String playerGuid = Configuration.getString(Constants.CONFIG_PLAYER_GUID, container);
        String playerName = Configuration.getString(Constants.CONFIG_PLAYER_NAME, container);
        players.add(new Player(playerName, playerGuid));
        vertx.eventBus().registerHandler(Constants.getPublicQueueAddressForGame(guid), createHandler(this::handlePublicMessages));
        vertx.eventBus().registerHandler(Constants.getPrivateQueueAddressForGame(guid), createHandler(this::handlePrivateMessages));
    }

    private JsonObject handlePublicMessages(Message<JsonObject> message) {
        return null;
    }

    private JsonObject handlePrivateMessages(Message<JsonObject> message) {
        JsonObject result = new JsonObject();
        JsonObject body = message.body();
        switch (body.getString("type")) {
            case Constants.ACTION_ADD_PLAYER:
                result = addPlayer(body);
                break;
        }
        return result;
    }

    private JsonObject addPlayer(JsonObject message) {
        if (players.get(1) != null) {
            return new ErrorDTO("Game is full");
        }
        String playerGuid = Configuration.getMandatoryString(Constants.CONFIG_PLAYER_GUID, message);
        String playerName = Configuration.getMandatoryString(Constants.CONFIG_PLAYER_NAME, message);
        players.set(1, new Player(playerName, playerGuid));
        return new AddPlayerDTO(playerGuid);
    }

    public static class Constants {
        public static final String QUEUE_PUBLIC_PREFIX = "ch.erni.beer.vertx.GameVerticle.queue.public-";
        public static final String QUEUE_PRIVATE_PREFIX = "ch.erni.beer.vertx.GameVerticle.queue.private-";

        public static final String CONFIG_GAME_GUID = "gameGuid";
        public static final String CONFIG_PLAYER_GUID = "playerGuid";
        public static final String CONFIG_PLAYER_NAME = "playerName";

        public static final String ACTION_ADD_PLAYER = "addPlayer";

        public static String getPublicQueueAddressForGame(String guid) {
            return QUEUE_PUBLIC_PREFIX + guid;
        }

        public static String getPrivateQueueAddressForGame(String guid) {
            return QUEUE_PRIVATE_PREFIX + guid;
        }
    }
}
