package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import java.util.List;

@AbilityInfo(
    id = "chronos", name = "크로노스",
    description = "자신의 시간을 기록해 되감고 전장에 느리게 흐르는 시간대를 만듭니다.",
    normalSkill = "블레이즈 막대기 좌클릭: 안전한 발밑에 5초간 시간 좌표를 기록합니다. 다시 좌클릭하면 돌아가며 기록 후 잃은 체력을 최대 4 회복합니다.",
    normalStoneCost = 14, normalCooldownSeconds = 45,
    advancedSkill = "블레이즈 막대기 우클릭: 현재 위치에 반경 6블록 시간장을 3초 동안 만듭니다. 안에 있는 적의 이동과 채굴을 늦춥니다.",
    advancedStoneCost = 26, advancedCooldownSeconds = 90,
    passiveSkill = "되감기는 재료와 재사용 시간을 되돌리지 않으며, 위험해진 좌표로는 돌아갈 수 없습니다.",
    grade = AbilityGrade.S
)
final class ChronosAbility extends TransientAbility {
    private Location anchor;
    private double recordedHealth;
    private int anchorTask = -1;
    private boolean fieldActive;

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (anchor != null) {
            if (!active(context) || !player.getWorld().equals(anchor.getWorld()) || !safe(anchor)) {
                sendAbilityMessage(context, player, "failure", "시간 좌표가 다른 월드에 있거나 안전하지 않습니다.");
                return;
            }
            Location destination = anchor.clone();
            if (!player.teleport(destination)) return;
            restoreHealth(player, Math.min(4, Math.max(0, recordedHealth - player.getHealth())));
            player.setFallDistance(0);
            cancelScheduledTask(anchorTask);
            anchorTask = -1;
            anchor = null;
            feedback.clockFace(context, destination, 1.2D, 0);
            feedback.timer(context, "시간 되감기 완료");
            return;
        }
        Location location = player.getLocation();
        if (!safe(location)) {
            sendAbilityMessage(context, player, "failure", "안전한 바닥 위에서 시간을 기록하세요.");
            return;
        }
        if (!useNormal(context, player)) return;
        anchor = location;
        recordedHealth = player.getHealth();
        feedback.clockFace(context, anchor, 1.2D, 0);
        anchorTask = scheduleLater(context, () -> {
            anchor = null; anchorTask = -1;
            feedback.timer(context, "시간 좌표 소멸");
        }, 100);
    }

    private boolean safe(Location location) {
        // Check the player's width and head height, including blocks placed beside an old anchor.
        for (double x : new double[] {-0.3D, 0.3D}) for (double z : new double[] {-0.3D, 0.3D})
            for (double y : new double[] {0, 0.9D, 1.8D}) {
                Material type = location.clone().add(x, y, z).getBlock().getType();
                if (!type.name().endsWith("AIR")) return false;
            }
        Material floor = location.clone().add(0, -1, 0).getBlock().getType();
        String name = floor.name();
        return floor.isSolid() && !name.contains("MAGMA") && !name.contains("CACTUS")
            && !name.contains("CAMPFIRE");
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (fieldActive || !useAdvanced(context, player)) return;
        fieldActive = true;
        Location center = player.getLocation();
        for (int i = 0; i < 6; i++) {
            final int phase = i;
            scheduleLater(context, () -> {
                if (!active(context) || !player.getWorld().equals(center.getWorld())) return;
                feedback.clockFace(context, center, 6, phase);
                for (Player target : enemies(context, center, 6)) {
                    effectTicks(target, "SLOWNESS", "SLOW", 12, 3);
                    effectTicks(target, "MINING_FATIGUE", "SLOW_DIGGING", 12, 2);
                }
            }, i * 10L);
        }
        scheduleLater(context, () -> fieldActive = false, 60);
    }

    @Override
    public List<String> activeTimerLines() {
        List<String> lines = super.activeTimerLines();
        if (anchor != null) lines.add("시간 좌표 · 좌클릭으로 되감기");
        return lines;
    }

    @Override
    protected void clearTransientState() { anchor = null; anchorTask = -1; fieldActive = false; recordedHealth = 0; }
}
