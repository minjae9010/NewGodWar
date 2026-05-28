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
    id = "selene",
    name = "셀레네",
    description = "달빛으로 은신하고 적의 시야를 흐립니다.",
    normalSkill = "시간을 밤으로 바꾸고 잠시 투명화합니다.",
    normalStoneCost = 10,
    normalCooldownSeconds = 55,
    advancedSkill = "바라보는 적에게 실명과 감속을 부여합니다.",
    advancedStoneCost = 18,
    advancedCooldownSeconds = 100,
    passiveSkill = "야간 투시 효과를 유지합니다.",
    grade = AbilityGrade.B
)
final class SeleneAbility extends BaseAbility {
    @Override
    public void onAssign(AbilityPlayerContext context) {
        effect(context.player(), PotionEffectType.NIGHT_VISION, 24 * 60 * 60, 0);
    }

    @Override
    public void onRemove(AbilityPlayerContext context) {
        context.player().removePotionEffect(PotionEffectType.NIGHT_VISION);
    }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player) && setWorldTime(context, player, 18000)) {
            effect(player, PotionEffectType.INVISIBILITY, 8, 0);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        Player target = targetPlayerInSight(context, player, 18, false);
        if (target == null) {
            return;
        }
        if (useAdvanced(context, player)) {
            effect(target, PotionEffectType.BLINDNESS, 7, 0);
            effect(target, "SLOWNESS", "SLOW", 8, 1);
        }
    }

    @Override
    public void onRespawn(AbilityPlayerContext context, PlayerRespawnEvent event) {
        respawnEffect(context, PotionEffectType.NIGHT_VISION, 24 * 60 * 60, 0);
    }
}
