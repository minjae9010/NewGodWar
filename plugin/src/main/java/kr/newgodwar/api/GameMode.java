package kr.newgodwar.api;

import kr.newgodwar.game.GameManager;
import org.bukkit.entity.Player;

/** Complete replacement for default match startup and GameListener rules.
 * The addon owns participants, abilities, listeners, per-match tasks and victory conditions.
 */
public interface GameMode {
    void onStart(GameManager game);
    void onStop(GameManager game);
    default boolean canDamage(Player attacker, Player victim) { return true; }
}
