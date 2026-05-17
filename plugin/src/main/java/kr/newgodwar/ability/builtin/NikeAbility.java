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
    id = "nike",
    name = "니케",
    description = "짧은 기동력과 처치 후 작은 승전 효과를 얻습니다.",
    normalSkill = "짧게 신속과 점프 강화 효과를 얻습니다.",
    normalStoneCost = 10,
    normalCooldownSeconds = 45,
    advancedSkill = "주변 아군에게 짧은 신속과 공격력 증가를 부여합니다.",
    advancedStoneCost = 24,
    advancedCooldownSeconds = 120,
    passiveSkill = "직접 처치하면 짧은 재생과 신속을 얻습니다.",
    grade = AbilityGrade.B
)
final class NikeAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            effect(player, PotionEffectType.SPEED, 8, 1);
            effect(player, PotionEffectType.JUMP, 8, 0);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = nearbyPlayers(context, player, 10, true);
        targets.add(player);
        if (useAdvanced(context, player)) {
            for (Player target : targets) {
                effect(target, PotionEffectType.SPEED, 7, 0);
                effect(target, "STRENGTH", "INCREASE_DAMAGE", 5, 0);
            }
        }
    }

    @Override
    public void onKill(AbilityKillContext context) {
        effect(context.killer(), PotionEffectType.REGENERATION, 5, 0);
        effect(context.killer(), PotionEffectType.SPEED, 6, 0);
    }
}
