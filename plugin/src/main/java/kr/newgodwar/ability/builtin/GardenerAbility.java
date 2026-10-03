package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import kr.newgodwar.ability.feedback.AbilityStyle;
import kr.newgodwar.ability.feedback.AbilityTheme;
import kr.newgodwar.ability.feedback.EffectCue;
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
    description = "나무를 캐면 꽃과 조약돌이 함께 떨어집니다.",
    normalSkill = "없음",
    normalStoneCost = 0,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "능력을 받으면 묘목 5개와 연두색 염료를 받습니다. 원목을 캘 때마다 양귀비 1개와 조약돌 1개가 추가로 떨어집니다.",
    grade = AbilityGrade.B
)
final class GardenerAbility extends BaseAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.NATURE)
        .hit(EffectCue.BLOOM)
        .effect(EffectCue.BLOOM, AbilityDesigns.SEEDLING)
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    @Override
    public void onPrepare(AbilityPlayerContext context) {
        give(context.player(), new ItemStack(material("OAK_SAPLING", "SAPLING"), 5), dye("LIME_DYE", (short) 10));
    }

    @Override
    public void onBlockBreak(AbilityPlayerContext context, BlockBreakEvent event) {
        if (isLog(event.getBlock())) {
            feedback.impact(context, event.getBlock().getLocation().add(0.5D, 0, 0.5D));
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
