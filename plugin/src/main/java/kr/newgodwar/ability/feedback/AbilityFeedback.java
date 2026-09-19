package kr.newgodwar.ability.feedback;

import kr.newgodwar.ability.api.AbilityDefinition;
import kr.newgodwar.ability.api.AbilityPlayerContext;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Cosmetic feedback owned by one ability session; never changes combat state. */
public final class AbilityFeedback {
    private static final double VIEW_DISTANCE_SQUARED = 32.0D * 32.0D;
    private final Map<String, Long> notices = new LinkedHashMap<String, Long>();

    public void clear() { notices.clear(); }

    public boolean allow(String key, long intervalMillis) {
        long now = System.currentTimeMillis();
        Long last = notices.get(key);
        if (last != null && now - last < intervalMillis) return false;
        if (notices.size() >= 128) notices.clear();
        notices.put(key, now);
        return true;
    }

    public static String label(AbilityDefinition ability, boolean advanced) {
        return ability.name() + " · " + (advanced ? "고급" : "일반");
    }

    public static String summary(AbilityDefinition ability, boolean advanced) {
        String skill = advanced ? ability.advancedSkill() : ability.normalSkill();
        if (skill == null || skill.isEmpty() || "없음".equals(skill)) return ability.description();
        int colon = skill.indexOf(':');
        return colon < 0 ? skill : skill.substring(colon + 1).trim();
    }

    public void activated(AbilityPlayerContext context, Player player, boolean advanced) {
        AbilityTheme theme = AbilityTheme.of(context.ability().id());
        String label = label(context.ability(), advanced);
        String summary = summary(context.ability(), advanced);
        chat(context, player, "success", theme.color() + "✦ [" + label + "] " + ChatColor.WHITE + summary);
        actionBar(context, player, theme.color() + "✦ " + label + " 발동 " + ChatColor.WHITE + shorten(summary));
        if (advanced && enabled(context, "titles")) {
            context.plugin().nms().sendTitle(player, theme.color() + context.ability().name(),
                ChatColor.GOLD + "고급 능력 발동", 2, 18, 8);
        }
        Location origin = player.getLocation();
        boolean privateCast = AbilityTheme.privateCast(context.ability().id()) || hidden(player);
        List<Player> viewers = privateCast ? Collections.singletonList(player) : viewers(origin, player);
        ring(context, origin, advanced ? 1.7D : 1.0D, theme, viewers);
        burst(context, origin.clone().add(0, 1, 0), theme, viewers, advanced ? 18 : 9);
        sound(context, origin, viewers, theme.sound(), advanced ? 0.8F : 0.5F, advanced ? 0.8F : 1.2F);
    }

    public void failure(AbilityPlayerContext context, Player player, String message) {
        actionBar(context, player, message);
        sound(context, player.getLocation(), Collections.singletonList(player),
            AbilityTheme.sound("BLOCK_NOTE_BLOCK_BASS", "BLOCK_NOTE_BASS"), 0.3F, 0.7F);
    }

    public void ready(AbilityPlayerContext context, boolean advanced) {
        String message = ChatColor.GREEN + "✓ " + label(context.ability(), advanced) + " 다시 사용 가능";
        chat(context, context.player(), "timer", message);
        actionBar(context, context.player(), message);
        sound(context, context.player().getLocation(), Collections.singletonList(context.player()),
            AbilityTheme.sound("ENTITY_EXPERIENCE_ORB_PICKUP"), 0.45F, advanced ? 1.6F : 1.3F);
    }

    public void timer(AbilityPlayerContext context, String text) {
        if (!allow("timer:" + text, 750L)) return;
        String message = AbilityTheme.of(context.ability().id()).color() + "[" + context.ability().name() + "] " + text;
        chat(context, context.player(), "timer", message);
        actionBar(context, context.player(), message);
    }

    public void affected(AbilityPlayerContext context, Player target, String effect, boolean harmful) {
        if (target == null || !target.isOnline() || !allow("target:" + target.getUniqueId(), 900L)) return;
        String message = (harmful ? ChatColor.RED : ChatColor.GREEN) + "[" + context.ability().name() + "] " + effect;
        actionBar(context, target, message);
        AbilityTheme theme = AbilityTheme.of(context.ability().id());
        Location center = target.getLocation().add(0, 1, 0);
        List<Player> viewers = hidden(target) ? Collections.singletonList(target) : viewers(center, target);
        burst(context, center, theme, viewers, 12);
        sound(context, center, Collections.singletonList(target), theme.sound(), 0.4F, harmful ? 0.8F : 1.4F);
    }

    public void passive(AbilityPlayerContext context, String text) {
        if (!allow("passive", 1500L)) return;
        actionBar(context, context.player(), AbilityTheme.of(context.ability().id()).color()
            + "◆ " + context.ability().name() + " · " + text);
        pulse(context, context.player().getLocation(), 0.8D);
    }

