package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import kr.newgodwar.ability.feedback.AbilityStyle;
import kr.newgodwar.ability.feedback.AbilityTheme;
import kr.newgodwar.ability.feedback.EffectCue;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.List;

@AbilityInfo(
    id = "demeter", name = "데메테르",
    description = "곡식과 빵을 나누고 수확의 계절로 아군을 먹이고 회복시킵니다.",
    normalSkill = "블레이즈 막대기 좌클릭: 나눠 먹을 수 있는 빵 10개를 생성합니다.",
    normalStoneCost = 12, normalCooldownSeconds = 25,
    advancedSkill = "블레이즈 막대기 우클릭: 발밑에 반경 5블록 수확 영역을 6초간 만듭니다. 2초마다 영역 안 아군의 체력 2와 허기 4를 회복합니다.",
    advancedStoneCost = 22, advancedCooldownSeconds = 70,
    passiveSkill = "허기가 항상 20으로 유지됩니다. 수확 영역은 처음 2초간 자란 뒤 세 번 수확되며 실제 지형은 바꾸지 않습니다.",
    grade = AbilityGrade.A
)
final class DemeterAbility extends TransientAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.NATURE)
        .normal(EffectCue.ITEM)
        .dedicated()
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    private boolean harvesting;

    @Override
    public void onAssign(AbilityPlayerContext context) { context.player().setFoodLevel(20); }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            give(player, Material.BREAD, 10);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (harvesting || !useAdvanced(context, player)) return;
        harvesting = true;
        Location center = player.getLocation();
        harvest(context, center, 0);
        for (int i = 1; i <= 3; i++) {
            final int stage = i;
            scheduleLater(context, () -> {
                if (stage == 3) harvesting = false;
                if (!active(context) || !player.getWorld().equals(center.getWorld())) return;
                harvest(context, center, stage);
                for (Player target : alliesInRange(context, center, 5)) {
                    restoreHealth(target, 2);
                    target.setFoodLevel(Math.min(20, target.getFoodLevel() + 4));
                    feedback.affected(context, target, "풍성한 수확 · 체력과 허기 회복", false);
                }
            }, i * 40L);
        }
    }

    @Override
    public void onFoodLevelChange(AbilityPlayerContext context, FoodLevelChangeEvent event) {
        event.setCancelled(true); context.player().setFoodLevel(20);
    }

    @Override
    public void onRespawn(AbilityPlayerContext context, PlayerRespawnEvent event) { respawnFoodLevel(context, 20); }

    @Override
    protected void clearTransientState() { harvesting = false; }

    private void harvest(AbilityPlayerContext context, Location center, int stage) {
        if (center == null || center.getWorld() == null) return;
        List<Player> audience = feedback.effectViewers(context, center);
        double height = 0.2D + Math.min(3, Math.max(0, stage)) * 0.3D;
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4;
            Location stalk = center.clone().add(Math.cos(angle) * 3, 0.2D, Math.sin(angle) * 3);
            feedback.segment(context, stalk, stalk.clone().add(0, height, 0), AbilityTheme.NATURE, audience);
            feedback.particle(context, stalk.clone().add(0, height, 0), AbilityTheme.SWARM.particle(), audience, 3, 0.1D);
        }
        feedback.sound(context, center, audience, AbilityTheme.NATURE.sound(), 0.4F, 0.8F + stage * 0.2F);
    }
}
