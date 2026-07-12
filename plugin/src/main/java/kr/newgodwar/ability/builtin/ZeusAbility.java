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
    id = "zeus",
    name = "제우스",
    description = "번개를 내리고 번개/폭발 피해를 무시합니다.",
    normalSkill = "바라보는 위치에 번개를 내립니다.",
    normalStoneCost = 15,
    normalCooldownSeconds = 90,
    advancedSkill = "지정 위치 주변에 연속 번개를 내립니다.",
    advancedStoneCost = 25,
    advancedCooldownSeconds = 150,
    passiveSkill = "번개와 폭발 피해를 무시합니다.",
    grade = AbilityGrade.S
)
final class ZeusAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            strikeLightning(context, player, targetLocation(player, 50));
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useAdvanced(context, player)) {
            Location center = targetLocation(player, 30);
            for (int i = 0; i < 5; i++) {
                strikeLightning(context, player, center.clone().add(RANDOM.nextInt(11) - 5, 0, RANDOM.nextInt(11) - 5));
            }
        }
    }

    @Override
    public void onGenericDamage(AbilityPlayerContext context, EntityDamageEvent event) {
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.LIGHTNING || cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
            event.setCancelled(true);
            context.player().setFireTicks(0);
        }
    }
}
