package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

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
    private boolean harvesting;

    @Override
    public void onAssign(AbilityPlayerContext context) { context.player().setFoodLevel(20); }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            give(player, Material.BREAD, 10);
            feedback.harvest(context, player.getLocation(), 2);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (harvesting || !useAdvanced(context, player)) return;
        harvesting = true;
        Location center = player.getLocation();
        feedback.harvest(context, center, 0);
        for (int i = 1; i <= 3; i++) {
            final int stage = i;
            scheduleLater(context, () -> {
                if (stage == 3) harvesting = false;
                if (!active(context) || !player.getWorld().equals(center.getWorld())) return;
                feedback.harvest(context, center, stage);
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
}
