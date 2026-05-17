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
        give(context.player(), material("OAK_SAPLING", "SAPLING"), 5);
        context.player().getInventory().addItem(dye("LIME_DYE", (short) 10));
    }

    @Override
    public void onBlockBreak(AbilityPlayerContext context, BlockBreakEvent event) {
        if (isLog(event.getBlock())) {
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(material("POPPY", "RED_ROSE"), 1));
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(Material.COBBLESTONE, 1));
        }
    }

    private static ItemStack dye(String modernName, short legacyDamage) {
        Material modern = Material.matchMaterial(modernName);
        if (modern != null) {
            return new ItemStack(modern, 1);
        }
        ItemStack stack = new ItemStack(resolveMaterial("INK_SAC", "INK_SACK"), 1);
        stack.setDurability(legacyDamage);
        return stack;
    }

    private static boolean isLog(Block block) {
        if (block == null) {
            return false;
        }
        String name = block.getType().name();
        return "LOG".equals(name) || "LOG_2".equals(name) || name.endsWith("_LOG") || name.endsWith("_STEM");
    }

    private static Material resolveMaterial(String modernName, String legacyName) {
        Material material = Material.matchMaterial(modernName);
        if (material == null) {
            material = Material.matchMaterial(legacyName);
        }
        return material == null ? Material.AIR : material;
    }
}
