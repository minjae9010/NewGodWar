package kr.newgodwar.ability.api;

import org.bukkit.entity.Player;

public interface GodAbility {

    default void onAssign(AbilityPlayerContext context) {
    }

    default void onRemove(AbilityPlayerContext context) {
    }

    default void onDamage(AbilityDamageContext context) {
    }

    default void onTick(AbilityPlayerContext context) {
    }

    default void onKill(AbilityKillContext context) {
    }

    default boolean supports(Player player) {
        return true;
    }
}
