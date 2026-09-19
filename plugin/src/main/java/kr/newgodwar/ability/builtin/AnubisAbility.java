package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import java.util.List;

@AbilityInfo(
    id = "anubis", name = "아누비스",
    description = "적의 심장을 저울에 올리고 자신이 받은 피해의 무게만큼 심판합니다.",
    normalSkill = "블레이즈 막대기 좌클릭: 바라보는 20블록 안의 적을 8초간 심판 대상으로 지정합니다. 그 적에게 받은 피해를 최대 4까지 저울에 기록합니다.",
    normalStoneCost = 10, normalCooldownSeconds = 35,
    advancedSkill = "블레이즈 막대기 우클릭: 24블록 안에서 보이는 심판 대상에게 피해 4 + 저울 무게를 주고, 실제로 깎은 체력만큼 최대 3 회복합니다.",
    advancedStoneCost = 22, advancedCooldownSeconds = 70,
    passiveSkill = "아누비스를 공격한 심판 대상일수록 더 무거운 대가를 치릅니다. 막힌 피해로는 체력을 흡수하지 못합니다.",
    grade = AbilityGrade.A
)
final class AnubisAbility extends TransientAbility {
    private Player judged;
    private double burden;
    private int scaleTask = -1;

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (judged != null) return;
        Player target = targetPlayerInSight(context, player, 20, false);
        if (target == null || !useNormal(context, player)) return;
        judged = target; burden = 0;
        feedback.affected(context, target, "심장의 저울 · 아누비스에게 준 피해가 심판에 더해집니다", true);
        final int[] remaining = {16};
        scaleTask = scheduleRepeating(context, () -> {
            if (!validEnemy(context, judged, 24) || remaining[0]-- <= 0) { endJudgment(); return; }
            feedback.scales(context, judged, burden);
        }, 1, 10);
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (!attacker && opponent.equals(judged) && validEnemy(context, opponent, 24) && !event.isCancelled())
            burden = Math.min(4, burden + Math.max(0, event.getFinalDamage()));
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (!validEnemy(context, judged, 24) || !player.hasLineOfSight(judged)) {
            sendAbilityMessage(context, player, "failure", "시야 안에 심판 대상이 필요합니다.");
            return;
        }
        if (!useAdvanced(context, player)) return;
        Player target = judged;
        double amount = 4 + burden;
        feedback.scales(context, target, burden);
        endJudgment();
        double before = target.getHealth();
        damage(context, target, amount, player);
        if (active(context)) restoreHealth(player, Math.min(3, Math.max(0, before - target.getHealth())));
    }

    private void endJudgment() { cancelScheduledTask(scaleTask); scaleTask = -1; judged = null; burden = 0; }

    @Override
    public List<String> activeTimerLines() {
        List<String> lines = super.activeTimerLines();
        if (judged != null) lines.add("심장의 저울 · " + judged.getName() + " · " + (int) Math.ceil(burden) + "/4");
        return lines;
    }

    @Override
    protected void clearTransientState() { judged = null; burden = 0; scaleTask = -1; }
}
