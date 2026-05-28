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
    id = "iris",
    name = "이리스",
    description = "안전한 위치로 짧게 건너가고 아군에게 이동 보조를 제공합니다.",
    normalSkill = "25블록 안의 바라보는 안전한 위치로 이동합니다.",
    normalStoneCost = 14,
    normalCooldownSeconds = 55,
    advancedSkill = "반경 8블록 아군에게 신속과 재생을 부여합니다.",
    advancedStoneCost = 18,
    advancedCooldownSeconds = 95,
    passiveSkill = "이동 후 짧은 신속을 얻습니다.",
    grade = AbilityGrade.A
)
final class IrisAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            teleportToTargetBlock(player);
            effect(player, PotionEffectType.SPEED, 7, 0);
        }
    }

    private void teleportToTargetBlock(Player player) {
        Block block = targetBlock(player, 25);
        Location location = block.getLocation().add(0.5D, 1.0D, 0.5D);
        if (location.getBlock().getType() == Material.AIR && location.clone().add(0, 1, 0).getBlock().getType() == Material.AIR) {
            location.setPitch(player.getLocation().getPitch());
            location.setYaw(player.getLocation().getYaw());
            player.teleport(location);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = nearbyPlayers(context, player, 8, true);
        targets.add(player);
        if (useAdvanced(context, player)) {
            for (Player target : targets) {
                effect(target, PotionEffectType.SPEED, 8, 0);
                effect(target, PotionEffectType.REGENERATION, 7, 0);
            }
        }
    }
}
