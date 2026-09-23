package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import kr.newgodwar.ability.feedback.AbilityStyle;
import kr.newgodwar.ability.feedback.AbilityTheme;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.List;

@AbilityInfo(
    id = "odin", name = "오딘",
    description = "두 까마귀로 적을 정찰하고 표적을 좇는 궁니르로 심판합니다.",
    normalSkill = "블레이즈 막대기 좌클릭: 바라보는 24블록 안의 적을 두 까마귀로 8초간 추적합니다.",
    normalStoneCost = 10, normalCooldownSeconds = 30,
    advancedSkill = "블레이즈 막대기 우클릭: 정찰 표식을 소비해 궁니르를 던집니다. 0.6초 뒤 24블록 안에서 시야가 닿는 표적을 추적해 피해 7을 줍니다.",
    advancedStoneCost = 24, advancedCooldownSeconds = 65,
    passiveSkill = "까마귀는 움직이는 표적을 따라다닙니다. 벽 뒤나 추적 범위 밖의 적에게 궁니르는 적중하지 않습니다.",
    grade = AbilityGrade.S
)
final class OdinAbility extends TransientAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.RUNE)
        .dedicated()
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    private Player prey;
    private int scoutTask = -1;
    private boolean spearPending;

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (prey != null || spearPending) return;
        Player target = targetPlayerInSight(context, player, 24, false);
        if (target == null || !useNormal(context, player)) return;
        prey = target;
        feedback.affected(context, target, "후긴과 무닌이 당신을 추적합니다 · 8초", true);
        final int[] phase = {0};
        scoutTask = scheduleRepeating(context, () -> {
            if (!validEnemy(context, prey, 24) || phase[0] >= 16) { stopScout(); return; }
            feedback.flock(context, prey, phase[0]++, false);
        }, 1, 10);
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (spearPending || !validEnemy(context, prey, 24) || !player.hasLineOfSight(prey)) {
            sendAbilityMessage(context, player, "failure", "시야 안에 까마귀가 추적 중인 적이 필요합니다.");
            return;
        }
        if (!useAdvanced(context, player)) return;
        final Player target = prey;
        stopScout();
        spearPending = true;
        feedback.affected(context, target, "궁니르가 날아옵니다 · 엄폐하세요", true);
        feedback.sigil(context, target.getLocation(), 1);
        scheduleLater(context, () -> {
            spearPending = false;
            if (!validEnemy(context, target, 24) || !player.hasLineOfSight(target)) return;
            feedback.spear(context, player.getEyeLocation(), target.getEyeLocation());
            damage(context, target, 7, player);
        }, 12);
    }

    private void stopScout() {
        cancelScheduledTask(scoutTask);
        scoutTask = -1;
        prey = null;
    }

    @Override
    public List<String> activeTimerLines() {
        List<String> lines = super.activeTimerLines();
        if (prey != null) lines.add("까마귀 정찰 · " + prey.getName());
        return lines;
    }

    @Override
    protected void clearTransientState() { prey = null; scoutTask = -1; spearPending = false; }
}
