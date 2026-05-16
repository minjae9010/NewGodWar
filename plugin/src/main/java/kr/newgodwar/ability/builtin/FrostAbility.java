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
    description = "얼음을 만들고 지정한 적을 얼음 안에 가둡니다.",
    normalSkill = "바라보는 위치에 얼음 구체를 만듭니다.",
    normalStoneCost = 10,
    advancedSkill = "지정한 적을 얼음 구체 안에 가둡니다.",
    advancedStoneCost = 20,
    passiveSkill = "타깃 지정 명령을 사용할 수 있습니다."
)
final class FrostAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (use(context, player, 1, COBBLESTONE, 10, 8)) {
            iceSphere(context, targetLocation(player, 15), 3, 5);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        Player target = targetPlayerInSight(context, player, 20, false);
        if (target == null) {
            return;
        }
        if (use(context, player, 2, COBBLESTONE, 20, 140)) {
            iceSphere(context, target.getLocation(), 5, 8);
        }
    }
}
