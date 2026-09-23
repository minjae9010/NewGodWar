package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import kr.newgodwar.ability.feedback.AbilityStyle;
import kr.newgodwar.ability.feedback.AbilityTheme;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Collections;
import java.util.List;

@AbilityInfo(
    id = "hera", name = "헤라",
    description = "아군 한 명과 수호의 서약을 맺고 함께 있을 때 왕후의 축복을 나눕니다.",
    normalSkill = "블레이즈 막대기 좌클릭: 바라보는 8블록 안의 아군과 8초간 서약합니다. 서로 8블록 이내에 있으면 둘 모두 저항 I을 얻으며 멀어지면 서약이 끝납니다.",
    normalStoneCost = 12, normalCooldownSeconds = 35,
    advancedSkill = "블레이즈 막대기 우클릭: 유지 중인 서약을 소비해 두 사람의 체력을 4 회복하고 4초간 흡수 I을 부여합니다.",
    advancedStoneCost = 20, advancedCooldownSeconds = 70,
    passiveSkill = "서약은 한 명과만 맺을 수 있으며 동료가 사망하거나 팀을 떠나도 종료됩니다.",
    grade = AbilityGrade.B
)
final class HeraAbility extends TransientAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.GUARD)
        .dedicated()
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    private Player partner;
    private int bondTask = -1;

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (partner != null) return;
        Player target = targetPlayerInSight(context, player, 8, true);
        if (target == null || target.isDead() || !useNormal(context, player)) return;
        partner = target;
        feedback.affected(context, target, "헤라와 수호의 서약 · 서로 8블록 이내를 유지하세요", false);
        final int[] remaining = {8};
        bondTask = scheduleRepeating(context, () -> {
            if (!bondValid(context) || remaining[0]-- <= 0) { endBond(); return; }
            effectTicks(player, "RESISTANCE", "DAMAGE_RESISTANCE", 22, 0);
            effectTicks(partner, "RESISTANCE", "DAMAGE_RESISTANCE", 22, 0);
            oath(context, player, partner);
        }, 1, 20);
    }

    private boolean bondValid(AbilityPlayerContext context) {
        return partner != null && alliesInRange(context, context.player().getLocation(), 8).contains(partner);
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (!bondValid(context)) {
            endBond();
            sendAbilityMessage(context, player, "failure", "가까이 있는 아군과 먼저 서약을 맺으세요.");
            return;
        }
        if (!useAdvanced(context, player)) return;
        Player target = partner;
        endBond();
        restoreHealth(player, 4); restoreHealth(target, 4);
        effect(context, player, "ABSORPTION", "ABSORPTION", 4, 0);
        effect(context, target, "ABSORPTION", "ABSORPTION", 4, 0);
        feedback.cue(context, player, kr.newgodwar.ability.feedback.EffectCue.HEAL);
        feedback.cue(context, target, kr.newgodwar.ability.feedback.EffectCue.HEAL);
        feedback.affected(context, target, "서약 완성 · 회복과 흡수", false);
    }

    private void endBond() { cancelScheduledTask(bondTask); bondTask = -1; partner = null; }

    @Override
    public List<String> activeTimerLines() {
        List<String> lines = super.activeTimerLines();
        if (partner != null) lines.add("수호의 서약 · " + partner.getName());
        return lines;
    }

    @Override
    protected void clearTransientState() { partner = null; bondTask = -1; }

    private void oath(AbilityPlayerContext context, Player first, Player second) {
        Location from = first.getLocation().add(0, 1, 0), to = second.getLocation().add(0, 1, 0);
        if (!from.getWorld().equals(to.getWorld())) return;
        List<Player> audience = feedback.hidden(first) || feedback.hidden(second) ? Collections.singletonList(first) : feedback.targetViewers(context, second, to);
        feedback.segment(context, from, to, AbilityTheme.GUARD, audience);
    }
}
