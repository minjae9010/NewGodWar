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
    id = "akasha",
    name = "아카샤",
    description = "아군에게 향락을, 적에게 고통을 부여합니다.",
    normalSkill = "주변 아군에게 신속과 재생을 부여합니다.",
    normalStoneCost = 10,
    normalCooldownSeconds = 60,
    advancedSkill = "주변 적에게 혼란과 피해를 줍니다.",
    advancedStoneCost = 20,
    advancedCooldownSeconds = 80,
    passiveSkill = "없음",
    grade = AbilityGrade.A
)
final class AkashaAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            teamBuff(context, player);
        }
    }

    private void teamBuff(AbilityPlayerContext context, Player player) {
        for (Player target : nearbyPlayers(context, player, 20, true)) {
            effect(target, PotionEffectType.SPEED, 15, 0);
            effect(target, PotionEffectType.REGENERATION, 15, 0);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useAdvanced(context, player)) {
            for (Player target : nearbyPlayers(context, player, 10, false)) {
                effect(target, "NAUSEA", "CONFUSION", 8, 0);
                damage(target, 4.0D, player);
            }
        }
    }
}
