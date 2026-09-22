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
    id = "teleporter",
    name = "텔레포터",
    description = "바라보는 안전한 위치로 이동하거나 시야 안의 아군과 위치를 바꿉니다.",
    normalSkill = "블레이즈 막대기 좌클릭: 25블록 안의 바라보는 안전한 위치로 이동합니다.",
    normalStoneCost = 15,
    normalCooldownSeconds = 40,
    advancedSkill = "블레이즈 막대기 우클릭: 30블록 안의 바라보는 아군과 위치를 바꿉니다.",
    advancedStoneCost = 25,
    advancedCooldownSeconds = 60,
    passiveSkill = "없음",
    grade = AbilityGrade.A
)
final class TeleporterAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        teleportNormalToSight(context, player, 25);
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        Player target = targetPlayerInSight(context, player, 30, true);
        if (target == null) {
            return;
        }
        if (useAdvanced(context, player)) {
            Location first = player.getLocation();
            Location second = target.getLocation();
            if (player.teleport(second)) feedback.cue(context, player, kr.newgodwar.ability.feedback.EffectCue.PORTAL);
            if (target.teleport(first)) {
                feedback.cue(context, target, kr.newgodwar.ability.feedback.EffectCue.PORTAL);
                feedback.affected(context, target, "아군과 위치 교환", false);
            }
        }
    }
}
