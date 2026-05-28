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
    id = "loki",
    name = "로키",
    description = "은신과 위치 교란으로 적을 속입니다.",
    normalSkill = "잠시 투명화하고 신속을 얻습니다.",
    normalStoneCost = 12,
    normalCooldownSeconds = 55,
    advancedSkill = "바라보는 적과 위치를 바꾸고 시야를 흐립니다.",
    advancedStoneCost = 22,
    advancedCooldownSeconds = 105,
    passiveSkill = "피격 시 가끔 공격자에게 혼란을 부여합니다.",
    grade = AbilityGrade.A
)
final class LokiAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            effect(player, PotionEffectType.INVISIBILITY, 9, 0);
            effect(player, PotionEffectType.SPEED, 9, 1);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        Player target = targetPlayerInSight(context, player, 20, false);
        if (target == null) {
            return;
        }
        if (useAdvanced(context, player)) {
            Location first = player.getLocation();
            Location second = target.getLocation();
            player.teleport(second);
            target.teleport(first);
            effect(target, PotionEffectType.BLINDNESS, 6, 0);
            effect(target, "NAUSEA", "CONFUSION", 8, 0);
        }
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (!attacker && rollChance(1, 5)) {
            effect(opponent, "NAUSEA", "CONFUSION", 8, 0);
        }
    }
}
