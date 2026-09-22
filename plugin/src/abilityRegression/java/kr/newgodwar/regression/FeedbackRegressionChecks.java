package kr.newgodwar.regression;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.ability.api.AbilityPlayerContext;
import kr.newgodwar.ability.builtin.BaseAbility;
import kr.newgodwar.ability.feedback.AbilityFeedback;
import kr.newgodwar.ability.feedback.AbilityTheme;
import kr.newgodwar.ability.feedback.AbilityStyle;
import kr.newgodwar.ability.feedback.EffectCue;
import kr.newgodwar.nms.NmsAdapter;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitScheduler;

import java.lang.reflect.*;
import java.util.*;

/** Executes feedback against the server's real particle/sound registries with recording viewers. */
final class FeedbackRegressionChecks {
    private final NewGodWarPlugin core;
    private final Map<String, Integer> particles = new HashMap<String, Integer>();
    private final List<Location> points = new ArrayList<Location>();
    private final List<Particle> kinds = new ArrayList<Particle>();
    private final Map<String, Integer> sounds = new HashMap<String, Integer>();
    private final List<String> messages = new ArrayList<String>();
    private final List<String> bars = new ArrayList<String>();
    private final List<String> titles = new ArrayList<String>();
    private final Map<Integer, Runnable> tasks = new LinkedHashMap<Integer, Runnable>();
    private final List<Player> viewers = new ArrayList<Player>();
    private World world;
    private Player caster;
    private int stones = 100;
    private int nextTask;
    private boolean invisible;
    private boolean targetInvisible;
    private boolean online = true;
    private float yaw;
    private org.bukkit.potion.PotionEffect existingPotion;

    FeedbackRegressionChecks(NewGodWarPlugin core) { this.core = core; }

