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
    id = "zet",
    name = "제트기관",
    description = "화염 피해를 받으면 높은 속도로 가속합니다.",
    normalSkill = "없음",
    normalStoneCost = 0,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "화염 피해를 받으면 확률적으로 더 강한 신속 효과를 얻습니다.",
    grade = AbilityGrade.B
)
final class ZetAbility extends BaseAbility {
    @Override
    public void onGenericDamage(AbilityPlayerContext context, EntityDamageEvent event) {
        if (fire(event.getCause()) && !context.player().hasPotionEffect(PotionEffectType.SPEED) && RANDOM.nextBoolean()) {
            effect(context.player(), PotionEffectType.SPEED, 5, 1);
        }
    }
}
