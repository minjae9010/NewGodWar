package kr.newgodwar.ability.feedback;

import kr.newgodwar.ability.api.AbilityDefinition;
import kr.newgodwar.ability.api.AbilityPlayerContext;
import kr.newgodwar.ability.api.AbilityVisuals;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/** Cosmetic feedback owned by one ability session; never changes combat state. */
public final class AbilityFeedback {
    private static final double VIEW_DISTANCE_SQUARED = 32.0D * 32.0D;
    private final Map<String, Long> notices = new LinkedHashMap<String, Long>();
    private final Map<UUID, Reaction> reactions = new LinkedHashMap<UUID, Reaction>();
    private int reactionTask = -1;
    private final Map<String, DesignedScene> designedScenes = new LinkedHashMap<String, DesignedScene>();
    private int designTask = -1;
    private final ObjectEffects objects = new ObjectEffects();
    private final AbilityVisuals visualPolicy;

    public AbilityFeedback() { this(new AbilityVisuals() { }); }

    /** Bind once to the owning ability; do not share this renderer across ability sessions. */
    public AbilityFeedback(AbilityVisuals visuals) {
        this.visualPolicy = java.util.Objects.requireNonNull(visuals, "visuals");
    }

    private AbilityStyle style() { return visualPolicy.style(); }
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
        designedScenes.clear();
        if (designTask >= 0) Bukkit.getScheduler().cancelTask(designTask);
        designTask = -1;
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
        AbilityTheme theme = style().theme();
        String label = label(context.ability(), advanced);
        String summary = summary(context.ability(), advanced);
        chat(context, player, "success", theme.color() + "✦ " + label + ChatColor.WHITE + " 사용!");
        actionBar(context, player, theme.color() + "✦ " + label + " 발동 " + ChatColor.WHITE + shorten(summary));
        if (advanced && enabled(context, "titles")) {
            context.plugin().nms().sendTitle(player, theme.color() + context.ability().name(),
                ChatColor.GOLD + "고급 능력 발동", 2, 18, 8);
        }
        cue(context, player, style().cast(advanced));
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

    public void drawCue(AbilityPlayerContext context, Location center, EffectCue cue, List<Player> audience) {
        drawCue(context, center, cue, audience, null, true);
    }

    public boolean visuals(AbilityPlayerContext context) { return enabled(context, "particles") || objects.enabled(context); }

