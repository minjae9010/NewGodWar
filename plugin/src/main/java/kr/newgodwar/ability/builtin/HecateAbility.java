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
    id = "hecate",
    name = "헤카테",
    description = "짧은 은신과 지정한 적에게 약한 저주를 사용합니다.",
    normalSkill = "잠시 투명화하고 신속을 얻습니다.",
    normalStoneCost = 14,
    normalCooldownSeconds = 75,
    advancedSkill = "지정한 적에게 실명과 감속을 짧게 부여합니다.",
    advancedStoneCost = 20,
    advancedCooldownSeconds = 115,
    passiveSkill = "타깃 지정 명령을 사용할 수 있습니다.",
    grade = AbilityGrade.B
)
final class HecateAbility extends BaseAbility {
    @Override
    public boolean requiresTarget() {
        return true;
    }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            effect(player, PotionEffectType.INVISIBILITY, 8, 0);
            effect(player, PotionEffectType.SPEED, 8, 0);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        Player target = commandTargetPlayer(context, player, false);
        if (target == null) {
            return;
        }
        if (useAdvanced(context, player)) {
            effect(target, PotionEffectType.BLINDNESS, 7, 0);
            effect(target, "SLOWNESS", "SLOW", 8, 1);
            target.sendMessage(ChatColor.DARK_PURPLE + "헤카테의 저주가 시야를 흐립니다.");
        }
    }
}
