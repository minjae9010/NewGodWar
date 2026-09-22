package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
            feedback.rune(context, rune.center, 3, rune.frost);
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
            feedback.runeBurst(context, rune.center, rune.frost);
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
        feedback.rune(context, rune.center, 0.8D, rune.frost);
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
}