    void run() throws Exception {
        // Paper rewrites legacy plugin bytecode against CraftServer while loading each class.
        AbilityStyle.ids();
        for (EffectCue cue : EffectCue.values()) cue.draw((ink, x, y, z, rgb) -> { });
        Class.forName("kr.newgodwar.ability.feedback.AbilityFeedback$Reaction", true, core.getClass().getClassLoader());
        Class.forName("kr.newgodwar.ability.feedback.ColoredDust", true, core.getClass().getClassLoader());
        for (AbilityTheme theme : AbilityTheme.values()) {
            require(theme.particle() != null && theme.particle().getDataType() == Void.class,
                "Unsupported/data-requiring particle: " + theme);
            require(theme.sound() != null, "Unsupported sound: " + theme);
        }
        UUID worldId = UUID.randomUUID();
        world = proxy(World.class, (p, m, a) -> {
            if (m.getName().equals("getPlayers")) return viewers;
            if (m.getName().equals("getUID")) return worldId;
            return defaultValue(m.getReturnType());
        });
        caster = player("Caster", 0, true);
        viewers.add(caster);
        viewers.add(player("Near", 5, true));
        viewers.add(player("Far", 100, true));
        viewers.add(player("CannotSee", 5, false));

        Server original = Bukkit.getServer();
        Field serverField = field(Bukkit.class, "server");
        Field nmsField = field(NewGodWarPlugin.class, "nmsAdapter");
        Object oldNms = nmsField.get(core);
        Map<String, Object> oldConfig = new HashMap<String, Object>();
        for (String key : Arrays.asList("abilities.effects", "abilities.messages", "game.urf.enabled")) {
            Object value = core.getConfig().get(key);
            if (value instanceof org.bukkit.configuration.ConfigurationSection)
                value = new LinkedHashMap<String, Object>(((org.bukkit.configuration.ConfigurationSection) value).getValues(true));
            oldConfig.put(key, value);
        }
        ProbeAbility ability = new ProbeAbility();
        AbilityPlayerContext context = context("zeus");
        BukkitScheduler scheduler = proxy(BukkitScheduler.class, (p, m, a) -> {
            if (m.getName().equals("scheduleSyncDelayedTask")) { tasks.put(++nextTask, (Runnable) a[1]); return nextTask; }
            if (m.getName().equals("cancelTask")) { tasks.remove(a[0]); return null; }
            return invoke(original.getScheduler(), m, a);
        });
        try {
            nmsField.set(core, new NmsAdapter() {
                public String getServerVersion() { return "feedback-probe"; }
                public void sendActionBar(Player p, String text) { bars.add(text); }
                public void sendTitle(Player p, String title, String subtitle, int in, int stay, int out) { titles.add(title); }
            });
            serverField.set(null, proxy(Server.class, (p, m, a) -> {
                if (m.getName().equals("getScoreboardManager")) return null;
                if (m.getName().equals("getScheduler")) return scheduler;
                return invoke(original, m, a);
            }));
            core.getConfig().set("game.urf.enabled", false);
            core.getConfig().set("abilities.messages.enabled", true);
            for (String key : Arrays.asList("enabled", "particles", "sounds", "action-bar", "titles"))
                core.getConfig().set("abilities.effects." + key, true);
            core.getConfig().set("abilities.effects.animations", false);
            core.getConfig().set("abilities.effects.objects", false);

            require(ability.cast(context, true), "Advanced activation failed");
            require(stones == 75 && ability.cooldownRemainingMillis(0) > 0, "Feedback changed resource/cooldown accounting");
            require(particles.isEmpty() && sounds.isEmpty() && tasks.isEmpty(),
                "Native lightning acquired a second cast effect");
            require(count(particles, "Far") == 0 && count(sounds, "Far") == 0 && count(particles, "CannotSee") == 0,
                "Feedback leaked to distant/hidden viewers");
            require(contains(messages, "제우스 · 고급") && contains(messages, "연속 번개") && titles.size() == 1,
                "Advanced slot 0 must use the advanced description/title");
            int castParticles = count(particles, "Caster");
            messages.clear();
            require(!ability.cast(context, true) && !ability.cast(context, true), "Cooldown was bypassed");
            require(stones == 75 && count(particles, "Caster") == castParticles && messages.size() == 1,
                "Failed casts emitted success particles, charged resources, or spammed chat");

            @SuppressWarnings("unchecked") Map<Integer, Long> cooldowns = (Map<Integer, Long>) field(BaseAbility.class, "cooldowns").get(ability);
            cooldowns.put(0, 0L);
            clearOutput();
            ability.onCountdownTick(context);
            ability.onCountdownTick(context);
            require(messages.size() == 1 && contains(messages, "고급 다시 사용 가능"), "Ready notification missing or duplicated");

            clearOutput(); stones = 0;
            require(!new ProbeAbility().cast(context, false), "Missing resources were accepted");
            require(particles.isEmpty() && titles.isEmpty() && contains(messages, "부족"), "Resource failure looked like activation");
            stones = 100;
            clearOutput();
            require(new ProbeAbility().cast(context("clocking"), false), "Stealth cast failed");
            new AbilityFeedback().status(context("clocking"), caster, "INVISIBILITY");
            flush();
            require(count(particles, "Caster") > 0 && count(particles, "Near") == 0 && count(sounds, "Near") == 0,
                "Stealth activation revealed the caster before invisibility was applied");
            clearOutput(); invisible = true;
            new AbilityFeedback().activated(context, caster, false);
            require(count(particles, "Near") == 0 && count(sounds, "Near") == 0, "Invisible caster leaked cosmetic effects");
            invisible = false;

            clearOutput();
            AbilityFeedback feedback = new AbilityFeedback();
            feedback.affected(context("gaia"), viewers.get(1), "대지 속박 · 7초", true);
            feedback.affected(context("gaia"), viewers.get(1), "대지 속박 · 7초", true);
            flush();
            require(bars.size() == 1 && count(particles, "Near") == 24, "Target feedback missing or unthrottled");
            Location from = new Location(world, 0, 65, 0), to = new Location(world, 20, 65, 0);
            clearOutput(); feedback.link(context, from, to);
            require(count(particles, "Near") <= 25 && from.getX() == 0 && to.getX() == 20,
                "Particle path is unbounded or mutated gameplay locations");

            clearOutput(); feedback.spiral(context("graviton"), from, 1000);
            require(count(particles, "Near") == 24 && from.getY() == 65,
                "Spiral must have bounded particles and preserve its center");
            clearOutput(); feedback.sigil(context("runesmith"), from, 1000, AbilityTheme.FROST);
            require(count(particles, "Near") == 64 && from.getX() == 0,
                "Rune sigil must have bounded particles and preserve its center");

            clearOutput(); signatureEffects(feedback, from);
            require(count(particles, "Near") > 150 && count(particles, "Near") < 300 && count(sounds, "Near") == 3,
                "Named ability signatures are missing or unbounded");
            require(count(particles, "Far") == 0 && count(particles, "CannotSee") == 0 && from.getY() == 65,
                "Signature effects ignored visibility/range or mutated their position");
            clearOutput(); namedKitEffects(feedback, from);
            require(count(particles, "Near") > 350 && count(particles, "Near") < 850 && count(sounds, "Near") >= 5,
                "Named kit effects were missing or unbounded");
            require(count(particles, "Far") == 0 && count(particles, "CannotSee") == 0 && from.getY() == 65,
                "Named kit effects leaked visibility/range or changed their anchor");
            clearOutput(); invisible = true; namedKitEffects(feedback, from);
            require(count(particles, "Near") == 0 && count(sounds, "Near") == 0, "Named kits revealed an invisible caster");
            invisible = false;

            clearOutput();
            for (String key : Arrays.asList("particles", "sounds", "action-bar", "titles"))
                core.getConfig().set("abilities.effects." + key, false);
            feedback.activated(context, caster, true);
            require(particles.isEmpty() && sounds.isEmpty() && bars.isEmpty() && titles.isEmpty() && !messages.isEmpty(),
                "Individual cosmetic toggles did not preserve chat settings");
            for (String key : Arrays.asList("particles", "sounds", "action-bar", "titles"))
                core.getConfig().set("abilities.effects." + key, true);
            core.getConfig().set("abilities.effects.enabled", false);
            clearOutput(); feedback.activated(context, caster, true);
            feedback.spiral(context, from, 3);
            feedback.sigil(context, from, 3);
            signatureEffects(feedback, from);
            namedKitEffects(feedback, from);
            require(particles.isEmpty() && sounds.isEmpty() && bars.isEmpty() && titles.isEmpty(), "Master effects toggle ignored");
            core.getConfig().set("abilities.effects.enabled", true);

            clearOutput();
            ProbeAbility timed = new ProbeAbility();
            timed.timer(context);
            clearOutput();
            timed.cancelScheduledTasks();
            require(tasks.isEmpty() && timed.cleaned == 1 && bars.isEmpty(), "Cancellation left tasks or announced normal completion");
            timed.timer(context);
            clearOutput();
            List<Runnable> pending = new ArrayList<Runnable>(tasks.values()); tasks.clear();
            for (Runnable task : pending) task.run();
            require(timed.cleaned == 2 && contains(bars, "보호 종료 완료"), "Timer completion feedback missing");
            checkAllStylesAndReactions();
            core.getLogger().info("PASS feedback: server particle/sound aliases, cast/failure/ready, resource accounting, visibility, range, toggles, throttle and cleanup");
        } finally {
            serverField.set(null, original);
            nmsField.set(core, oldNms);
            for (Map.Entry<String, Object> entry : oldConfig.entrySet()) core.getConfig().set(entry.getKey(), entry.getValue());
        }
    }

