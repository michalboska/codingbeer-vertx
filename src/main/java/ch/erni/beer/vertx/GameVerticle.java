package ch.erni.beer.vertx;

import ch.erni.beer.vertx.dto.ErrorDTO;
import ch.erni.beer.vertx.dto.game.GameCommandDTO;
import ch.erni.beer.vertx.dto.game.GameStateDTO;
import ch.erni.beer.vertx.dto.lobby.AddPlayerDTO;
import ch.erni.beer.vertx.dto.lobby.GameEndedDTO;
import ch.erni.beer.vertx.entity.Player;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by Michal Boska on 3. 12. 2014.
 */
public class GameVerticle extends PongVerticle {

    private Player[] players = new Player[2];
    private String guid, publicAddress, privateAddress, inputAddress;
    private Point ball = new Point(512, 300);
    private float ballSpeed = 10;
    private Point ballVector = new Point(-1, -1);
    //we don't want computeNewBallCoordinates to always return new instance to elliminate GC overhead,
    // so we will always modify the same one
    private Point newBallCoordinates = new Point();
    private JsonObject playerMoveResponse = new JsonObject();
    private GameStateDTO state = new GameStateDTO();
    private GameCommandDTO command = new GameCommandDTO();

    private byte speedCounter = 0;
    private long gameTimer;

    @Override
    public void start() {
        guid = Configuration.getString(Constants.CONFIG_GAME_GUID, container);
        String playerGuid = Configuration.getString(Constants.CONFIG_PLAYER_GUID, container);
        String playerName = Configuration.getString(Constants.CONFIG_PLAYER_NAME, container);
        players[0] = new Player(playerName, playerGuid);
        publicAddress = Constants.getPublicQueueAddressForGame(guid);
        privateAddress = Constants.getPrivateQueueAddressForGame(guid);
        inputAddress = Constants.getInputQueueAddressForGame(guid);
        vertx.eventBus().registerHandler(inputAddress, createHandler(this::handleInputMessages));
        vertx.eventBus().registerHandler(privateAddress, createHandler(this::handlePrivateMessages));
    }

    private JsonObject handleInputMessages(Message<JsonObject> message) {
        JsonObject result = null;
        JsonObject body = message.body();
        switch (body.getString("type")) {
            case "move":
                result = movePlayer(body);
                break;
        }
        return result;
    }

    private JsonObject handlePrivateMessages(Message<JsonObject> message) {
        JsonObject result = null;
        JsonObject body = message.body();
        switch (body.getString("type")) {
            case Constants.ACTION_ADD_PLAYER:
                result = addPlayer(body);
                break;
        }
        return result;
    }

    private void gameTick(long timerID) {
        move();
        populateGameState();
        vertx.eventBus().publish(publicAddress, state);
        int winning = getWinningPlayer();
        if (winning != -1) { //if we have a winner, end game
            command.setCommand("win" + (winning + 1));
            vertx.cancelTimer(timerID);
            vertx.eventBus().publish(publicAddress, command);
            vertx.eventBus().send(GameLobbyVerticle.QUEUE_LOBBY_PRIVATE, new GameEndedDTO(guid));
        }
    }

    private void move() {
        computeNewBallCoordinates();
        if (newBallCoordinates.getY() < 20) {
            ballVector.setY(1);
            computeNewBallCoordinates();
        } else if (newBallCoordinates.getY() > 560) {
            ballVector.setY(-1);
            computeNewBallCoordinates();
        }
        if (newBallCoordinates.getX() < 20) { //ball is at player1's level
            if (ballCollidesWith(0)) { //player has caught the ball
                ballVector.setX(1);
                decideSpeed();
                computeNewBallCoordinates();
            } else { //missed the ball
                players[1].setScore(players[1].getScore() + 1);
                resetBall();
                computeNewBallCoordinates();
            }
        } else if (newBallCoordinates.getX() > 994) { //ball is at player2's level
            if (ballCollidesWith(1)) {
                ballVector.setX(-1);
                decideSpeed();
                computeNewBallCoordinates();
            } else { //missed the ball
                players[0].setScore(players[0].getScore() + 1);
                resetBall();
                computeNewBallCoordinates();
            }
        }
        ball.set(newBallCoordinates);
    }

