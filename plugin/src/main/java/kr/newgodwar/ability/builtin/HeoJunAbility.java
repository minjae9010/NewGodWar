package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import kr.newgodwar.ability.feedback.AbilityStyle;
import kr.newgodwar.ability.feedback.AbilityTheme;
import kr.newgodwar.ability.feedback.EffectCue;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

@AbilityInfo(
    id = "heojun",
    name = "허준",
    description = "동의보감으로 치명적인 상태 이상을 씻어내고 아군을 크게 회복시킵니다.",
    normalSkill = "블레이즈 막대기 좌클릭: 자신을 완전히 회복하고 주요 해로운 효과를 제거합니다.",
    normalStoneCost = 36,
    normalCooldownSeconds = 160,
    advancedSkill = "블레이즈 막대기 우클릭: 자신과 주변 아군을 완전히 회복하고 흡수, 재생, 저항을 부여합니다.",
    advancedStoneCost = 64,
    advancedCooldownSeconds = 320,
    passiveSkill = "독과 위더 피해를 무시하고 받는 회복량이 증가합니다.",
    grade = AbilityGrade.S
)
final class HeoJunAbility extends BaseAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.HEALING)
        .benefit(EffectCue.HEAL)
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (!useNormal(context, player)) {
            return;
        }
        heal(player);
        cleanse(player);
        feedback.affected(context, player, "동의보감 · 완전 회복 / 해로운 효과 해제", false);
        player.sendMessage(ChatColor.GREEN + "동의보감의 처방으로 몸을 회복했습니다.");
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = nearbyPlayers(context, player, 10, true);
        targets.add(player);
        if (!useAdvanced(context, player)) {
            return;
        }
        for (Player target : targets) {
            heal(target);
            cleanse(target);
            effect(context, target, "ABSORPTION", "ABSORPTION", 12, 1);
            effect(context, target, PotionEffectType.REGENERATION, 12, 1);
            effect(context, target, "RESISTANCE", "DAMAGE_RESISTANCE", 10, 0);
            feedback.affected(context, target, "대처방 · 완전 회복 / 정화 / 보호", false);
        }
        player.sendMessage(ChatColor.GREEN + "동의보감의 대처방이 아군을 살렸습니다.");
    }

    @Override
    public void onGenericDamage(AbilityPlayerContext context, EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.POISON
            || event.getCause() == EntityDamageEvent.DamageCause.WITHER) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onRegainHealth(AbilityPlayerContext context, EntityRegainHealthEvent event) {
        event.setAmount(event.getAmount() * 1.35D);
    }

    private void cleanse(Player player) {
        player.removePotionEffect(PotionEffectType.POISON);
        player.removePotionEffect(PotionEffectType.WITHER);
        player.removePotionEffect(PotionEffectType.HUNGER);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.WEAKNESS);
        player.removePotionEffect(PotionEffectType.CONFUSION);
        removeEffect(player, "SLOWNESS", "SLOW");
        removeEffect(player, "MINING_FATIGUE", "SLOW_DIGGING");
    }
}