    private void checkAllStylesAndReactions() {
        core.getConfig().set("abilities.effects.animations", true);
        for (String id : AbilityStyle.ids()) {
            for (boolean advanced : new boolean[] {false, true}) {
                clearOutput();
                AbilityFeedback feedback = new AbilityFeedback();
                EffectCue cue = AbilityStyle.of(id).cast(advanced);
                feedback.activated(context(id), caster, advanced);
                require(tasks.size() == (cue == EffectCue.NONE ? 0 : 1), "Unexpected cast tasks: " + id);
                flush();
                final int[] expected = {0}; cue.draw((ink, x, y, z, rgb) -> expected[0]++);
                require(count(particles, "Caster") == expected[0] && tasks.isEmpty(), "Cast duplicated: " + id);
                require(count(particles, "CannotSee") == 0 && count(particles, "Far") == 0, "Cast visibility leaked: " + id);
                if (AbilityTheme.privateCast(id)) require(count(particles, "Near") == 0 && count(sounds, "Near") == 0,
                    "Private cast leaked: " + id);
                feedback.clear();
            }
        }
        // The same invocation can reach the activation, potion, damage and message hooks.
        clearOutput();
        AbilityFeedback feedback = new AbilityFeedback();
        AbilityPlayerContext healing = context("gaia"); Player target = viewers.get(1);
        feedback.activated(healing, caster, false);
        feedback.status(healing, target, "SPEED");
        feedback.status(healing, target, "REGENERATION");
        feedback.affected(healing, target, "회복", false);
        feedback.impact(healing, target);
        require(tasks.size() == 1 && particles.isEmpty(), "Reactions did not merge before rendering");
        flush();
        require(count(particles, "Near") == 3 && kinds.size() == 3, "Healing stacked status and impact decoration");
        for (int i = 0; i < points.size(); i++) {
            require(kinds.get(i) == AbilityTheme.HEALING.particle(), "Healing used the wrong native sprite");
            require(Math.abs(points.get(i).getX() - 5) <= 0.4D, "Healing was attached to the caster instead of the target");
        }
        feedback.clear(); clearOutput();
        new ProbeAbility().buff(healing, 0); flush();
        require(count(particles, "Caster") == 3, "New regeneration effect has no healing cue");
        existingPotion = new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION, 100, 0);
        clearOutput(); new ProbeAbility().buff(healing, 0);
        require(tasks.isEmpty() && particles.isEmpty(), "Refreshing a maintained buff replayed its cue");
        new ProbeAbility().buff(healing, 1); flush();
        require(count(particles, "Caster") == 3, "A stronger buff was mistaken for a routine refresh");
        existingPotion = null; clearOutput();
        yaw = 90; feedback.cue(healing, caster, EffectCue.ITEM); flush();
        for (Location point : points) require(point.getX() < -0.3D && point.getZ() < -0.2D,
            "Hand sparks did not rotate with the player's facing/right hand");
        yaw = 0; feedback.clear(); clearOutput();
        for (EffectCue cue : EffectCue.values()) {
            feedback.cue(healing, target, cue);
            require(tasks.size() <= 1, "One invocation accumulated reaction tasks");
        }
        feedback.clear(); require(tasks.isEmpty(), "Session cleanup left queued reactions");

