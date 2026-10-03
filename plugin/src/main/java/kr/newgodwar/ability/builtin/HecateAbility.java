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
    id = "hecate",
    name = "헤카테",
    description = "짧은 은신과 지정한 적에게 약한 저주를 사용해요.",
    normalSkill = "블레이즈 막대기 좌클릭: 잠시 투명화하고 신속을 얻어요.",
    normalStoneCost = 14,
    normalCooldownSeconds = 75,
    advancedSkill = "먼저 /x <플레이어>로 적 지정. 블레이즈 막대기 우클릭: 같은 월드의 지정한 적에게 실명과 감속을 짧게 줘요. 거리·시야 제한은 없어요.",
    advancedStoneCost = 20,
    advancedCooldownSeconds = 115,
    passiveSkill = "타깃 지정 명령을 사용할 수 있어요.",
    grade = AbilityGrade.B
)
final class HecateAbility extends BaseAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.SHADOW)
        .hit(EffectCue.STEALTH)
        .privateCast()
        .effect(EffectCue.STEALTH, AbilityDesigns.WITCH_MOONS)
        .effect(EffectCue.POISON, AbilityDesigns.WITCH_MOONS)
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    @Override
    public boolean requiresTarget() {
        return true;
    }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            effect(context, player, PotionEffectType.INVISIBILITY, 8, 0);
            effect(context, player, PotionEffectType.SPEED, 8, 0);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        Player target = commandTargetPlayer(context, player, false);
        if (target == null) {
            return;
        }
        if (useAdvanced(context, player)) {
            effect(context, target, PotionEffectType.BLINDNESS, 7, 0);
            effect(context, target, "SLOWNESS", "SLOW", 8, 1);
            target.sendMessage(ChatColor.DARK_PURPLE + "헤카테의 저주가 시야를 흐립니다.");
        }
    }
}
