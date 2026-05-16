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
    id = "snow",
    name = "사이코스노우",
    description = "눈덩이 피해와 성장하는 공격 지수를 가집니다.",
    normalSkill = "눈덩이를 생성합니다.",
    normalStoneCost = 1,
    advancedSkill = "현재 공격 지수를 확인합니다.",
    advancedStoneCost = 0,
    passiveSkill = "눈덩이 피해가 공격 지수를 따르고 사망할 때 성장합니다."
)
final class SnowAbility extends BaseAbility {
    private int snowAttack;

    @Override
    public void onAssign(AbilityPlayerContext context) {
        give(context.player(), Material.SNOW_BALL, 1);
    }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player, 0)) {
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