        clearOutput(); feedback.cue(context("hermes"), caster, EffectCue.WINGS);
        invisible = true; flush();
        require(count(particles, "Caster") == 30 && count(particles, "Near") == 0, "Delayed reaction revealed a newly invisible caster");
        invisible = false; feedback.clear(); clearOutput();
        feedback.cue(healing, target, EffectCue.ROOT); online = false; flush();
        require(particles.isEmpty(), "Reaction continued after caster disconnect"); online = true;
        feedback.clear(); clearOutput();
        feedback.cue(healing, target, EffectCue.ROOT); targetInvisible = true; flush();
        require(particles.isEmpty(), "Delayed reaction revealed an invisible target"); targetInvisible = false;
        feedback.clear(); clearOutput();
        feedback.cue(healing, target, EffectCue.ROOT);
        World priorWorld = world; world = proxy(World.class, (p, m, a) -> defaultValue(m.getReturnType()));
        flush(); require(particles.isEmpty(), "Reaction crossed a world change"); world = priorWorld;
        feedback.clear(); clearOutput();
        feedback.affected(context("hecate"), target, "저주", true); flush();
        require(count(particles, "Caster") > 0 && count(particles, "CannotSee") == 0 && count(particles, "Near") == 0,
            "Target effect revealed a private caster");

