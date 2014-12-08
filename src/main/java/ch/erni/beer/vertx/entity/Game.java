package ch.erni.beer.vertx.entity;

/**
 * Created by bol on 8. 12. 2014.
 */
public class Game extends Entity {

    private Player[] players = new Player[2];

    public Game(String name, String guid, Player firstPlayer) {
        super(name, guid);
        this.players[0] = firstPlayer;
    }

    /**
     *
     * @param index 1 or 2
     * @return
     */
    public Player getPlayer(int index) {
        return players[index - 1];
    }

    public void addSecondPlayer(Player player) {
        if (isFull()) {
            throw new IllegalStateException("The game " + guid + " is already full");
        }
        players[1] = player;
    }

    public boolean isFull() {
        return players[0] != null && players[1] != null;
    }
}
