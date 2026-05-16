package kr.newgodwar.ability.api;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

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

    default void onDeath(AbilityPlayerContext context, PlayerDeathEvent event) {
    }

    default void onInteract(AbilityPlayerContext context, PlayerInteractEvent event) {
    }

    default void onGenericDamage(AbilityPlayerContext context, EntityDamageEvent event) {
    }

    default void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
    }

    default void onProjectileHit(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player victim) {
    }

    default void onProjectileLaunch(AbilityPlayerContext context, ProjectileLaunchEvent event) {
    }

    default void onBlockBreak(AbilityPlayerContext context, BlockBreakEvent event) {
    }

    default void onBlockPlace(AbilityPlayerContext context, BlockPlaceEvent event) {
    }

    default void onBlockExplode(BlockExplodeEvent event) {
    }

    default void onSignChange(AbilityPlayerContext context, SignChangeEvent event) {
    }

    default void onFoodLevelChange(AbilityPlayerContext context, FoodLevelChangeEvent event) {
    }

    default void onRegainHealth(AbilityPlayerContext context, EntityRegainHealthEvent event) {
    }

    default void onRespawn(AbilityPlayerContext context, PlayerRespawnEvent event) {
    }

    default void onMove(AbilityPlayerContext context, PlayerMoveEvent event) {
    }

    default void onChat(AbilityPlayerContext context, AsyncPlayerChatEvent event) {
    }

    default void onFish(AbilityPlayerContext context, PlayerFishEvent event) {
    }

    default void setTarget(AbilityPlayerContext context, CommandSender sender, String targetName) {
        sender.sendMessage("타깃을 사용하는 능력이 아닙니다.");
    }

    default long cooldownRemainingMillis(int slot) {
        return 0L;
    }

    default void clearCooldowns() {
    }

    default boolean supports(Player player) {
        return true;
    }
}
