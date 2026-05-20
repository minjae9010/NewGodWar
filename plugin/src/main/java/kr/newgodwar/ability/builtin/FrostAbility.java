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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    private void iceSphere(final AbilityPlayerContext context, Location center, int radius, int seconds) {
        final Map<Location, Material> oldBlocks = new LinkedHashMap<Location, Material>();
        for (Location location : sphere(center, radius)) {
            Block block = location.getBlock();
            if (block.getType() != Material.DIAMOND_BLOCK) {
                oldBlocks.put(block.getLocation(), block.getType());
                block.setType(Material.ICE);
            }
        }
        later(context, seconds, "얼음 구체 복구", "얼음 구체 복구", () -> {
            for (Map.Entry<Location, Material> entry : oldBlocks.entrySet()) {
                entry.getKey().getBlock().setType(entry.getValue());
            }
        });
    }

    private List<Location> sphere(Location center, int radius) {
        List<Location> locations = new ArrayList<Location>();
        int bx = center.getBlockX();
        int by = center.getBlockY();
        int bz = center.getBlockZ();
        for (int x = bx - radius; x <= bx + radius; x++) {
            for (int y = by - radius; y <= by + radius; y++) {
                for (int z = bz - radius; z <= bz + radius; z++) {
                    double distance = (bx - x) * (bx - x) + (by - y) * (by - y) + (bz - z) * (bz - z);
                    if (distance < radius * radius && distance >= (radius - 1) * (radius - 1)) {
                        locations.add(new Location(center.getWorld(), x, y, z));
                    }
                }
            }
        }
        return locations;
    }
}
