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
final class MeguminBuiltInAbility extends BuiltInAbility {
    private boolean oneTimeUsed;

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, final Player player, PlayerInteractEvent event) {
        if (oneTimeUsed || !use(context, player, 0, COBBLESTONE, 25, 0)) {
            return;
        }
        oneTimeUsed = true;
        final Block block = targetBlock(player, 25);
        player.sendMessage(ChatColor.RED + "익스플로전!");
        later(context, 3, new Runnable() {
            @Override
            public void run() {
                player.getWorld().createExplosion(block.getLocation(), 5.0F);
                player.setHealth(0.0D);
            }
        });
    }
}
