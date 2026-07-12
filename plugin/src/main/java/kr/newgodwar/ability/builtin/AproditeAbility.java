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
    id = "aprodite",
    name = "아프로디테",
    description = "지면에 서 있을 때 주변 플레이어를 자신의 섬 위치로 끌어옵니다.",
    normalSkill = "반경 20블록 플레이어를 자신의 위치로 끌어옵니다.",
    normalStoneCost = 24,
    normalCooldownSeconds = 120,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "없음",
    grade = AbilityGrade.A
)
final class AproditeAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (player.isSneaking() || player.getLocation().clone().add(0, -1, 0).getBlock().getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "웅크리고 있거나 발 밑의 블록이 없어 능력이 발동되지 않았습니다.");
            return;
        }
        List<Player> targets = nearbyPlayers(context, player, 20, false);
        if (targets.isEmpty()) {
            player.sendMessage(ChatColor.RED + "능력을 사용할 수 있는 대상이 없습니다.");
            return;
        }
        if (useNormal(context, player)) {
            for (Player target : targets) {
                target.teleport(player);
            }
        }
    }
}
