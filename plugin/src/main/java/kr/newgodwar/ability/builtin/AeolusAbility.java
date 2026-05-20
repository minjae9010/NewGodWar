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
    id = "aeolus",
    name = "아이올로스",
    description = "섬 전투에서 아군의 이동을 돕고 적을 바깥쪽으로 밀어냅니다.",
    normalSkill = "반경 20블록 아군에게 15초 신속과 재생을 부여합니다.",
    normalStoneCost = 10,
    normalCooldownSeconds = 60,
    advancedSkill = "반경 10블록 적을 밀쳐내고 5초 약화/감속을 부여합니다.",
    advancedStoneCost = 24,
    advancedCooldownSeconds = 170,
    passiveSkill = "없음",
    grade = AbilityGrade.A
)
final class AeolusAbility extends BaseAbility {
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
        List<Player> targets = nearbyPlayers(context, player, 10, false);
        if (targets.isEmpty()) {
            player.sendMessage("능력을 사용할 수 있는 대상이 없습니다.");
            return;
        }
        if (useAdvanced(context, player)) {
            push(context, player, targets, 2.4D, 20L);
            for (Player target : targets) {
                effect(target, PotionEffectType.WEAKNESS, 5, 0);
                effect(target, "SLOWNESS", "SLOW", 5, 0);
            }
        }
    }
}
