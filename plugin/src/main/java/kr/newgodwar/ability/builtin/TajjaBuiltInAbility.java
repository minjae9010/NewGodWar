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
final class TajjaBuiltInAbility extends BuiltInAbility {
    private int tajjaDamage;
    private int tajjaUses;

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (use(context, player, 0, COBBLESTONE, 10, 60)) {
            stealSword(player);
        }
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (attacker && context.player().getItemInHand().getType() == Material.AIR && tajjaDamage > 0) {
            event.setDamage(tajjaDamage);
            tajjaUses--;
            if (tajjaUses <= 0) {
                tajjaDamage = 0;
            }
        }
    }

    private void stealSword(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && isSword(item.getType())) {
                tajjaDamage = swordDamage(item.getType());
                tajjaUses = 10;
                player.getInventory().removeItem(new ItemStack(item.getType(), 1));
                player.sendMessage("손은 눈보다 빠르다.");
                return;
            }
        }
        player.sendMessage("소비할 검이 인벤토리에 없습니다.");
    }
}
