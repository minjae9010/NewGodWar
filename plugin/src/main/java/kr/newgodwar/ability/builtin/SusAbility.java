package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

@AbilityInfo(
    id = "sus",
    name = "수상한녀석",
    description = "의심스러운 움직임으로 숨어들고 적을 교란합니다.",
    normalSkill = "잠시 투명화하고 신속을 얻습니다.",
    normalStoneCost = 16,
    normalCooldownSeconds = 70,
    advancedSkill = "주변 적 하나와 위치를 바꾸고 서로 실명합니다.",
    advancedStoneCost = 22,
    advancedCooldownSeconds = 105,
    passiveSkill = "피격 시 가끔 짧게 투명화합니다.",
    grade = AbilityGrade.B
)
final class SusAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            effect(player, PotionEffectType.INVISIBILITY, 8, 0);
            effect(player, PotionEffectType.SPEED, 8, 0);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = nearbyPlayers(context, player, 10, false);
        if (targets.isEmpty()) {
            player.sendMessage("능력을 사용할 수 있는 대상이 없습니다.");
            return;
        }
        if (useAdvanced(context, player)) {
            Player target = targets.get(RANDOM.nextInt(targets.size()));
            Location playerLocation = player.getLocation();
            Location targetLocation = target.getLocation();
            player.teleport(targetLocation);
            target.teleport(playerLocation);
            effect(player, PotionEffectType.BLINDNESS, 2, 0);
            effect(target, PotionEffectType.BLINDNESS, 5, 0);
        }
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (!attacker && rollChance(1, 5)) {
            effect(context.player(), PotionEffectType.INVISIBILITY, 4, 0);
        }
    }
}
