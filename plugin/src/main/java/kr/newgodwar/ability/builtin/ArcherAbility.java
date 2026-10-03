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
    id = "archer",
    name = "아처",
    description = "화살과 활을 만들고 화살 피해가 늘어납니다.",
    normalSkill = "블레이즈 막대기 좌클릭: 화살 4개를 만듭니다.",
    normalStoneCost = 5,
    normalCooldownSeconds = 20,
    advancedSkill = "블레이즈 막대기 우클릭: 활을 만듭니다.",
    advancedStoneCost = 15,
    advancedCooldownSeconds = 60,
    passiveSkill = "화살 피해가 30% 늘어납니다.",
    grade = AbilityGrade.B
)
final class ArcherAbility extends BaseAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.HUNT)
        .normal(EffectCue.CHARGE)
        .advanced(EffectCue.ITEM)
        .hit(EffectCue.HIT)
        .effect(EffectCue.CHARGE, AbilityDesigns.ARROW_BUNDLE)
        .effect(EffectCue.ITEM, AbilityDesigns.ARCHER_BOW)
        .effect(EffectCue.HIT, AbilityDesigns.BULLET)
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            give(player, Material.ARROW, 4);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useAdvanced(context, player)) {
            give(player, Material.BOW, 1);
        }
    }

    @Override
    public void onProjectileHit(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player victim) {
        event.setDamage(event.getDamage() * 1.3D);
        feedback.impact(context, victim);
    }
}
