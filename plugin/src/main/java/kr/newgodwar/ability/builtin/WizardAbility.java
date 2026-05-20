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
    id = "wizard",
    name = "마법사",
    description = "주변 플레이어를 밀쳐내거나 공중에 띄운 뒤 번개 심판을 내립니다.",
    normalSkill = "반경 10블록 플레이어를 강하게 밀쳐냅니다.",
    normalStoneCost = 12,
    normalCooldownSeconds = 110,
    advancedSkill = "반경 5블록 플레이어를 띄운 뒤 번개와 화염을 발생시킵니다.",
    advancedStoneCost = 24,
    advancedCooldownSeconds = 210,
    passiveSkill = "없음",
    grade = AbilityGrade.A
)
final class WizardAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = nearbyPlayers(player, 10);
        if (targets.isEmpty()) {
            player.sendMessage("능력을 사용할 수 있는 대상이 없습니다.");
            return;
        }
        if (useNormal(context, player)) {
            push(context, player, targets, 2.4D, 4L);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (nearbyPlayers(player, 5).isEmpty()) {
            player.sendMessage("능력을 사용할 수 있는 대상이 없습니다.");
            return;
        }
        if (useAdvanced(context, player)) {
            judgment(context, player);
        }
    }

    private void judgment(AbilityPlayerContext context, final Player player) {
        final List<Player> targets = nearbyPlayers(player, 5);
        if (targets.isEmpty()) {
            player.sendMessage("능력을 사용할 수 있는 대상이 없습니다.");
            return;
        }
        player.setHealth(Math.max(1.0D, player.getHealth() / 2.0D));
        for (Player target : targets) {
            target.setVelocity(new Vector(0, 1.6D, 0));
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(context.plugin(), () -> {
            for (Player target : targets) {
                target.getWorld().strikeLightning(target.getLocation());
                target.setFireTicks(100);
            }
        }, 4L);
    }
}
