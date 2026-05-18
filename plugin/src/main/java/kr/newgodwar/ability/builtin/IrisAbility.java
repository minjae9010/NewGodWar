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
    description = "무지개 길로 짧게 이동하고 아군을 보조합니다.",
    normalSkill = "바라보는 안전한 위치로 짧게 이동합니다.",
    normalStoneCost = 10,
    normalCooldownSeconds = 40,
    advancedSkill = "주변 아군에게 신속과 재생을 부여합니다.",
    advancedStoneCost = 18,
    advancedCooldownSeconds = 95,
    passiveSkill = "이동 후 짧은 신속을 얻습니다.",
    grade = AbilityGrade.B
)
final class IrisAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            teleportToTargetBlock(player);
            effect(player, PotionEffectType.SPEED, 4, 0);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = nearbyPlayers(context, player, 8, true);
        targets.add(player);
        if (useAdvanced(context, player)) {
            for (Player target : targets) {
                effect(target, PotionEffectType.SPEED, 6, 0);
                effect(target, PotionEffectType.REGENERATION, 4, 0);
            }
        }
    }
}
