package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;
import java.util.List;

@AbilityInfo(
    id = "athena", name = "아테나",
    description = "아이기스로 공격을 받아내 반격하고 팔랑크스 진형으로 아군을 보호합니다.",
    normalSkill = "블레이즈 막대기 좌클릭: 4초 안에 받는 첫 근접 공격의 피해를 60% 줄입니다. 성공하면 6초 안의 다음 근접 공격에 피해 3을 더합니다.",
    normalStoneCost = 10, normalCooldownSeconds = 25,
    advancedSkill = "블레이즈 막대기 우클릭: 현재 위치와 방향을 기준으로 전방 반경 5블록에 6초간 팔랑크스를 세워 진형 안 아군에게 저항 II를 부여합니다.",
    advancedStoneCost = 24, advancedCooldownSeconds = 75,
    passiveSkill = "반격은 방어 성공 후 한 번만 사용할 수 있습니다. 팔랑크스는 시전 시의 위치와 방향을 유지합니다.",
    grade = AbilityGrade.A
)
final class AthenaAbility extends TransientAbility {
    private boolean guarding, riposte, formation;
    private int combatTask = -1;

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (guarding || riposte || !useNormal(context, player)) return;
        guarding = true;
        feedback.shield(context, player.getLocation(), 1);
        combatTask = scheduleLater(context, () -> { guarding = false; combatTask = -1; }, 80);
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (event.isCancelled() || event.getDamage() <= 0) return;
        if (!attacker && guarding && event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK
            && event.getDamager().equals(opponent) && validEnemy(context, opponent, 5)) {
            guarding = false; riposte = true;
            event.setDamage(event.getDamage() * 0.4D);
            cancelScheduledTask(combatTask);
            combatTask = scheduleLater(context, () -> { riposte = false; combatTask = -1; }, 120);
            feedback.shield(context, context.player().getLocation(), 1.2D);
            feedback.passive(context, "아이기스 방어 성공 · 다음 공격으로 반격");
        } else if (riposte && directAttack(context, event, opponent, attacker)) {
            riposte = false; cancelScheduledTask(combatTask); combatTask = -1;
            event.setDamage(event.getDamage() + 3);
            feedback.spear(context, context.player().getEyeLocation(), opponent.getEyeLocation());
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (formation || !useAdvanced(context, player)) return;
        formation = true;
        Location center = player.getLocation();
        Vector facing = center.getDirection().setY(0);
        if (facing.lengthSquared() < 0.01D) facing.setZ(1);
        facing.normalize();
        for (int i = 0; i < 6; i++) scheduleLater(context, () -> {
            if (!active(context) || !player.getWorld().equals(center.getWorld())) return;
            feedback.phalanx(context, center, facing);
            for (Player target : alliesInRange(context, center, 5)) {
                Vector offset = target.getLocation().toVector().subtract(center.toVector()).setY(0);
                if (offset.dot(facing) >= -0.25D) effectTicks(target, "RESISTANCE", "DAMAGE_RESISTANCE", 22, 1);
            }
        }, i * 20L);
        scheduleLater(context, () -> formation = false, 120);
    }

    @Override
    public List<String> activeTimerLines() {
        List<String> lines = super.activeTimerLines();
        if (guarding) lines.add("아이기스 · 첫 근접 공격 방어");
        if (riposte) lines.add("아이기스 · 반격 준비");
        return lines;
    }

    @Override
    protected void clearTransientState() { guarding = false; riposte = false; formation = false; combatTask = -1; }
}
