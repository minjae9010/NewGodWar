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
    id = "hera",
    name = "헤라",
    description = "아군을 보호하고 피격 시 낮은 확률로 공격자를 약화시킵니다.",
    normalSkill = "자신에게 짧은 저항과 흡수를 부여합니다.",
    normalStoneCost = 12,
    normalCooldownSeconds = 70,
    advancedSkill = "주변 아군에게 짧은 저항을 부여합니다.",
    advancedStoneCost = 24,
    advancedCooldownSeconds = 130,
    passiveSkill = "피격 시 낮은 확률로 공격자에게 약화를 부여합니다.",
    grade = AbilityGrade.B
)
final class HeraAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            effect(player, "RESISTANCE", "DAMAGE_RESISTANCE", 10, 0);
            effect(player, PotionEffectType.ABSORPTION, 10, 0);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = nearbyPlayers(context, player, 8, true);
        targets.add(player);
        if (useAdvanced(context, player)) {
            for (Player target : targets) {
                effect(target, "RESISTANCE", "DAMAGE_RESISTANCE", 8, 0);
            }
        }
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (!attacker && rollChance(2, 20)) {
            effect(opponent, PotionEffectType.WEAKNESS, 8, 0);
            context.player().sendMessage(ChatColor.LIGHT_PURPLE + "헤라의 위엄이 공격자를 약화시켰습니다.");
        }
    }
}
