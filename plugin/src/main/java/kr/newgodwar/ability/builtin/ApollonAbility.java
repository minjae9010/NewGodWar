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
final class ApollonAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (use(context, player, 1, COBBLESTONE, 1, 30)) {
            player.getWorld().setTime(6000);
            Bukkit.broadcastMessage(ChatColor.YELLOW + "태양의 신이 해를 띄웠습니다.");
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (use(context, player, 2, COBBLESTONE, 15, 90)) {
            player.getWorld().setTime(6000);
            for (Player target : nearbyPlayers(context, player, 15, false)) {
                if (target.getLocation().getBlock().getLightFromSky() > 10) {
                    target.setFireTicks(80);
                    target.damage(2.0D, player);
                }
            }
        }
    }
}
