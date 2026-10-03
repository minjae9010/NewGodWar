package kr.newgodwar.regression;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.ability.api.AbilityPlayerContext;
import kr.newgodwar.ability.api.AbilityVisuals;
import kr.newgodwar.ability.builtin.BaseAbility;
import kr.newgodwar.ability.feedback.AbilityFeedback;
import kr.newgodwar.ability.feedback.AbilityTheme;
import kr.newgodwar.ability.feedback.AbilityStyle;
import kr.newgodwar.ability.feedback.AbilitySounds;
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
    private final VisualProbe visuals;
    // Construct before installing the recording Server proxy so Paper can convert this class normally.
    private final AbilityVisuals customVisuals = new AbilityVisuals() {
        private final AbilityStyle style = AbilityStyle.builder(AbilityTheme.WATER)
            .normal(EffectCue.ITEM).privateCast().build();
        @Override public AbilityStyle style() { return style; }
    };
    private final Map<String, Integer> particles = new HashMap<String, Integer>();
    private final List<Location> points = new ArrayList<Location>();
    private final List<Particle> kinds = new ArrayList<Particle>();
    private final Map<String, Integer> sounds = new HashMap<String, Integer>();
    private final List<String> messages = new ArrayList<String>();
    private final List<String> bars = new ArrayList<String>();
    private final List<String> titles = new ArrayList<String>();
    private final Map<Integer, Runnable> animations = new LinkedHashMap<Integer, Runnable>();
    private final Map<Integer, Runnable> tasks = new LinkedHashMap<Integer, Runnable>();
    private final List<Player> viewers = new ArrayList<Player>();
    private World world;
    private Player caster;
    private int stones = 100;
    private int nextTask;
    private boolean invisible;
    private boolean targetInvisible;
    private boolean online = true;
    private float yaw, pitch;
    private org.bukkit.potion.PotionEffect existingPotion;

    FeedbackRegressionChecks(NewGodWarPlugin core) { this.core = core; this.visuals = new VisualProbe(core); }

    void run() throws Exception {
        // Paper rewrites legacy plugin bytecode against CraftServer while loading each class.
        visuals.models();
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
        viewers.add(player("Observer", 8, true));

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
            if (m.getName().equals("scheduleSyncRepeatingTask")) { animations.put(++nextTask, (Runnable) a[1]); return nextTask; }
            if (m.getName().equals("cancelTask")) { tasks.remove(a[0]); animations.remove(a[0]); return null; }
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
            // Legacy configs may enable every channel; compact presentation still wins by default.
            core.getConfig().set("abilities.messages.compact", null);
            core.getConfig().set("abilities.messages.success", true);
            core.getConfig().set("abilities.messages.failure", true);
            core.getConfig().set("abilities.messages.timer", true);
            for (String key : Arrays.asList("enabled", "particles", "sounds", "action-bar", "titles"))
                core.getConfig().set("abilities.effects." + key, true);
            core.getConfig().set("abilities.effects.animations", false);
            core.getConfig().set("abilities.effects.objects", false);

            require(ability.cast(context, true), "Advanced activation failed");
            require(stones == 75 && ability.cooldownRemainingMillis(0) > 0, "Feedback changed resource/cooldown accounting");
            require(particles.isEmpty() && count(sounds,"Caster") == 1 && tasks.isEmpty(),
                "Dedicated lightning must acknowledge the cast once without adding particles or a second thunder strike");
            require(count(particles, "Far") == 0 && count(sounds, "Far") == 0 && count(particles, "CannotSee") == 0,
                "Feedback leaked to distant/hidden viewers");
            require(messages.isEmpty() && bars.isEmpty() && titles.isEmpty(),
                "Default activation must not cover combat with titles, descriptions or duplicate chat");
            int castParticles = count(particles, "Caster");
            messages.clear();
            require(!ability.cast(context, true) && !ability.cast(context, true), "Cooldown was bypassed");
            require(stones == 75 && count(particles, "Caster") == castParticles && messages.isEmpty()
                && bars.size() == 1 && contains(bars, "재사용까지"),
                "Failed casts must show one concise notice without duplicate chat or charging resources");

            @SuppressWarnings("unchecked") Map<Integer, Long> cooldowns = (Map<Integer, Long>) field(BaseAbility.class, "cooldowns").get(ability);
            cooldowns.put(0, 0L);
            clearOutput();
            ability.onCountdownTick(context);
            ability.onCountdownTick(context);
            require(messages.isEmpty() && bars.size() == 1 && contains(bars, "고급 능력 준비됨"), "Ready notification missing or duplicated");

            clearOutput(); stones = 0;
            require(!new ProbeAbility().cast(context, false), "Missing resources were accepted");
            require(particles.isEmpty() && titles.isEmpty() && messages.isEmpty() && contains(bars, "부족"), "Resource failure looked like activation");
            clearOutput();
            core.getConfig().set("abilities.effects.action-bar", false);
            require(!new ProbeAbility().cast(context, false), "Missing resources were accepted with the action bar disabled");
            require(bars.isEmpty() && contains(messages, "부족"), "Disabling the action bar lost the failure reason");
            core.getConfig().set("abilities.effects.action-bar", true);
            stones = 100;
            clearOutput();
            require(new ProbeAbility().cast(context("clocking"), false), "Stealth cast failed");
            new AbilityFeedback(visuals.ability("clocking")).status(context("clocking"), caster, "INVISIBILITY");
            flush();
            require(count(particles, "Caster") > 0 && count(particles, "Near") == 0 && count(sounds, "Near") == 0,
                "Stealth activation revealed the caster before invisibility was applied");
            clearOutput(); invisible = true;
            new AbilityFeedback(visuals.ability("zeus")).activated(context, caster, false);
            require(count(particles, "Near") == 0 && count(sounds, "Near") == 0, "Invisible caster leaked cosmetic effects");
            invisible = false;

            clearOutput();
            AbilityFeedback feedback = new AbilityFeedback(visuals.ability("gaia"));
            feedback.affected(context("gaia"), viewers.get(1), "대지 속박 · 7초", true);
            feedback.affected(context("gaia"), viewers.get(1), "대지 속박 · 7초", true);
            flush();
            require(bars.size() == 1 && count(particles, "Near") > 0 && count(particles, "Near") <= 64, "Target feedback missing or unthrottled");
            clearOutput();
            AbilityFeedback otherCaster = new AbilityFeedback(visuals.ability("gaia"));
            otherCaster.affected(context("gaia"), viewers.get(1), "속박", true);
            otherCaster.passive(context("gaia"), "반복 회복");
            feedback.affected(context, viewers.get(4), "회복", false);
            require(bars.isEmpty() && messages.isEmpty(), "Compact mode spammed passive/benefit or multi-caster text");
            feedback.affected(context, viewers.get(1), "축복 · 신속 30초", false, true);
            feedback.affected(context, viewers.get(1), "축복 · 신속 30초", false, true);
            feedback.notice(context, viewers.get(1), "능력 봉인 · 12초", true, true);
            require(bars.size() == 2 && contains(bars, "축복") && contains(bars, "봉인") && messages.isEmpty(),
                "Important buff/status information was hidden by incidental notices or duplicated");
            clearOutput();
            feedback.progress(context, "망치 전하 1/3");
            feedback.progress(context, "망치 전하 2/3");
            require(bars.size() == 2 && bars.get(1).contains("2/3") && messages.isEmpty(),
                "Rapid progress changes left the player with stale charge information");
            clearOutput();
            feedback.timer(context, "이동으로 연주 중단");
            require(bars.size() == 1 && contains(bars, "연주 중단") && messages.isEmpty(),
                "Compact mode hid the reason a channelled ability stopped");
            clearOutput();
            ProbeAbility quietTimer = new ProbeAbility();
            quietTimer.timer(context);
            require(bars.size() == 1 && contains(bars, "3초 후") && messages.isEmpty(), "Effect duration was hidden");
            clearOutput();
            quietTimer.onCountdownTick(context);
            require(bars.isEmpty() && messages.isEmpty(), "Compact mode spammed timer countdowns");
            quietTimer.cancelScheduledTasks();
            clearOutput();
            // A new timer session must still report actual expiration once, without repeating countdowns.
            quietTimer = new ProbeAbility(); quietTimer.timer(context);
            clearOutput(); flush();
            require(contains(bars, "보호 종료") && messages.isEmpty(), "Compact mode hid effect expiration");
            quietTimer.cancelScheduledTasks();
            clearOutput();
            core.getConfig().set("abilities.effects.action-bar", false);
            feedback.progress(context, "다음 룬: 서리");
            feedback.ready(context, true);
            require(bars.isEmpty() && contains(messages, "다음 룬: 서리") && contains(messages, "준비됨"),
                "Disabling the action bar lost essential gameplay state");
            core.getConfig().set("abilities.effects.action-bar", true);
            otherCaster.clear(); feedback.clear();
            core.getConfig().set("abilities.messages.compact", false);
            clearOutput();
            feedback.activated(context, caster, true);
            require(titles.isEmpty() && bars.size() == 1 && contains(bars, "제우스 · 고급")
                && !contains(bars, "번개를 5번") && !messages.isEmpty(),
                "Detailed mode must honor chat settings without restoring titles or long descriptions");
            feedback.clear();
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
            require(count(particles, "Near") > 0 && count(particles, "Observer") == 0 && count(sounds, "Observer") == 0,
                "An affected recipient lost feedback or a bystander saw an invisible caster");
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
            require(timed.cleaned == 2 && contains(bars, "보호 종료"), "Timer completion feedback missing");
            checkAllStylesAndReactions();
            checkFallbackFacing();
            core.getLogger().info("PASS feedback: server particle/sound aliases, cast/failure/ready, resource accounting, visibility, range, toggles, throttle and cleanup");
        } finally {
            visuals.clear();
            serverField.set(null, original);
            nmsField.set(core, oldNms);
            for (Map.Entry<String, Object> entry : oldConfig.entrySet()) core.getConfig().set(entry.getKey(), entry.getValue());
        }
    }

    private void checkFallbackFacing() {
        AbilityFeedback body = new AbilityFeedback();
        for (EffectCue cue : Arrays.asList(EffectCue.GUARD, EffectCue.WINGS)) {
            for (float heading : new float[] {0, 90, 180, 270}) {
                List<Location> reference = null;
                yaw = heading;
                for (float looking : new float[] {0, -90, 90}) {
                    clearOutput(); pitch = looking;
                    body.drawCue(context("hermes"), caster.getLocation(), cue, Collections.singletonList(caster), caster, true);
                    require(!points.isEmpty() && points.size() <= 64, "Missing body outline on this server");
                    if (reference == null) reference = new ArrayList<Location>(points);
                    else {
                        require(reference.size() == points.size(), "Fallback changed with the camera angle");
                        for (int i=0;i<points.size();i++) require(reference.get(i).distanceSquared(points.get(i)) < 0.000001,
                            "Body outline tipped when looking up/down");
                    }
                }
            }
        }
        yaw = 0; pitch = 0; body.clear(); clearOutput();
        core.getLogger().info("PASS fallback facing: shield/wing model outlines stay upright at four headings and three camera pitches");
    }

    private void checkAllStylesAndReactions() {
        core.getConfig().set("abilities.effects.animations", true);
        // A renderer must use its supplied policy, even when the context has a built-in id.
        clearOutput();
        AbilityFeedback custom = new AbilityFeedback(customVisuals);
        custom.activated(context("zeus"), caster, false);
        flush();
        require(count(particles, "Caster") > 0 && count(particles, "Near") == 0,
            "Custom visual policy was replaced by context-id defaults");
        custom.clear();
        for (String id : core.abilities().registry().ids()) {
            for (boolean advanced : new boolean[] {false, true}) {
                clearOutput();
                AbilityFeedback feedback = new AbilityFeedback(visuals.ability(id));
                EffectCue cue = visuals.ability(id).style().cast(advanced);
                feedback.activated(context(id), caster, advanced);
                require(tasks.size() == (cue == EffectCue.NONE ? 0 : 1), "Unexpected cast tasks: " + id);
                flush();
                final int[] expected = {0}; cue.draw((ink, x, y, z, rgb) -> expected[0]++);
                boolean designed = visuals.ability(id).style().effect(cue) != null;
                boolean modelCue = cue == EffectCue.WINGS || cue == EffectCue.GUARD;
                boolean forge = AbilitySounds.forge(visuals.ability(id).style().effect(cue)) || (!designed && cue == EffectCue.FORGE);
                require((designed || modelCue ? count(particles, "Caster") > 0 && count(particles, "Caster") <= 64
                    : count(particles, "Caster") == expected[0]) && tasks.size() == (forge ? 3 : 0), "Cast duplicated: " + id);
                if(forge) {
                    int soundBefore=count(sounds,"Caster");flush();
                    require(count(sounds,"Caster")==soundBefore+3 && tasks.isEmpty(),"Three hammer contacts lost their sound: "+id);
                }
                require(animations.size() == (designed ? 1 : 0), "Unexpected design animation count: " + id);
                require(count(particles, "CannotSee") == 0 && count(particles, "Far") == 0, "Cast visibility leaked: " + id);
                if (visuals.ability(id).style().privateCast()) require(count(particles, "Near") == 0 && count(sounds, "Near") == 0,
                    "Private cast leaked: " + id);
                feedback.clear();
            }
        }
        // The same invocation can reach the activation, potion, damage and message hooks.
        clearOutput();
        AbilityFeedback feedback = new AbilityFeedback(visuals.ability("gaia"));
        AbilityPlayerContext healing = context("gaia"); Player target = viewers.get(1);
        feedback.activated(healing, caster, false);
        feedback.status(healing, target, "SPEED");
        feedback.status(healing, target, "REGENERATION");
        feedback.affected(healing, target, "회복", false);
        feedback.impact(healing, target);
        require(tasks.size() == 1 && particles.isEmpty(), "Reactions did not merge before rendering");
        flush();
        require(animations.size() == 1 && count(particles,"Caster") <= 128,
            "Cast and received roles must share one bounded scheduler");
        // Isolate the recipient scene when checking its anchor and changing recipient visibility.
        feedback.clear(); clearOutput();
        feedback.status(healing, target, "REGENERATION"); flush();
        require(count(particles, "Near") > 0 && count(particles, "Near") <= 64 && animations.size() == 1, "Healing stacked status and impact decoration");
        for (int i = 0; i < points.size(); i++) {
            Location point = points.get(i);
            // Shoulder props may extend beyond the target's body, but must stay anchored to that recipient.
            require(point.distanceSquared(target.getLocation()) < point.distanceSquared(caster.getLocation()),
                "Healing was attached to the caster instead of the target");
        }
        int before = count(particles, "Near");
        int casterBefore = count(particles, "Caster");
        targetInvisible = true;
        for (Runnable animation : new ArrayList<Runnable>(animations.values())) animation.run();
        require(count(particles, "Near") > before && count(particles, "Caster") == casterBefore,
            "In-flight design hid the recipient's own effect or revealed their invisible position");
        targetInvisible = false;
        online = false;
        for (Runnable animation : new ArrayList<Runnable>(animations.values())) animation.run();
        require(animations.isEmpty(), "Design animation survived owner disconnect");
        online = true;
        feedback.clear(); clearOutput();
        ProbeAbility buff = new ProbeAbility();
        buff.buff(healing, 0); flush();
        require(count(particles, "Caster") > 0 && count(particles, "Caster") <= 64 && animations.size() == 1,
            "New regeneration effect must use one bounded healing animation");
        buff.cancelScheduledTasks();
        require(animations.isEmpty(), "Healing animation survived ability cleanup");
        existingPotion = new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION, 100, 0);
        clearOutput(); buff = new ProbeAbility(); buff.buff(healing, 0);
        require(tasks.isEmpty() && particles.isEmpty() && animations.isEmpty(), "Refreshing a maintained buff replayed its cue");
        buff.buff(healing, 1); flush();
        require(count(particles, "Caster") > 0 && count(particles, "Caster") <= 64 && animations.size() == 1,
            "A stronger buff was mistaken for a routine refresh");
        buff.cancelScheduledTasks();
        existingPotion = null; clearOutput();
        yaw = 0;
        feedback.drawCue(healing, caster.getLocation(), EffectCue.ITEM, Collections.singletonList(caster), caster, false);
        List<Location> itemPoints = new ArrayList<Location>(points);
        require(!itemPoints.isEmpty() && itemPoints.size() <= 64 && animations.size() == 1,
            "Item reaction must use one bounded model animation");
        feedback.clear(); clearOutput();
        yaw = 90;
        feedback.drawCue(healing, caster.getLocation(), EffectCue.ITEM, Collections.singletonList(caster), caster, false);
        require(points.size() == itemPoints.size(), "Item model changed when the player turned");
        Location anchor = caster.getLocation();
        for (int i = 0; i < points.size(); i++) {
            Location reference = itemPoints.get(i);
            Location rotated = new Location(world, anchor.getX() - (reference.getZ() - anchor.getZ()),
                reference.getY(), anchor.getZ() + (reference.getX() - anchor.getX()));
            require(points.get(i).distanceSquared(rotated) < 0.000001,
                "Item model did not rotate with the player's facing");
        }
        feedback.clear(); clearOutput();
        feedback = new AbilityFeedback(visuals.ability("sniper"));
        yaw = 0;
        feedback.impact(context("sniper"),target,caster.getLocation()); flush();
        List<Location> incomingPoints = new ArrayList<Location>(points);
        require(!incomingPoints.isEmpty(),"Confirmed recipient impact was missing");
        feedback.clear(); clearOutput(); yaw = 135;
        feedback.impact(context("sniper"),target,caster.getLocation()); flush();
        require(points.size()==incomingPoints.size(),"Victim heading changed impact parts");
        for(int i=0;i<points.size();i++) require(points.get(i).distanceSquared(incomingPoints.get(i))<.000001,
            "Victim heading rotated the incoming impact; it must face the attack source");
        feedback.clear(); clearOutput();
        feedback = new AbilityFeedback(visuals.ability("gaia"));
        feedback.notice(healing,target,"application was blocked",true); flush();
        require(points.isEmpty() && tasks.isEmpty() && animations.isEmpty(),"Notification invented an applied effect");
        yaw = 0; feedback.clear(); clearOutput();
        for (EffectCue cue : EffectCue.values()) {
            feedback.cue(healing, target, cue);
            require(tasks.size() <= 1, "One invocation accumulated reaction tasks");
        }
        feedback.clear(); require(tasks.isEmpty() && animations.isEmpty(), "Session cleanup left queued reactions");

        clearOutput(); feedback.cue(context("hermes"), caster, EffectCue.WINGS);
        invisible = true; flush();
        require(count(particles, "Caster") > 0 && count(particles, "Caster") <= 64 && count(particles, "Near") == 0,
            "Delayed reaction revealed a newly invisible caster or exceeded the model budget");
        invisible = false; feedback.clear(); clearOutput();
        feedback.cue(healing, target, EffectCue.ROOT); online = false; flush();
        require(particles.isEmpty(), "Reaction continued after caster disconnect"); online = true;
        feedback.clear(); clearOutput();
        feedback.cue(healing, target, EffectCue.ROOT); targetInvisible = true; flush();
        require(count(particles, "Near") > 0 && count(particles, "Caster") == 0 && count(particles, "CannotSee") == 0,
            "Invisible recipient lost their own feedback or was revealed to another player"); targetInvisible = false;
        feedback.clear(); clearOutput();
        feedback.cue(healing, target, EffectCue.ROOT);
        World priorWorld = world; world = proxy(World.class, (p, m, a) -> defaultValue(m.getReturnType()));
        flush(); require(particles.isEmpty(), "Reaction crossed a world change"); world = priorWorld;
        feedback.clear(); clearOutput();
        feedback = new AbilityFeedback(visuals.ability("hecate"));
        feedback.affected(context("hecate"), target, "저주", true); flush();
        require(count(particles, "Caster") > 0 && count(particles, "CannotSee") == 0 && count(particles, "Near") > 0,
            "Private caster's applied effect was hidden from its recipient or leaked to bystanders");

        feedback.clear(); clearOutput();
        feedback = new AbilityFeedback(visuals.ability("thor"));
        AbilityPlayerContext thor = context("thor");
        feedback.activated(thor, caster, true);
        feedback.status(thor, target, "SLOWNESS");
        feedback.affected(thor, target, "천둥 강타", true);
        feedback.impact(thor, target);
        require(tasks.isEmpty() && particles.isEmpty(), "Thor stacked generic feedback on his dedicated effect");
        visuals.effect("thunderbolt", thor, target.getLocation());
        require(count(particles, "Caster") == 25, "Thunderbolt was duplicated");
        clearOutput(); visuals.effect("echoSlash", context("echo"), target.getLocation(), 3);
        require(count(particles, "Caster") == 1 && kinds.get(0) == AbilityTheme.ECHO.particle(), "Native crescent stacked into a disc");
        clearOutput(); new AbilityFeedback(visuals.ability("echo")).pulse(context("echo"), target.getLocation(), 3);
        require(!kinds.contains(AbilityTheme.ECHO.particle()), "Echo warning ring still stacks full crescent sprites");
        clearOutput(); visuals.effect("melody", context("pan"), caster.getLocation(), 7, 1, false);
        require(count(particles, "Caster") == 6, "Melody filled the area with overlapping note sprites");
        clearOutput(); visuals.effect("gravityWell", context("graviton"), target.getLocation(), 5, 1);
        require(count(particles, "Caster") == 24, "Gravity streams were missing or duplicated");
        clearOutput(); visuals.effect("frostCage", context("frost"), target.getLocation(), 3);
        require(count(particles, "Caster") <= 36 && count(particles, "Caster") > 0, "Ice cage was unbounded");
        feedback.clear(); clearOutput();
        feedback = new AbilityFeedback(visuals.ability("gaia"));
        feedback.cue(healing, target, EffectCue.ROOT); flush();
        require(animations.size() == 1, "Design did not start its bounded timeline");
        for (int frame = 0; frame < 5; frame++)
            for (Runnable animation : new ArrayList<Runnable>(animations.values())) animation.run();
        require(animations.isEmpty(), "Design timeline exceeded its lifetime");
        feedback.clear(); clearOutput();
        core.getConfig().set("abilities.effects.particles", false);
        feedback.cue(healing, target, EffectCue.HEAL);
        require(tasks.size()==1, "Sound-only reactions must still be coalesced");
        flush();
        require(particles.isEmpty() && count(sounds,"Near")==1 && animations.isEmpty(),"Disabling visuals also disabled reaction audio");
        feedback.clear();clearOutput();
        core.getConfig().set("abilities.effects.sounds",false);
        feedback.cue(healing,target,EffectCue.HEAL);
        require(tasks.isEmpty(),"Fully disabled reactions scheduled work");
        core.getConfig().set("abilities.effects.sounds",true);
        feedback.drawCue(healing,caster.getLocation(),EffectCue.FORGE,Collections.singletonList(caster),caster,false);
        require(tasks.size()==3,"Forge must schedule exactly three contacts");
        feedback.clear();
        require(tasks.isEmpty(),"Cancelled ability left queued hammer sounds");
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
        visuals.effect("hammer", context("thor"), center);
        visuals.effect("thunderbolt", context("thor"), center);
        visuals.effect("huntMark", context("artemis"), viewers.get(1), 2);
        visuals.effect("echoSlash", context("echo"), center, 3);
        visuals.effect("rune", context("runesmith"), center, 3, true);
        visuals.effect("runeBurst", context("runesmith"), center, false);
        visuals.effect("shield", context("hermione"), center, 5);
    }
    private void namedKitEffects(AbilityFeedback feedback, Location center) {
        visuals.effect("clockFace", context("chronos"), center, 6, 2);
        feedback.flock(context("odin"), viewers.get(1), 0, false);
        feedback.spear(context("odin"), center, center.clone().add(0, 1, 5));
        visuals.effect("forge", context("hephaestus"), center, true);
        visuals.effect("phalanx", context("athena"), center, new org.bukkit.util.Vector(0, 0, 1));
        visuals.effect("oath", context("hera"), caster, viewers.get(1));
        visuals.effect("scales", context("anubis"), viewers.get(1), 4);
        feedback.flock(context("queenbee"), viewers.get(1), 0, true);
        visuals.effect("honeycomb", context("queenbee"), center);
        visuals.effect("wings", context("nike"), center, 3);
        visuals.effect("harvest", context("demeter"), center, 3);
        visuals.effect("melody", context("pan"), center, 7, 2, true);
    }
    private void clearOutput() { visuals.clear(); points.clear(); kinds.clear(); particles.clear(); sounds.clear(); messages.clear(); bars.clear(); titles.clear(); }
    private int count(Map<String, Integer> map, String key) { return map.containsKey(key) ? map.get(key) : 0; }
    private boolean contains(List<String> lines, String text) { for (String line : lines) if (line.contains(text)) return true; return false; }

    private Player player(String name, double x, boolean visible) {
        UUID id = UUID.randomUUID();
        PlayerInventory inventory = proxy(PlayerInventory.class, (p, m, a) -> {
            if (m.getName().equals("getStorageContents")) return new ItemStack[] {stones > 0 ? new ItemStack(Material.COBBLESTONE, stones) : null};
            if (m.getName().equals("setItem")) { stones = a[1] == null ? 0 : ((ItemStack) a[1]).getAmount(); return null; }
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
                case "getLocation": case "getEyeLocation": return new Location(world, x, 65, 0, yaw, pitch);
                case "getInventory": return inventory;
                case "isOnline": return !name.equals("Caster") || online;
                case "canSee": return visible;
                case "hasPotionEffect": return (invisible && name.equals("Caster")) || (targetInvisible && name.equals("Near"));
                case "getPotionEffect": return existingPotion != null && existingPotion.getType().equals(a[0]) ? existingPotion : null;
                case "addPotionEffect": existingPotion = (org.bukkit.potion.PotionEffect) a[0]; return true;
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
        private AbilityStyle appearance=AbilityStyle.DEFAULT;
        @Override public AbilityStyle style() { return appearance; }
        boolean cast(AbilityPlayerContext context, boolean advanced) {
            appearance=context.ability().create().style();
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
