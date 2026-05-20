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
    id = "assasin",
    name = "암살자",
    description = "짧은 도약으로 거리를 좁히고 근처 적의 뒤로 파고듭니다.",
    normalSkill = "바라보는 방향으로 도약합니다.",
    normalStoneCost = 3,
    normalCooldownSeconds = 1,
    advancedSkill = "반경 10블록 안의 적 뒤로 이동합니다.",
    advancedStoneCost = 18,
    advancedCooldownSeconds = 30,
    passiveSkill = "없음",
    grade = AbilityGrade.A
)
final class AssasinAbility extends BaseAbility {
    private boolean dashLocked;

    @Override
    public void onRemove(AbilityPlayerContext context) {
        dashLocked = false;
    }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (dashLocked) {
            sendAbilityMessage(context, player, "failure", ChatColor.YELLOW + "아직 도약할 수 없습니다.");
            return;
        }
        if (useNormal(context, player)) {
            dash(player);
            dashLocked = true;
            Bukkit.getScheduler().scheduleSyncDelayedTask(context.plugin(), () -> dashLocked = false, 12L);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useAdvanced(context, player)) {
            backstab(context, player);
        }
    }
}
