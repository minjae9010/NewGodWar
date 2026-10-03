package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import kr.newgodwar.ability.feedback.AbilityStyle;
import kr.newgodwar.ability.feedback.AbilityTheme;
import kr.newgodwar.ability.feedback.ObjectModel;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

import static kr.newgodwar.ability.feedback.ModelParts.*;

@AbilityInfo(
    id = "graviton", name = "중력술사",
    description = "공간에 중력핵을 만들고 인력과 반발력으로 적의 위치를 조절합니다.",
    normalSkill = "블레이즈 막대기 좌클릭: 16블록 안의 바라보는 위치에 반경 5블록 중력핵을 4초 동안 만듭니다. 0.5초마다 공중의 적까지 중심으로 당깁니다.",
    normalStoneCost = 14, normalCooldownSeconds = 35,
    advancedSkill = "블레이즈 막대기 우클릭: 현재 위치에 1초의 예고 후 반경 6블록 반발장을 폭발시켜 피해 5를 주고 적을 밀쳐냅니다.",
    advancedStoneCost = 26, advancedCooldownSeconds = 80,
    passiveSkill = "중력장은 지형을 바꾸지 않으며 범위 밖으로 벗어나면 영향을 받지 않습니다.",
    grade = AbilityGrade.A
)
final class GravitonAbility extends TransientAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.GRAVITY)
        .dedicated()
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    private boolean wellActive;
    private boolean repulsionPending;

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (wellActive) {
            sendAbilityMessage(context, player, "failure", ChatColor.YELLOW + "이미 중력장이 유지 중입니다.");
            return;
        }
        Location center = targetLocation(player, 16).add(0.5D, 1.0D, 0.5D);
        if (!useNormal(context, player)) return;
        wellActive = true;
        for (int i = 0; i < 8; i++) {
            final int phase = i;
            scheduleLater(context, () -> {
                if (!active(context) || !player.getWorld().equals(center.getWorld())) return;
                gravityWell(context, center, 5, phase);
                for (Player target : enemies(context, center, 5)) {
                    Vector pull = center.toVector().subtract(target.getLocation().toVector());
                    if (pull.lengthSquared() < 0.25D) continue;
                    target.setVelocity(pull.normalize().multiply(0.35D));
                    feedback.affected(context, target, "중력장 · 중심으로 끌려갑니다", true);
                }
            }, i * 10L);
        }
        scheduleLater(context, () -> wellActive = false, 80L);
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (repulsionPending) return;
        if (!useAdvanced(context, player)) return;
        repulsionPending = true;
        Location center = player.getLocation();
        feedback.pulse(context, center, 6);
        scheduleLater(context, () -> {
            repulsionPending = false;
            if (!active(context) || !player.getWorld().equals(center.getWorld())) return;
            feedback.pulse(context, center, 6);
            for (Player target : enemies(context, center, 6)) {
                damage(context, target, 5, player);
                if (active(context) && !target.isDead() && center.getWorld().equals(target.getWorld())
                    && canAffectEnemy(context, player, target)) {
                    Vector outward = target.getLocation().toVector().subtract(center.toVector());
                    if (outward.lengthSquared() < 0.01D) outward.setY(1);
                    target.setVelocity(outward.normalize().multiply(1.1D));
                }
            }
        }, 20L);
    }

    @Override
    protected void clearTransientState() { wellActive = false; repulsionPending = false; }

    static final ObjectModel GRAVITY = ObjectModel.animated((phase, detail) -> {
        List<ObjectModel.Part> out = new ArrayList<ObjectModel.Part>();
        box(out, "CRYING_OBSIDIAN", 0, 0.65, 0, 0.42, 0.42, 0.42, phase * 0.08, phase * 0.06);
        for (int i = 0; i < 3; i++) {
            double a = phase * 0.1 + i * Math.PI * 2 / 3;
            box(out, "AMETHYST_BLOCK", Math.cos(a) * 0.85, 0.65 + Math.sin(a * 2) * 0.25,
                Math.sin(a) * 0.85, 0.16, 0.16, 0.16, a, a);
        }
        return out;
    });

    /** Three inward streams show the direction of the actual pull. */
    private void gravityWell(AbilityPlayerContext context, Location center, double radius, int phase) {
        if (center == null || center.getWorld() == null) return;
        if (feedback.object(context, "gravity:" + feedback.positionKey(center), GRAVITY, center, 12, radius)) return;
        List<Player> audience = feedback.effectViewers(context, center);
        double size = Math.max(0.5D, Math.min(6, radius));
        for (int arm = 0; arm < 3; arm++) for (int i = 0; i < 8; i++) {
            double fraction = (i + 1) / 8D;
            double angle = arm * Math.PI * 2 / 3 + phase * 0.45D + fraction * 1.5D;
            feedback.particle(context, center.clone().add(Math.cos(angle) * size * fraction,
                0.15D + (1 - fraction) * 0.65D, Math.sin(angle) * size * fraction),
                AbilityTheme.GRAVITY.particle(), audience, 1, 0);
        }
    }
}
