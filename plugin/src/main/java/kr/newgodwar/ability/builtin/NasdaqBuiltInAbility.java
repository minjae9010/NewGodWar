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
final class NasdaqBuiltInAbility extends BuiltInAbility {
    @Override
    public void onInteract(AbilityPlayerContext context, PlayerInteractEvent event) {
        Player player = context.player();
        if (isLeft(event.getAction()) && (holding(player, Material.IRON_INGOT) || holding(player, Material.DIAMOND))) {
            nasdaq(player);
        }
    }

    private void nasdaq(Player player) {
        if (!readyCooldown(player, 0, 30) || !has(player, COBBLESTONE, 20)) {
            return;
        }
        ItemStack item = player.getItemInHand();
        player.getInventory().removeItem(new ItemStack(COBBLESTONE, 20));
        setRawCooldown(0, 30000L);
        if (RANDOM.nextInt(4) < 3) {
            player.getInventory().addItem(item.clone());
        } else {
            player.getInventory().removeItem(item.clone());
        }
    }
}
