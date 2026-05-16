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
final class SnowAbility extends BaseAbility {
    private int snowAttack;

    @Override
    public void onAssign(AbilityPlayerContext context) {
        give(context.player(), Material.SNOW_BALL, 1);
    }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (use(context, player, 0, COBBLESTONE, 1, 0)) {
            give(player, Material.SNOW_BALL, 1);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        player.sendMessage("공격 지수 : " + snowAttack);
    }

    @Override
    public void onProjectileHit(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player victim) {
        Projectile projectile = (Projectile) event.getDamager();
        if (projectile.getType().name().contains("SNOW")) {
            event.setCancelled(true);
            damage(victim, Math.max(1, snowAttack), context.player());
        }
    }

    @Override
    public void onGenericDamage(AbilityPlayerContext context, EntityDamageEvent event) {
        if (fire(event.getCause())) {
            event.setDamage(event.getDamage() * 2.0D);
        }
    }

    @Override
    public void onDeath(AbilityPlayerContext context, PlayerDeathEvent event) {
        if (event.getEntity().equals(context.player()) && snowAttack < 8) {
            snowAttack++;
        }
    }
}
