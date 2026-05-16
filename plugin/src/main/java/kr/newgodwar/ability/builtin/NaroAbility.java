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
    description = "낙하 피해를 무시하고 크게 도약합니다.",
    normalSkill = "바라보는 방향으로 크게 도약합니다.",
    normalStoneCost = 2,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "낙하 피해를 무시합니다."
)
final class NaroAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (use(context, player, 0, COBBLESTONE, 2, 10)) {
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
