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
    id = "chronos",
    name = "크로노스",
    description = "주변 적의 시간을 늦추고 짧은 시간 정지를 일으킵니다.",
    normalSkill = "주변 적에게 강한 감속을 부여합니다.",
    normalStoneCost = 14,
    normalCooldownSeconds = 65,
    advancedSkill = "주변 적을 잠시 거의 움직이지 못하게 만듭니다.",
    advancedStoneCost = 32,
    advancedCooldownSeconds = 160,
    passiveSkill = "피격 시 가끔 공격자를 감속시킵니다.",
    grade = AbilityGrade.S
)
final class ChronosAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = nearbyPlayers(context, player, 10, false);
        if (targets.isEmpty()) {
            player.sendMessage("능력을 사용할 수 있는 대상이 없습니다.");
            return;
        }
        if (useNormal(context, player)) {
            for (Player target : targets) {
                effect(target, "SLOWNESS", "SLOW", 8, 2);
            }
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = nearbyPlayers(context, player, 8, false);
        if (targets.isEmpty()) {
            player.sendMessage("능력을 사용할 수 있는 대상이 없습니다.");
            return;
        }
        if (useAdvanced(context, player)) {
            for (Player target : targets) {
                effect(target, "SLOWNESS", "SLOW", 7, 8);
                effect(target, "MINING_FATIGUE", "SLOW_DIGGING", 7, 4);
                effect(target, PotionEffectType.WEAKNESS, 7, 1);
                target.setVelocity(new Vector(0, 0, 0));
            }
        }
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (!attacker && rollChance(1, 4)) {
            effect(opponent, "SLOWNESS", "SLOW", 7, 1);
        }
    }
}
