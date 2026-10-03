package kr.newgodwar.regression;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.ability.api.AbilityPlayerContext;
import kr.newgodwar.ability.feedback.AbilityFeedback;
import kr.newgodwar.ability.feedback.ObjectEffects;
import kr.newgodwar.ability.feedback.ObjectModel;
import kr.newgodwar.ability.feedback.EffectCue;
import kr.newgodwar.ability.feedback.SharedModels;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Uses actual world display entities; only the viewing player/connection is a recorder. */
final class ObjectEffectRegressionChecks {
    private final NewGodWarPlugin core;
    private final VisualProbe visuals;
    private World world;
    private Location location;
    private boolean online = true, invisible, flying, dead;
    private double viewerOffset;
    private int particles, shown, hidden;
    private final List<Location> emittedPoints = new ArrayList<Location>();

    ObjectEffectRegressionChecks(NewGodWarPlugin core) { this.core = core; this.visuals = new VisualProbe(core); }

    void run() throws Exception {
        // Initialize modern compatibility while Bukkit still references the real CraftServer.
        boolean supported = ObjectEffects.supported();
        if (Bukkit.getBukkitVersion().startsWith("26.")) require(supported, "Modern server failed Display capability discovery");
        Map<String, Object> saved = new HashMap<String, Object>();
        for (String key : Arrays.asList("enabled", "particles", "objects", "object-limit", "animations")) {
            saved.put(key, core.getConfig().get("abilities.effects." + key));
        }
        world = Bukkit.getWorlds().get(0); world.getChunkAt(0, 0).load();
        location = new Location(world, 8, 80, 8);
        Player player = viewer();
        AbilityPlayerContext context = new AbilityPlayerContext(core, player, core.abilities().registry().get("thor"));
        ObjectEffects renderer = new ObjectEffects();
        AbilityFeedback feedback = visuals.feedback("thor");
        try {
            for (String key : Arrays.asList("enabled", "particles", "objects")) core.getConfig().set("abilities.effects." + key, true);
            core.getConfig().set("abilities.effects.object-limit", 48);
            visuals.effect("hammer", context, location);
            if (!supported) {
                require(displays().isEmpty() && particles > 0, "Legacy server did not select the detailed particle fallback");
                core.getLogger().info("PASS object effects: unavailable Display API uses bounded particle models on this server");
                return;
            }
            require(!displays().isEmpty() && particles == 0, "Modern hammer failed or stacked with its particle fallback");
            feedback.clear(); require(displays().isEmpty(), "Feedback.clear leaked its assembly");

            ObjectModel custom = ObjectModel.animated((phase, detail) -> Collections.singletonList(
                new ObjectModel.Part("GOLD_BLOCK", false, 0, Math.sin(phase) * 0.1, 0, 0.2, 0.2, 0.2, 0, 0)));
            require(show(renderer, context, player, custom), "Unregistered custom model failed");
            Set<UUID> customIds = ids(displays());
            step(renderer);
            require(displays().size() == 1 && customIds.equals(ids(displays())), "Custom model animation replaced entities");
            renderer.clear();

            for (ObjectModel model : visuals.models()) {
                // Many designs are exercised now; keep each fixture inside the loaded chunk.
                location.setX(8);
                require(show(renderer, context, player, model), "Display model failed: " + model);
                List<Entity> created = displays();
                require(created.size() == model.parts(0, 1).size(), "Incomplete/duplicated model: " + model);
                Set<UUID> ids = ids(created);
                for (Entity entity : created) {
                    require(!(Boolean) Entity.class.getMethod("isVisibleByDefault").invoke(entity), "A display was publicly visible before filtering");
                    require(!(Boolean) Entity.class.getMethod("isPersistent").invoke(entity), "A cosmetic could survive a restart");
                    require(entity.isInvulnerable() && !entity.hasGravity(), "A cosmetic affects physical gameplay");
                }
                location.add(0.2, 0, 0); location.setYaw(45);
                require(show(renderer, context, player, model), "Renewing model failed: " + model);
                step(renderer);
                require(ids.equals(ids(displays())), "Movement spawned ghost copies: " + model);
                for (Entity entity : displays()) require(entity.getLocation().distanceSquared(location) < 0.001, "Model did not follow its anchor");
                renderer.clear(); require(displays().isEmpty(), "Model cleanup leaked: " + model);
            }
            require(shown > 0, "Private display visibility was never granted to the audience");
            checkRenderScenarios(context, player, renderer);

            core.getConfig().set("abilities.effects.object-limit", 0);
            particles = 0; visuals.effect("hammer", context, location);
            require(displays().isEmpty() && particles > 0, "Budget exhaustion did not fall back exclusively");
            core.getConfig().set("abilities.effects.object-limit", 48);
            core.getConfig().set("abilities.effects.objects", false);
            particles = 0; visuals.effect("hammer", context, location);
            require(displays().isEmpty() && particles > 0, "Forced particle mode ignored");
            core.getConfig().set("abilities.effects.objects", true);

            require(show(renderer, context, player, visuals.model("thor", "HAMMER")), "Toggle fixture did not spawn");
            core.getConfig().set("abilities.effects.objects", false); step(renderer);
            require(displays().isEmpty(), "Disabling object effects left a live assembly");
            core.getConfig().set("abilities.effects.objects", true);

            // Airborne wings keep the same entities while moving and disappear on landing.
            AbilityPlayerContext flight = new AbilityPlayerContext(core, player, core.abilities().registry().get("hermes"));
            feedback = visuals.feedback("hermes");
            flying = true; feedback.flight(flight);
            require(displays().size() == 10, "Flight did not create its feather wings");
            Set<UUID> wings = ids(displays());
            location.add(0.1, 0.2, 0); feedback.flight(flight);
            require(wings.equals(ids(displays())), "Flight renewals stacked wings");
            java.lang.reflect.Field owned = AbilityFeedback.class.getDeclaredField("objects"); owned.setAccessible(true);
            ObjectEffects flightRenderer = (ObjectEffects) owned.get(feedback);
            flying = false; step(flightRenderer);
            require(displays().isEmpty(), "Landing did not remove the wings");
            feedback.clear();

            core.getConfig().set("abilities.effects.particles", false);
            particles = 0;
            feedback = visuals.feedback("gaia");
            feedback.impact(new AbilityPlayerContext(core, player, core.abilities().registry().get("gaia")), location);
            require(particles == 0 && !displays().isEmpty(), "Designed roots did not retain objects when particles were disabled");
            feedback.clear();
            core.getConfig().set("abilities.effects.particles", true);

            require(show(renderer, context, player, visuals.model("thor", "HAMMER")), "Expiry fixture did not spawn");
            for (int i = 0; i < 5; i++) step(renderer);
            require(displays().isEmpty(), "Expired scene leaked entities");
            require(show(renderer, context, player, visuals.model("thor", "HAMMER")), "Disconnect fixture did not spawn");
            online = false; step(renderer); require(displays().isEmpty(), "Offline owner left entities"); online = true;
            require(show(renderer, context, player, visuals.model("thor", "HAMMER")), "World fixture did not spawn");
            location.setWorld(null); step(renderer); require(displays().isEmpty(), "Lost anchor left entities"); location.setWorld(world);

            visuals.effect("scales", new AbilityPlayerContext(core, player, core.abilities().registry().get("anubis")), player, 3D);
            require(!displays().isEmpty(), "Target-following scales failed");
            // Making the target invisible restricts its scene to itself; removing visibility completely hides it.
            ObjectEffects privateRenderer = new ObjectEffects();
            visuals.clear();
            final boolean[] visible = {true};
            require(privateRenderer.show("private", context, visuals.model("anubis", "SCALES"), () -> location.clone(),
                () -> visible[0] ? Collections.singletonList(player) : Collections.emptyList(), 12, 3), "Visibility fixture failed");
            visible[0] = false; step(privateRenderer);
            require(hidden > 0, "Existing display stayed visible after its audience changed");
            privateRenderer.clear();

            SpawnBlocker blocker = new SpawnBlocker();
            Bukkit.getPluginManager().registerEvents(blocker, core);
            try {
                ObjectEffects failed = new ObjectEffects();
                require(!show(failed, context, player, visuals.model("thor", "HAMMER")), "Cancelled partial spawn was reported as success");
                require(displays().isEmpty(), "A partially created assembly leaked entities");
            } finally { HandlerList.unregisterAll(blocker); }

            require(show(renderer, context, player, visuals.model("athena", "PHALANX")), "Shutdown fixture failed");
            ObjectEffects.clearAll(); require(displays().isEmpty(), "Global shutdown left cosmetics");
            for (int i = 0; i < 7; i++) require(show(new ObjectEffects(), context, player, visuals.model("athena", "PHALANX")), "Global budget admitted too few displays");
            require(!show(new ObjectEffects(), context, player, visuals.model("athena", "PHALANX")), "Global display limit was exceeded");
            require(displays().size() == 245, "Budget refusal left a partial formation");
            ObjectEffects.clearAll(); require(displays().isEmpty(), "Global budget entities were not reclaimed");
            core.getLogger().info("PASS object effects: all original and action designs, actual Display entities, reuse, visibility, expiry, disconnect, partial-spawn rollback, budgets and exclusive fallback");
        } finally {
            visuals.clear(); renderer.clear(); ObjectEffects.clearAll();
            for (Map.Entry<String, Object> entry : saved.entrySet()) core.getConfig().set("abilities.effects." + entry.getKey(), entry.getValue());
        }
    }

