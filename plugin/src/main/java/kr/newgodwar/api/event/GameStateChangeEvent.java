package kr.newgodwar.api.event;

import kr.newgodwar.game.GameManager;
import kr.newgodwar.game.GameState;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Notification after synchronous transition setup/cleanup has completed.
 * World restoration may still be asynchronous. Schedule further transitions next tick.
 */
public final class GameStateChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final GameManager game;
    private final GameState previousState;
    private final GameState newState;

    public GameStateChangeEvent(GameManager game, GameState previousState, GameState newState) {
        this.game = game;
        this.previousState = previousState;
        this.newState = newState;
    }

    public GameManager getGame() { return game; }
    public GameState getPreviousState() { return previousState; }
    public GameState getNewState() { return newState; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
