package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import kr.newgodwar.ability.feedback.AbilityStyle;
import kr.newgodwar.ability.feedback.AbilityTheme;
import kr.newgodwar.ability.feedback.EffectCue;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

@AbilityInfo(
    id = "sejong",
    name = "세종대왕",
    description = "집현전의 지혜로 아군을 강화하고 훈민정음의 칙령으로 적의 능력을 봉인해요.",
    normalSkill = "블레이즈 막대기 좌클릭: 자신과 반경 10블록 아군에게 재생, 저항, 성급함을 부여하고 경험 레벨을 나눠요.",
    normalStoneCost = 40,
    normalCooldownSeconds = 180,
    advancedSkill = "블레이즈 막대기 우클릭: 28블록 안에서 바라보는 적의 능력을 봉인하고 실명, 약화, 감속을 줘요.",
    advancedStoneCost = 64,
    advancedCooldownSeconds = 300,
    passiveSkill = "책을 들고 공격하면 피해가 증가하고, 능력을 받으면 책을 받아요.",
    grade = AbilityGrade.S
)
final class SejongAbility extends BaseAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.RUNE)
        .hit(EffectCue.SEAL)
        .benefit(EffectCue.HEAL)
        .effect(EffectCue.SEAL, AbilityDesigns.ROYAL_DECREE)
        .effect(EffectCue.HEAL, AbilityDesigns.ROYAL_GRACE)
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    @Override
    public void onPrepare(AbilityPlayerContext context) {
        give(context.player(), Material.BOOK, 1);
    }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = nearbyPlayers(context, player, 10, true);
        targets.add(player);
        if (!useNormal(context, player)) {
            return;
        }
        for (Player target : targets) {
            effect(context, target, PotionEffectType.REGENERATION, 10, 1);
            effect(context, target, "RESISTANCE", "DAMAGE_RESISTANCE", 10, 0);
            effect(context, target, "HASTE", "FAST_DIGGING", 14, 1);
            target.setLevel(target.getLevel() + 2);
        }
        player.sendMessage(ChatColor.AQUA + "집현전의 지혜가 아군에게 퍼졌습니다.");
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        Player target = targetPlayerInSight(context, player, 28, false);
        if (target == null) {
            return;
        }
        if (!useAdvanced(context, player)) {
            return;
        }
        if (context.plugin().abilities().session(target) != null) {
            context.plugin().abilities().suppressAbility(target, 10);
        }
        effect(context, target, PotionEffectType.BLINDNESS, 8, 0);
        effect(context, target, PotionEffectType.WEAKNESS, 12, 0);
        effect(context, target, "SLOWNESS", "SLOW", 12, 2);
        target.sendMessage(ChatColor.DARK_PURPLE + "훈민정음의 칙령이 능력을 봉합니다.");
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (attacker && holding(context.player(), Material.BOOK)) {
            event.setDamage(event.getDamage() * 1.35D);
            feedback.cue(context, opponent, kr.newgodwar.ability.feedback.EffectCue.HIT);
        }
    }
}
