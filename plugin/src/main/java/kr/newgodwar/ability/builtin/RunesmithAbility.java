package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import kr.newgodwar.ability.feedback.AbilityStyle;
import kr.newgodwar.ability.feedback.AbilityTheme;
import kr.newgodwar.ability.feedback.ObjectModel;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static kr.newgodwar.ability.feedback.ModelParts.*;

@AbilityInfo(
    id = "runesmith", name = "룬 세공사",
    description = "화염과 서리 룬을 배치하고 원하는 순간 함께 기폭합니다.",
    normalSkill = "블레이즈 막대기 좌클릭: 발밑에 20초 동안 유지되는 룬을 설치합니다(최대 3개). 웅크린 채 좌클릭하면 비용 없이 화염/서리 모드를 바꿉니다.",
    normalStoneCost = 6, normalCooldownSeconds = 7,
    advancedSkill = "블레이즈 막대기 우클릭: 같은 월드의 24블록 안에 있는 룬을 0.5초 후 기폭합니다. 각 룬은 반경 3블록에 화염 피해 4 또는 서리 피해 2와 감속 3초를 줍니다. 중첩 피해는 최대 8입니다.",
    advancedStoneCost = 16, advancedCooldownSeconds = 40,
    passiveSkill = "룬은 지형을 바꾸지 않으며 모드를 바꾸어도 이미 설치한 룬의 속성은 유지됩니다.",
    grade = AbilityGrade.B
)
final class RunesmithAbility extends TransientAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.RUNE)
        .dedicated()
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    private final List<Rune> runes = new ArrayList<Rune>();
    private boolean frost;
    private boolean detonating;

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (player.isSneaking()) {
            if (!feedback.allow("rune-mode", 250L)) return;
            frost = !frost;
            feedback.passive(context, "다음 룬: " + (frost ? "서리" : "화염"));
            return;
        }
        if (runes.size() >= 3) {
            sendAbilityMessage(context, player, "failure", ChatColor.YELLOW + "룬은 최대 3개입니다. 우클릭으로 기폭하세요.");
            return;
        }
        if (!useNormal(context, player)) return;
        Rune rune = new Rune(player.getLocation(), frost);
        runes.add(rune);
        draw(context, rune);
        rune.task = scheduleRepeating(context, () -> {
            if (!active(context) || ++rune.age >= 20) {
                runes.remove(rune);
                cancelScheduledTask(rune.task);
                return;
            }
            if (player.getWorld().equals(rune.center.getWorld())) draw(context, rune);
        }, 20L, 20L);
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (detonating) return;
        List<Rune> selected = new ArrayList<Rune>();
        for (Rune rune : runes) {
            if (player.getWorld().equals(rune.center.getWorld())
                && player.getLocation().distanceSquared(rune.center) <= 24 * 24) selected.add(rune);
        }
        if (selected.isEmpty()) {
            sendAbilityMessage(context, player, "failure", ChatColor.YELLOW + "24블록 안에 설치된 룬이 없습니다.");
            return;
        }
        if (!useAdvanced(context, player)) return;
        detonating = true;
        runes.removeAll(selected);
        for (Rune rune : selected) {
            cancelScheduledTask(rune.task);
            rune(context, rune.center, 3, rune.frost);
        }
        scheduleLater(context, () -> detonate(context, selected), 10L);
    }

    private void detonate(AbilityPlayerContext context, List<Rune> selected) {
        detonating = false;
        if (!active(context)) return;
        Map<Player, Double> damage = new LinkedHashMap<Player, Double>();
        List<Player> chilled = new ArrayList<Player>();
        for (Rune rune : selected) {
            if (!context.player().getWorld().equals(rune.center.getWorld())) continue;
            runeBurst(context, rune.center, rune.frost);
            for (Player target : enemies(context, rune.center, 3)) {
                double previous = damage.containsKey(target) ? damage.get(target) : 0;
                damage.put(target, Math.min(8, previous + (rune.frost ? 2 : 4)));
                if (rune.frost && !chilled.contains(target)) chilled.add(target);
            }
        }
        for (Map.Entry<Player, Double> hit : damage.entrySet()) {
            Player target = hit.getKey();
            damage(context, target, hit.getValue(), context.player());
            if (!target.isDead() && canAffectEnemy(context, context.player(), target) && chilled.contains(target)) {
                effect(context, target, "SLOWNESS", "SLOW", 3, 1);
            }
        }
    }

    private void draw(AbilityPlayerContext context, Rune rune) {
        rune(context, rune.center, 0.8D, rune.frost);
    }

    @Override
    public List<String> activeTimerLines() {
        List<String> lines = super.activeTimerLines();
        lines.add(ChatColor.GOLD + "룬 " + runes.size() + "/3 · " + (frost ? "서리" : "화염"));
        return lines;
    }

    @Override
    protected void clearTransientState() { runes.clear(); frost = false; detonating = false; }

    private static final class Rune {
        private final Location center;
        private final boolean frost;
        private int task = -1;
        private int age;

        private Rune(Location center, boolean frost) { this.center = center.clone(); this.frost = frost; }
    }

    static final ObjectModel FIRE_RUNE = runeModel(false);
    static final ObjectModel FROST_RUNE = runeModel(true);

    private static ObjectModel runeModel(boolean frost) {
        return ObjectModel.animated((phase, detail) -> {
            List<ObjectModel.Part> out = new ArrayList<ObjectModel.Part>();
            double radius = Math.max(0.5, Math.min(3, detail));
            int arms = frost ? 6 : 3;
            String material = frost ? "SEA_LANTERN" : "GLOWSTONE";
            for (int i = 0; i < arms; i++) {
                double a = Math.PI * 2 * i / arms, b = Math.PI * 2 * (i + 1) / arms;
                bar(out, material, Math.cos(a) * radius, Math.sin(a) * radius,
                    Math.cos(b) * radius, Math.sin(b) * radius);
                bar(out, material, 0, 0, Math.cos(a) * radius * 0.75, Math.sin(a) * radius * 0.75);
            }
            out.add(new ObjectModel.Part(frost ? "PRISMARINE_CRYSTALS" : "BLAZE_POWDER", true,
                0, 0.35 + Math.sin(phase * 0.15) * 0.06, 0, 0.5, 0.5, 0.5, 0, phase * 0.03));
            return out;
        });
    }

    private void rune(AbilityPlayerContext context, Location center, double radius, boolean frost) {
        if (center == null || center.getWorld() == null || !feedback.visuals(context)) return;
        if (feedback.object(context, "rune:" + feedback.positionKey(center), frost ? FROST_RUNE : FIRE_RUNE, center, 22, radius)) return;
        AbilityTheme theme = frost ? AbilityTheme.FROST : AbilityTheme.FIRE;
        List<Player> audience = feedback.effectViewers(context, center);
        double size = Math.max(0.5D, Math.min(3, radius));
        feedback.ring(context, center, size, theme, audience);
        int arms = frost ? 6 : 3;
        for (int arm = 0; arm < arms; arm++) {
            double angle = Math.PI * 2.0D * arm / arms;
            for (int i = 1; i <= 5; i++) {
                double length = size * i / 5.0D;
                feedback.particle(context, center.clone().add(Math.cos(angle) * length, 0.2D, Math.sin(angle) * length),
                    theme.particle(), audience, 1, 0);
            }
        }
    }

    private void runeBurst(AbilityPlayerContext context, Location center, boolean frost) {
        if (center == null || center.getWorld() == null) return;
        feedback.removeObject("rune:" + feedback.positionKey(center));
        AbilityTheme theme = frost ? AbilityTheme.FROST : AbilityTheme.FIRE;
        List<Player> audience = feedback.effectViewers(context, center);
        for (int i = 0; i < 16; i++) {
            double angle = i * Math.PI / 8;
            feedback.particle(context, center.clone().add(Math.cos(angle) * 2.0D, 0.3D + (i % 3) * 0.3D,
                Math.sin(angle) * 2.0D), theme.particle(), audience, 1, 0.05D);
        }
        if (feedback.allow("rune-sound", 150L)) feedback.sound(context, center, audience, theme.sound(), 0.6F, frost ? 1.4F : 0.8F);
    }
}
