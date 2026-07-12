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
    id = "naro",
    name = "나로호",
    description = "강한 수직 도약으로 진입하거나 탈출하고 낙하 피해를 무시합니다.",
    normalSkill = "바라보는 방향으로 높게 도약합니다.",
    normalStoneCost = 5,
    normalCooldownSeconds = 18,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "낙하 피해를 무시합니다.",
    grade = AbilityGrade.B
)
final class NaroAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            Vector vector = player.getEyeLocation().getDirection();
            vector.setY(3.0D);
            player.setVelocity(vector);
        }
    }

    @Override
    public void onGenericDamage(AbilityPlayerContext context, EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
        }
    }
}
