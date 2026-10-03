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
    id = "fisher",
    name = "노인과바다",
    description = "낚시로 잡동사니와 광물을 얻습니다.",
    normalSkill = "없음",
    normalStoneCost = 0,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "능력을 받으면 낚싯대를 받습니다. 물고기 대신 철괴 1~2개(64%), 원목 3개(15%), 블레이즈 막대기(15%), 다이아몬드 1~2개(6%)를 낚습니다.",
    grade = AbilityGrade.B
)
final class FisherAbility extends BaseAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.WATER)
        .hit(EffectCue.WATER)
        .passive(EffectCue.WATER)
        .effect(EffectCue.WATER, AbilityDesigns.FISH_HOOK)
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    @Override
    public void onPrepare(AbilityPlayerContext context) {
        give(context.player(), Material.FISHING_ROD, 1);
    }

    @Override
    public void onFish(AbilityPlayerContext context, PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            if (event.getCaught() != null) {
                event.getCaught().remove();
            }
            feedback.passive(context, "바다의 선물");
            int roll = RANDOM.nextInt(100);
            if (roll < 5) {
                give(context.player(), Material.DIAMOND, 1);
            } else if (roll < 20) {
                give(context.player(), material("OAK_LOG", "LOG"), 3);
            } else if (roll < 35) {
                give(context.player(), STAFF, 1);
            } else if (roll < 99) {
                give(context.player(), Material.IRON_INGOT, roll < 80 ? 1 : 2);
            } else {
                give(context.player(), Material.DIAMOND, 2);
            }
        }
    }
}
