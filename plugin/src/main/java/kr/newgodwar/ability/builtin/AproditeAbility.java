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
    id = "aprodite",
    name = "아프로디테",
    description = "땅에 서서 주변 플레이어를 내 위치로 끌어와요.",
    normalSkill = "블레이즈 막대기 좌클릭: 반경 20블록의 생존 적을 자신의 위치로 끌어와요. 아군은 이동하지 않아요.",
    normalStoneCost = 24,
    normalCooldownSeconds = 120,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "없음",
    grade = AbilityGrade.A
)
final class AproditeAbility extends BaseAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.HEALING)
        .hit(EffectCue.PORTAL)
        .effect(EffectCue.PORTAL, AbilityDesigns.ROSE_GATE)
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

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
                if (target.teleport(player)) feedback.affected(context, target, "매혹 · 끌어오기", true);
            }
        }
    }
}
