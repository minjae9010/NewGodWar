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
    id = "apollon",
    name = "아폴론",
    description = "태양을 띄우고 밝은 곳의 플레이어를 장시간 불태웁니다.",
    normalSkill = "시간을 낮으로 바꿉니다.",
    normalStoneCost = 3,
    normalCooldownSeconds = 45,
    advancedSkill = "밝기 15인 곳의 다른 플레이어를 반복해서 불태웁니다.",
    advancedStoneCost = 18,
    advancedCooldownSeconds = 110,
    passiveSkill = "없음",
    grade = AbilityGrade.A
)
final class ApollonAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            if (setWorldTime(context, player, 6000)) {
                Bukkit.broadcastMessage(ChatColor.YELLOW + "태양의 신이 해를 띄웠습니다.");
            }
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useAdvanced(context, player)) {
            if (setWorldTime(context, player, 6000)) {
                player.getWorld().setStorm(false);
                scorchPlayers(context, player);
            }
        }
    }

    private void scorchPlayers(final AbilityPlayerContext context, final Player player) {
        Bukkit.broadcastMessage(ChatColor.RED + "태양이 매우 뜨거워집니다.");
        final String playerName = player.getName();
        final int[] count = new int[] {15};
        final int[] taskId = new int[1];
        taskId[0] = Bukkit.getScheduler().scheduleSyncRepeatingTask(context.plugin(), () -> {
            Player caster = Bukkit.getPlayer(playerName);
            if (caster == null || !caster.isOnline()) {
                Bukkit.getScheduler().cancelTask(taskId[0]);
                return;
            }
            if (count[0] > 0) {
                for (Player target : caster.getWorld().getPlayers()) {
                    if (!target.equals(caster) && target.getLocation().getBlock().getLightLevel() == 15) {
                        target.setFireTicks(100);
                    }
                }
            } else {
                Bukkit.broadcastMessage("태양이 힘을 잃었습니다.");
                caster.getWorld().setTime(18000);
                Bukkit.getScheduler().cancelTask(taskId[0]);
            }
            count[0]--;
        }, 100L, 40L);
    }
}
