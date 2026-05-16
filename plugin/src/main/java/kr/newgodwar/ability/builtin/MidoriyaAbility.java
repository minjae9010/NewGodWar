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
final class MidoriyaAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (has(player, COBBLESTONE, cost(context, 50)) && readyCooldown(player, 0, 150)) {
            ready = true;
            player.sendMessage(ChatColor.YELLOW + "원" + ChatColor.GREEN + " 포 " + ChatColor.AQUA + "올" + ChatColor.WHITE + "이 준비되었습니다!");
        }
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        Player player = context.player();
        if (attacker && ready && player.getItemInHand().getType() == Material.AIR && use(context, player, 0, COBBLESTONE, 50, 150)) {
            ready = false;
            event.setDamage(200.0D);
            effect(player, PotionEffectType.CONFUSION, 10, 0);
            effect(player, PotionEffectType.HUNGER, 10, 0);
            effect(player, PotionEffectType.WEAKNESS, 10, 0);
            effect(player, PotionEffectType.SLOW, 10, 0);
        }
    }
}
