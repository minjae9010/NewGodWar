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
    id = "assasin",
    name = "암살자",
    description = "더블 점프와 기습 이동을 사용합니다.",
    normalSkill = "앞으로 도약합니다.",
    normalStoneCost = 0,
    advancedSkill = "주변 적의 뒤로 이동합니다.",
    advancedStoneCost = 15,
    advancedCooldownSeconds = 15,
    passiveSkill = "없음",
    grade = AbilityGrade.A
)
final class AssasinAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        dash(player);
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useAdvanced(context, player)) {
            backstab(context, player);
        }
    }
}
