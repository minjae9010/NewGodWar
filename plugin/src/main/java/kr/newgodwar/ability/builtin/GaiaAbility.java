package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import kr.newgodwar.ability.feedback.AbilityStyle;
import kr.newgodwar.ability.feedback.AbilityTheme;
import kr.newgodwar.ability.feedback.EffectCue;
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
    normalSkill = "블레이즈 막대기 좌클릭: 자신과 반경 8블록 아군의 체력을 6 회복하고 8초 재생을 줍니다.",
    normalStoneCost = 16,
    normalCooldownSeconds = 70,
    advancedSkill = "블레이즈 막대기 우클릭: 반경 9블록 적에게 8초 동안 감속 V(속박)와 약화를 줍니다.",
    advancedStoneCost = 30,
    advancedCooldownSeconds = 145,
    passiveSkill = "낙하 피해를 65% 줄입니다. 흙이나 잔디 블록 위에 서 있으면 재생을 유지합니다.",
    grade = AbilityGrade.S
)
final class GaiaAbility extends BaseAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.NATURE)
        .normal(EffectCue.HEAL)
        .hit(EffectCue.ROOT)
        .benefit(EffectCue.HEAL)
        .received(EffectCue.ROOT, AbilityDesigns.EARTH_ROOTS)
        .effect(EffectCue.HEAL, AbilityDesigns.EARTH_FLOWER)
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = nearbyPlayers(context, player, 8, true);
        targets.add(player);
        if (useNormal(context, player)) {
            for (Player target : targets) {
                restoreHealthApplied(context, target, 6.0D);
                effect(context, target, PotionEffectType.REGENERATION, 8, 0);
                feedback.notice(context, target, "대지의 치유 · 회복 / 재생 8초", false, true);
            }
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = nearbyPlayers(context, player, 9, false);
        if (targets.isEmpty()) {
            sendAbilityMessage(context, player, "failure", "능력을 사용할 수 있는 대상이 없습니다.");
            return;
        }
        if (useAdvanced(context, player)) {
            for (Player target : targets) {
                effect(context, target, "SLOWNESS", "SLOW", 8, 4);
                effect(context, target, PotionEffectType.WEAKNESS, 8, 0);
                feedback.notice(context, target, "대지의 속박 · 감속 / 약화 8초", true, true);
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
            effect(context, context.player(), PotionEffectType.REGENERATION, 6, 0);
        }
    }
}
