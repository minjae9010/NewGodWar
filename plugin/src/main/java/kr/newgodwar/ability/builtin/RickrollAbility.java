package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

@AbilityInfo(
    id = "rickroll",
    name = "릭롤",
    description = "예상 밖의 공연으로 적의 움직임과 시야를 흔듭니다.",
    normalSkill = "주변 적에게 혼란, 감속, 약화를 부여합니다.",
    normalStoneCost = 14,
    normalCooldownSeconds = 60,
    advancedSkill = "바라보는 적을 자신의 앞으로 끌어오고 실명시킵니다.",
    advancedStoneCost = 24,
    advancedCooldownSeconds = 115,
    passiveSkill = "피격 시 가끔 공격자를 혼란시킵니다.",
    grade = AbilityGrade.B
)
final class RickrollAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = nearbyPlayers(context, player, 8, false);
        if (targets.isEmpty()) {
            player.sendMessage("능력을 사용할 수 있는 대상이 없습니다.");
            return;
        }
        if (useNormal(context, player)) {
            for (Player target : targets) {
                effect(target, "NAUSEA", "CONFUSION", 7, 0);
                effect(target, "SLOWNESS", "SLOW", 7, 1);
                effect(target, PotionEffectType.WEAKNESS, 7, 0);
            }
            player.sendMessage(ChatColor.LIGHT_PURPLE + "예상 밖의 공연이 시작됩니다.");
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        Player target = targetPlayerInSight(context, player, 22, false);
        if (target != null && useAdvanced(context, player)) {
            target.teleport(player.getLocation().add(player.getEyeLocation().getDirection().normalize().multiply(1.5D)));
            effect(target, PotionEffectType.BLINDNESS, 4, 0);
            effect(target, "NAUSEA", "CONFUSION", 6, 0);
        }
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, org.bukkit.event.entity.EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (!attacker && rollChance(1, 5)) {
            effect(opponent, "NAUSEA", "CONFUSION", 5, 0);
        }
    }
}
