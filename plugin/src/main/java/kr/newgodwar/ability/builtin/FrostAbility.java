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
    id = "frost",
    name = "잭프로스트",
    description = "얼음 구체로 길을 막고 시야 안의 적을 얼음 안에 가둡니다.",
    normalSkill = "15블록 안의 바라보는 위치에 5초 동안 얼음 구체를 만듭니다.",
    normalStoneCost = 12,
    normalCooldownSeconds = 20,
    advancedSkill = "20블록 안의 바라보는 적을 8초 동안 큰 얼음 구체에 가둡니다.",
    advancedStoneCost = 24,
    advancedCooldownSeconds = 150,
    passiveSkill = "타깃 지정 명령을 사용할 수 있습니다.",
    grade = AbilityGrade.A
)
final class FrostAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            iceSphere(context, targetLocation(player, 15), 3, 5);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        Player target = targetPlayerInSight(context, player, 20, false);
        if (target == null) {
            return;
        }
        if (useAdvanced(context, player)) {
            iceSphere(context, target.getLocation(), 5, 8);
        }
    }
}
