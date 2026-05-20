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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
@AbilityInfo(
    id = "hephaestus",
    name = "헤파이토스",
    description = "짧은 용암 장악과 화염 면역으로 근접 전장을 압박합니다.",
    normalSkill = "5블록 안의 바라보는 위치에 2초 동안 용암을 만듭니다.",
    normalStoneCost = 4,
    normalCooldownSeconds = 20,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "화염 피해를 무시하지만 익사 피해를 2배로 받습니다.",
    grade = AbilityGrade.A
)
final class HephaestusAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        lava(player, context);
    }

    private void lava(Player player, AbilityPlayerContext context) {
        Block base = targetBlock(player, 5);
        final Block block = base.getLocation().add(0, 1, 0).getBlock();
        if (block.getType() == Material.AIR && useNormal(context, player, 0)) {
            final Map<Location, Material> cleanupBlocks = temporaryLavaCleanupBlocks(block);
            block.setType(Material.LAVA);
            later(context, 2, "용암 제거", "용암 제거", () -> {
                for (Map.Entry<Location, Material> entry : cleanupBlocks.entrySet()) {
                    Block cleanupBlock = entry.getKey().getBlock();
                    Material current = cleanupBlock.getType();
                    Material original = entry.getValue();
                    if (isTemporaryLavaProduct(current)) {
                        cleanupBlock.setType(original);
                    } else if (isLava(current) && original == Material.AIR) {
                        cleanupBlock.setType(Material.AIR);
                    } else if (isLava(current) && isWater(original)) {
                        cleanupBlock.setType(original);
                    }
                }
            });
        }
    }

    private Map<Location, Material> temporaryLavaCleanupBlocks(Block center) {
        Map<Location, Material> blocks = new LinkedHashMap<Location, Material>();
        Location location = center.getLocation();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Block block = location.clone().add(x, y, z).getBlock();
                    Material type = block.getType();
                    if (type == Material.AIR || isWater(type)) {
                        blocks.put(block.getLocation(), type);
                    }
                }
            }
        }
        return blocks;
    }

    private boolean isTemporaryLavaProduct(Material material) {
        return material == Material.OBSIDIAN || material == Material.COBBLESTONE || material == Material.STONE;
    }

    private boolean isLava(Material material) {
        return material == Material.LAVA || "STATIONARY_LAVA".equals(material.name());
    }

    private boolean isWater(Material material) {
        return material == Material.WATER || "STATIONARY_WATER".equals(material.name());
    }

    @Override
    public void onGenericDamage(AbilityPlayerContext context, EntityDamageEvent event) {
        if (fire(event.getCause())) {
            event.setCancelled(true);
            context.player().setFireTicks(0);
        } else if (event.getCause() == EntityDamageEvent.DamageCause.DROWNING) {
            event.setDamage(event.getDamage() * 2.0D);
        }
    }
}
