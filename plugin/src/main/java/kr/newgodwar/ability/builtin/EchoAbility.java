package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.List;

@AbilityInfo(
    id = "echo", name = "메아리 검사",
    description = "검을 휘두른 자리에 참격을 되풀이하고 세 번의 잔향으로 전장을 베어냅니다.",
    normalSkill = "블레이즈 막대기 좌클릭: 6초 안의 다음 검 직접 공격을 기록합니다. 0.75초 후 같은 적이 원래 타격 지점 2블록 안에 남아 있으면 피해의 절반(최대 4)을 반복합니다.",
    normalStoneCost = 10, normalCooldownSeconds = 25,
    advancedSkill = "블레이즈 막대기 우클릭: 최근 8초 안의 타격 지점에서 1초 후 참격을 0.5초 간격으로 3번 반복합니다. 24블록 안의 기록을 사용하며 반경 3블록 적에게 매번 피해 2를 줍니다.",
    advancedStoneCost = 22, advancedCooldownSeconds = 65,
    passiveSkill = "검 직접 공격이 마지막 타격 위치를 8초 동안 기록합니다. 잔향 범위를 벗어나면 후속 공격을 피할 수 있습니다.",
    grade = AbilityGrade.A
)
final class EchoAbility extends TransientAbility {
    private long armedUntil;
    private long recordedUntil;
    private Location recorded;
    private boolean replaying;

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) armedUntil = System.currentTimeMillis() + 6000L;
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (!attacker || replaying || event.isCancelled() || event.getDamage() <= 0
            || event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK
            || !event.getDamager().equals(context.player()) || !isSword(context.player().getItemInHand().getType())
            || !validEnemy(context, opponent, 5)) return;
        recorded = opponent.getLocation();
        recordedUntil = System.currentTimeMillis() + 8000L;
        if (System.currentTimeMillis() >= armedUntil) return;
        armedUntil = 0;
        double amount = Math.min(4, event.getDamage() * 0.5D);
        Location strike = recorded.clone();
        feedback.echoSlash(context, strike, 2);
        feedback.affected(context, opponent, "메아리 예고 · 0.75초 안에 거리를 벌리세요!", true);
        scheduleLater(context, () -> {
            if (!validEnemy(context, opponent, 24) || opponent.getLocation().distanceSquared(strike) > 4
                || event.isCancelled() || !context.player().hasLineOfSight(opponent)) return;
            echoDamage(context, opponent, amount);
            feedback.echoSlash(context, strike, 2);
        }, 15L);
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (recorded == null || System.currentTimeMillis() >= recordedUntil
            || !player.getWorld().equals(recorded.getWorld()) || player.getLocation().distanceSquared(recorded) > 24 * 24) {
            sendAbilityMessage(context, player, "failure", ChatColor.YELLOW + "검으로 적을 공격해 가까운 타격 지점을 먼저 기록하세요. 기록은 8초간 유지됩니다.");
            return;
        }
        if (!useAdvanced(context, player)) return;
        Location center = recorded.clone();
        recorded = null;
        recordedUntil = 0;
        feedback.pulse(context, center, 3);
        for (int i = 0; i < 3; i++) {
            scheduleLater(context, () -> {
                if (!active(context) || !player.getWorld().equals(center.getWorld())) return;
                feedback.echoSlash(context, center, 3);
                for (Player target : enemies(context, center, 3)) echoDamage(context, target, 2);
            }, 20L + i * 10L);
        }
    }

    private void echoDamage(AbilityPlayerContext context, Player target, double amount) {
        replaying = true;
        try { damage(context, target, amount, context.player()); }
        finally { replaying = false; }
    }

    @Override
    public List<String> activeTimerLines() {
        List<String> lines = super.activeTimerLines();
        long now = System.currentTimeMillis();
        if (armedUntil > now) lines.add(ChatColor.AQUA + "메아리 준비 " + ((armedUntil - now + 999) / 1000) + "초");
        if (recordedUntil > now) lines.add(ChatColor.AQUA + "타격 기록 " + ((recordedUntil - now + 999) / 1000) + "초");
        return lines;
    }

    @Override
    protected void clearTransientState() { armedUntil = 0; recordedUntil = 0; recorded = null; }
}
