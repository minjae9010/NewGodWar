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
    id = "quetzalcoatl",
    name = "케찰코아틀",
    description = "바람과 깃털의 힘으로 도약하고 적을 날려보냅니다.",
    normalSkill = "바라보는 방향으로 크게 도약합니다.",
    normalStoneCost = 8,
    normalCooldownSeconds = 35,
    advancedSkill = "주변 적을 공중으로 띄우고 밀쳐냅니다.",
    advancedStoneCost = 22,
    advancedCooldownSeconds = 110,
    passiveSkill = "낙하 피해를 무시합니다.",
    grade = AbilityGrade.A
)
final class QuetzalcoatlAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            Vector vector = player.getEyeLocation().getDirection().normalize().multiply(1.5D);
            vector.setY(1.0D);
            player.setVelocity(vector);
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
            for (Player target : targets) {
                target.setVelocity(new Vector(0, 1.25D, 0));
                effect(target, "SLOWNESS", "SLOW", 4, 0);
            }
            push(context, player, targets, 1.5D, 6L);
        }
    }

    @Override
    public void onGenericDamage(AbilityPlayerContext context, EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
        }
    }
}
