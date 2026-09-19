package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import java.util.List;

@AbilityInfo(
    id = "hephaestus", name = "헤파이토스",
    description = "대장간의 열기로 무기를 달구고 남은 열기를 방어구에 담금질합니다.",
    normalSkill = "블레이즈 막대기 좌클릭: 8초 동안 검과 도끼의 다음 3회 근접 공격에 피해 2를 추가하는 화로 열기를 얻습니다.",
    normalStoneCost = 10, normalCooldownSeconds = 30,
    advancedSkill = "블레이즈 막대기 우클릭: 남은 열기를 모두 소비해 6초간 흡수를 얻습니다. 열기 1개마다 추가 체력 4를 얻습니다.",
    advancedStoneCost = 16, advancedCooldownSeconds = 60,
    passiveSkill = "화염 피해를 무시하지만 익사 피해를 2배로 받습니다. 열기는 공격 강화와 방어구 담금질 중 선택해 사용합니다.",
    grade = AbilityGrade.A
)
final class HephaestusAbility extends TransientAbility {
    private int heat;
    private int heatTask = -1;

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (heat > 0 || !useNormal(context, player)) return;
        heat = 3;
        feedback.forge(context, player.getLocation(), false);
        heatTask = scheduleLater(context, () -> { heat = 0; heatTask = -1; }, 160);
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (heat == 0) {
            sendAbilityMessage(context, player, "failure", "먼저 화로에서 열기를 얻으세요.");
            return;
        }
        if (!useAdvanced(context, player)) return;
        int strength = heat;
        heat = 0;
        cancelScheduledTask(heatTask); heatTask = -1;
        effect(player, "ABSORPTION", "ABSORPTION", 6, strength - 1);
        feedback.forge(context, player.getLocation(), true);
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (heat == 0 || !directAttack(context, event, opponent, attacker) || context.player().getItemInHand() == null) return;
        String weapon = context.player().getItemInHand().getType().name();
        if (!isSword(context.player().getItemInHand().getType()) && !weapon.endsWith("_AXE")) return;
        heat--;
        event.setDamage(event.getDamage() + 2);
        feedback.forge(context, opponent.getLocation(), false);
        if (heat == 0) { cancelScheduledTask(heatTask); heatTask = -1; }
    }

    @Override
    public void onGenericDamage(AbilityPlayerContext context, EntityDamageEvent event) {
        if (fire(event.getCause())) {
            event.setCancelled(true);
            context.player().setFireTicks(0);
        } else if (event.getCause() == EntityDamageEvent.DamageCause.DROWNING && !event.isCancelled()) {
            event.setDamage(event.getDamage() * 2);
        }
    }

    @Override
    public List<String> activeTimerLines() {
        List<String> lines = super.activeTimerLines();
        lines.add("화로 열기 · " + heat + "/3");
        return lines;
    }

    @Override
    protected void clearTransientState() { heat = 0; heatTask = -1; }
}