    private void checkRenderScenarios(AbilityPlayerContext context, Player player, ObjectEffects renderer) throws Exception {
        AbilityFeedback body = new AbilityFeedback();
        for (EffectCue cue : Arrays.asList(EffectCue.GUARD, EffectCue.WINGS)) {
            for (float yaw : new float[] {0,90,180,270}) {
                List<Location> reference = null;
                for (float pitch : new float[] {0,-90,90}) {
                    location.setYaw(yaw); location.setPitch(pitch);
                    core.getConfig().set("abilities.effects.objects", true);
                    body.drawCue(context, location, cue, Collections.singletonList(player), player, true);
                    require(!displays().isEmpty(), "Body attachment failed at " + yaw + "/" + pitch);
                    for (Entity display : displays()) {
                        require(display.getLocation().getPitch() == 0, "Body attachment inherited camera pitch");
                        require(Math.abs((display.getLocation().getYaw() - yaw + 540) % 360 - 180) < 0.01, "Body attachment lost its heading");
                    }
                    body.clear();
                    core.getConfig().set("abilities.effects.objects", false); emittedPoints.clear();
                    body.drawCue(context, location, cue, Collections.singletonList(player), player, true);
                    require(!emittedPoints.isEmpty() && emittedPoints.size() <= 64, "Body fallback missing or unbounded");
                    if (reference == null) reference = new ArrayList<Location>(emittedPoints);
                    else {
                        require(reference.size() == emittedPoints.size(), "Camera pitch changed fallback detail");
                        for (int i=0;i<reference.size();i++) require(reference.get(i).distanceSquared(emittedPoints.get(i)) < 0.000001,
                            "Camera pitch moved body fallback through the player");
                    }
                    require(location.getPitch() == pitch, "Renderer mutated the caller's location");
                }
            }
        }
        location.setYaw(0); location.setPitch(0);
        core.getConfig().set("abilities.effects.objects", true);
        core.getConfig().set("abilities.effects.animations", true);
        AbilityPlayerContext roots = new AbilityPlayerContext(core, player, core.abilities().registry().get("gaia"));
        AbilityFeedback design = visuals.feedback("gaia");
        core.getConfig().set("abilities.effects.object-limit", 0); particles=0;
        design.drawCue(roots, location, EffectCue.ROOT, Collections.singletonList(player), player, true);
        require(displays().isEmpty() && particles>0, "Full-budget design did not start in fallback");
        int prior=particles;
        core.getConfig().set("abilities.effects.object-limit", 48); stepDesign(design);
        require(displays().isEmpty() && particles>prior, "Fallback upgraded mid-animation and restarted the pose");
        design.clear(); particles=0;
        design.drawCue(roots, location, EffectCue.ROOT, Collections.singletonList(player), player, true);
        require(!displays().isEmpty() && particles==0, "New design did not use the restored display capacity");
        core.getConfig().set("abilities.effects.objects", false); stepDesign(design);
        require(displays().isEmpty() && particles>0, "Display failure did not continue in fallback");
        core.getConfig().set("abilities.effects.objects", true); stepDesign(design);
        require(displays().isEmpty(), "Restored display setting restarted an in-flight fallback");
        design.clear();

        // Sudden movement, pitch changes and lifecycle events reuse/reclaim the same attachments.
        AbilityFeedback flight=visuals.feedback("hermes");
        AbilityPlayerContext flyingContext=new AbilityPlayerContext(core,player,core.abilities().registry().get("hermes"));
        flying=true; location.setPitch(-90); flight.flight(flyingContext);
        Set<UUID> ids=ids(displays()); location.add(1,0.5,1); location.setYaw(180); location.setPitch(90);
        flight.flight(flyingContext);
        require(ids.equals(ids(displays())), "Moving flight spawned duplicate wings");
        for(Entity display:displays()) require(display.getLocation().getPitch()==0 && display.getLocation().getYaw()==180,
            "Flight camera pitch rotated the attachment");
        flight.clear(); flying=false;
        visuals.effect("scales",new AbilityPlayerContext(core,player,core.abilities().registry().get("anubis")),player,3D);
        for(Entity display:displays()) require(display.getLocation().getPitch()==0,"Judgment scales tipped with the target's camera");
        visuals.clear(); location.setPitch(0); location.setYaw(0);

        require(show(renderer,context,player,SharedModels.SHIELD),"Range fixture did not spawn");
        int beforeHide=hidden,beforeShow=shown;
        viewerOffset=33;step(renderer);require(hidden>beforeHide,"Leaving 32 blocks did not hide existing objects");
        viewerOffset=0;step(renderer);require(shown>beforeShow,"Returning viewer could not see the live scene");
        dead=true;step(renderer);require(displays().isEmpty(),"Dead owner left live displays");dead=false;
        renderer.clear(); body.clear();
        core.getLogger().info("PASS rendering scenarios: four headings, up/down camera, model/fallback body alignment, moving flight, scales, mid-animation budget/settings changes, range and death");
    }