    public void drawCue(AbilityPlayerContext context, Location center, EffectCue cue, List<Player> audience, Player subject, boolean useObjects) {
        if (center == null || center.getWorld() == null || !visuals(context)) return;
        DesignedEffect design = style().effect(cue);
        if (design != null) {
            designed(context, center, cue, design, audience, subject, useObjects);
            cueSound(context, center, cue, audience);
            return;
        }
        ObjectModel model = cue == EffectCue.GUARD ? SharedModels.SHIELD : cue == EffectCue.WINGS
            ? (style().theme() == AbilityTheme.FIRE ? SharedModels.FIRE_WINGS : SharedModels.WINGS) : null;
        if (model != null) {
            String key = "cue:" + cue + ":" + (subject == null ? positionKey(center) : subject.getUniqueId());
            Location fixed = upright(center);
            if (useObjects && objects.show(key, context, model,
                () -> subject == null ? fixed.clone() : subject.isOnline() && !subject.isDead() ? upright(subject.getLocation()) : null,
                () -> subject == null ? effectViewers(context, fixed) : targetViewers(context, subject, subject.getLocation()),
                cue == EffectCue.GUARD || style().flightModel() != null ? 22 : 16, 1)) return;
            modelOutline(context, fixed, model, 0, 1, audience);
            return;
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
                    ? (style().theme() == AbilityTheme.FIRE ? AbilityTheme.FIRE.particle() : AbilityTheme.LIGHTNING.particle())
                    : INKS.get(ink);
                particle(context, point, kind, audience, 1, 0);
            }
        });
        cueSound(context, center, cue, audience);
    }

    private void cueSound(AbilityPlayerContext context, Location center, EffectCue cue, List<Player> audience) {
        if (cue == EffectCue.FORGE || cue == EffectCue.ITEM)
            sound(context, center, audience, cue == EffectCue.FORGE ? AbilityTheme.CRAFT.sound()
                : AbilityTheme.sound("ENTITY_EXPERIENCE_ORB_PICKUP"), 0.25F, 1.5F);
    }

    /** Every frame revalidates ownership, target visibility and world before showing anything. */
    private void designed(AbilityPlayerContext context, Location center, EffectCue cue, DesignedEffect effect,
                          List<Player> audience, Player subject, boolean useObjects) {
        String key = "design:" + cue + ":" + (subject == null ? positionKey(center) : subject.getUniqueId());
        if (designedScenes.containsKey(key) || designedScenes.size() >= 8) return;
        boolean animate = enabled(context, "animations");
        DesignedScene scene = new DesignedScene(context, center, subject, audience, effect.model(animate), useObjects, animate);
        if (!renderDesign(key, scene)) return;
        // Static mode retains the readable pose; ObjectEffects owns its expiration.
        if (!animate) return;
        designedScenes.put(key, scene);
        if (designTask >= 0) return;
        designTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(context.plugin(), this::updateDesigns, 4L, 4L);
    }

    private void updateDesigns() {
        for (String active : new ArrayList<String>(designedScenes.keySet())) {
            DesignedScene current = designedScenes.get(active);
            current.age += 4;
            if (current.age >= DesignedEffect.DURATION || !renderDesign(active, current)) {
                designedScenes.remove(active); objects.remove(active);
            }
        }
        if (designedScenes.isEmpty()) {
            Bukkit.getScheduler().cancelTask(designTask); designTask = -1;
        }
    }

    private boolean renderDesign(String key, DesignedScene scene) {
        Location center = scene.anchor();
        if (center == null || !visuals(scene.context)) return false;
        if (scene.animate && !enabled(scene.context, "animations")) return false;
        Supplier<List<Player>> audience = () -> scene.audience(this);
        if (audience.get().isEmpty()) return false;
        if (!scene.particlesOnly && objects.show(key, scene.context, scene.model, scene::anchor, audience,
                DesignedEffect.DURATION - scene.age, 1)) return true;
        // A fallback keeps the same timeline even when entity capacity becomes available later.
        scene.particlesOnly = true;
        modelOutline(scene.context, center, scene.model, scene.animate ? scene.age : 8, 1, audience.get(), 64);
        return true;
    }

    private static final class DesignedScene {
        final AbilityPlayerContext context;
        final Location origin;
        final Player subject;
        final org.bukkit.World ownerWorld;
        final List<Player> permitted;
        final ObjectModel model;
        final boolean animate;
        boolean particlesOnly;
        int age;
        DesignedScene(AbilityPlayerContext context, Location origin, Player subject, List<Player> permitted,
                      ObjectModel model, boolean useObjects, boolean animate) {
            this.context = context; this.origin = origin.clone(); this.subject = subject;
            this.ownerWorld = context.player().getWorld(); this.permitted = new ArrayList<Player>(permitted);
            this.model = model; this.particlesOnly = !useObjects; this.animate = animate;
        }
        Location anchor() {
            Player owner = context.player();
            if (!owner.isOnline() || owner.isDead() || !ownerWorld.equals(owner.getWorld())) return null;
            if (subject != null && (!subject.isOnline() || subject.isDead() || !origin.getWorld().equals(subject.getWorld()))) return null;
            return upright(subject == null ? origin : subject.getLocation());
        }
        List<Player> audience(AbilityFeedback feedback) {
            Location at = anchor();
            if (at == null) return Collections.emptyList();
            List<Player> fresh = new ArrayList<Player>(subject == null ? feedback.effectViewers(context, at)
                : feedback.targetViewers(context, subject, at));
            fresh.retainAll(permitted); return fresh;
        }
    }

    public void impact(AbilityPlayerContext context, Location center) {
        if (center == null || center.getWorld() == null || !allow("impact", 700L)) return;
        drawCue(context, center, style().hit(), effectViewers(context, center));
    }

    public void impact(AbilityPlayerContext context, Player target) {
        cue(context, target, style().hit());
    }

    public void departure(AbilityPlayerContext context, Location origin) {
        drawCue(context, origin, EffectCue.PORTAL, effectViewers(context, origin));
    }

    public void status(AbilityPlayerContext context, Player target, String potion) {
        cue(context, target, style().status(potion));
    }

    public boolean hasParticles(AbilityPlayerContext context) { return enabled(context, "particles"); }

    /** Body attachments follow heading, never the up/down camera angle. Does not mutate the input. */
    public static Location upright(Location location) {
        Location result = location.clone(); result.setPitch(0); return result;
    }

    public void flight(AbilityPlayerContext context) {
        Player player = context.player();
        ObjectModel model = style().flightModel();
        if (model == null || !player.isFlying()) return;
        objects.show("cue:WINGS:" + player.getUniqueId(), context, model,
            () -> player.isOnline() && !player.isDead() && player.isFlying() ? upright(player.getLocation()) : null,
            () -> targetViewers(context, player, player.getLocation()), 22, 1);
    }

    public void trail(AbilityPlayerContext context, Location center) {
        if (center == null || center.getWorld() == null || !enabled(context, "animations")) return;
        if (style().flightModel() != null && objects.enabled(context)) {
            flight(context);
            if (objects.contains("cue:WINGS:" + context.player().getUniqueId())) return;
        }
        particle(context, center, style().trailTheme().particle(), effectViewers(context, center), 2, 0.02D);
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
        String message = style().theme().color() + "[" + context.ability().name() + "] " + text;
        chat(context, context.player(), "timer", message);
        actionBar(context, context.player(), message);
    }

    public void affected(AbilityPlayerContext context, Player target, String effect, boolean harmful) {
        if (target == null || !target.isOnline() || !allow("target:" + target.getUniqueId(), 900L)) return;
        String message = (harmful ? ChatColor.RED : ChatColor.GREEN) + "[" + context.ability().name() + "] " + effect;
        actionBar(context, target, message);
        AbilityStyle style = style();
        cue(context, target, harmful ? style.hit() : style.benefit());
    }

    public void passive(AbilityPlayerContext context, String text) {
        if (!allow("passive", 1500L)) return;
        actionBar(context, context.player(), style().theme().color()
            + "◆ " + context.ability().name() + " · " + text);
        cue(context, context.player(), style().passive());
    }

    public void pulse(AbilityPlayerContext context, Location center, double radius) {
        if (center == null || center.getWorld() == null) return;
        AbilityTheme theme = style().theme();
        List<Player> viewers = style().privateCast() || hidden(context.player())
            ? Collections.singletonList(context.player()) : viewers(center, context.player());
        ring(context, center, Math.max(0.5D, Math.min(12.0D, radius)), theme, viewers);
    }

    public void link(AbilityPlayerContext context, Location from, Location to) {
        if (from == null || to == null || from.getWorld() == null || !from.getWorld().equals(to.getWorld())) return;
        if (!enabled(context, "particles")) return;
        AbilityTheme theme = style().theme();
        List<Player> viewers = hidden(context.player()) || style().privateCast()
            ? Collections.singletonList(context.player()) : viewers(to, context.player());
        int steps = Math.max(1, Math.min(24, (int) Math.ceil(from.distance(to) * 2.0D)));
        for (int i = 0; i <= steps; i++) {
            double fraction = i / (double) steps;
            Location point = from.clone().add((to.getX() - from.getX()) * fraction,
                (to.getY() - from.getY()) * fraction, (to.getZ() - from.getZ()) * fraction);
            particle(context, point, theme.particle(), viewers, 1, 0.02D);
        }
    }

    public void ring(AbilityPlayerContext context, Location center, double radius, AbilityTheme theme, List<Player> viewers) {
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
        AbilityTheme theme = style().theme();
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
        sigil(context, center, radius, style().theme());
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

    public List<Player> effectViewers(AbilityPlayerContext context, Location center) {
        return hidden(context.player()) || style().privateCast()
            ? Collections.singletonList(context.player()) : viewers(center, context.player());
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

    public void flock(AbilityPlayerContext context, Player target, int phase, boolean bees, ObjectModel model) {
        if (target == null || !target.isOnline() || target.isDead()) return;
        Supplier<Location> anchor = () -> {
            if (!target.isOnline() || target.isDead()) return null;
            Location at = target.getLocation(); at.setPitch(0); at.setYaw(0); return at;
        };
        Supplier<List<Player>> audience = () -> targetViewers(context, target, target.getLocation());
        if (!followObject("flock:" + target.getUniqueId(), context, model, anchor, audience, 22, 1))
            modelOutline(context, anchor.get(), model, phase * 10, 1, audience.get());
        if (bees && phase % 4 == 0) sound(context, target.getLocation(), audience.get(), AbilityTheme.SWARM.sound(), 0.25F, 1.5F);
    }

    public void spear(AbilityPlayerContext context, Location from, Location to) {
        if (from == null || to == null || from.getWorld() == null || !from.getWorld().equals(to.getWorld())) return;
        List<Player> audience = effectViewers(context, to);
        Location tip = to.clone();
        org.bukkit.util.Vector direction = to.toVector().subtract(from.toVector());
        if (direction.lengthSquared() < 0.001D) return;
        tip.setDirection(direction);
        if (!object(context, "spear", SharedModels.SPEAR, tip, 10, 1)) {
            // A slim trajectory leads to the same spearhead silhouette on old clients.
            segment(context, from, to, AbilityTheme.HUNT, audience);
            modelOutline(context, tip, SharedModels.SPEAR, 0, 1, audience);
        }
        sound(context, to, audience, AbilityTheme.COMBAT.sound(), 0.5F, 0.7F);
    }

    public boolean object(AbilityPlayerContext context, String key, ObjectModel model, Location center, int ticks, double detail) {
        Location anchor = center.clone();
        return objects.show(key, context, model, () -> anchor.clone(), () -> effectViewers(context, anchor), ticks, detail);
    }

    public String positionKey(Location location) {
        return location.getWorld().getUID() + ":" + location.getX() + ":" + location.getY() + ":" + location.getZ();
    }

    public void modelOutline(AbilityPlayerContext context, Location center, ObjectModel model, double phase, double detail, List<Player> audience) {
        modelOutline(context, center, model, phase, detail, audience, 64);
    }

    private void modelOutline(AbilityPlayerContext context, Location center, ObjectModel model, double phase, double detail, List<Player> audience, int budget) {
        if (!enabled(context, "particles")) return;
        double yaw = Math.toRadians(center.getYaw()), pitch = Math.toRadians(center.getPitch());
        List<ObjectModel.Part> parts = model.parts(phase, detail);
        int samples = Math.max(1, Math.min(8, budget / Math.max(1, parts.size())));
        for (ObjectModel.Part part : parts) {
            // Sample the longest face, so thin shafts retain their length in the fallback.
            boolean top = part.sz > part.sy;
            for (int sample = 0; sample < samples; sample++) {
                if (budget-- <= 0) return;
                double perimeter = sample * 4D / samples;
                int edge = (int) perimeter;
                double t = perimeter - edge;
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

    public List<Player> targetViewers(AbilityPlayerContext context, Player target, Location center) {
        // A recipient must see their own applied effect even while invisible. Other players must not learn their position.
        if (hidden(target)) return Collections.singletonList(target);
        if (hidden(context.player()) || style().privateCast()) {
            List<Player> audience = new ArrayList<Player>();
            audience.add(target);
            if (!target.equals(context.player()) && context.player().canSee(target)) audience.add(context.player());
            return audience;
        }
        List<Player> audience = viewers(center, target);
        audience.removeIf(viewer -> !viewer.equals(context.player()) && !viewer.canSee(context.player()));
        return audience;
    }

    public void segment(AbilityPlayerContext context, Location from, Location to, AbilityTheme theme, List<Player> audience) {
        if (!enabled(context, "particles")) return;
        int steps = Math.max(2, Math.min(24, (int) Math.ceil(from.distance(to) * 3)));
        org.bukkit.util.Vector delta = to.toVector().subtract(from.toVector());
        for (int i = 0; i <= steps; i++)
            particle(context, from.clone().add(delta.clone().multiply(i / (double) steps)), theme.particle(), audience, 1, 0);
    }

    public void particle(AbilityPlayerContext context, Location location, Particle particle, List<Player> viewers, int count, double spread) {
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

    public void sound(AbilityPlayerContext context, Location location, List<Player> viewers, Sound sound, float volume, float pitch) {
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

    public boolean hidden(Player player) { return player.hasPotionEffect(PotionEffectType.INVISIBILITY); }

    private void actionBar(AbilityPlayerContext context, Player player, String message) {
        if (enabled(context, "action-bar") && player.isOnline()) context.plugin().nms().sendActionBar(player, message);
    }

    private void chat(AbilityPlayerContext context, Player player, String type, String text) {
        if (context.plugin().getConfig().getBoolean("abilities.messages.enabled", true)
            && context.plugin().getConfig().getBoolean("abilities.messages." + type, true)) player.sendMessage(text);
    }

    public boolean enabled(AbilityPlayerContext context, String feature) {
        return context.plugin().getConfig().getBoolean("abilities.effects.enabled", true)
            && context.plugin().getConfig().getBoolean("abilities.effects." + feature, true);
    }

    private String shorten(String text) { return text.length() > 55 ? text.substring(0, 54) + "…" : text; }

    /** Track a moving model in this ability session, using a freshly evaluated audience each frame. */
    public boolean followObject(String key, AbilityPlayerContext context, ObjectModel model,
                                Supplier<Location> anchor, Supplier<List<Player>> audience, int ticks, double detail) {
        return objects.show(key, context, model, anchor, audience, ticks, detail);
    }

    public void removeObject(String key) { objects.remove(key); }
}
