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
    id = "scrooge",
    name = "스크루지",
    description = "팀 전체의 능력 조약돌 비용을 절반으로 낮춥니다.",
    normalSkill = "없음",
    normalStoneCost = 0,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "자신을 포함한 같은 팀 전원의 능력 조약돌 비용이 절반(내림)이 됩니다. 철괴 같은 다른 재료는 그대로입니다.",
    grade = AbilityGrade.S
)
final class ScroogeAbility extends BaseAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.CRAFT)
        .passive(EffectCue.ITEM)
        .effect(EffectCue.ITEM, AbilityDesigns.LEDGER)
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    @Override
    public void onAssign(AbilityPlayerContext context) {
        feedback.passive(context, "팀 조약돌 비용 50% 절감");
    }
}
