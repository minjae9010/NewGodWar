package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import kr.newgodwar.ability.feedback.AbilityStyle;
import kr.newgodwar.ability.feedback.AbilityTheme;
import kr.newgodwar.ability.feedback.EffectCue;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

@AbilityInfo(
    id = "shinsaimdang",
    name = "신사임당",
    description = "난초 그림으로 아군을 보호하고 치유해 전열을 유지합니다.",
    normalSkill = "블레이즈 막대기 좌클릭: 난초 그림을 펼쳐 자신과 반경 8블록 아군에게 흡수 I(추가 체력 4)을 6초 동안 줍니다.",
    normalStoneCost = 8,
    normalCooldownSeconds = 25,
    advancedSkill = "블레이즈 막대기 우클릭: 자신과 반경 8블록 아군의 체력을 5 회복하고 8초 재생을 줍니다.",
    advancedStoneCost = 20,
    advancedCooldownSeconds = 115,
    passiveSkill = "공격받으면 20% 확률로 공격자에게 8초 약화를 줍니다.",
    grade = AbilityGrade.B
)
final class ShinSaimdangAbility extends BaseAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.NATURE)
        .advanced(EffectCue.HEAL)
        .normal(EffectCue.GUARD)
        .benefit(EffectCue.HEAL)
        .effect(EffectCue.GUARD, AbilityDesigns.INK_ORCHID)
        .effect(EffectCue.HEAL, AbilityDesigns.INK_ORCHID)
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (!useNormal(context, player)) {
            return;
        }
        List<Player> targets = nearbyPlayers(context, player, 8, true);
        targets.add(player);
        for (Player target : targets) effect(context, target, "ABSORPTION", "ABSORPTION", 6, 0);
        player.sendMessage(ChatColor.LIGHT_PURPLE + "난초 그림이 아군을 감쌉니다.");
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = nearbyPlayers(context, player, 8, true);
        targets.add(player);
        if (!useAdvanced(context, player)) {
            return;
        }
        for (Player target : targets) {
            restoreHealthApplied(context, target, 5.0D);
            effect(context, target, PotionEffectType.REGENERATION, 8, 0);
        }
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (!attacker && oneIn(5)) {
            effect(context, opponent, PotionEffectType.WEAKNESS, 8, 0);
        }
    }
}
