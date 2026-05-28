package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffectType;

@AbilityInfo(
    id = "anjunggeun",
    name = "안중근",
    description = "의거의 결의로 적 하나를 정확히 제압합니다.",
    normalSkill = "바라보는 적에게 피해와 감속, 약화를 부여합니다.",
    normalStoneCost = 22,
    normalCooldownSeconds = 95,
    advancedSkill = "바라보는 적에게 큰 피해를 주고 짧게 능력을 봉인합니다.",
    advancedStoneCost = 38,
    advancedCooldownSeconds = 180,
    passiveSkill = "검 공격 시 낮은 확률로 짧은 공격력 증가를 얻습니다.",
    grade = AbilityGrade.A
)
final class AhnJungGeunAbility extends BaseAbility {
    @Override
    public void onAssign(AbilityPlayerContext context) {
        give(context.player(), Material.IRON_SWORD, 1);
    }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        Player target = targetPlayerInSight(context, player, 24, false);
        if (target == null) {
            return;
        }
        if (!useNormal(context, player)) {
            return;
        }
        damage(target, 5.0D, player);
        effect(target, "SLOWNESS", "SLOW", 9, 1);
        effect(target, PotionEffectType.WEAKNESS, 9, 0);
        target.sendMessage(ChatColor.RED + "의거의 결의가 움직임을 꺾었습니다.");
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        Player target = targetPlayerInSight(context, player, 28, false);
        if (target == null) {
            return;
        }
        if (!useAdvanced(context, player)) {
            return;
        }
        damage(target, 9.0D, player);
        effect(target, PotionEffectType.BLINDNESS, 7, 0);
        effect(target, "SLOWNESS", "SLOW", 9, 2);
        if (context.plugin().abilities().session(target) != null) {
            context.plugin().abilities().suppressAbility(target, 5);
        }
        target.sendMessage(ChatColor.DARK_PURPLE + "결의의 일격이 능력을 흔들었습니다.");
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (attacker && isSword(context.player().getItemInHand().getType()) && oneIn(5)) {
            effect(context.player(), "STRENGTH", "INCREASE_DAMAGE", 7, 0);
        }
    }
}
