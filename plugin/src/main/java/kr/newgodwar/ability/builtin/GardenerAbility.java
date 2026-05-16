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
    id = "gardener",
    name = "정원사",
    description = "나무를 캐면 꽃과 코블스톤을 얻습니다.",
    normalSkill = "없음",
    normalStoneCost = 0,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "배정 시 묘목과 염료를 받고 나무 채굴 시 보상을 얻습니다."
)
final class GardenerAbility extends BaseAbility {
    @Override
    public void onAssign(AbilityPlayerContext context) {
        give(context.player(), Material.SAPLING, 5);
        context.player().getInventory().addItem(new ItemStack(351, 1, (short) 10));
    }

    @Override
    public void onBlockBreak(AbilityPlayerContext context, BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.LOG) {
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(Material.RED_ROSE, 1));
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(Material.COBBLESTONE, 1));
        }
    }
}
