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
    id = "iris",
    name = "이리스",
    description = "안전한 위치로 짧게 건너가고 아군에게 이동 보조를 제공합니다.",
    normalSkill = "블레이즈 막대기 좌클릭: 25블록 안의 바라보는 안전한 위치로 이동합니다.",
    normalStoneCost = 14,
    normalCooldownSeconds = 55,
    advancedSkill = "블레이즈 막대기 우클릭: 반경 8블록 아군에게 신속과 재생을 부여합니다.",
    advancedStoneCost = 18,
    advancedCooldownSeconds = 95,
    passiveSkill = "이동 후 짧은 신속을 얻습니다.",
    grade = AbilityGrade.A
)
final class IrisAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (teleportNormalToSight(context, player, 25)) {
            effect(player, PotionEffectType.SPEED, 7, 0);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = nearbyPlayers(context, player, 8, true);
        targets.add(player);
        if (useAdvanced(context, player)) {
            for (Player target : targets) {
                effect(target, PotionEffectType.SPEED, 8, 0);
                effect(target, PotionEffectType.REGENERATION, 7, 0);
                feedback.affected(context, target, "무지개 축복 · 신속 8초 / 재생 7초", false);
            }
        }
    }
}
