package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

@AbilityInfo(
    id = "yugwansun",
    name = "유관순",
    description = "만세의 함성으로 아군의 사기를 끌어올리고 적을 흔듭니다.",
    normalSkill = "주변 아군에게 신속, 공격력 증가, 저항을 주고 주변 적에게 약화와 감속을 부여합니다.",
    normalStoneCost = 44,
    normalCooldownSeconds = 190,
    advancedSkill = "주변 아군에게 강한 생존 효과를 주고 적에게 실명, 혼란, 화염을 부여합니다.",
    advancedStoneCost = 64,
    advancedCooldownSeconds = 280,
    passiveSkill = "치명상을 입으면 짧게 흡수와 저항을 얻습니다.",
    grade = AbilityGrade.S
)
final class YuGwanSunAbility extends BaseAbility {
    private long lastIndependenceGuard;

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> allies = nearbyPlayers(context, player, 12, true);
        List<Player> enemies = nearbyPlayers(context, player, 12, false);
        allies.add(player);
        if (!useNormal(context, player)) {
            return;
        }
        for (Player ally : allies) {
            effect(ally, PotionEffectType.SPEED, 10, 1);
            effect(ally, "STRENGTH", "INCREASE_DAMAGE", 8, 0);
            effect(ally, "RESISTANCE", "DAMAGE_RESISTANCE", 7, 0);
        }
        for (Player enemy : enemies) {
            effect(enemy, PotionEffectType.WEAKNESS, 8, 0);
            effect(enemy, "SLOWNESS", "SLOW", 8, 1);
        }
        player.sendMessage(ChatColor.AQUA + "만세의 함성이 전장에 울려 퍼졌습니다.");
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> allies = nearbyPlayers(context, player, 14, true);
        List<Player> enemies = nearbyPlayers(context, player, 14, false);
        if (enemies.isEmpty()) {
            player.sendMessage("능력을 사용할 수 있는 대상이 없습니다.");
            return;
        }
        allies.add(player);
        if (!useAdvanced(context, player)) {
            return;
        }
        for (Player ally : allies) {
            effect(ally, "ABSORPTION", "ABSORPTION", 12, 1);
            effect(ally, PotionEffectType.REGENERATION, 8, 1);
            effect(ally, "RESISTANCE", "DAMAGE_RESISTANCE", 8, 0);
        }
        for (Player enemy : enemies) {
            enemy.setFireTicks(120);
            effect(enemy, PotionEffectType.BLINDNESS, 5, 0);
            effect(enemy, PotionEffectType.CONFUSION, 7, 0);
            effect(enemy, PotionEffectType.WEAKNESS, 7, 0);
        }
        player.sendMessage(ChatColor.GOLD + "독립의 횃불이 적진을 흔들었습니다.");
    }

    @Override
    public void onGenericDamage(AbilityPlayerContext context, EntityDamageEvent event) {
        long now = System.currentTimeMillis();
        Player player = context.player();
        if (now < lastIndependenceGuard || event.getFinalDamage() < player.getHealth() || player.getHealth() > 8.0D) {
            return;
        }
        lastIndependenceGuard = now + context.plugin().abilities().scaleCooldownMillis(90 * 1000L);
        effect(player, "ABSORPTION", "ABSORPTION", 10, 1);
        effect(player, "RESISTANCE", "DAMAGE_RESISTANCE", 6, 1);
        effect(player, PotionEffectType.REGENERATION, 5, 1);
        player.sendMessage(ChatColor.AQUA + "꺼지지 않는 의지가 치명상을 버텨냈습니다.");
    }
}
