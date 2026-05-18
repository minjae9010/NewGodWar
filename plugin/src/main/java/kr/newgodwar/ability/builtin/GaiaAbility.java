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
    id = "gaia",
    name = "가이아",
    description = "대지의 회복과 속박으로 전장을 장악합니다.",
    normalSkill = "주변 아군을 회복시키고 재생을 부여합니다.",
    normalStoneCost = 16,
    normalCooldownSeconds = 70,
    advancedSkill = "주변 적을 속박하고 약화시킵니다.",
    advancedStoneCost = 30,
    advancedCooldownSeconds = 145,
    passiveSkill = "낙하 피해를 줄이고 흙/잔디 위에서 재생을 유지합니다.",
    grade = AbilityGrade.S
)
final class GaiaAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = nearbyPlayers(context, player, 8, true);
        targets.add(player);
        if (useNormal(context, player)) {
            for (Player target : targets) {
                target.setHealth(Math.min(target.getMaxHealth(), target.getHealth() + 6.0D));
                effect(target, PotionEffectType.REGENERATION, 5, 0);
            }
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
                effect(target, "SLOWNESS", "SLOW", 6, 4);
                effect(target, PotionEffectType.WEAKNESS, 6, 0);
            }
        }
    }

    @Override
    public void onGenericDamage(AbilityPlayerContext context, EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setDamage(event.getDamage() * 0.35D);
        }
    }

    @Override
    public void onTick(AbilityPlayerContext context) {
        Material below = context.player().getLocation().clone().add(0, -1, 0).getBlock().getType();
        if (below == Material.GRASS || below == Material.DIRT) {
            effect(context.player(), PotionEffectType.REGENERATION, 3, 0);
        }
    }
}
