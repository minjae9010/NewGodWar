package kr.newgodwar.ability.feedback;

import kr.newgodwar.ability.api.AbilityDefinition;
import kr.newgodwar.ability.api.AbilityPlayerContext;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
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
import java.util.UUID;
import java.util.EnumMap;

/** Cosmetic feedback owned by one ability session; never changes combat state. */
public final class AbilityFeedback {
    private static final double VIEW_DISTANCE_SQUARED = 32.0D * 32.0D;
    private final Map<String, Long> notices = new LinkedHashMap<String, Long>();
    private final Map<UUID, Reaction> reactions = new LinkedHashMap<UUID, Reaction>();
    private int reactionTask = -1;
    private final ObjectEffects objects = new ObjectEffects();
    private int hammerFrame;
    private static final Map<EffectCue.Ink, Particle> INKS = new EnumMap<EffectCue.Ink, Particle>(EffectCue.Ink.class);
    static {
        INKS.put(EffectCue.Ink.CRIT, AbilityTheme.COMBAT.particle());
        INKS.put(EffectCue.Ink.SWEEP, AbilityTheme.ECHO.particle());
        INKS.put(EffectCue.Ink.HEART, AbilityTheme.HEALING.particle());
        INKS.put(EffectCue.Ink.LIGHT, AbilityTheme.LIGHTNING.particle());
        INKS.put(EffectCue.Ink.WITCH, AbilityTheme.SHADOW.particle());
        INKS.put(EffectCue.Ink.SMOKE, AbilityTheme.resolve(Particle.class, "SMOKE", "SMOKE_NORMAL"));
        INKS.put(EffectCue.Ink.FLAME, AbilityTheme.FIRE.particle());
        INKS.put(EffectCue.Ink.CLOUD, AbilityTheme.WIND.particle());
        INKS.put(EffectCue.Ink.SPARK, AbilityTheme.CRAFT.particle());
        INKS.put(EffectCue.Ink.PORTAL, AbilityTheme.GRAVITY.particle());
        INKS.put(EffectCue.Ink.NOTE, AbilityTheme.MUSIC.particle());
        INKS.put(EffectCue.Ink.SNOW, AbilityTheme.FROST.particle());
        INKS.put(EffectCue.Ink.WATER, AbilityTheme.WATER.particle());
        INKS.put(EffectCue.Ink.ENCHANT, AbilityTheme.ARCANE.particle());
        INKS.put(EffectCue.Ink.LEAF, AbilityTheme.NATURE.particle());
    }