    public void pulse(AbilityPlayerContext context, Location center, double radius) {
        if (center == null || center.getWorld() == null) return;
        AbilityTheme theme = AbilityTheme.of(context.ability().id());
        List<Player> viewers = AbilityTheme.privateCast(context.ability().id()) || hidden(context.player())
            ? Collections.singletonList(context.player()) : viewers(center, context.player());
        ring(context, center, Math.max(0.5D, Math.min(12.0D, radius)), theme, viewers);
    }

    public void link(AbilityPlayerContext context, Location from, Location to) {
        if (from == null || to == null || from.getWorld() == null || !from.getWorld().equals(to.getWorld())) return;
        if (!enabled(context, "particles")) return;
        AbilityTheme theme = AbilityTheme.of(context.ability().id());
        List<Player> viewers = hidden(context.player()) || AbilityTheme.privateCast(context.ability().id())
            ? Collections.singletonList(context.player()) : viewers(to, context.player());
        int steps = Math.max(1, Math.min(24, (int) Math.ceil(from.distance(to) * 2.0D)));
        for (int i = 0; i <= steps; i++) {
            double fraction = i / (double) steps;
            Location point = from.clone().add((to.getX() - from.getX()) * fraction,
                (to.getY() - from.getY()) * fraction, (to.getZ() - from.getZ()) * fraction);
            particle(context, point, theme.particle(), viewers, 1, 0.02D);
        }
    }

    private void ring(AbilityPlayerContext context, Location center, double radius, AbilityTheme theme, List<Player> viewers) {
        if (!enabled(context, "particles")) return;
        int points = radius > 3.0D ? 32 : 18;
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0D * i / points;
            particle(context, center.clone().add(Math.cos(angle) * radius, 0.15D, Math.sin(angle) * radius),
                theme.particle(), viewers, 1, 0.0D);
        }
    }

    private void burst(AbilityPlayerContext context, Location center, AbilityTheme theme, List<Player> viewers, int count) {
        particle(context, center, theme.particle(), viewers, count, 0.45D);
    }

    private void particle(AbilityPlayerContext context, Location location, Particle particle, List<Player> viewers, int count, double spread) {
        if (particle == null || !enabled(context, "particles")) return;
        for (Player viewer : viewers) {
            if (!near(viewer, location)) continue;
            try {
                viewer.spawnParticle(particle, location, count, spread, spread, spread, 0.015D);
            } catch (IllegalArgumentException | LinkageError ignored) {
                // An unsupported cosmetic must never interrupt an ability after its cost was paid.
            }
        }
    }

    private void sound(AbilityPlayerContext context, Location location, List<Player> viewers, Sound sound, float volume, float pitch) {
        if (sound == null || !enabled(context, "sounds")) return;
        for (Player viewer : viewers) {
            if (!near(viewer, location)) continue;
            try {
                viewer.playSound(location, sound, volume, pitch);
            } catch (IllegalArgumentException | LinkageError ignored) {
                // Optional sound names vary by Minecraft version.
            }
        }
    }

    private List<Player> viewers(Location location, Player subject) {
        List<Player> viewers = new ArrayList<Player>();
        if (location.getWorld() == null) return viewers;
        for (Player player : location.getWorld().getPlayers()) {
            if (near(player, location) && (player.equals(subject) || player.canSee(subject))) viewers.add(player);
        }
        if (near(subject, location) && !viewers.contains(subject)) viewers.add(subject);
        return viewers;
    }

    private boolean near(Player player, Location location) {
        return player.isOnline() && location.getWorld() != null && location.getWorld().equals(player.getWorld())
            && player.getLocation().distanceSquared(location) <= VIEW_DISTANCE_SQUARED;
    }

    private boolean hidden(Player player) { return player.hasPotionEffect(PotionEffectType.INVISIBILITY); }

    private void actionBar(AbilityPlayerContext context, Player player, String message) {
        if (enabled(context, "action-bar") && player.isOnline()) context.plugin().nms().sendActionBar(player, message);
    }

    private void chat(AbilityPlayerContext context, Player player, String type, String text) {
        if (context.plugin().getConfig().getBoolean("abilities.messages.enabled", true)
            && context.plugin().getConfig().getBoolean("abilities.messages." + type, true)) player.sendMessage(text);
    }

    private boolean enabled(AbilityPlayerContext context, String feature) {
        return context.plugin().getConfig().getBoolean("abilities.effects.enabled", true)
            && context.plugin().getConfig().getBoolean("abilities.effects." + feature, true);
    }

    private String shorten(String text) { return text.length() > 55 ? text.substring(0, 54) + "…" : text; }
}
