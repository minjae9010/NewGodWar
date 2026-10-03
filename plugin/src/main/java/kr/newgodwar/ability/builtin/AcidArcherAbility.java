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
    id = "acidarcher",
    name = "독화살아처",
    description = "화살로 피해 대신 독을 퍼뜨리는 궁수입니다.",
    normalSkill = "블레이즈 막대기 좌클릭: 화살 2개를 만듭니다.",
    normalStoneCost = 5,
    normalCooldownSeconds = 20,
    advancedSkill = "블레이즈 막대기 우클릭: 활을 만듭니다.",
    advancedStoneCost = 15,
    advancedCooldownSeconds = 60,
    passiveSkill = "화살로 맞힌 적에게 피해 대신 12초 독을 줍니다.",
    grade = AbilityGrade.A
)
final class AcidArcherAbility extends BaseAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.HUNT)
        .normal(EffectCue.CHARGE)
        .advanced(EffectCue.ITEM)
        .hit(EffectCue.POISON)
        .trail(AbilityTheme.SHADOW)
        .effect(EffectCue.CHARGE, AbilityDesigns.VENOM_ARROWS)
        .effect(EffectCue.ITEM, AbilityDesigns.VENOM_BOW)
        .effect(EffectCue.POISON, AbilityDesigns.VENOM)
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            give(player, Material.ARROW, 2);
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
        event.setDamage(0.0D);
        effect(context, victim, PotionEffectType.POISON, 12, 0);
    }
}
