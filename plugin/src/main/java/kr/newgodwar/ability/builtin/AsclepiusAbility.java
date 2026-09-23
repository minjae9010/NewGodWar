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
    id = "asclepius",
    name = "아스클리피어스",
    description = "자신 또는 주변 아군을 완전히 회복합니다.",
    normalSkill = "블레이즈 막대기 좌클릭: 자신을 완전히 회복합니다.",
    normalStoneCost = 10,
    normalCooldownSeconds = 60,
    advancedSkill = "블레이즈 막대기 우클릭: 주변 아군을 완전히 회복합니다.",
    advancedStoneCost = 15,
    advancedCooldownSeconds = 120,
    passiveSkill = "없음",
    grade = AbilityGrade.A
)
final class AsclepiusAbility extends BaseAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.HEALING)
        .benefit(EffectCue.HEAL)
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            heal(player);
            feedback.affected(context, player, "완전 회복", false);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = nearbyPlayers(context, player, 5, true);
        if (targets.isEmpty()) {
            player.sendMessage("사용 가능한 대상이 없습니다.");
            return;
        }
        if (useAdvanced(context, player)) {
            for (Player target : targets) {
                heal(target);
                feedback.affected(context, target, "완전 회복", false);
            }
        }
    }
}
