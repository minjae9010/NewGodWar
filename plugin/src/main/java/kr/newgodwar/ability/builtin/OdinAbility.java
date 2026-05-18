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
    id = "odin",
    name = "오딘",
    description = "룬의 가호로 버티고 적 하나에게 강한 룬 저주를 겁니다.",
    normalSkill = "자신에게 재생과 저항을 부여합니다.",
    normalStoneCost = 18,
    normalCooldownSeconds = 80,
    advancedSkill = "바라보는 적에게 실명, 약화, 감속을 부여합니다.",
    advancedStoneCost = 28,
    advancedCooldownSeconds = 130,
    passiveSkill = "치명상을 입으면 가끔 흡수와 저항을 얻습니다.",
    grade = AbilityGrade.S
)
final class OdinAbility extends BaseAbility {
    private long lastRuneGuard;

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            effect(player, PotionEffectType.REGENERATION, 7, 1);
            effect(player, "RESISTANCE", "DAMAGE_RESISTANCE", 8, 0);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        Player target = targetPlayerInSight(context, player, 22, false);
        if (target == null) {
            return;
        }
        if (useAdvanced(context, player)) {
            effect(target, PotionEffectType.BLINDNESS, 5, 0);
            effect(target, PotionEffectType.WEAKNESS, 7, 0);
            effect(target, "SLOWNESS", "SLOW", 7, 2);
            target.sendMessage(ChatColor.DARK_PURPLE + "오딘의 룬이 몸을 무겁게 합니다.");
        }
    }

    @Override
    public void onGenericDamage(AbilityPlayerContext context, EntityDamageEvent event) {
        long now = System.currentTimeMillis();
        Player player = context.player();
        if (now < lastRuneGuard || event.getFinalDamage() < player.getHealth() || player.getHealth() > 8.0D) {
            return;
        }
        if (rollChance(1, 3)) {
            lastRuneGuard = now + context.plugin().abilities().scaleCooldownMillis(75 * 1000L);
            effect(player, "ABSORPTION", "ABSORPTION", 8, 1);
            effect(player, "RESISTANCE", "DAMAGE_RESISTANCE", 5, 0);
            player.sendMessage(ChatColor.AQUA + "오딘의 룬 가호가 발동했습니다.");
        }
    }
}
