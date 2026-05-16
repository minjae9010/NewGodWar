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
final class VoodooBuiltInAbility extends BuiltInAbility {
    @Override
    public void onBlockPlace(AbilityPlayerContext context, BlockPlaceEvent event) {
        if (event.getBlock().getType() == Material.SIGN && (!readyCooldown(context.player(), 0, 180) || !has(context.player(), COBBLESTONE, cost(context, 5)))) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onSignChange(final AbilityPlayerContext context, SignChangeEvent event) {
        Player target = Bukkit.getPlayer(event.getLine(0));
        if (target != null && use(context, context.player(), 0, COBBLESTONE, 5, 180)) {
            targetName = target.getName();
            final Block sign = event.getBlock();
            later(context, 7, new Runnable() {
                @Override
                public void run() {
                    targetName = null;
                    sign.breakNaturally();
                }
            });
        }
    }
}
