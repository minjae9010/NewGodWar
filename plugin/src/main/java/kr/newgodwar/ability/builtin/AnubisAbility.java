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
    id = "anubis",
    name = "아누비스",
    description = "저승의 심판으로 적의 생명력을 빼앗습니다.",
    normalSkill = "바라보는 적에게 위더와 약화를 부여합니다.",
    normalStoneCost = 14,
    normalCooldownSeconds = 65,
    advancedSkill = "주변 적에게 피해를 주고 자신을 회복합니다.",
    advancedStoneCost = 26,
    advancedCooldownSeconds = 125,
    passiveSkill = "치명상을 입으면 가끔 흡수를 얻습니다.",
    grade = AbilityGrade.A
)
final class AnubisAbility extends BaseAbility {
    private long lastScales;

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        Player target = targetPlayerInSight(context, player, 20, false);
        if (target == null) {
            return;
        }
        if (useNormal(context, player)) {
            effect(target, "WITHER", "WITHER", 6, 0);
            effect(target, PotionEffectType.WEAKNESS, 6, 0);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = nearbyPlayers(context, player, 7, false);
        if (targets.isEmpty()) {
            player.sendMessage("능력을 사용할 수 있는 대상이 없습니다.");
            return;
        }
        if (useAdvanced(context, player)) {
            double healed = 0.0D;
            for (Player target : targets) {
                damage(target, 4.0D, player);
                healed += 2.0D;
            }
            player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + healed));
        }
    }

    @Override
    public void onGenericDamage(AbilityPlayerContext context, EntityDamageEvent event) {
        long now = System.currentTimeMillis();
        Player player = context.player();
        if (now < lastScales || event.getFinalDamage() < player.getHealth() || player.getHealth() > 6.0D) {
            return;
        }
        if (rollChance(1, 4)) {
            lastScales = now + context.plugin().abilities().scaleCooldownMillis(90 * 1000L);
            effect(player, "ABSORPTION", "ABSORPTION", 8, 1);
            player.sendMessage(ChatColor.GOLD + "아누비스의 저울이 죽음을 잠시 미룹니다.");
        }
    }
}
