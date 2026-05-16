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
    description = "바라보는 곳으로 이동하거나 아군과 위치를 바꿉니다.",
    normalSkill = "바라보는 안전한 위치로 이동합니다.",
    normalStoneCost = 15,
    advancedSkill = "지정한 아군과 위치를 바꿉니다.",
    advancedStoneCost = 25,
    passiveSkill = "타깃 지정 명령을 사용할 수 있습니다."
)
final class TeleporterAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (use(context, player, 1, COBBLESTONE, 15, 25)) {
            teleportToTargetBlock(player);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        Player target = targetPlayerInSight(context, player, 30, true);
        if (target == null) {
            return;
        }
        if (use(context, player, 2, COBBLESTONE, 25, 30)) {
            Location first = player.getLocation();
            Location second = target.getLocation();
            player.teleport(second);
            target.teleport(first);
        }
    }
}
