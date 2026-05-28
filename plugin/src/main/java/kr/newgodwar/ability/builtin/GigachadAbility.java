package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;

@AbilityInfo(
    id = "gigachad",
    name = "기가채드",
    description = "압도적인 자신감으로 적을 밀쳐내고 위기에서 버팁니다.",
    normalSkill = "주변 적을 밀쳐내고 자신에게 저항과 흡수를 부여합니다.",
    normalStoneCost = 16,
    normalCooldownSeconds = 70,
    advancedSkill = "짧게 신속, 공격력 증가, 저항을 얻습니다.",
    advancedStoneCost = 28,
    advancedCooldownSeconds = 135,
    passiveSkill = "치명적인 피해를 받을 때 가끔 흡수와 저항을 얻습니다.",
    grade = AbilityGrade.A
)
final class GigachadAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = nearbyPlayers(context, player, 6, false);
        if (targets.isEmpty()) {
            player.sendMessage("능력을 사용할 수 있는 대상이 없습니다.");
            return;
        }
        if (useNormal(context, player)) {
            for (Player target : targets) {
                Vector vector = target.getLocation().toVector().subtract(player.getLocation().toVector());
                if (vector.lengthSquared() == 0.0D) {
                    vector = player.getEyeLocation().getDirection();
                }
                target.setVelocity(vector.normalize().multiply(1.4D).setY(0.55D));
            }
            effect(player, "RESISTANCE", "DAMAGE_RESISTANCE", 10, 0);
            effect(player, PotionEffectType.ABSORPTION, 10, 0);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useAdvanced(context, player)) {
            effect(player, PotionEffectType.SPEED, 11, 1);
            effect(player, "STRENGTH", "INCREASE_DAMAGE", 11, 0);
            effect(player, "RESISTANCE", "DAMAGE_RESISTANCE", 11, 0);
        }
    }

    @Override
    public void onGenericDamage(AbilityPlayerContext context, EntityDamageEvent event) {
        Player player = context.player();
        if (player.getHealth() - event.getFinalDamage() <= 6.0D && rollChance(1, 4)) {
            effect(player, PotionEffectType.ABSORPTION, 10, 1);
            effect(player, "RESISTANCE", "DAMAGE_RESISTANCE", 8, 0);
            player.sendMessage(ChatColor.GOLD + "기가채드의 자신감이 버티게 합니다.");
        }
    }
}
