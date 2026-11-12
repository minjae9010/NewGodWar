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
    id = "goldspoon",
    name = "금수저",
    description = "상속받은 장비로 출발하고 주기적으로 새 장비를 뽑습니다.",
    normalSkill = "블레이즈 막대기 좌클릭: 금 레깅스(90%) 또는 다이아몬드 레깅스(10%)를 받습니다. 조약돌은 들지 않습니다.",
    normalStoneCost = 0,
    normalCooldownSeconds = 45,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "능력을 받으면 상속 장비를 한 번 받고 보급 쿨타임이 시작됩니다. 사망으로 장비가 추가 지급되거나 쿨타임이 초기화되지 않습니다.",
    grade = AbilityGrade.B
)
final class GoldspoonAbility extends BaseAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.CRAFT)
        .normal(EffectCue.ITEM)
        .hit(EffectCue.ITEM)
        .effect(EffectCue.ITEM, AbilityDesigns.GOLD_SPOON)
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    @Override
    public void onPrepare(AbilityPlayerContext context) {
        inherit(context);
        setCooldown(context, 1, context.ability().normalCooldownSeconds());
    }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) inherit(context);
    }

    private void inherit(AbilityPlayerContext context) {
        give(context.player(), rollChance(9, 10) ? material("GOLDEN_LEGGINGS", "GOLD_LEGGINGS") : Material.DIAMOND_LEGGINGS, 1);
        feedback.castCue(context, context.player(), EffectCue.ITEM);
    }
}