    private void stepDesign(AbilityFeedback feedback) throws Exception {
        Method method=AbilityFeedback.class.getDeclaredMethod("updateDesigns");method.setAccessible(true);method.invoke(feedback);
    }

    public static final class SpawnBlocker implements Listener {
        int calls;
        @EventHandler public void onSpawn(EntitySpawnEvent event) {
            if (event.getEntity().getScoreboardTags().contains("newgodwar_cosmetic") && ++calls == 2) event.setCancelled(true);
        }
    }

    private boolean show(ObjectEffects renderer, AbilityPlayerContext context, Player player, ObjectModel model) {
        return renderer.show("model", context, model, () -> location.clone(), () -> Collections.singletonList(player), 10, 1);
    }
    private void step(ObjectEffects renderer) throws Exception { Method method = ObjectEffects.class.getDeclaredMethod("update"); method.setAccessible(true); method.invoke(renderer); }
    private List<Entity> displays() {
        List<Entity> result = new ArrayList<Entity>();
        for (Entity entity : world.getEntities()) if (entity.getScoreboardTags().contains("newgodwar_cosmetic")) result.add(entity);
        return result;
    }
    private Set<UUID> ids(List<Entity> entities) { Set<UUID> ids = new HashSet<UUID>(); for (Entity entity : entities) ids.add(entity.getUniqueId()); return ids; }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }

    private Player viewer() {
        UUID id = UUID.randomUUID();
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[] {Player.class}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "getUniqueId": return id;
                case "getName": return "ObjectViewer";
                case "getWorld": return world;
                case "getLocation": return location.clone().add(viewerOffset,0,0);
                case "isOnline": return online;
                case "isDead": return dead;
                case "isFlying": return flying;
                case "canSee": return true;
                case "hasPotionEffect": return invisible && args[0].equals(PotionEffectType.INVISIBILITY);
                case "spawnParticle": particles++; emittedPoints.add(((Location)args[1]).clone()); return null;
                case "showEntity": shown++; return null;
                case "hideEntity": hidden++; return null;
                case "equals": return proxy == args[0];
                case "hashCode": return id.hashCode();
                case "toString": return "ObjectViewer";
                default:
                    Class<?> type = method.getReturnType();
                    if (!type.isPrimitive() || type == void.class) return null;
                    if (type == boolean.class) return false;
                    if (type == double.class) return 0D;
                    if (type == float.class) return 0F;
                    if (type == long.class) return 0L;
                    return 0;
            }
        });
    }
}
