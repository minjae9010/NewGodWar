package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import kr.newgodwar.ability.feedback.AbilityStyle;
import kr.newgodwar.ability.feedback.AbilityTheme;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.List;

@AbilityInfo(
    id = "queenbee", name = "여왕벌",
    description = "일벌에게 표적을 지시하고 벌집의 꿀로 아군을 돌봅니다.",
    normalSkill = "블레이즈 막대기 좌클릭: 바라보는 20블록 안의 적에게 벌떼를 보냅니다. 3초 동안 3회 쏘아 각각 피해 2를 줍니다.",
    normalStoneCost = 12, normalCooldownSeconds = 30,
    advancedSkill = "블레이즈 막대기 우클릭: 발밑에 반경 4블록 벌집 영역을 4초간 만듭니다. 안에 있는 아군의 체력 1과 허기 2를 매초 회복합니다.",
    advancedStoneCost = 20, advancedCooldownSeconds = 65,
    passiveSkill = "벌떼는 움직이는 표적을 추적하지만 벽 뒤나 20블록 밖으로 벗어나면 흩어집니다.",
    grade = AbilityGrade.A
)
final class QueenBeeAbility extends TransientAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.SWARM)
        .dedicated()
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    private boolean swarming, hive;
    private int swarmTask = -1;

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (swarming) return;
        final Player target = targetPlayerInSight(context, player, 20, false);
        if (target == null || !useNormal(context, player)) return;
        swarming = true;
        feedback.affected(context, target, "벌떼가 추적합니다 · 엄폐하거나 거리를 벌리세요", true);
        final int[] phase = {0};
        swarmTask = scheduleRepeating(context, () -> {
            if (!validEnemy(context, target, 20) || !player.hasLineOfSight(target)) { stopSwarm(); return; }
            feedback.flock(context, target, phase[0], true);
            if (++phase[0] % 4 == 0) damage(context, target, 2, player);
            if (phase[0] >= 12) stopSwarm();
        }, 5, 5);
    }

    private void stopSwarm() { cancelScheduledTask(swarmTask); swarmTask = -1; swarming = false; }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (hive || !useAdvanced(context, player)) return;
        hive = true;
        Location center = player.getLocation();
        for (int i = 0; i < 4; i++) scheduleLater(context, () -> {
            if (!active(context) || !player.getWorld().equals(center.getWorld())) return;
            honeycomb(context, center);
            for (Player target : alliesInRange(context, center, 4)) {
                restoreHealth(target, 1);
                target.setFoodLevel(Math.min(20, target.getFoodLevel() + 2));
            }
        }, i * 20L);
        scheduleLater(context, () -> hive = false, 80);
    }

    @Override
    protected void clearTransientState() { swarming = false; hive = false; swarmTask = -1; }

    private void honeycomb(AbilityPlayerContext context, Location center) {
        if (center == null || center.getWorld() == null) return;
        List<Player> audience = feedback.effectViewers(context, center);
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3, b = (i + 1) * Math.PI / 3;
            feedback.segment(context, center.clone().add(Math.cos(a) * 4, 0.2D, Math.sin(a) * 4),
                center.clone().add(Math.cos(b) * 4, 0.2D, Math.sin(b) * 4), AbilityTheme.SWARM, audience);
        }
        feedback.particle(context, center.clone().add(0, 0.5D, 0), AbilityTheme.HEALING.particle(), audience, 5, 0.4D);
    }
}
