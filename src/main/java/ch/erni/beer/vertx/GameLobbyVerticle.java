package ch.erni.beer.vertx;

import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import java.util.Set;
import java.util.TreeSet;

/**
 * Created by Michal Boska on 3. 12. 2014.
 */
public class GameLobbyVerticle extends Verticle {
    public static final String QUEUE_LOBBY = "ch.erni.beer.vertx.GameLobbyVerticle.queue";

    private Set<GameTuple> activeGames = new TreeSet<>();

    @Override
    public void start() {
        vertx.eventBus().registerHandler(QUEUE_LOBBY, this::handleMessage);
    }

    private void handleMessage(Message<JsonObject> message) {

    }

    private class GameTuple implements Comparable<GameTuple> {
        String name;
        String guid;
        int numPlayers;

        public GameTuple(String name, String guid, int numPlayers) {
            this.name = name;
            this.guid = guid;
            this.numPlayers = numPlayers;
        }

        @Override
        public int compareTo(GameTuple o) {
            return guid.compareTo(o.guid);
        }
    }
}

