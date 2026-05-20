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
    id = "girl",
    name = "안락소녀",
    description = "가까운 적을 자신의 섬 위치로 끌어와 허기와 움직임을 끊습니다.",
    normalSkill = "수평 반경 5블록 적을 끌어오고 허기를 0으로 만듭니다.",
    normalStoneCost = 22,
    normalCooldownSeconds = 90,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "없음",
    grade = AbilityGrade.A
)
final class GirlAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player, 0)) {
            for (Player target : nearbyPlayers(context, player, 5, 0, 5, false)) {
                target.teleport(player);
                target.setFoodLevel(0);
                effectTicks(target, "SLOWNESS", "SLOW", 2, 200);
            }
        }
    }
}
