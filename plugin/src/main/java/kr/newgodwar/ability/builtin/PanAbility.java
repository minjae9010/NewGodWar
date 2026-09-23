package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import kr.newgodwar.ability.feedback.AbilityStyle;
import kr.newgodwar.ability.feedback.AbilityTheme;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.List;

@AbilityInfo(
    id = "pan", name = "판",
    description = "제자리에 서서 피리를 불어 퍼져 나가는 선율로 적을 겁주고 아군을 춤추게 합니다.",
    normalSkill = "블레이즈 막대기 좌클릭: 2초간 공포의 피리를 연주합니다. 반경 3·5·7블록으로 퍼지는 세 선율이 적을 감속시키고 마지막 선율이 밀쳐냅니다.",
    normalStoneCost = 12, normalCooldownSeconds = 35,
    advancedSkill = "블레이즈 막대기 우클릭: 2초간 목동의 피리를 연주합니다. 세 선율이 아군에게 4초간 신속과 점프 강화를 주고 끝까지 연주하면 체력 2를 회복합니다.",
    advancedStoneCost = 18, advancedCooldownSeconds = 65,
    passiveSkill = "연주 시작점에서 1블록 넘게 이동하면 연주가 중단됩니다. 염소의 다리로 낙하 피해를 30% 줄입니다.",
    grade = AbilityGrade.B
)
final class PanAbility extends TransientAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.MUSIC)
        .dedicated()
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    private Location stage;
    private int songTask = -1;

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) { play(context, player, false); }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) { play(context, player, true); }

    private void play(AbilityPlayerContext context, Player player, boolean joyful) {
        if (stage != null) return;
        if (!(joyful ? useAdvanced(context, player) : useNormal(context, player))) return;
        stage = player.getLocation();
        final int[] note = {0};
        sing(context, joyful, note[0]++);
        songTask = scheduleRepeating(context, () -> {
            if (!active(context) || moved(player.getLocation())) { stopSong(); return; }
            sing(context, joyful, note[0]++);
            if (note[0] >= 3) stopSong();
        }, 20, 20);
    }

    private void sing(AbilityPlayerContext context, boolean joyful, int note) {
        double radius = 3 + note * 2;
        melody(context, stage, radius, note, joyful);
        if (joyful) {
            for (Player target : alliesInRange(context, stage, radius)) {
                effect(context, target, "SPEED", "SPEED", 4, 0);
                effect(context, target, "JUMP_BOOST", "JUMP", 4, 0);
                if (note == 2) restoreHealth(target, 2);
            }
        } else {
            for (Player target : enemies(context, stage, radius)) {
                effect(context, target, "SLOWNESS", "SLOW", 2, 1);
                if (note == 2) repel(target, stage, 0.85D);
            }
        }
    }

    private boolean moved(Location position) {
        return stage == null || !stage.getWorld().equals(position.getWorld()) || stage.distanceSquared(position) > 1;
    }

    @Override
    public void onMove(AbilityPlayerContext context, PlayerMoveEvent event) {
        if (stage != null && !event.isCancelled() && event.getTo() != null && moved(event.getTo())) {
            stopSong();
            feedback.timer(context, "이동으로 피리 연주가 중단되었습니다.");
        }
    }

    private void stopSong() { cancelScheduledTask(songTask); songTask = -1; stage = null; }

    @Override
    public void onGenericDamage(AbilityPlayerContext context, EntityDamageEvent event) {
        if (!event.isCancelled() && event.getCause() == EntityDamageEvent.DamageCause.FALL) event.setDamage(event.getDamage() * 0.7D);
    }

    @Override
    protected void clearTransientState() { stage = null; songTask = -1; }

    private void melody(AbilityPlayerContext context, Location center, double radius, int note, boolean joyful) {
        if (center == null || center.getWorld() == null) return;
        List<Player> audience = feedback.effectViewers(context, center);
        double size = Math.max(0.5D, Math.min(8, radius));
        for (int i = 0; i < 6; i++) {
            double angle = i * Math.PI / 3 + note * 0.3D;
            feedback.particle(context, center.clone().add(Math.cos(angle) * size, 1.5D + Math.sin(angle * 2) * 0.25D,
                Math.sin(angle) * size), AbilityTheme.MUSIC.particle(), audience, 1, 0);
        }
        feedback.sound(context, center, audience, AbilityTheme.MUSIC.sound(), 0.6F, (joyful ? 1.0F : 0.5F) + note * 0.2F);
    }
}
