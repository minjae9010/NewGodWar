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
    id = "pan",
    name = "판",
    description = "광란의 피리로 적의 섬 이동을 방해하고 아군의 진입을 돕습니다.",
    normalSkill = "반경 8블록 적에게 혼란과 감속을 부여합니다.",
    normalStoneCost = 12,
    normalCooldownSeconds = 60,
    advancedSkill = "반경 9블록 아군에게 신속과 점프 강화 효과를 부여합니다.",
    advancedStoneCost = 18,
    advancedCooldownSeconds = 100,
    passiveSkill = "피격 시 12.5% 확률로 공격자를 밀쳐냅니다.",
    grade = AbilityGrade.B
)
final class PanAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = nearbyPlayers(context, player, 8, false);
        if (targets.isEmpty()) {
            player.sendMessage("능력을 사용할 수 있는 대상이 없습니다.");
            return;
        }
        if (useNormal(context, player)) {
            for (Player target : targets) {
                effect(target, "NAUSEA", "CONFUSION", 6, 0);
                effect(target, "SLOWNESS", "SLOW", 5, 0);
            }
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = nearbyPlayers(context, player, 9, true);
        targets.add(player);
        if (useAdvanced(context, player)) {
            for (Player target : targets) {
                effect(target, PotionEffectType.SPEED, 8, 0);
                effect(target, PotionEffectType.JUMP, 8, 0);
            }
        }
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (!attacker && oneIn(8)) {
            Vector vector = opponent.getLocation().toVector().subtract(context.player().getLocation().toVector());
            if (vector.lengthSquared() > 0.0D) {
                opponent.setVelocity(vector.normalize().multiply(1.2D));
            }
        }
    }
}
