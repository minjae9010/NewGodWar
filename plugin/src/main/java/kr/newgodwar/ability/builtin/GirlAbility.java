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
    description = "주변 적을 끌어와 굶주리게 합니다.",
    normalSkill = "주변 적을 끌어오고 허기를 0으로 만듭니다.",
    normalStoneCost = 15,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "없음"
)
final class GirlAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (use(context, player, 0, COBBLESTONE, 15, 60)) {
            for (Player target : nearbyPlayers(context, player, 5, false)) {
                target.teleport(player);
                target.setFoodLevel(0);
            }
        }
    }
}
