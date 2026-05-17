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
    id = "hermes",
    name = "헤르메스",
    description = "빠른 이동과 짧은 비행을 사용합니다.",
    normalSkill = "잠시 비행합니다.",
    normalStoneCost = 10,
    normalCooldownSeconds = 60,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "상시 신속 효과를 받습니다.",
    grade = AbilityGrade.A
)
final class HermesAbility extends BaseAbility {
    @Override
    public void onAssign(AbilityPlayerContext context) {
        effect(context.player(), PotionEffectType.SPEED, 24 * 60 * 60, 0);
    }

    @Override
    public void onRemove(AbilityPlayerContext context) {
        context.player().removePotionEffect(PotionEffectType.SPEED);
    }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player, 0)) {
            fly(context, player, 7);
        }
    }

    @Override
    public void onRespawn(AbilityPlayerContext context, PlayerRespawnEvent event) {
        effect(context.player(), PotionEffectType.SPEED, 24 * 60 * 60, 0);
    }
}
