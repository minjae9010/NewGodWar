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
    id = "poseidon",
    name = "포세이돈",
    description = "물을 잠시 만들고 파도로 적을 밀쳐내며 익사 피해를 무시합니다.",
    normalSkill = "바라보는 위치에 잠시 물을 만듭니다.",
    normalStoneCost = 8,
    normalCooldownSeconds = 35,
    advancedSkill = "주변 적을 밀쳐내고 짧게 감속시킵니다.",
    advancedStoneCost = 18,
    advancedCooldownSeconds = 100,
    passiveSkill = "익사 피해를 무시하고 물속 호흡 효과를 유지합니다.",
    grade = AbilityGrade.A
)
final class PoseidonAbility extends BaseAbility {
    @Override
    public void onAssign(AbilityPlayerContext context) {
        effect(context.player(), PotionEffectType.WATER_BREATHING, 24 * 60 * 60, 0);
    }

    @Override
    public void onRemove(AbilityPlayerContext context) {
        context.player().removePotionEffect(PotionEffectType.WATER_BREATHING);
    }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        Block base = targetBlock(player, 10);
        final Block block = base.getLocation().add(0, 1, 0).getBlock();
        if (block.getType() != Material.AIR) {
            player.sendMessage(ChatColor.RED + "물을 만들 공간이 없습니다.");
            return;
        }
        if (useNormal(context, player)) {
            block.setType(Material.WATER);
            later(context, 5, "해류 소멸", "해류 소멸", () -> {
                if (block.getType() == Material.WATER || block.getType() == Material.STATIONARY_WATER) {
                    block.setType(Material.AIR);
                }
            });
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = nearbyPlayers(context, player, 9, false);
        if (targets.isEmpty()) {
            player.sendMessage("능력을 사용할 수 있는 대상이 없습니다.");
            return;
        }
        if (useAdvanced(context, player)) {
            push(context, player, targets, 1.8D, 10L);
            for (Player target : targets) {
                effect(target, "SLOWNESS", "SLOW", 5, 0);
            }
        }
    }

    @Override
    public void onGenericDamage(AbilityPlayerContext context, EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.DROWNING) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onRespawn(AbilityPlayerContext context, PlayerRespawnEvent event) {
        effect(context.player(), PotionEffectType.WATER_BREATHING, 24 * 60 * 60, 0);
    }
}
