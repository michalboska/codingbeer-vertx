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
import java.util.TreeMap;
import java.util.stream.IntStream;

/**
 * Created by Michal Boska on 3. 12. 2014.
 */
public class GameLobbyVerticle extends PongVerticle {
    public static final String QUEUE_LOBBY = "ch.erni.beer.vertx.GameLobbyVerticle.queue";
    public static final String QUEUE_LOBBY_PRIVATE = "ch.erni.beer.vertx.GameLobbyVerticle.private-queue";

    private static final String ERROR_NO_SUCH_PLAYER = "No such player exists";
    private static final String ERROR_NO_SUCH_GAME = "No such game exists";

    private Map<String, Game> activeGames = new HashMap<>();
    private Map<String, Game> activeGamesByName = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private Map<String, Player> activePlayers = new HashMap<>();
    private Map<String, Player> activePlayersByName = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private Map<String, String> deploymentIDs = new HashMap<>();

    private Game joinableGame;


    @Override
    public void start() {
        vertx.eventBus().registerHandler(QUEUE_LOBBY, createHandler(this::handleMessage));
        vertx.eventBus().registerHandler(QUEUE_LOBBY_PRIVATE, createHandler(this::handlePrivateMessage));
        vertx.eventBus().registerHandler(HTTPServerVerticle.TOPIC_SOCKJS_MESSAGES, createHandler(this::handleSocketMessage));
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

    private JsonObject handlePrivateMessage(Message<JsonObject> message) {
        JsonObject result = null;
        JsonObject body = message.body();
        switch (body.getString("type")) {
            case "gameEnded":
                result = endGame(body);
                break;
        }
        return result;
    }

    private JsonObject handleSocketMessage(Message<JsonObject> message) {
        JsonObject body = message.body();
        if (body != null && "disconnect".equals(body.getString("type"))) {
            playerDisconnected(body.getString("playerGuid"));
        }
        return AsyncHandlerDTO.getInstance();
    }

    private JsonObject addPlayer(JsonObject message) {
        String name = message.getString("name");
        boolean exists = activePlayersByName.containsKey(name);
        if (exists) {
            return new ErrorDTO("Player name already exists");
        }
        Player player = new Player(StringEscapeUtils.escapeHtml4(name), Entity.generateGUID());
        activePlayers.put(player.getGuid(), player);
        activePlayersByName.put(player.getName(), player);
        return new AddPlayerDTO(player.getGuid());
    }

    private JsonObject addGame(Message<JsonObject> message) {
        JsonObject body = message.body();
        String name = body.getString("name");
        boolean exists = activeGames.containsKey(name);
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
        activeGamesByName.put(name, game);
        //deploy and configure a new game verticle
        JsonObject config = new JsonObject();
        config.putString(GameVerticle.Constants.CONFIG_GAME_GUID, guid);
        config.putString(GameVerticle.Constants.CONFIG_PLAYER_GUID, playerGuid);
        config.putString(GameVerticle.Constants.CONFIG_PLAYER_NAME, player.getName());
        container.deployVerticle(GameVerticle.class.getName(), config, result -> {
            if (result.succeeded()) {
                deploymentIDs.put(guid, result.result());
                container.logger().info(String.format("Deployed a new verticle for game %s with deployment ID: %s", guid, result.result()));
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

    private JsonObject endGame(JsonObject game) {
        String guid = game.getString("guid");
        Game gameInMap = activeGames.get(guid);
        if (gameInMap != null) {
            activeGamesByName.remove(gameInMap.getName());
            IntStream.rangeClosed(1, 2).forEach(i -> {
                playerDisconnected(gameInMap.getPlayer(i).getGuid());
            });
        }
        activeGames.remove(guid);
        String id = deploymentIDs.get(guid);
        if (id != null) {
            container.logger().info(String.format("Destroying verticle for game %s", guid));
            deploymentIDs.remove(guid);
            container.undeployVerticle(id);
        }
        return AsyncHandlerDTO.getInstance();
    }

    private JsonObject getAvailableGame() {
        return new AvailableGameDTO(joinableGame);
    }

    private JsonObject listPlayers() {
        String[] strings = new String[activePlayersByName.size()];
        strings = activePlayersByName.keySet().toArray(strings);
        return new ListPlayersDTO(strings);
    }

    private void playerDisconnected(String playerGuid) {
        Player player = activePlayers.get(playerGuid);
        if (player == null) {
            return;
        }
        activePlayers.remove(playerGuid);
        activePlayersByName.remove(player.getName());
    }

}

