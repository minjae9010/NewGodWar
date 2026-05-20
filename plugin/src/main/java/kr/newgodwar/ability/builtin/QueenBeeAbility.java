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
    id = "queenbee",
    name = "여왕벌",
    description = "가까운 지정 대상을 끌어오고 피격 시 독으로 반격합니다.",
    normalSkill = "10블록 안의 지정한 적을 자신의 위치로 끌어옵니다.",
    normalStoneCost = 30,
    normalCooldownSeconds = 150,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "피격 시 50% 확률로 공격자에게 5초 독을 겁니다.",
    grade = AbilityGrade.A
)
final class QueenBeeAbility extends BaseAbility {
    @Override
    public boolean requiresTarget() {
        return true;
    }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        Player target = commandTargetPlayerInRange(context, player, 10, false);
        if (target == null) {
            return;
        }
        if (useNormal(context, player, 0)) {
            target.teleport(player);
        }
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (!attacker && RANDOM.nextBoolean()) {
            effect(opponent, PotionEffectType.POISON, 5, 0);
        }
    }
}
