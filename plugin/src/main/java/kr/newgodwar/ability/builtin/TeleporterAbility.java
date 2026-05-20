package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import kr.newgodwar.game.GodTeam;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;
@AbilityInfo(
    id = "teleporter",
    name = "텔레포터",
    description = "바라보는 안전한 위치로 이동하거나 시야 안의 아군과 위치를 바꿉니다.",
    normalSkill = "25블록 안의 바라보는 안전한 위치로 이동합니다.",
    normalStoneCost = 15,
    normalCooldownSeconds = 40,
    advancedSkill = "30블록 안의 바라보는 아군과 위치를 바꿉니다.",
    advancedStoneCost = 25,
    advancedCooldownSeconds = 60,
    passiveSkill = "없음",
    grade = AbilityGrade.A
)
final class TeleporterAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            teleportToTargetBlock(player);
        }
    }

    private void teleportToTargetBlock(Player player) {
        Block block = targetBlock(player, 25);
        Location location = block.getLocation().add(0.5D, 1.0D, 0.5D);
        if (location.getBlock().getType() == Material.AIR && location.clone().add(0, 1, 0).getBlock().getType() == Material.AIR) {
            location.setPitch(player.getLocation().getPitch());
            location.setYaw(player.getLocation().getYaw());
            player.teleport(location);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        Player target = targetPlayerInSight(context, player, 30, true);
        if (target == null) {
            return;
        }
        if (useAdvanced(context, player)) {
            Location first = player.getLocation();
            Location second = target.getLocation();
            player.teleport(second);
            target.teleport(first);
        }
    }
}