    public void clear() {
        notices.clear(); reactions.clear(); objects.clear();
        if (reactionTask >= 0) Bukkit.getScheduler().cancelTask(reactionTask);
        reactionTask = -1;
    }

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
        cue(context, player, AbilityStyle.of(context.ability().id()).cast(advanced));
    }

    /** Merge all reactions produced by one skill invocation, then render just the main action. */
    public void cue(AbilityPlayerContext context, Player target, EffectCue cue) {
        if (cue == EffectCue.NONE || target == null || !target.isOnline() || !visuals(context)) return;
        UUID id = target.getUniqueId();
        Reaction pending = reactions.get(id);
        if (pending != null) {
            if (cue.priority() > pending.cue.priority()) pending.cue = cue;
            return;
        }
        if (reactions.size() >= 64 || !allow("reaction:" + id, 700L)) return;
        reactions.put(id, new Reaction(context, target, cue));
        if (reactionTask >= 0) return;
        reactionTask = Bukkit.getScheduler().scheduleSyncDelayedTask(context.plugin(), () -> {
            reactionTask = -1;
            List<Reaction> batch = new ArrayList<Reaction>(reactions.values()); reactions.clear();
            for (Reaction reaction : batch) {
                Player owner = reaction.context.player(), recipient = reaction.target;
                if (!owner.isOnline() || owner.isDead() || !recipient.isOnline() || recipient.isDead()
                    || !reaction.world.equals(recipient.getWorld()) || !reaction.ownerWorld.equals(owner.getWorld())) continue;
                Location center = recipient.getLocation();
                drawCue(reaction.context, center, reaction.cue, targetViewers(reaction.context, recipient, center), recipient, true);
            }
        }, 1L);
    }

    private static final class Reaction {
        final AbilityPlayerContext context;
        final Player target;
        final org.bukkit.World world, ownerWorld;
        EffectCue cue;
        Reaction(AbilityPlayerContext context, Player target, EffectCue cue) {
            this.context = context; this.target = target; this.cue = cue;
            world = target.getWorld(); ownerWorld = context.player().getWorld();
        }
    }

    private void drawCue(AbilityPlayerContext context, Location center, EffectCue cue, List<Player> audience) {
        drawCue(context, center, cue, audience, null, true);
    }

    private boolean visuals(AbilityPlayerContext context) { return enabled(context, "particles") || objects.enabled(context); }

    private void drawCue(AbilityPlayerContext context, Location center, EffectCue cue, List<Player> audience, Player subject, boolean useObjects) {
        if (center == null || center.getWorld() == null || !visuals(context)) return;
        ObjectModel model = cue == EffectCue.GUARD ? ObjectModel.SHIELD : cue == EffectCue.WINGS
            ? (AbilityTheme.of(context.ability().id()) == AbilityTheme.FIRE ? ObjectModel.FIRE_WINGS : ObjectModel.WINGS) : null;
        if (useObjects && model != null) {
            String key = "cue:" + cue + ":" + (subject == null ? positionKey(center) : subject.getUniqueId());
            Location fixed = center.clone();
            if (objects.show(key, context, model,
                () -> subject == null ? fixed.clone() : subject.isOnline() && !subject.isDead() ? subject.getLocation() : null,
                () -> subject == null ? effectViewers(context, fixed) : targetViewers(context, subject, subject.getLocation()),
                cue == EffectCue.GUARD || "hermes".equals(context.ability().id()) || "jujak".equals(context.ability().id()) ? 22 : 16, 1)) return;
        }
        if (!enabled(context, "particles")) return;
        double yaw = Math.toRadians(center.getYaw()), cos = Math.cos(yaw), sin = Math.sin(yaw);
        cue.draw((ink, x, y, z, rgb) -> {
            Location point = center.clone().add(-x * cos - z * sin, y, -x * sin + z * cos);
            if (ink == EffectCue.Ink.COLOR) {
                for (Player viewer : audience) if (near(viewer, point)) {
                    try { ColoredDust.spawn(viewer, point, rgb, AbilityTheme.LIGHTNING.particle()); }
                    catch (IllegalArgumentException | LinkageError ignored) { }
                }
            } else {
                Particle kind = ink == EffectCue.Ink.ELEMENT
                    ? (AbilityTheme.of(context.ability().id()) == AbilityTheme.FIRE ? AbilityTheme.FIRE.particle() : AbilityTheme.LIGHTNING.particle())
                    : INKS.get(ink);
                particle(context, point, kind, audience, 1, 0);
            }
        });
        if (cue == EffectCue.FORGE || cue == EffectCue.ITEM)
            sound(context, center, audience, cue == EffectCue.FORGE ? AbilityTheme.CRAFT.sound()
                : AbilityTheme.sound("ENTITY_EXPERIENCE_ORB_PICKUP"), 0.25F, 1.5F);
    }

    public void impact(AbilityPlayerContext context, Location center) {
        if (center == null || center.getWorld() == null || !allow("impact", 700L)) return;
        drawCue(context, center, AbilityStyle.of(context.ability().id()).hit(), effectViewers(context, center));
    }

    public void impact(AbilityPlayerContext context, Player target) {
        cue(context, target, AbilityStyle.of(context.ability().id()).hit());
    }

    public void departure(AbilityPlayerContext context, Location origin) {
        drawCue(context, origin, EffectCue.PORTAL, effectViewers(context, origin));
    }

    /** Three inward streams show the direction of the actual pull. */
    public void gravityWell(AbilityPlayerContext context, Location center, double radius, int phase) {
        if (center == null || center.getWorld() == null) return;
        if (object(context, "gravity:" + positionKey(center), ObjectModel.GRAVITY, center, 12, radius)) return;
        List<Player> audience = effectViewers(context, center);
        double size = Math.max(0.5D, Math.min(6, radius));
        for (int arm = 0; arm < 3; arm++) for (int i = 0; i < 8; i++) {
            double fraction = (i + 1) / 8D;
            double angle = arm * Math.PI * 2 / 3 + phase * 0.45D + fraction * 1.5D;
            particle(context, center.clone().add(Math.cos(angle) * size * fraction,
                0.15D + (1 - fraction) * 0.65D, Math.sin(angle) * size * fraction),
                AbilityTheme.GRAVITY.particle(), audience, 1, 0);
        }
    }

    /** Four sparse icy ribs follow the sphere's surface without filling it with particles. */
    public void frostCage(AbilityPlayerContext context, Location center, double radius) {
        if (center == null || center.getWorld() == null) return;
        List<Player> audience = effectViewers(context, center);
        double size = Math.max(0.5D, Math.min(6, radius));
        for (int rib = 0; rib < 4; rib++) for (int i = 0; i <= 8; i++) {
            double elevation = i * Math.PI / 8, angle = rib * Math.PI / 4;
            particle(context, center.clone().add(Math.cos(angle) * Math.cos(elevation) * size,
                Math.sin(elevation) * size, Math.sin(angle) * Math.cos(elevation) * size),
                AbilityTheme.FROST.particle(), audience, 1, 0);
        }
    }

    public void status(AbilityPlayerContext context, Player target, String potion) {
        cue(context, target, AbilityStyle.of(context.ability().id()).status(potion));
    }

    public boolean hasParticles(AbilityPlayerContext context) { return enabled(context, "particles"); }

    public void flight(AbilityPlayerContext context) {
        Player player = context.player();
        String id = context.ability().id();
        if (!("hermes".equals(id) || "jujak".equals(id)) || !player.isFlying()) return;
        objects.show("cue:WINGS:" + player.getUniqueId(), context, "jujak".equals(id) ? ObjectModel.FIRE_WINGS : ObjectModel.WINGS,
            () -> player.isOnline() && !player.isDead() && player.isFlying() ? player.getLocation() : null,
            () -> targetViewers(context, player, player.getLocation()), 22, 1);
    }

    public void trail(AbilityPlayerContext context, Location center) {
        if (center == null || center.getWorld() == null || !enabled(context, "animations")) return;
        String id = context.ability().id();
        if (("hermes".equals(id) || "jujak".equals(id)) && objects.enabled(context)) {
            flight(context);
            if (objects.contains("cue:WINGS:" + context.player().getUniqueId())) return;
        }
        Particle kind = "acidarcher".equals(id) ? AbilityTheme.SHADOW.particle()
            : "artemis".equals(id) ? AbilityTheme.LIGHTNING.particle()
            : "sniper".equals(id) || "archer".equals(id) ? AbilityTheme.HUNT.particle()
            : AbilityTheme.of(id).particle();
        particle(context, center, kind, effectViewers(context, center), 2, 0.02D);
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
        AbilityStyle style = AbilityStyle.of(context.ability().id());
        cue(context, target, harmful ? style.hit() : style.benefit());
    }

    public void passive(AbilityPlayerContext context, String text) {
        if (!allow("passive", 1500L)) return;
        actionBar(context, context.player(), AbilityTheme.of(context.ability().id()).color()
            + "◆ " + context.ability().name() + " · " + text);
        cue(context, context.player(), AbilityStyle.of(context.ability().id()).passive());
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
                theme == AbilityTheme.ECHO ? AbilityTheme.HUNT.particle() : theme.particle(), viewers, 1, 0.0D);
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
        if (center == null || center.getWorld() == null || !visuals(context)) return;
        if (object(context, "hammer", ObjectModel.HAMMER, center, 5, 1)) return;
        modelOutline(context, center, ObjectModel.HAMMER, ++hammerFrame, 1, effectViewers(context, center));
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
        if (allow("thunder-sound", 150L)) sound(context, center, audience, AbilityTheme.LIGHTNING.sound(), 0.6F, 1.1F);
    }

    /** Silver crescent and one star per mark, attached to the hunted player's position. */
    public void huntMark(AbilityPlayerContext context, Player target, int marks) {
        if (target == null || !target.isOnline() || !enabled(context, "particles")) return;
        Location center = target.getLocation().add(0, 2.3D, 0);
        List<Player> audience = targetViewers(context, target, center);
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
        // SWEEP_ATTACK already draws a full crescent; repeating it made a white disc.
        particle(context, center.clone().add(0, 1, 0), AbilityTheme.ECHO.particle(), audience, 1, 0);
        sound(context, center, audience, AbilityTheme.ECHO.sound(), 0.4F, 1.2F);
    }

    public void rune(AbilityPlayerContext context, Location center, double radius, boolean frost) {
        if (center == null || center.getWorld() == null || !visuals(context)) return;
        if (object(context, "rune:" + positionKey(center), frost ? ObjectModel.FROST_RUNE : ObjectModel.FIRE_RUNE, center, 22, radius)) return;
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
        objects.remove("rune:" + positionKey(center));
        AbilityTheme theme = frost ? AbilityTheme.FROST : AbilityTheme.FIRE;
        List<Player> audience = effectViewers(context, center);
        for (int i = 0; i < 16; i++) {
            double angle = i * Math.PI / 8;
            particle(context, center.clone().add(Math.cos(angle) * 2.0D, 0.3D + (i % 3) * 0.3D,
                Math.sin(angle) * 2.0D), theme.particle(), audience, 1, 0.05D);
        }
        if (allow("rune-sound", 150L)) sound(context, center, audience, theme.sound(), 0.6F, frost ? 1.4F : 0.8F);
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
        Location tip = to.clone();
        org.bukkit.util.Vector direction = to.toVector().subtract(from.toVector());
        if (direction.lengthSquared() < 0.001D) return;
        tip.setDirection(direction);
        if (!object(context, "spear", ObjectModel.SPEAR, tip, 10, 1)) {
            // A slim trajectory leads to the same spearhead silhouette on old clients.
            segment(context, from, to, AbilityTheme.HUNT, audience);
            modelOutline(context, tip, ObjectModel.SPEAR, 0, 1, audience);
        }
        sound(context, to, audience, AbilityTheme.COMBAT.sound(), 0.5F, 0.7F);
    }

    public void forge(AbilityPlayerContext context, Location center, boolean quenched) {
        if (center == null || center.getWorld() == null) return;
        List<Player> audience = effectViewers(context, center);
        if (quenched) {
            double yaw = Math.toRadians(center.getYaw());
            Location hand = center.clone().add(-0.38D * Math.cos(yaw) - 0.4D * Math.sin(yaw), 1.1D,
                -0.38D * Math.sin(yaw) + 0.4D * Math.cos(yaw));
            particle(context, hand, AbilityTheme.WIND.particle(), audience, 4, 0.1D);
            sound(context, hand, audience, AbilityTheme.sound("BLOCK_FIRE_EXTINGUISH"), 0.3F, 1.5F);
        } else drawCue(context, center, EffectCue.FORGE, audience);
    }

    public void phalanx(AbilityPlayerContext context, Location center, org.bukkit.util.Vector facing) {
        if (center == null || center.getWorld() == null) return;
        Location anchor = center.clone().setDirection(facing);
        if (object(context, "phalanx", ObjectModel.PHALANX, anchor, 22, 1)) return;
        List<Player> audience = effectViewers(context, center);
        org.bukkit.util.Vector side = new org.bukkit.util.Vector(-facing.getZ(), 0, facing.getX());
        for (int i = 0; i < 5; i++) {
            double angle = (i - 2) * Math.PI / 5;
            Location point = center.clone().add(facing.clone().multiply(Math.cos(angle) * 4.5D))
                .add(side.clone().multiply(Math.sin(angle) * 4.5D));
            point.setDirection(facing);
            drawCue(context, point, EffectCue.GUARD, audience, null, false);
        }
    }

    public void oath(AbilityPlayerContext context, Player first, Player second) {
        Location from = first.getLocation().add(0, 1, 0), to = second.getLocation().add(0, 1, 0);
        if (!from.getWorld().equals(to.getWorld())) return;
        List<Player> audience = hidden(first) || hidden(second) ? Collections.singletonList(first) : targetViewers(context, second, to);
        segment(context, from, to, AbilityTheme.GUARD, audience);
    }

    public void scales(AbilityPlayerContext context, Player target, double burden) {
        if (target == null || !target.isOnline()) return;
        Location center = target.getLocation().add(0, 2.6D, 0);
        if (objects.show("scales:" + target.getUniqueId(), context, ObjectModel.SCALES,
            () -> target.isOnline() && !target.isDead() ? target.getLocation().add(0, 2.6D, 0) : null,
            () -> targetViewers(context, target, target.getLocation()), 12, burden)) return;
        modelOutline(context, center, ObjectModel.SCALES, 0, burden, targetViewers(context, target, center));
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
        drawCue(context, center, EffectCue.WINGS, audience);
        for (int i = 0; i < Math.min(3, laurels); i++)
            particle(context, center.clone().add((i - 1) * 0.3D, 2.4D, 0), AbilityTheme.NATURE.particle(), audience, 2, 0);
    }

    public void harvest(AbilityPlayerContext context, Location center, int stage) {
        if (center == null || center.getWorld() == null) return;
        List<Player> audience = effectViewers(context, center);
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
        double size = Math.max(0.5D, Math.min(8, radius));
        for (int i = 0; i < 6; i++) {
            double angle = i * Math.PI / 3 + note * 0.3D;
            particle(context, center.clone().add(Math.cos(angle) * size, 1.5D + Math.sin(angle * 2) * 0.25D,
                Math.sin(angle) * size), AbilityTheme.MUSIC.particle(), audience, 1, 0);
        }
        sound(context, center, audience, AbilityTheme.MUSIC.sound(), 0.6F, (joyful ? 1.0F : 0.5F) + note * 0.2F);
    }

    private boolean object(AbilityPlayerContext context, String key, ObjectModel model, Location center, int ticks, double detail) {
        Location anchor = center.clone();
        return objects.show(key, context, model, () -> anchor.clone(), () -> effectViewers(context, anchor), ticks, detail);
    }

    private String positionKey(Location location) {
        return location.getWorld().getUID() + ":" + location.getX() + ":" + location.getY() + ":" + location.getZ();
    }

    private void modelOutline(AbilityPlayerContext context, Location center, ObjectModel model, double phase, double detail, List<Player> audience) {
        if (!enabled(context, "particles")) return;
        double yaw = Math.toRadians(center.getYaw()), pitch = Math.toRadians(center.getPitch());
        int budget = 64;
        for (ObjectModel.Part part : model.parts(phase, detail)) {
            // Sample the longest face, so thin shafts retain their length in the fallback.
            boolean top = part.sz > part.sy;
            for (int edge = 0; edge < 4; edge++) for (int step = 0; step < 2; step++) {
                if (budget-- <= 0) return;
                double t = step / 2D;
                double u = edge == 0 ? -0.5 + t : edge == 1 ? 0.5 : edge == 2 ? 0.5 - t : -0.5;
                double v = edge == 0 ? -0.5 : edge == 1 ? -0.5 + t : edge == 2 ? 0.5 : 0.5 - t;
                double x = u * part.sx, y = top ? part.sy / 2 : v * part.sy, z = top ? v * part.sz : part.sz / 2;
                double rx = x * Math.cos(part.turn) + z * Math.sin(part.turn);
                double rz = -x * Math.sin(part.turn) + z * Math.cos(part.turn);
                x = part.x + rx * Math.cos(part.roll) - y * Math.sin(part.roll);
                y = part.y + rx * Math.sin(part.roll) + y * Math.cos(part.roll); z = part.z + rz;
                double py = y * Math.cos(pitch) - z * Math.sin(pitch), pz = y * Math.sin(pitch) + z * Math.cos(pitch);
                Location point = center.clone().add(x * Math.cos(yaw) - pz * Math.sin(yaw), py, x * Math.sin(yaw) + pz * Math.cos(yaw));
                for (Player viewer : audience) if (near(viewer, point)) {
                    try { ColoredDust.spawn(viewer, point, part.color(), AbilityTheme.LIGHTNING.particle()); }
                    catch (IllegalArgumentException | LinkageError ignored) { }
                }
            }
        }
    }

    private List<Player> targetViewers(AbilityPlayerContext context, Player target, Location center) {
        if (hidden(target)) return target.equals(context.player()) ? Collections.singletonList(target) : Collections.emptyList();
        if (hidden(context.player()) || AbilityTheme.privateCast(context.ability().id()))
            return context.player().canSee(target) ? Collections.singletonList(context.player()) : Collections.emptyList();
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

    private void particle(AbilityPlayerContext context, Location location, Particle particle, List<Player> viewers, int count, double spread) {
        if (particle == null || !enabled(context, "particles")) return;
        for (Player viewer : viewers) {
            if (!near(viewer, location)) continue;
            try {
                viewer.spawnParticle(particle, location, count, spread, spread, spread, 0D);
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
