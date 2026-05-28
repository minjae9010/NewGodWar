package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

@AbilityInfo(
    id = "thisisfine",
    name = "괜찮아",
    description = "불타는 상황에서도 태연하게 버티고 주변을 태웁니다.",
    normalSkill = "자신에게 화염 저항을 주고 주변 적을 불태웁니다.",
    normalStoneCost = 12,
    normalCooldownSeconds = 55,
    advancedSkill = "불을 끄고 체력을 회복하며 짧게 저항을 얻습니다.",
    advancedStoneCost = 20,
    advancedCooldownSeconds = 95,
    passiveSkill = "화염 피해를 무시하고 불이 붙으면 신속을 얻습니다.",
    grade = AbilityGrade.B
)
final class ThisIsFineAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = nearbyPlayers(context, player, 7, false);
        if (targets.isEmpty()) {
            player.sendMessage("능력을 사용할 수 있는 대상이 없습니다.");
            return;
        }
        if (useNormal(context, player)) {
            effect(player, PotionEffectType.FIRE_RESISTANCE, 12, 0);
            for (Player target : targets) {
                target.setFireTicks(100);
            }
            player.sendMessage(ChatColor.GOLD + "괜찮아. 다 괜찮아.");
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useAdvanced(context, player)) {
            player.setFireTicks(0);
            player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 6.0D));
            effect(player, "RESISTANCE", "DAMAGE_RESISTANCE", 8, 0);
        }
    }

    @Override
    public void onGenericDamage(AbilityPlayerContext context, EntityDamageEvent event) {
        if (fire(event.getCause())) {
            event.setCancelled(true);
            effect(context.player(), PotionEffectType.SPEED, 6, 0);
        }
    }
}
