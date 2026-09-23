package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import kr.newgodwar.ability.feedback.AbilityStyle;
import kr.newgodwar.ability.feedback.AbilityTheme;
import kr.newgodwar.ability.feedback.EffectCue;
import kr.newgodwar.game.GodTeam;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;

import java.util.List;

@AbilityInfo(
    id = "nike", name = "니케",
    description = "승리의 날개로 진입하고 처치로 얻은 월계관을 아군의 승전 축복으로 바꿉니다.",
    normalSkill = "블레이즈 막대기 좌클릭: 바라보는 방향으로 날개 돌진합니다. 6초 안의 첫 낙하 피해를 무시합니다.",
    normalStoneCost = 10, normalCooldownSeconds = 25,
    advancedSkill = "블레이즈 막대기 우클릭: 월계관을 모두 소비해 반경 8블록 아군을 월계관 수 + 2만큼 회복하고 4 + 월계관 수 × 2초간 신속 I을 부여합니다.",
    advancedStoneCost = 18, advancedCooldownSeconds = 60,
    passiveSkill = "적을 직접 처치하면 월계관을 1개 얻습니다. 최대 3개이며 사망하거나 능력을 잃으면 사라집니다.",
    grade = AbilityGrade.B
)
final class NikeAbility extends TransientAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.WIND)
        .dedicated()
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    private int laurels;
    private boolean wingGuard;
    private int wingTask = -1;

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (!useNormal(context, player)) return;
        Vector direction = player.getLocation().getDirection().setY(0);
        if (direction.lengthSquared() < 0.01D) direction.setZ(1);
        player.setVelocity(direction.normalize().multiply(1.1D).setY(0.35D));
        wingGuard = true;
        cancelScheduledTask(wingTask);
        wingTask = scheduleLater(context, () -> { wingGuard = false; wingTask = -1; }, 120);
        wings(context, player.getLocation(), laurels);
    }

    @Override
    public void onGenericDamage(AbilityPlayerContext context, EntityDamageEvent event) {
        if (wingGuard && !event.isCancelled() && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
            wingGuard = false; cancelScheduledTask(wingTask); wingTask = -1;
        }
    }

    @Override
    public void onKill(AbilityKillContext context) {
        AbilityPlayerContext playerContext = new AbilityPlayerContext(context.plugin(), context.killer(), context.ability());
        GodTeam victimTeam = context.plugin().game().teamOf(context.victim());
        if (!active(playerContext) || victimTeam == null || victimTeam == context.plugin().game().teamOf(context.killer())) return;
        laurels = Math.min(3, laurels + 1);
        feedback.passive(playerContext, "승리의 월계관 · " + laurels + "/3");
        wings(playerContext, context.killer().getLocation(), laurels);
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (laurels == 0) {
            sendAbilityMessage(context, player, "failure", "적 처치로 승리의 월계관을 얻으세요.");
            return;
        }
        if (!useAdvanced(context, player)) return;
        int victory = laurels;
        laurels = 0;
        for (Player target : alliesInRange(context, player.getLocation(), 8)) {
            restoreHealth(target, 2 + victory);
            effect(context, target, "SPEED", "SPEED", 4 + victory * 2, 0);
            feedback.cue(context, target, kr.newgodwar.ability.feedback.EffectCue.HEAL);
        }
    }

    @Override
    public List<String> activeTimerLines() {
        List<String> lines = super.activeTimerLines();
        lines.add("승리의 월계관 · " + laurels + "/3");
        return lines;
    }

    @Override
    protected void clearTransientState() { laurels = 0; wingGuard = false; wingTask = -1; }

    private void wings(AbilityPlayerContext context, Location center, int laurels) {
        if (center == null || center.getWorld() == null) return;
        List<Player> audience = feedback.effectViewers(context, center);
        feedback.drawCue(context, center, EffectCue.WINGS, audience);
        for (int i = 0; i < Math.min(3, laurels); i++)
            feedback.particle(context, center.clone().add((i - 1) * 0.3D, 2.4D, 0), AbilityTheme.NATURE.particle(), audience, 2, 0);
    }
}