        feedback.clear(); clearOutput();
        AbilityPlayerContext thor = context("thor");
        feedback.activated(thor, caster, true);
        feedback.status(thor, target, "SLOWNESS");
        feedback.affected(thor, target, "천둥 강타", true);
        feedback.impact(thor, target);
        require(tasks.isEmpty() && particles.isEmpty(), "Thor stacked generic feedback on his dedicated effect");
        feedback.thunderbolt(thor, target.getLocation());
        require(count(particles, "Caster") == 25, "Thunderbolt was duplicated");
        clearOutput(); feedback.echoSlash(context("echo"), target.getLocation(), 3);
        require(count(particles, "Caster") == 1 && kinds.get(0) == AbilityTheme.ECHO.particle(), "Native crescent stacked into a disc");
        clearOutput(); feedback.pulse(context("echo"), target.getLocation(), 3);
        require(!kinds.contains(AbilityTheme.ECHO.particle()), "Echo warning ring still stacks full crescent sprites");
        clearOutput(); feedback.melody(context("pan"), caster.getLocation(), 7, 1, false);
        require(count(particles, "Caster") == 6, "Melody filled the area with overlapping note sprites");
        clearOutput(); feedback.gravityWell(context("graviton"), target.getLocation(), 5, 1);
        require(count(particles, "Caster") == 24, "Gravity streams were missing or duplicated");
        clearOutput(); feedback.frostCage(context("frost"), target.getLocation(), 3);
        require(count(particles, "Caster") <= 36 && count(particles, "Caster") > 0, "Ice cage was unbounded");
        feedback.clear(); clearOutput();
        core.getConfig().set("abilities.effects.particles", false);
        feedback.cue(healing, target, EffectCue.HEAL);
        require(tasks.isEmpty(), "Disabled reactions scheduled work");
        core.getConfig().set("abilities.effects.particles", true);
        core.getConfig().set("abilities.effects.animations", false);
        core.getLogger().info("PASS 93 ability styles: 186 casts, native action cues, coalescing, dedicated effects, target anchors, cancellation and stealth");
    }

    private void flush() {
        List<Runnable> pending = new ArrayList<Runnable>(tasks.values()); tasks.clear();
        for (Runnable task : pending) task.run();
    }

    private AbilityPlayerContext context(String id) { return new AbilityPlayerContext(core, caster, core.abilities().registry().get(id)); }
    private void signatureEffects(AbilityFeedback feedback, Location center) {
        feedback.hammer(context("thor"), center);
        feedback.thunderbolt(context("thor"), center);
        feedback.huntMark(context("artemis"), viewers.get(1), 2);
        feedback.echoSlash(context("echo"), center, 3);
        feedback.rune(context("runesmith"), center, 3, true);
        feedback.runeBurst(context("runesmith"), center, false);
        feedback.shield(context("hermione"), center, 5);
    }
    private void namedKitEffects(AbilityFeedback feedback, Location center) {
        feedback.clockFace(context("chronos"), center, 6, 2);
        feedback.flock(context("odin"), viewers.get(1), 0, false);
        feedback.spear(context("odin"), center, center.clone().add(0, 1, 5));
        feedback.forge(context("hephaestus"), center, true);
        feedback.phalanx(context("athena"), center, new org.bukkit.util.Vector(0, 0, 1));
        feedback.oath(context("hera"), caster, viewers.get(1));
        feedback.scales(context("anubis"), viewers.get(1), 4);
        feedback.flock(context("queenbee"), viewers.get(1), 0, true);
        feedback.honeycomb(context("queenbee"), center);
        feedback.wings(context("nike"), center, 3);
        feedback.harvest(context("demeter"), center, 3);
        feedback.melody(context("pan"), center, 7, 2, true);
    }
    private void clearOutput() { points.clear(); kinds.clear(); particles.clear(); sounds.clear(); messages.clear(); bars.clear(); titles.clear(); }
    private int count(Map<String, Integer> map, String key) { return map.containsKey(key) ? map.get(key) : 0; }
    private boolean contains(List<String> lines, String text) { for (String line : lines) if (line.contains(text)) return true; return false; }

    private Player player(String name, double x, boolean visible) {
        UUID id = UUID.randomUUID();
        PlayerInventory inventory = proxy(PlayerInventory.class, (p, m, a) -> {
            if (m.getName().equals("contains")) return stones >= ((Number) a[1]).intValue();
            if (m.getName().equals("removeItem")) {
                for (ItemStack item : (ItemStack[]) a[0]) stones -= item.getAmount();
                return new HashMap<Integer, ItemStack>();
            }
            return defaultValue(m.getReturnType());
        });
        return proxy(Player.class, (p, m, a) -> {
            switch (m.getName()) {
                case "getUniqueId": return id;
                case "getName": return name;
                case "getWorld": return world;
                case "getLocation": case "getEyeLocation": return new Location(world, x, 65, 0, yaw, 0);
                case "getInventory": return inventory;
                case "isOnline": return !name.equals("Caster") || online;
                case "canSee": return visible;
                case "hasPotionEffect": return (invisible && name.equals("Caster")) || (targetInvisible && name.equals("Near"));
                case "getPotionEffect": return existingPotion != null && existingPotion.getType().equals(a[0]) ? existingPotion : null;
                case "sendMessage": if (a[0] instanceof String) messages.add((String) a[0]); return null;
                case "spawnParticle":
                    Particle particle = (Particle) a[0];
                    if (name.equals("Caster")) { points.add(((Location) a[1]).clone()); kinds.add(particle); }
                    if (particle.getDataType() != Void.class)
                        require(a.length >= 8 && particle.getDataType().isInstance(a[7]), "Invalid particle data: " + particle);
                    particles.put(name, count(particles, name) + Math.max(1, ((Number) a[2]).intValue())); return null;
                case "playSound": sounds.put(name, count(sounds, name) + 1); return null;
                default: return defaultValue(m.getReturnType());
            }
        });
    }

    private static final class ProbeAbility extends BaseAbility {
        int cleaned;
        boolean cast(AbilityPlayerContext context, boolean advanced) {
            return advanced ? useAdvanced(context, context.player(), 0) : useNormal(context, context.player());
        }
        void timer(AbilityPlayerContext context) { laterCleanup(context, 3, "보호 종료", "보호 종료", () -> cleaned++); }
        void buff(AbilityPlayerContext context, int amplifier) {
            effect(context, context.player(), org.bukkit.potion.PotionEffectType.REGENERATION, 6, amplifier);
        }
    }

    private static Field field(Class<?> type, String name) throws Exception { Field f = type.getDeclaredField(name); f.setAccessible(true); return f; }
    private static Object invoke(Object target, Method method, Object[] args) throws Throwable {
        try { return method.invoke(target, args); } catch (InvocationTargetException ex) { throw ex.getCause(); }
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (p, m, a) -> {
            if (m.getName().equals("equals")) return p == a[0];
            if (m.getName().equals("hashCode")) return System.identityHashCode(p);
            if (m.getName().equals("toString")) return type.getSimpleName() + "FeedbackFixture";
            return handler.invoke(p, m, a);
        }));
    }
    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive() || type == void.class) return null;
        if (type == boolean.class) return false;
        if (type == double.class) return 0D;
        if (type == float.class) return 0F;
        if (type == long.class) return 0L;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == char.class) return (char) 0;
        return 0;
    }
}
