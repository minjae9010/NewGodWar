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
    id = "zet",
    name = "제트기관",
    description = "화염 피해를 받으면 높은 속도로 가속합니다.",
    normalSkill = "없음",
    normalStoneCost = 0,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "신속이 없을 때 화염 피해를 받으면 50% 확률로 8초 동안 신속 II를 얻습니다.",
    grade = AbilityGrade.B
)
final class ZetAbility extends BaseAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.FIRE)
        .hit(EffectCue.FIRE)
        .passive(EffectCue.WIND)
        .effect(EffectCue.FIRE, AbilityDesigns.JET_EXHAUST)
        .effect(EffectCue.WIND, AbilityDesigns.JET_EXHAUST)
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    @Override
    public void onGenericDamage(AbilityPlayerContext context, EntityDamageEvent event) {
        if (fire(event.getCause()) && !context.player().hasPotionEffect(PotionEffectType.SPEED) && RANDOM.nextBoolean()) {
            effect(context, context.player(), PotionEffectType.SPEED, 8, 1);
        }
    }
}
