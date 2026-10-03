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
    id = "blacksmith",
    name = "대장장이",
    description = "조약돌을 철로, 철괴를 다이아몬드로 바꿔 장비 성장을 돕습니다.",
    normalSkill = "블레이즈 막대기 좌클릭: 조약돌 64개로 철괴 3개를 만듭니다.",
    normalStoneCost = 64,
    normalCooldownSeconds = 40,
    advancedSkill = "블레이즈 막대기 우클릭: 철괴 15개로 다이아몬드 1개를 만듭니다. 조약돌은 들지 않습니다.",
    advancedStoneCost = 0,
    advancedCooldownSeconds = 180,
    passiveSkill = "없음",
    grade = AbilityGrade.B
)
final class BlacksmithAbility extends BaseAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.CRAFT)
        .normal(EffectCue.FORGE)
        .advanced(EffectCue.CHARGE)
        .effect(EffectCue.FORGE, AbilityDesigns.IRON_FORGE)
        .effect(EffectCue.CHARGE, AbilityDesigns.GEM_FORGE)
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            give(player, Material.IRON_INGOT, 3);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (use(context, player, 2, Material.IRON_INGOT, 15, 180)) {
            give(player, Material.DIAMOND, 1);
        }
    }
}
