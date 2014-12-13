package ch.erni.beer.vertx;

import ch.erni.beer.vertx.dto.AsyncHandlerDTO;
import ch.erni.beer.vertx.dto.ErrorDTO;
import ch.erni.beer.vertx.dto.lobby.*;
import ch.erni.beer.vertx.entity.Entity;
import ch.erni.beer.vertx.entity.Game;
import ch.erni.beer.vertx.entity.Player;
import org.apache.commons.lang3.StringEscapeUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Michal Boska on 3. 12. 2014.
 */
public class GameLobbyVerticle extends PongVerticle {
    public static final String QUEUE_LOBBY = "ch.erni.beer.vertx.GameLobbyVerticle.queue";

    private static final String ERROR_NO_SUCH_PLAYER = "No such player exists";
    private static final String ERROR_NO_SUCH_GAME = "No such game exists";

    private Map<String, Game> activeGames = new HashMap<>();
    private Map<String, Player> activePlayers = new HashMap<>();

    private Game joinableGame;


    @Override
    public void start() {
        vertx.eventBus().registerHandler(QUEUE_LOBBY, createHandler(this::handleMessage));
    }

    private JsonObject handleMessage(Message<JsonObject> message) {
        JsonObject result = null;
        JsonObject body = message.body();
        switch (body.getString("type")) {
            case "addPlayer":
                container.logger().info("Adding a new player");
                result = addPlayer(body);
                break;
            case "addGame":
                container.logger().info("Creating a new game");
                result = addGame(message);
                break;
            case "joinGame":
                container.logger().info("Joining an existing game");
                result = joinGame(message);
                break;
            case "getAvailableGame":
                container.logger().info("Getting available game");
                result = getAvailableGame();
                break;
            case "listPlayers":
                container.logger().info("Listing players");
                result = listPlayers();
                break;
        }
        if (result != null && ErrorDTO.isError(result)) {
            container.logger().error(result.getString("error"));
        }
        return result;
    }

    private JsonObject addPlayer(JsonObject message) {
        String name = message.getString("name");
        boolean exists = activePlayers.values().stream().anyMatch(p -> p.getName().equalsIgnoreCase(name));
        if (exists) {
            return new ErrorDTO("Player name already exists");
        }
        Player player = new Player(StringEscapeUtils.escapeHtml4(name), Entity.generateGUID());
        activePlayers.put(player.getGuid(), player);
        return new AddPlayerDTO(player.getGuid());
    }

    private JsonObject addGame(Message<JsonObject> message) {
        JsonObject body = message.body();
        String name = body.getString("name");
        boolean exists = activeGames.values().stream().anyMatch(g -> g.getName().equalsIgnoreCase(name));
        if (exists) {
            return new ErrorDTO("Game with this name already exists");
        }
        String playerGuid = body.getString("playerGuid");
        Player player = activePlayers.get(playerGuid);
        if (player == null) {
            return new ErrorDTO(ERROR_NO_SUCH_PLAYER);
        }
        String guid = Entity.generateGUID();
        Game game = new Game(StringEscapeUtils.escapeHtml4(name), guid, player);
        activeGames.put(guid, game);
        //deploy and configure a new game verticle
        JsonObject config = new JsonObject();
        config.putString(GameVerticle.Constants.CONFIG_GAME_GUID, guid);
        config.putString(GameVerticle.Constants.CONFIG_PLAYER_GUID, playerGuid);
        config.putString(GameVerticle.Constants.CONFIG_PLAYER_NAME, player.getName());
        //todo: stop finished games
        container.deployVerticle(GameVerticle.class.getName(), config, result -> {
            if (result.succeeded()) {
                message.reply(new AddGameDTO(guid));
            } else {
                message.reply(new ErrorDTO(result.cause()));
            }
        });
        joinableGame = game;
        return AsyncHandlerDTO.getInstance();
    }

    private JsonObject joinGame(Message<JsonObject> message) {
        JsonObject body = message.body();
        String playerGuid = body.getString("playerGuid");
        String gameGuid = body.getString("gameGuid");
        Player player = activePlayers.get(playerGuid);
        if (player == null) {
            return new ErrorDTO(ERROR_NO_SUCH_PLAYER);
        }
        Game game = activeGames.get(gameGuid);
        if (game == null) {
            return new ErrorDTO(ERROR_NO_SUCH_GAME);
        }
        if (game.isFull()) {
            return new ErrorDTO("Game is full");
        }
        game.addSecondPlayer(player);
        //send message to existing verticle that new player has joined
        String address = GameVerticle.Constants.getPrivateQueueAddressForGame(gameGuid);
        JsonObject joinMessage = new JsonObject();
        joinMessage.putString("type", GameVerticle.Constants.ACTION_ADD_PLAYER);
        joinMessage.putString(GameVerticle.Constants.CONFIG_PLAYER_GUID, playerGuid);
        joinMessage.putString(GameVerticle.Constants.CONFIG_PLAYER_NAME, player.getName());
        vertx.eventBus().send(address, joinMessage, (Handler<Message<JsonObject>>) objReply -> {
            JsonObject reply = objReply.body();
            if (!ErrorDTO.isError(reply)) {
                joinableGame = null;
                message.reply(new JoinGameDTO());
            } else {
                message.reply(reply);
            }
        });
        return AsyncHandlerDTO.getInstance();
    }

    private JsonObject getAvailableGame() {
        return new AvailableGameDTO(joinableGame);
    }

    private JsonObject listPlayers() {
        String[] strings = activePlayers.values().stream().map(p -> p.getName()).toArray(size -> new String[size]);
        return new ListPlayersDTO(strings);
    }

}

