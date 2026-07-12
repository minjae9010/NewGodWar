package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

@AbilityInfo(
    id = "yisunsin",
    name = "이순신",
    description = "학익진으로 전열을 세우고 거북선 포격으로 적진을 무너뜨립니다.",
    normalSkill = "주변 적을 밀쳐내고 자신에게 저항과 공격력 증가를 부여합니다.",
    normalStoneCost = 48,
    normalCooldownSeconds = 170,
    advancedSkill = "바라보는 적에게 강한 포격 폭발과 감속, 약화를 부여합니다.",
    advancedStoneCost = 64,
    advancedCooldownSeconds = 320,
    passiveSkill = "폭발 피해를 줄이고, 체력이 낮을수록 공격 피해가 증가합니다.",
    grade = AbilityGrade.S
)
final class YiSunSinAbility extends BaseAbility {
    @Override
    public void onPrepare(AbilityPlayerContext context) {
        give(context.player(), Material.IRON_SWORD, 1);
    }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = nearbyPlayers(context, player, 10, false);
        if (targets.isEmpty()) {
            player.sendMessage("능력을 사용할 수 있는 대상이 없습니다.");
            return;
        }
        if (!useNormal(context, player)) {
            return;
        }
        push(context, player, targets, 2.4D, 5L);
        effect(player, "RESISTANCE", "DAMAGE_RESISTANCE", 11, 0);
        effect(player, "STRENGTH", "INCREASE_DAMAGE", 9, 0);
        player.sendMessage(ChatColor.AQUA + "학익진으로 전열을 뒤흔들었습니다.");
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        Player target = targetPlayerInSight(context, player, 32, false);
        if (target == null) {
            return;
        }
        if (!useAdvanced(context, player)) {
            return;
        }
        createExplosion(context, player, target.getLocation(), 2.0F, false, true);
        damage(context, target, 8.0D, player);
        effect(target, "SLOWNESS", "SLOW", 11, 2);
        effect(target, PotionEffectType.WEAKNESS, 11, 0);
        target.sendMessage(ChatColor.RED + "거북선 포격이 전장을 뒤덮었습니다.");
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (!attacker) {
            return;
        }
        Player player = context.player();
        if (player.getHealth() <= 6.0D) {
            event.setDamage(event.getDamage() * 1.45D);
        } else if (player.getHealth() <= 10.0D) {
            event.setDamage(event.getDamage() * 1.25D);
        }
    }

    @Override
    public void onGenericDamage(AbilityPlayerContext context, EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
            || event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
            event.setDamage(event.getDamage() * 0.45D);
        }
    }
}
