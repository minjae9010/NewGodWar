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
    id = "amaterasu",
    name = "아마테라스",
    description = "태양의 빛으로 적을 태우고 화염 피해를 무시합니다.",
    normalSkill = "시간을 낮으로 바꾸고 자신을 강화합니다.",
    normalStoneCost = 10,
    normalCooldownSeconds = 55,
    advancedSkill = "바라보는 적에게 태양 피해와 실명을 부여합니다.",
    advancedStoneCost = 24,
    advancedCooldownSeconds = 120,
    passiveSkill = "화염 피해를 무시합니다.",
    grade = AbilityGrade.S
)
final class AmaterasuAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player) && setWorldTime(context, player, 1000)) {
            effect(player, "STRENGTH", "INCREASE_DAMAGE", 6, 0);
            effect(player, PotionEffectType.SPEED, 6, 0);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        Player target = targetPlayerInSight(context, player, 24, false);
        if (target == null) {
            return;
        }
        if (useAdvanced(context, player)) {
            target.setFireTicks(120);
            effect(target, PotionEffectType.BLINDNESS, 4, 0);
            damage(target, 5.0D, player);
        }
    }

    @Override
    public void onGenericDamage(AbilityPlayerContext context, EntityDamageEvent event) {
        if (fire(event.getCause())) {
            event.setCancelled(true);
            context.player().setFireTicks(0);
        }
    }
}