    private void decideSpeed() {
        speedCounter++;
        if (speedCounter == 5) {
            ballSpeed *= 1.3;
            speedCounter = 0;
        }
    }

    private JsonObject movePlayer(JsonObject message) {
        Player player = null;
        String guid = message.getString("guid");
        for (Player p: players) {
            if (p != null && p.getGuid().equalsIgnoreCase(guid)) {
                player = p;
            }
        }
        playerMoveResponse.putNumber("y", 0);
        if (player == null) {
            return playerMoveResponse;
        }
        playerMoveResponse.putNumber("y", player.getPosition());
        Number newPosition = message.getNumber("y");
        if (newPosition == null) {
            return playerMoveResponse;
        }
        int value = Math.round(newPosition.floatValue());
        if (value >= 20 && value <= 480 && Math.abs(value - player.getPosition()) <= 10) {
            player.setPosition(value);
            populateGameState();
            vertx.eventBus().publish(publicAddress, state);
        }
        return playerMoveResponse;
    }

    /**
     * @return index of winning player (0 or 1) or -1 if no one is winning
     */
    private int getWinningPlayer() {
        for (int i = 0; i <= 1; i++) {
            if (players[i].getScore() >= 10) {
                return i;
            }
        }
        return -1;
    }

    private void populateGameState() {
        state.setBallPosition(ball.getX(), ball.getY());
        state.setPlayer1position(players[0].getPosition());
        state.setPlayer1score(players[0].getScore());
        if (players[1] != null) {
            state.setPlayer2position(players[1].getPosition());
            state.setPlayer2score(players[1].getScore());
        }
    }

    /**
     * Only Y coordinate is taken into account
     *
     * @param playerIndex
     * @return
     */
    private boolean ballCollidesWith(int playerIndex) {
        Player player = players[playerIndex];
        int playerYLow = player.getPosition();
        int playerYHigh = playerYLow + 100;
        int ballYLow = newBallCoordinates.getY();
        int ballYHigh = ballYLow + 20;
        return ballYHigh >= playerYLow && ballYLow <= playerYHigh;
    }

    private void computeNewBallCoordinates() {
        newBallCoordinates.setX(ball.getX() + Math.round(ballSpeed * ballVector.getX()));
        newBallCoordinates.setY(ball.getY() + Math.round(ballSpeed * ballVector.getY()));
    }

    private void resetBall() {
        ball.setX(500);
        ball.setY(400);
        ballVector = new Point(-1, -1);
        ballSpeed = 5;
        speedCounter = 0;
    }

    private void startGame() {
        container.logger().info(String.format("Game %s is starting", guid));
        if (gameTimer != 0) {
            vertx.cancelTimer(gameTimer);
        }
        resetBall();
        players[0].setPosition(230);
        players[1].setPosition(230);
        players[0].setScore(0);
        players[1].setScore(0);
        populateGameState();
        command.setCommand("start");
        vertx.eventBus().publish(publicAddress, command);
        gameTimer = vertx.setPeriodic(20, this::gameTick);
    }

    private JsonObject addPlayer(JsonObject message) {
        if (players[1] != null) {
            return new ErrorDTO("Game is full");
        }
        String playerGuid = Configuration.getMandatoryString(Constants.CONFIG_PLAYER_GUID, message);
        String playerName = Configuration.getMandatoryString(Constants.CONFIG_PLAYER_NAME, message);
        players[1] = new Player(playerName, playerGuid);
        vertx.setTimer(2000, l -> startGame());
        return new AddPlayerDTO(playerGuid);
    }

    public static class Constants {
        public static final String QUEUE_PUBLIC_PREFIX = "Game.public-";
        public static final String QUEUE_PRIVATE_PREFIX = "Game.private-";
        public static final String QUEUE_INPUT_PREFIX = "Game.input-";

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
        public static String getInputQueueAddressForGame(String guid) {
            return QUEUE_INPUT_PREFIX + guid;
        }
    }
}
