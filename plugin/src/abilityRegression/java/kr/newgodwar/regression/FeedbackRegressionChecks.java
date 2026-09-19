package kr.newgodwar.regression;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.ability.api.AbilityPlayerContext;
import kr.newgodwar.ability.builtin.BaseAbility;
import kr.newgodwar.ability.feedback.AbilityFeedback;
import kr.newgodwar.ability.feedback.AbilityTheme;
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

    FeedbackRegressionChecks(NewGodWarPlugin core) { this.core = core; }

    void run() throws Exception {
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

            require(ability.cast(context, true), "Advanced activation failed");
            require(stones == 75 && ability.cooldownRemainingMillis(0) > 0, "Feedback changed resource/cooldown accounting");
            require(count(particles, "Caster") > 0 && count(particles, "Near") > 0 && count(sounds, "Near") > 0,
                "Activation missing local particles or sound");
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
            require(count(particles, "Caster") > 0 && count(particles, "Near") == 0 && count(sounds, "Near") == 0,
                "Stealth activation revealed the caster before invisibility was applied");
            clearOutput(); invisible = true;
            new AbilityFeedback().activated(context, caster, false);
            require(count(particles, "Near") == 0 && count(sounds, "Near") == 0, "Invisible caster leaked cosmetic effects");
            invisible = false;

            clearOutput();
            AbilityFeedback feedback = new AbilityFeedback();
            feedback.affected(context, viewers.get(1), "시간 정지 · 7초", true);
            feedback.affected(context, viewers.get(1), "시간 정지 · 7초", true);
            require(bars.size() == 1 && count(particles, "Near") == 12, "Target feedback missing or unthrottled");
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
            core.getLogger().info("PASS feedback: server particle/sound aliases, cast/failure/ready, resource accounting, visibility, range, toggles, throttle and cleanup");
        } finally {
            serverField.set(null, original);
            nmsField.set(core, oldNms);
            for (Map.Entry<String, Object> entry : oldConfig.entrySet()) core.getConfig().set(entry.getKey(), entry.getValue());
        }
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
    private void clearOutput() { particles.clear(); sounds.clear(); messages.clear(); bars.clear(); titles.clear(); }
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
                case "getLocation": case "getEyeLocation": return new Location(world, x, 65, 0);
                case "getInventory": return inventory;
                case "isOnline": return true;
                case "canSee": return visible;
                case "hasPotionEffect": return invisible && name.equals("Caster");
                case "sendMessage": if (a[0] instanceof String) messages.add((String) a[0]); return null;
                case "spawnParticle": particles.put(name, count(particles, name) + ((Number) a[2]).intValue()); return null;
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
