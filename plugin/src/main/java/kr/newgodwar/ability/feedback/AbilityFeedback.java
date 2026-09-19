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

    /** A bounded spiral for gravity wells, charged weapons and marked prey. */
    public void spiral(AbilityPlayerContext context, Location center, double radius) {
        if (center == null || center.getWorld() == null || !enabled(context, "particles")) return;
        AbilityTheme theme = AbilityTheme.of(context.ability().id());
        List<Player> audience = effectViewers(context, center);
        double size = Math.max(0.3D, Math.min(6.0D, radius));
        for (int i = 0; i < 24; i++) {
            double angle = Math.PI * 4.0D * i / 24.0D;
            particle(context, center.clone().add(Math.cos(angle) * size, i / 16.0D, Math.sin(angle) * size),
                theme.particle(), audience, 1, 0.0D);
        }
    }

    /** A visible four-point seal; gameplay continues when cosmetics are disabled. */
    public void sigil(AbilityPlayerContext context, Location center, double radius) {
        sigil(context, center, radius, AbilityTheme.of(context.ability().id()));
    }

    public void sigil(AbilityPlayerContext context, Location center, double radius, AbilityTheme theme) {
        if (center == null || center.getWorld() == null || !enabled(context, "particles")) return;
        List<Player> audience = effectViewers(context, center);
        double size = Math.max(0.5D, Math.min(6.0D, radius));
        ring(context, center, size, theme, audience);
        for (int i = 0; i < 16; i++) {
            double offset = size * (i / 7.5D - 1.0D);
            particle(context, center.clone().add(offset, 0.2D, 0), theme.particle(), audience, 1, 0);
            particle(context, center.clone().add(0, 0.2D, offset), theme.particle(), audience, 1, 0);
        }
    }

    private List<Player> effectViewers(AbilityPlayerContext context, Location center) {
        return hidden(context.player()) || AbilityTheme.privateCast(context.ability().id())
            ? Collections.singletonList(context.player()) : viewers(center, context.player());
    }

    /** A small hammer silhouette, used along Mjolnir's actual outbound/return path. */
    public void hammer(AbilityPlayerContext context, Location center) {
        if (center == null || center.getWorld() == null || !enabled(context, "particles")) return;
        List<Player> audience = effectViewers(context, center);
        for (int i = 0; i < 7; i++) {
            particle(context, center.clone().add((i - 3) * 0.13D, 0.25D, 0), AbilityTheme.LIGHTNING.particle(), audience, 1, 0);
            particle(context, center.clone().add(0, 0.25D - i * 0.13D, 0), AbilityTheme.LIGHTNING.particle(), audience, 1, 0);
        }
    }

    /** Cosmetic lightning never ignites blocks or damages teammates. */
    public void thunderbolt(AbilityPlayerContext context, Location center) {
        if (center == null || center.getWorld() == null) return;
        List<Player> audience = effectViewers(context, center);
        for (int i = 0; i <= 24; i++) {
            double offset = i == 24 ? 0 : Math.sin(i * 1.9D) * 0.35D;
            particle(context, center.clone().add(offset, 6.0D - i * 0.25D, -offset),
                AbilityTheme.LIGHTNING.particle(), audience, 1, 0);
        }
        sound(context, center, audience, AbilityTheme.LIGHTNING.sound(), 0.6F, 1.1F);
    }

    /** Silver crescent and one star per mark, attached to the hunted player's position. */
    public void huntMark(AbilityPlayerContext context, Player target, int marks) {
        if (target == null || !target.isOnline() || !enabled(context, "particles")) return;
        Location center = target.getLocation().add(0, 2.3D, 0);
        List<Player> audience = hidden(target) || hidden(context.player())
            ? Collections.singletonList(context.player()) : viewers(center, target);
        for (int i = 0; i < 15; i++) {
            double angle = Math.PI * (0.25D + i * 1.5D / 14.0D);
            particle(context, center.clone().add(Math.cos(angle) * 0.4D, Math.sin(angle) * 0.4D, 0),
                AbilityTheme.LIGHTNING.particle(), audience, 1, 0);
        }
        for (int i = 0; i < Math.min(3, Math.max(0, marks)); i++) {
            particle(context, center.clone().add((i - 1) * 0.3D, 0.65D, 0), AbilityTheme.HUNT.particle(), audience, 2, 0);
        }
    }

    public void echoSlash(AbilityPlayerContext context, Location center, double radius) {
        if (center == null || center.getWorld() == null) return;
        List<Player> audience = effectViewers(context, center);
        double size = Math.max(0.5D, Math.min(3, radius));
        for (int i = 0; i < 24; i++) {
            double angle = Math.PI * 2.0D * i / 24.0D;
            particle(context, center.clone().add(Math.cos(angle) * size, 0.8D + Math.sin(angle) * 0.3D, Math.sin(angle) * size),
                AbilityTheme.ECHO.particle(), audience, 1, 0);
        }
        sound(context, center, audience, AbilityTheme.ECHO.sound(), 0.4F, 1.2F);
    }

    public void rune(AbilityPlayerContext context, Location center, double radius, boolean frost) {
        if (center == null || center.getWorld() == null || !enabled(context, "particles")) return;
        AbilityTheme theme = frost ? AbilityTheme.FROST : AbilityTheme.FIRE;
        List<Player> audience = effectViewers(context, center);
        double size = Math.max(0.5D, Math.min(3, radius));
        ring(context, center, size, theme, audience);
        int arms = frost ? 6 : 3;
        for (int arm = 0; arm < arms; arm++) {
            double angle = Math.PI * 2.0D * arm / arms;
            for (int i = 1; i <= 5; i++) {
                double length = size * i / 5.0D;
                particle(context, center.clone().add(Math.cos(angle) * length, 0.2D, Math.sin(angle) * length),
                    theme.particle(), audience, 1, 0);
            }
        }
    }

    public void shield(AbilityPlayerContext context, Location center, double radius) {
        if (center == null || center.getWorld() == null || !enabled(context, "particles")) return;
        List<Player> audience = effectViewers(context, center);
        double size = Math.max(0.5D, Math.min(5, radius));
        for (int layer = 0; layer < 3; layer++) {
            double elevation = layer * Math.PI / 6.0D;
            for (int i = 0; i < 16; i++) {
                double angle = i * Math.PI / 8.0D;
                particle(context, center.clone().add(Math.cos(angle) * Math.cos(elevation) * size,
                    0.2D + Math.sin(elevation) * size, Math.sin(angle) * Math.cos(elevation) * size),
                    AbilityTheme.GUARD.particle(), audience, 1, 0);
            }
        }
    }

    public void runeBurst(AbilityPlayerContext context, Location center, boolean frost) {
        if (center == null || center.getWorld() == null) return;
        rune(context, center, 3, frost);
        AbilityTheme theme = frost ? AbilityTheme.FROST : AbilityTheme.FIRE;
        List<Player> audience = effectViewers(context, center);
        burst(context, center.clone().add(0, 1, 0), theme, audience, 16);
        sound(context, center, audience, theme.sound(), 0.6F, frost ? 1.4F : 0.8F);
    }

    public void clockFace(AbilityPlayerContext context, Location center, double radius, int phase) {
        if (center == null || center.getWorld() == null) return;
        List<Player> audience = effectViewers(context, center);
        double size = Math.max(0.5D, Math.min(6, radius));
        for (int i = 0; i < 12; i++) {
            double angle = i * Math.PI / 6;
            particle(context, center.clone().add(Math.cos(angle) * size, 0.15D, Math.sin(angle) * size),
                AbilityTheme.TIME.particle(), audience, 2, 0);
        }
        double angle = phase * Math.PI / 3;
        segment(context, center.clone().add(0, 0.2D, 0),
            center.clone().add(Math.cos(angle) * size, 0.2D, Math.sin(angle) * size), AbilityTheme.TIME, audience);
        sound(context, center, audience, AbilityTheme.TIME.sound(), 0.3F, 1.4F);
    }

    /** Small wing silhouettes follow a visible target; no persistent entities are spawned. */
    public void flock(AbilityPlayerContext context, Player target, int phase, boolean bees) {
        if (target == null || !target.isOnline()) return;
        Location center = target.getLocation().add(0, bees ? 1.2D : 2.5D, 0);
        List<Player> audience = targetViewers(context, target, center);
        Particle kind = bees ? AbilityTheme.SWARM.particle()
            : AbilityTheme.resolve(Particle.class, "SMOKE", "SMOKE_NORMAL");
        int count = bees ? 3 : 2;
        for (int i = 0; i < count; i++) {
            double angle = phase * 0.7D + Math.PI * 2 * i / count;
            Location bird = center.clone().add(Math.cos(angle) * 0.8D, Math.sin(angle * 2) * 0.2D, Math.sin(angle) * 0.8D);
            for (int wing = -2; wing <= 2; wing++)
                particle(context, bird.clone().add(wing * 0.12D, Math.abs(wing) * (phase % 2 == 0 ? 0.1D : -0.1D), 0),
                    kind, audience, 1, 0);
        }
        if (bees && phase % 4 == 0) sound(context, center, audience, AbilityTheme.SWARM.sound(), 0.25F, 1.5F);
    }

    public void spear(AbilityPlayerContext context, Location from, Location to) {
        if (from == null || to == null || from.getWorld() == null || !from.getWorld().equals(to.getWorld())) return;
        List<Player> audience = effectViewers(context, to);
        segment(context, from, to, AbilityTheme.GUARD, audience);
        segment(context, to.clone().add(-0.3D, 0.4D, 0), to, AbilityTheme.GUARD, audience);
        segment(context, to.clone().add(0.3D, 0.4D, 0), to, AbilityTheme.GUARD, audience);
        sound(context, to, audience, AbilityTheme.COMBAT.sound(), 0.5F, 0.7F);
    }

    public void forge(AbilityPlayerContext context, Location center, boolean quenched) {
        if (center == null || center.getWorld() == null) return;
        List<Player> audience = effectViewers(context, center);
        AbilityTheme theme = quenched ? AbilityTheme.WATER : AbilityTheme.FIRE;
        for (int i = 0; i < 12; i++) {
            double angle = i * Math.PI / 6;
            particle(context, center.clone().add(Math.cos(angle) * 0.65D, 0.7D, Math.sin(angle) * 0.65D),
                theme.particle(), audience, 2, 0.05D);
        }
        burst(context, center.clone().add(0, 1, 0), AbilityTheme.CRAFT, audience, 8);
        sound(context, center, audience, AbilityTheme.CRAFT.sound(), 0.4F, quenched ? 1.6F : 0.8F);
    }

    public void phalanx(AbilityPlayerContext context, Location center, org.bukkit.util.Vector facing) {
        if (center == null || center.getWorld() == null) return;
        List<Player> audience = effectViewers(context, center);
        org.bukkit.util.Vector side = new org.bukkit.util.Vector(-facing.getZ(), 0, facing.getX());
        for (int i = 0; i < 5; i++) {
            double angle = (i - 2) * Math.PI / 5;
            Location point = center.clone().add(facing.clone().multiply(Math.cos(angle) * 5))
                .add(side.clone().multiply(Math.sin(angle) * 5));
            for (int y = 0; y < 4; y++)
                particle(context, point.clone().add(0, 0.3D + y * 0.4D, 0), AbilityTheme.GUARD.particle(), audience, 2, 0.1D);
        }
        segment(context, center.clone().add(side.clone().multiply(-5)),
            center.clone().add(side.clone().multiply(5)), AbilityTheme.GUARD, audience);
    }

    public void oath(AbilityPlayerContext context, Player first, Player second) {
        Location from = first.getLocation().add(0, 1, 0), to = second.getLocation().add(0, 1, 0);
        if (!from.getWorld().equals(to.getWorld())) return;
        List<Player> audience = hidden(first) || hidden(second) ? Collections.singletonList(first) : targetViewers(context, second, to);
        segment(context, from, to, AbilityTheme.GUARD, audience);
        for (Location center : new Location[] {from, to}) {
            ring(context, center, 0.6D, AbilityTheme.GUARD, audience);
            particle(context, center.clone().add(0, 1.2D, 0), AbilityTheme.HEALING.particle(), audience, 2, 0.1D);
        }
    }

    public void scales(AbilityPlayerContext context, Player target, double burden) {
        if (target == null || !target.isOnline()) return;
        Location center = target.getLocation().add(0, 2.6D, 0);
        List<Player> audience = targetViewers(context, target, center);
        double tilt = Math.max(0, Math.min(4, burden)) * 0.1D;
        segment(context, center.clone().add(0, -0.5D, 0), center.clone().add(0, 0.4D, 0), AbilityTheme.GUARD, audience);
        segment(context, center.clone().add(-0.6D, tilt, 0), center.clone().add(0.6D, -tilt, 0), AbilityTheme.GUARD, audience);
        for (int side : new int[] {-1, 1}) {
            Location pan = center.clone().add(side * 0.6D, -side * tilt - 0.3D, 0);
            segment(context, pan.clone().add(0, 0.3D, 0), pan, AbilityTheme.SHADOW, audience);
            ring(context, pan, 0.2D, AbilityTheme.SHADOW, audience);
        }
    }

    public void honeycomb(AbilityPlayerContext context, Location center) {
        if (center == null || center.getWorld() == null) return;
        List<Player> audience = effectViewers(context, center);
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3, b = (i + 1) * Math.PI / 3;
            segment(context, center.clone().add(Math.cos(a) * 4, 0.2D, Math.sin(a) * 4),
                center.clone().add(Math.cos(b) * 4, 0.2D, Math.sin(b) * 4), AbilityTheme.SWARM, audience);
        }
        particle(context, center.clone().add(0, 0.5D, 0), AbilityTheme.HEALING.particle(), audience, 5, 0.4D);
    }

    public void wings(AbilityPlayerContext context, Location center, int laurels) {
        if (center == null || center.getWorld() == null) return;
        List<Player> audience = effectViewers(context, center);
        for (int side : new int[] {-1, 1}) for (int feather = 0; feather < 6; feather++) {
            double x = side * (0.3D + feather * 0.22D);
            segment(context, center.clone().add(x, 1.2D + feather * 0.12D, 0),
                center.clone().add(x, 0.7D + feather * 0.07D, 0.25D), AbilityTheme.WIND, audience);
        }
        for (int i = 0; i < Math.min(3, laurels); i++)
            particle(context, center.clone().add((i - 1) * 0.3D, 2.4D, 0), AbilityTheme.NATURE.particle(), audience, 2, 0);
    }

    public void harvest(AbilityPlayerContext context, Location center, int stage) {
        if (center == null || center.getWorld() == null) return;
        List<Player> audience = effectViewers(context, center);
        ring(context, center, 5, AbilityTheme.NATURE, audience);
        double height = 0.2D + Math.min(3, Math.max(0, stage)) * 0.3D;
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4;
            Location stalk = center.clone().add(Math.cos(angle) * 3, 0.2D, Math.sin(angle) * 3);
            segment(context, stalk, stalk.clone().add(0, height, 0), AbilityTheme.NATURE, audience);
            particle(context, stalk.clone().add(0, height, 0), AbilityTheme.SWARM.particle(), audience, 3, 0.1D);
        }
        sound(context, center, audience, AbilityTheme.NATURE.sound(), 0.4F, 0.8F + stage * 0.2F);
    }

    public void melody(AbilityPlayerContext context, Location center, double radius, int note, boolean joyful) {
        if (center == null || center.getWorld() == null) return;
        List<Player> audience = effectViewers(context, center);
        ring(context, center, radius, AbilityTheme.MUSIC, audience);
        particle(context, center.clone().add(0, 1.8D, 0), AbilityTheme.MUSIC.particle(), audience, 3, 0.3D);
        sound(context, center, audience, AbilityTheme.MUSIC.sound(), 0.6F, (joyful ? 1.0F : 0.5F) + note * 0.2F);
    }

    private List<Player> targetViewers(AbilityPlayerContext context, Player target, Location center) {
        if (hidden(target) || hidden(context.player())) return Collections.singletonList(context.player());
        List<Player> audience = viewers(center, target);
        audience.removeIf(viewer -> !viewer.equals(context.player()) && !viewer.canSee(context.player()));
        return audience;
    }

    private void segment(AbilityPlayerContext context, Location from, Location to, AbilityTheme theme, List<Player> audience) {
        if (!enabled(context, "particles")) return;
        int steps = Math.max(2, Math.min(24, (int) Math.ceil(from.distance(to) * 3)));
        org.bukkit.util.Vector delta = to.toVector().subtract(from.toVector());
        for (int i = 0; i <= steps; i++)
            particle(context, from.clone().add(delta.clone().multiply(i / (double) steps)), theme.particle(), audience, 1, 0);
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
