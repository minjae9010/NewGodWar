package kr.newgodwar.ability.feedback;

import kr.newgodwar.ability.api.AbilityPlayerContext;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/** One bounded renderer per ability session, with exclusive particle fallback on any failure. */
public final class ObjectEffects {
    private static final Set<ObjectEffects> ACTIVE = Collections.newSetFromMap(new IdentityHashMap<ObjectEffects, Boolean>());
    private static final int GLOBAL_LIMIT = 256;
    private static int totalParts;
    private final Map<String, Scene> scenes = new LinkedHashMap<String, Scene>();
    private int task = -1, tick, ownedParts;
    private boolean failed;

    public static boolean supported() { return DisplayBridge.INSTANCE != null; }

    public boolean enabled(AbilityPlayerContext context) {
        return !failed && supported() && context.plugin().getConfig().getBoolean("abilities.effects.enabled", true)
            && context.plugin().getConfig().getBoolean("abilities.effects.objects", true);
    }

    public boolean contains(String key) { return scenes.containsKey(key); }

    public boolean show(String key, AbilityPlayerContext context, ObjectModel model, Supplier<Location> anchor,
                        Supplier<List<Player>> audience, int lifetime, double detail) {
        if (!enabled(context)) { remove(key); return false; }
        Location location = anchor.get();
        if (!validLocation(location) || !context.player().isOnline() || context.player().isDead()) { remove(key); return false; }
        List<Player> viewers = audience.get();
        // An invisible scene is still handled: never reveal it with a public fallback.
        if (viewers.isEmpty()) { remove(key); return true; }
        Scene previous = scenes.get(key);
        if (previous != null && (previous.model != model || !previous.world.equals(location.getWorld()))) {
            remove(key); previous = null;
        }
        int ttl = Math.max(2, Math.min(40, lifetime));
        if (previous != null) {
            previous.anchor = anchor; previous.audience = audience; previous.detail = detail; previous.expires = tick + ttl;
            try { render(previous); return true; }
            catch (ReflectiveOperationException | RuntimeException | LinkageError ex) { fail(); return false; }
        }
        List<ObjectModel.Part> parts = model.parts(tick, detail);
        int limit = Math.max(0, Math.min(96, context.plugin().getConfig().getInt("abilities.effects.object-limit", 48)));
        if (ownedParts + parts.size() > limit || totalParts + parts.size() > GLOBAL_LIMIT) return false;
        Scene scene = new Scene(context, model, anchor, audience, location, tick + ttl, detail);
        try {
            for (ObjectModel.Part part : parts) DisplayBridge.INSTANCE.spawn(context.plugin(), location, part, scene.entities);
            scenes.put(key, scene); ownedParts += scene.entities.size(); totalParts += scene.entities.size(); ACTIVE.add(this);
            render(scene);
            if (task < 0) task = Bukkit.getScheduler().scheduleSyncRepeatingTask(context.plugin(), this::update, 2L, 2L);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
            if (!scenes.containsKey(key)) for (Entity entity : scene.entities) entity.remove();
            fail(); return false;
        }
    }

    private void update() {
        tick += 2;
        for (String key : new ArrayList<String>(scenes.keySet())) {
            Scene scene = scenes.get(key);
            Player owner = scene.context.player(); Location location = scene.anchor.get();
            if (tick >= scene.expires || !enabled(scene.context) || !owner.isOnline() || owner.isDead()
                || !scene.ownerWorld.equals(owner.getWorld()) || !validLocation(location) || !scene.world.equals(location.getWorld())) {
                remove(key); continue;
            }
            try { render(scene); }
            catch (ReflectiveOperationException | RuntimeException | LinkageError ex) { fail(); return; }
        }
        stopIfEmpty();
    }

    private void render(Scene scene) throws ReflectiveOperationException {
        Location location = scene.anchor.get();
        if (!validLocation(location) || !scene.world.equals(location.getWorld())) throw new IllegalStateException("Scene anchor left its world");
        List<ObjectModel.Part> parts = scene.model.parts(tick, scene.detail);
        Set<Player> audience = new LinkedHashSet<Player>(scene.audience.get());
        audience.removeIf(player -> !player.isOnline() || !scene.world.equals(player.getWorld())
            || player.getLocation().distanceSquared(location) > 32 * 32);
        for (Entity entity : scene.entities) {
            for (Player viewer : scene.viewers) if (!audience.contains(viewer) && viewer.isOnline())
                DisplayBridge.INSTANCE.visibility(scene.context.plugin(), viewer, entity, false);
            if (!entity.isValid() || !entity.teleport(location)) throw new IllegalStateException("Cosmetic display disappeared");
        }
        for (int i = 0; i < scene.entities.size(); i++) {
            Entity entity = scene.entities.get(i);
            DisplayBridge.INSTANCE.transform(entity, parts.get(i));
            for (Player viewer : audience) if (!scene.viewers.contains(viewer))
                DisplayBridge.INSTANCE.visibility(scene.context.plugin(), viewer, entity, true);
        }
        scene.viewers = audience;
    }

    private static boolean validLocation(Location location) {
        return location != null && location.getWorld() != null && Double.isFinite(location.getX())
            && Double.isFinite(location.getY()) && Double.isFinite(location.getZ())
            && location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    public void remove(String key) {
        Scene scene = scenes.remove(key);
        if (scene != null) {
            for (Entity entity : scene.entities) entity.remove();
            ownedParts -= scene.entities.size(); totalParts -= scene.entities.size();
        }
        stopIfEmpty();
    }

    public void clear() {
        for (String key : new ArrayList<String>(scenes.keySet())) remove(key);
        stopIfEmpty();
    }

    public static void clearAll() { for (ObjectEffects renderer : new ArrayList<ObjectEffects>(ACTIVE)) renderer.clear(); }

    private void fail() { clear(); failed = true; }

    private void stopIfEmpty() {
        if (scenes.isEmpty()) {
            if (task >= 0) Bukkit.getScheduler().cancelTask(task);
            task = -1; ACTIVE.remove(this);
        }
    }

    private static final class Scene {
        final AbilityPlayerContext context;
        final ObjectModel model;
        final World world, ownerWorld;
        final List<Entity> entities = new ArrayList<Entity>();
        Set<Player> viewers = new LinkedHashSet<Player>();
        Supplier<Location> anchor;
        Supplier<List<Player>> audience;
        int expires;
        double detail;
        Scene(AbilityPlayerContext context, ObjectModel model, Supplier<Location> anchor, Supplier<List<Player>> audience,
              Location location, int expires, double detail) {
            this.context = context; this.model = model; this.anchor = anchor; this.audience = audience;
            this.world = location.getWorld(); this.ownerWorld = context.player().getWorld(); this.expires = expires; this.detail = detail;
        }
    }
}
