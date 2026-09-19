package kr.newgodwar.regression;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.ability.AbilitySession;
import kr.newgodwar.ability.api.*;
import kr.newgodwar.game.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.*;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitScheduler;

import java.lang.reflect.*;
import java.util.*;

/** Deterministic combat and lifecycle checks against real Paper registries and plugin logic. */
final class AbilityVarietyChecks {
    private final NewGodWarPlugin core;
    private final List<Actor> actors = new ArrayList<Actor>();
    private final Map<Integer, Task> tasks = new LinkedHashMap<Integer, Task>();
    private Map<UUID, GodTeam> teams;
    private Map<UUID, AbilitySession> assignments;
    private World world;
    private Actor caster, enemy, ally, outsider, far;
    private GodAbility ability;
    private AbilityPlayerContext context;
    private long tick;
    private int nextTask;
    private boolean safeFloor = true;
    private boolean anchorObstructed;

    AbilityVarietyChecks(NewGodWarPlugin core) { this.core = core; }

    @SuppressWarnings("unchecked")
    void run() throws Exception {
        Server original = Bukkit.getServer();
        Field server = field(Bukkit.class, "server");
        teams = (Map<UUID, GodTeam>) field(GameManager.class, "teams").get(core.game());
        assignments = (Map<UUID, AbilitySession>) field(core.abilities().getClass(), "assignments").get(core.abilities());
        Map<String, Object> config = new LinkedHashMap<String, Object>();
        for (String key : Arrays.asList("abilities.effects.enabled", "game.killtime-seconds", "game.killtime-mode", "game.urf.enabled"))
            config.put(key, core.getConfig().get(key));
        UUID worldId = UUID.randomUUID();
        world = proxy(World.class, (p, m, a) -> {
            switch (m.getName()) {
                case "getUID": return worldId;
                case "getPlayers": return players();
                case "getNearbyEntities": return new ArrayList<Entity>(players());
                case "getBlockAt": return air(a.length == 1 ? (Location) a[0]
                    : new Location(world, ((Number) a[0]).intValue(), ((Number) a[1]).intValue(), ((Number) a[2]).intValue()));
                default: return defaultValue(m.getReturnType());
            }
        });
        caster = new Actor("VarietyCaster"); enemy = new Actor("VarietyEnemy"); ally = new Actor("VarietyAlly");
        outsider = new Actor("VarietyOutsider"); far = new Actor("VarietyFar");
        // Load legacy event/material adapters before temporarily replacing CraftServer.
        new PlayerInteractEvent(caster.player, Action.LEFT_CLICK_AIR, caster.held, null, BlockFace.SELF);
        attackEvent(caster.player, enemy.player, 4);
        new AbilityDamageContext(core, caster.player, enemy.player, core.abilities().registry().get("thor"),
            attackEvent(caster.player, enemy.player, 4));
        new Task(() -> { }, 0, 0);
        new PlayerMoveEvent(caster.player, caster.location, caster.location);
        new AbilityKillContext(core, caster.player, enemy.player, core.abilities().registry().get("nike"), deathEvent(enemy.player));
        Class.forName("kr.newgodwar.ability.builtin.RunesmithAbility$Rune", true, core.getClass().getClassLoader());
        Arrow arrow = proxy(Arrow.class, (p, m, a) -> defaultValue(m.getReturnType()));
        for (String id : Arrays.asList("thor", "artemis", "hermione", "graviton", "echo", "runesmith",
            "chronos", "odin", "hephaestus", "athena", "hera", "anubis", "queenbee", "nike", "demeter", "pan")) {
            require(core.abilities().registry().get(id) != null, "Missing registered ability " + id);
            core.abilities().registry().get(id).create();
        }
        BukkitScheduler scheduler = proxy(BukkitScheduler.class, (p, m, a) -> {
            if (m.getName().equals("scheduleSyncDelayedTask") || m.getName().equals("scheduleSyncRepeatingTask")) {
                long period = a.length > 3 ? ((Number) a[3]).longValue() : 0;
                tasks.put(++nextTask, new Task((Runnable) a[1], tick + Math.max(1L, ((Number) a[2]).longValue()), period));
                return nextTask;
            }
            if (m.getName().equals("cancelTask")) { tasks.remove(a[0]); return null; }
            return invoke(original.getScheduler(), m, a);
        });
        try {
            server.set(null, proxy(Server.class, (p, m, a) -> {
                if (m.getName().equals("getScheduler")) return scheduler;
                if (m.getName().equals("getScoreboardManager")) return null;
                if (m.getName().equals("getOnlinePlayers")) return players();
                if (m.getName().equals("getPlayer") || m.getName().equals("getPlayerExact")) {
                    for (Actor actor : actors) if (actor.id.equals(a[0]) || actor.name.equals(a[0])) return actor.player;
                    return null;
                }
                return invoke(original, m, a);
            }));
            checkArtemis(arrow);
            checkThor();
            checkEcho();
            checkRunes();
            checkGravity();
            checkHermione();
            checkChronos();
            checkOdin();
            checkForge();
            checkAthena();
            checkHera();
            checkAnubis();
            checkQueenBee();
            checkNike();
            checkDemeter();
            checkPan();
            checkNamedKitCleanup();
            core.getLogger().info("PASS ability variety: sixteen kits, rewind safety, parry/riposte, oath, judgment, swarms, laurels, harvest, interrupted music, team/killtime protection, cosmetics and cleanup");
        } finally {
            if (ability != null) ability.cancelScheduledTasks();
            server.set(null, original);
            for (Actor actor : actors) { teams.remove(actor.id); assignments.remove(actor.id); }
            for (Map.Entry<String, Object> entry : config.entrySet()) core.getConfig().set(entry.getKey(), entry.getValue());
        }
    }

    private void checkArtemis(Arrow arrow) throws Exception {
        reset("artemis");
        require(arrowHit(arrow, enemy, false) == 4 && arrowHit(arrow, enemy, false) == 4
            && arrowHit(arrow, enemy, false) == 8, "Artemis must reward the third arrow deterministically");
        require(ability.activeTimerLines().isEmpty(), "Completed hunt retained its marks");
        arrowHit(arrow, enemy, true);
        require(ability.activeTimerLines().isEmpty(), "Cancelled arrow added a mark");
        teams.put(outsider.id, GodTeam.BLUE);
        arrowHit(arrow, enemy, false); arrowHit(arrow, outsider, false);
        require(arrowHit(arrow, enemy, false) == 4, "Changing prey preserved old marks");
        field(ability.getClass(), "expiresAt").setLong(ability, 0);
        require(arrowHit(arrow, enemy, false) == 4, "Expired hunt completed");
        reset("artemis"); right();
        require(caster.stones == 180 && arrowHit(arrow, enemy, false) == 8, "Active hunt did not prime two marks");
        ability.cancelScheduledTasks();
        require(arrowHit(arrow, enemy, false) == 4, "Hunt survived removal");
        advance(160);
        require(tasks.isEmpty() && ability.activeTimerLines().isEmpty(), "Moving hunt marker never expired");
    }

    private double arrowHit(Arrow arrow, Actor victim, boolean cancelled) {
        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(arrow, victim.player, EntityDamageEvent.DamageCause.PROJECTILE, 4);
        event.setCancelled(cancelled);
        core.abilities().handleProjectileHit(caster.player, victim.player, event);
        return event.getDamage();
    }

    private void checkThor() throws Exception {
        reset("thor"); right();
        require(caster.stones == 200 && ability.cooldownRemainingMillis(2) == 0, "Empty charge cast consumed resources");
        left();
        require(enemy.damage == 0, "Thrown hammer hit before reaching its target");
        ability.clearCooldowns(); left();
        require(caster.stones == 188 && tasks.size() == 12, "A second hammer was thrown before the first returned");
        advance(12);
        require(enemy.damage == 5 && caster.stones == 188, "Hammer hit or cost differs");
        right();
        require(caster.stones == 188, "Hammer granted charge before returning");
        advance(12);
        right();
        require(enemy.damage == 10 && ally.damage == 0 && outsider.damage == 0 && far.damage == 0,
            "Charged shockwave damaged invalid targets or ignored its charge");
        reset("thor"); caster.held = new ItemStack(Material.IRON_AXE);
        EntityDamageByEntityEvent event = melee(enemy, 4);
        require(Math.abs(event.getDamage() - 4.6D) < 0.001D, "Axe passive missing");
        melee(enemy, 4);
        require(ability.activeTimerLines().get(0).contains("1/3"), "Axe generated more than one charge per second");
        ability.cancelScheduledTasks();
        require(ability.activeTimerLines().get(0).contains("0/3"), "Thor charge survived deactivation");
        reset("thor"); left(); enemy.location.setX(3); advance(24);
        require(enemy.damage == 0 && tasks.isEmpty() && ability.activeTimerLines().get(0).contains("1/3"),
            "Dodged hammer still hit or failed to return");
        reset("thor"); left(); ability.onDeath(context, deathEvent(caster.player)); advance(24);
        require(enemy.damage == 0 && tasks.isEmpty(), "Hammer survived its owner's death");
    }

    private void checkEcho() throws Exception {
        reset("echo"); right();
        require(caster.stones == 200, "Echo without a record charged resources");
        left(); caster.held = new ItemStack(Material.IRON_SWORD); melee(enemy, 6); advance(15);
        require(enemy.damage == 3 && tasks.isEmpty(), "Echo repeated recursively or used wrong damage");
        right(); advance(20);
        require(enemy.damage == 5, "Echo released all three slashes at once");
        advance(20);
        require(enemy.damage == 9 && ally.damage == 0 && outsider.damage == 0, "Echo burst hit an invalid target");
        reset("echo"); left(); caster.held = new ItemStack(Material.IRON_SWORD); melee(enemy, 6);
        enemy.location.setZ(6); advance(15);
        require(enemy.damage == 0, "Escaped target took echo damage");
        reset("echo"); caster.held = new ItemStack(Material.IRON_SWORD); melee(enemy, 6);
        right(); advance(20); enemy.location.setZ(12); advance(20);
        require(enemy.damage == 2 && tasks.isEmpty(), "Later echo slashes followed an escaping target");
        reset("echo"); left(); caster.held = new ItemStack(Material.IRON_SWORD); melee(enemy, 6);
        teams.put(enemy.id, GodTeam.RED); advance(15);
        require(enemy.damage == 0, "Echo damaged a new ally");
        reset("echo"); left(); caster.held = new ItemStack(Material.IRON_SWORD); melee(enemy, 6);
        ability.onDeath(context, deathEvent(caster.player));
        advance(20);
        require(enemy.damage == 0 && ability.activeTimerLines().isEmpty() && tasks.isEmpty(), "Death retained echo tasks/state");
    }

    private void checkRunes() throws Exception {
        reset("runesmith"); enemy.location.setZ(2);
        for (int i = 0; i < 3; i++) { ability.clearCooldowns(); left(); }
        int stones = caster.stones;
        ability.clearCooldowns(); left();
        require(caster.stones == stones && tasks.size() == 3, "Fourth rune was accepted/charged");
        right();
        require(enemy.damage == 0, "Runes detonated without warning");
        advance(10);
        require(enemy.damage == 8 && ally.damage == 0 && outsider.damage == 0 && tasks.isEmpty(), "Rune overlap cap/protection/cleanup failed");
        reset("runesmith"); caster.sneaking = true; left();
        require(caster.stones == 200 && ability.cooldownRemainingMillis(1) == 0, "Mode switching charged resources");
        caster.sneaking = false; left(); right(); advance(10);
        require(enemy.damage == 2 && !enemy.effects.isEmpty(), "Frost rune did not preserve its mode");
        reset("runesmith"); left(); advance(400);
        require(tasks.isEmpty() && ability.activeTimerLines().get(0).contains("0/3"), "Rune did not expire");
        reset("runesmith"); left(); right(); ability.cancelScheduledTasks(); advance(10);
        require(tasks.isEmpty() && enemy.damage == 0, "Removed runes still detonated");
    }

    private void checkGravity() throws Exception {
        reset("graviton"); left();
        int stones = caster.stones;
        ability.clearCooldowns(); left();
        require(caster.stones == stones && tasks.size() == 9, "URF/cleared cooldown stacked gravity wells");
        enemy.location.setZ(14); advance(10);
        require(enemy.velocities > 0 && ally.velocities == 0 && outsider.velocities == 0, "Gravity pull/protection failed");
        require(enemy.lastVelocity.getY() > 0, "Gravity ignored the vertical direction of its core");
        int previous = enemy.velocities; teams.put(enemy.id, GodTeam.RED); advance(70);
        require(enemy.velocities == previous && tasks.isEmpty(), "Gravity failed to revalidate a new ally");
        reset("graviton"); right(); enemy.location.setZ(20); advance(20);
        require(enemy.damage == 0, "Escaped target took repulsion damage");
        reset("graviton"); right();
        core.getConfig().set("game.killtime-seconds", 300);
        core.getConfig().set("game.killtime-mode", "player-combat");
        field(GameManager.class, "runningStartedAtMillis").setLong(core.game(), System.currentTimeMillis());
        advance(20);
        require(enemy.damage == 0 && enemy.velocities == 0, "Repulsion bypassed killtime");
        reset("graviton"); right(); core.getConfig().set("abilities.effects.enabled", true); advance(20);
        require(enemy.damage == 5 && enemy.velocities == 1 && caster.particles > 0, "Cosmetics or repulsion failed");
        reset("graviton"); right(); caster.online = false; advance(20);
        require(enemy.damage == 0, "Offline caster dealt delayed damage");
    }

    private void checkHermione() throws Exception {
        reset("hermione"); caster.health = 10; ally.health = 12; enemy.health = 10;
        ability.onChatMessage(context, "Reparo");
        require(caster.stones == 200 && ability.cooldownRemainingMillis(1) == 0, "Repair without damaged gear consumed resources");
        caster.held = new ItemStack(Material.IRON_SWORD);
        caster.held.setDurability((short) 70);
        org.bukkit.inventory.meta.ItemMeta meta = caster.held.getItemMeta();
        meta.setDisplayName("수리 보존 검사"); meta.setLore(Arrays.asList("유지해야 하는 설명"));
        caster.held.setItemMeta(meta);
        ally.armor[2] = new ItemStack(Material.IRON_CHESTPLATE);
        ally.armor[2].setDurability((short) 15);
        enemy.held = new ItemStack(Material.IRON_SWORD); enemy.held.setDurability((short) 70);
        ability.onChatMessage(context, " /Reparo ");
        require(caster.held.getDurability() == 30 && ally.armor[2].getDurability() == 0 && enemy.held.getDurability() == 70,
            "Reparo failed to repair/clamp allied equipment or repaired an enemy");
        require(caster.health == 10 && ally.health == 12 && enemy.health == 10, "Reparo healed health instead of repairing equipment");
        require("수리 보존 검사".equals(caster.held.getItemMeta().getDisplayName()) && caster.held.getItemMeta().hasLore(),
            "Reparo stripped item metadata");
        int stones = caster.stones;
        ability.onChatMessage(context, "Finite");
        require(caster.stones == stones, "Support spells did not share a cooldown");
        ability.clearCooldowns();
        ally.effects.put(PotionEffectType.POISON, new PotionEffect(PotionEffectType.POISON, 100, 0));
        ability.onChatMessage(context, "피니테");
        require(!ally.effects.containsKey(PotionEffectType.POISON), "Finite did not cleanse an ally");
        ability.onChatMessage(context, "Protego"); advance(1);
        require(!ally.effects.isEmpty() && enemy.effects.isEmpty(), "Protego protected enemies or missed allies");
        stones = caster.stones; ability.clearCooldowns(); ability.onChatMessage(context, "프로테고");
        require(caster.stones == stones, "Existing shield was stacked/charged");
        ally.effects.clear(); ally.location.setX(20); advance(20);
        require(ally.effects.isEmpty(), "Protego followed an ally outside its fixed circle");
        ability.cancelScheduledTasks();
        require(tasks.isEmpty(), "Protego survived deactivation");
    }

    private void checkChronos() throws Exception {
        reset("chronos"); safeFloor = false; left();
        require(caster.stones == 200 && tasks.isEmpty(), "Chronos recorded an unsafe/airborne anchor");
        safeFloor = true; left();
        caster.location.setX(5); caster.health = 10;
        left();
        require(caster.location.getX() == 0 && caster.health == 14 && caster.stones == 186 && tasks.isEmpty(),
            "Rewind missed its anchor, healed too much, or charged twice");
        require(ability.cooldownRemainingMillis(1) > 0, "Rewind reset its cooldown");
        reset("chronos"); caster.health = 10; left(); caster.health = 16; caster.location.setX(5); left();
        require(caster.health == 16, "Rewind removed health acquired after recording");
        reset("chronos"); left(); safeFloor = false; caster.health = 10; caster.location.setX(5); left();
        require(caster.location.getX() == 5 && caster.health == 10, "Rewind entered a newly unsafe anchor");
        safeFloor = true; caster.teleportAllowed = false; left();
        require(caster.health == 10, "Cancelled teleport still healed the caster");
        advance(100);
        require(tasks.isEmpty() && ability.activeTimerLines().isEmpty(), "Anchor never expired");
        reset("chronos"); caster.location.setX(0.9D); left();
        caster.location.setX(5); caster.health = 10; anchorObstructed = true; left();
        require(caster.location.getX() == 5 && caster.health == 10, "Rewind clipped into a block beside its anchor");
        reset("chronos"); right(); advance(1);
        require(!enemy.effects.isEmpty() && ally.effects.isEmpty() && far.effects.isEmpty(), "Time field ignored teams/radius");
        enemy.effects.clear(); enemy.location.setZ(10); advance(59);
        require(enemy.effects.isEmpty() && tasks.isEmpty(), "Time field followed a departing target");
    }

    private void checkOdin() throws Exception {
        reset("odin"); right();
        require(caster.stones == 200, "Gungnir without a raven mark charged resources");
        left(); ability.clearCooldowns(); left();
        require(caster.stones == 190 && tasks.size() == 1, "Raven scouting stacked");
        right(); enemy.location.setX(4); advance(12);
        require(enemy.damage == 7 && tasks.isEmpty(), "Gungnir failed to track visible moving prey");
        reset("odin"); left(); right(); caster.lineOfSight = false; advance(12);
        require(enemy.damage == 0, "Gungnir pierced cover");
        reset("odin"); left(); right(); teams.put(enemy.id, GodTeam.RED); advance(12);
        require(enemy.damage == 0, "Gungnir attacked a newly allied target");
        reset("odin"); left(); advance(161);
        require(tasks.isEmpty() && ability.activeTimerLines().isEmpty(), "Ravens did not expire");
    }

    private void checkForge() throws Exception {
        reset("hephaestus"); right();
        require(caster.stones == 200, "Cold forge charged for tempering");
        left(); ability.clearCooldowns(); left();
        require(caster.stones == 190 && melee(enemy, 4).getDamage() == 4, "Forge stacked or empowered its staff");
        caster.held = new ItemStack(Material.IRON_SWORD);
        for (int i = 0; i < 3; i++) require(melee(enemy, 4).getDamage() == 6, "Forged strike lacked bonus damage");
        require(melee(enemy, 4).getDamage() == 4 && tasks.isEmpty(), "Forge exceeded three charges");
        reset("hephaestus"); left(); right();
        PotionEffect absorption = caster.effects.get(PotionEffectType.ABSORPTION);
        require(absorption != null && absorption.getAmplifier() == 2 && absorption.getDuration() == 120 && tasks.isEmpty(),
            "Tempering did not consume the full heat into bounded absorption");
        EntityDamageEvent fire = new EntityDamageEvent(caster.player, EntityDamageEvent.DamageCause.LAVA, 4);
        ability.onGenericDamage(context, fire);
        require(fire.isCancelled(), "Forge lost fire immunity");
        EntityDamageEvent drowning = new EntityDamageEvent(caster.player, EntityDamageEvent.DamageCause.DROWNING, 4);
        ability.onGenericDamage(context, drowning);
        require(drowning.getDamage() == 8, "Forge lost its drowning tradeoff");
        reset("hephaestus"); left(); advance(160); caster.held = new ItemStack(Material.IRON_AXE);
        require(melee(enemy, 4).getDamage() == 4 && tasks.isEmpty(), "Expired forge still empowered attacks");
    }

    private EntityDamageByEntityEvent incoming(Actor attacker, double amount, boolean cancelled) {
        EntityDamageByEntityEvent event = attackEvent(attacker.player, caster.player, amount);
        event.setCancelled(cancelled);
        core.abilities().handleDamage(attacker.player, caster.player, event);
        return event;
    }

    private void checkAthena() throws Exception {
        reset("athena"); left();
        incoming(enemy, 4, true);
        require(Math.abs(incoming(enemy, 4, false).getDamage() - 1.6D) < 0.001D, "Aegis failed after a cancelled hit");
        require(incoming(enemy, 4, false).getDamage() == 4, "Aegis blocked multiple attacks");
        require(melee(enemy, 4).getDamage() == 7 && melee(enemy, 4).getDamage() == 4 && tasks.isEmpty(),
            "Aegis riposte was missing or repeatable");
        reset("athena"); left(); advance(80);
        require(incoming(enemy, 4, false).getDamage() == 4, "Expired Aegis still blocked");
        reset("athena"); right(); ally.location.setZ(3); advance(1);
        require(!ally.effects.isEmpty() && enemy.effects.isEmpty(), "Phalanx missed allied front line");
        ally.effects.clear(); ally.location.setZ(-3); caster.location.setYaw(180); advance(20);
        require(ally.effects.isEmpty(), "Phalanx rotated after casting or protected its rear");
        advance(99); require(tasks.isEmpty(), "Phalanx never ended");
    }

    private void checkHera() throws Exception {
        reset("hera"); right();
        require(caster.stones == 200, "Hera without a bond charged resources");
        ally.location.setZ(3); caster.health = 10; ally.health = 12; enemy.health = 10;
        left(); advance(1);
        require(!caster.effects.isEmpty() && !ally.effects.isEmpty() && enemy.effects.isEmpty(), "Oath protection failed");
        right();
        require(caster.health == 14 && ally.health == 16 && enemy.health == 10 && caster.stones == 168 && tasks.isEmpty(),
            "Oath completion did not heal both partners/consume its state");
        reset("hera"); ally.location.setZ(3); left(); ally.location.setZ(10); advance(1);
        require(tasks.isEmpty() && ability.activeTimerLines().isEmpty(), "Oath survived separation");
        reset("hera"); ally.location.setZ(3); left(); teams.put(ally.id, GodTeam.BLUE); right();
        require(caster.stones == 188 && ally.effects.isEmpty() && tasks.isEmpty(), "Oath blessed a new enemy");
        reset("hera"); ally.location.setZ(3); left(); advance(161);
        require(tasks.isEmpty(), "Oath did not expire");
    }

    private void checkAnubis() throws Exception {
        reset("anubis"); right();
        require(caster.stones == 200, "Judgment without a target charged resources");
        caster.health = 10; left();
        incoming(enemy, 20, true); incoming(ally, 20, false); incoming(enemy, 2, false);
        right();
        require(enemy.damage == 6 && caster.health == 13 && tasks.isEmpty(),
            "Scales counted cancelled/allied damage or failed to drain actual health");
        reset("anubis"); caster.health = 10; left(); incoming(enemy, 100, false); right();
        require(enemy.damage == 8 && caster.health == 13, "Scales exceeded its damage/heal cap");
        reset("anubis"); caster.health = 10; left(); enemy.immune = true; right();
        require(enemy.damage == 0 && caster.health == 10, "Anubis healed from cancelled damage");
        reset("anubis"); left(); advance(161);
        require(tasks.isEmpty() && ability.activeTimerLines().isEmpty(), "Judgment did not expire");
    }

    private void checkQueenBee() throws Exception {
        reset("queenbee"); left(); ability.clearCooldowns(); left(); advance(20);
        require(caster.stones == 188 && enemy.damage == 2 && ally.damage == 0 && outsider.damage == 0,
            "Bee swarm stacked or stung invalid targets");
        enemy.location.setX(4); advance(20);
        require(enemy.damage == 4, "Swarm did not follow moving prey");
        caster.lineOfSight = false; advance(20);
        require(enemy.damage == 4 && tasks.isEmpty(), "Swarm followed through cover");
        reset("queenbee"); left(); advance(60);
        require(enemy.damage == 6 && tasks.isEmpty(), "Swarm was not limited to three stings");
        reset("queenbee"); caster.health = 10; ally.health = 10; enemy.health = 10; right(); advance(1);
        require(caster.health == 11 && ally.health == 11 && enemy.health == 10 && ally.food == 12, "Honeycomb aid/protection failed");
        ally.location.setX(10); advance(79);
        require(caster.health == 14 && ally.health == 11 && tasks.isEmpty(), "Hive followed allies outside its radius");
    }

    private void checkNike() throws Exception {
        reset("nike"); right();
        require(caster.stones == 200, "Victory without laurels charged resources");
        ability.onKill(new AbilityKillContext(core, caster.player, ally.player, context.ability(), deathEvent(ally.player)));
        require(ability.activeTimerLines().get(0).contains("0/3"), "Friendly kill earned laurels");
        for (int i = 0; i < 4; i++)
            ability.onKill(new AbilityKillContext(core, caster.player, enemy.player, context.ability(), deathEvent(enemy.player)));
        require(ability.activeTimerLines().get(0).contains("3/3"), "Laurels were not capped at three");
        caster.health = 10; ally.health = 10; right();
        require(caster.health == 15 && ally.health == 15 && enemy.effects.isEmpty()
            && ability.activeTimerLines().get(0).contains("0/3"), "Victory aid failed to consume laurels/protect allies");
        left();
        require(caster.lastVelocity.getZ() > 1 && caster.lastVelocity.getY() > 0, "Wing dash did not propel the caster");
        EntityDamageEvent fall = new EntityDamageEvent(caster.player, EntityDamageEvent.DamageCause.FALL, 10);
        ability.onGenericDamage(context, fall);
        require(fall.isCancelled() && tasks.isEmpty(), "Wings failed to protect one landing");
        fall = new EntityDamageEvent(caster.player, EntityDamageEvent.DamageCause.FALL, 10);
        ability.onGenericDamage(context, fall);
        require(!fall.isCancelled(), "Wings protected unlimited landings");
    }

    private void checkDemeter() throws Exception {
        reset("demeter"); left();
        require(caster.granted == 10 && caster.stones == 188, "Demeter lost her bread supply");
        caster.health = 10; ally.health = 10; enemy.health = 10; right();
        ability.clearCooldowns(); right();
        require(caster.stones == 166 && tasks.size() == 3, "Harvest fields stacked");
        advance(39);
        require(ally.health == 10, "Harvest occurred before grain grew");
        advance(1);
        require(ally.health == 12 && ally.food == 14 && enemy.health == 10, "First harvest aid/protection failed");
        ally.location.setX(10); advance(80);
        require(caster.health == 16 && ally.health == 12 && tasks.isEmpty(), "Harvest exceeded its area/lifetime");
    }

    private void checkPan() throws Exception {
        reset("pan"); left(); ability.clearCooldowns(); right();
        require(caster.stones == 188 && tasks.size() == 1, "Pan played two songs at once");
        enemy.location.setZ(6); advance(40);
        require(enemy.velocities == 1 && ally.velocities == 0 && tasks.isEmpty(), "Panic finale missed or hit allies");
        reset("pan"); left();
        Location destination = caster.location.clone().add(2, 0, 0);
        PlayerMoveEvent move = new PlayerMoveEvent(caster.player, caster.location, destination);
        move.setCancelled(true); ability.onMove(context, move);
        require(!tasks.isEmpty(), "Cancelled movement interrupted Pan");
        move.setCancelled(false); ability.onMove(context, move);
        advance(40);
        require(tasks.isEmpty() && enemy.velocities == 0, "Interrupted flute still reached its finale");
        reset("pan"); caster.health = 10; ally.health = 10; right(); advance(40);
        require(caster.health == 12 && ally.health == 12 && enemy.effects.isEmpty(), "Pastoral finale failed");
        EntityDamageEvent fall = new EntityDamageEvent(caster.player, EntityDamageEvent.DamageCause.FALL, 10);
        ability.onGenericDamage(context, fall);
        require(fall.getDamage() == 7, "Goat legs did not reduce fall damage");
    }

    private void checkNamedKitCleanup() throws Exception {
        for (String id : Arrays.asList("chronos", "odin", "hephaestus", "athena", "hera", "anubis", "queenbee", "nike", "demeter", "pan")) {
            reset(id); ally.location.setZ(3);
            if (id.equals("demeter")) right(); else left();
            require(!tasks.isEmpty(), "Cleanup scenario did not arm " + id);
            ability.onDeath(context, deathEvent(caster.player));
            advance(200);
            require(tasks.isEmpty() && enemy.damage == 0, "Death left pending gameplay for " + id);
        }
    }

    private void reset(String id) throws Exception {
        if (ability != null) ability.cancelScheduledTasks();
        require(tasks.isEmpty(), "Previous ability leaked scheduled work");
        tick = 0; safeFloor = true; anchorObstructed = false;
        for (Actor actor : actors) {
            actor.location = new Location(world, 0, 160, 0); actor.damage = 0; actor.velocities = 0;
            actor.online = true; actor.stones = 200; actor.sneaking = false; actor.health = 20;
            actor.effects.clear(); actor.particles = 0; actor.held = new ItemStack(Material.BLAZE_ROD);
            actor.armor = new ItemStack[4]; actor.lastVelocity = new org.bukkit.util.Vector();
            actor.lineOfSight = true; actor.teleportAllowed = true; actor.immune = false; actor.food = 10; actor.granted = 0;
            teams.remove(actor.id);
        }
        enemy.location.setZ(3); ally.location.setX(1); outsider.location.setX(2);
        far.location.setX(9); far.location.setZ(9);
        teams.put(caster.id, GodTeam.RED); teams.put(ally.id, GodTeam.RED);
        teams.put(enemy.id, GodTeam.BLUE); teams.put(far.id, GodTeam.BLUE);
        core.getConfig().set("abilities.effects.enabled", false);
        core.getConfig().set("game.killtime-seconds", 0);
        core.getConfig().set("game.urf.enabled", false);
        field(GameManager.class, "state").set(core.game(), GameState.RUNNING);
        AbilityDefinition definition = core.abilities().registry().get(id);
        ability = definition.create(); context = new AbilityPlayerContext(core, caster.player, definition);
        assignments.put(caster.id, new AbilitySession(definition, ability));
    }

    private void left() { click(Action.LEFT_CLICK_AIR); }
    private void right() { click(Action.RIGHT_CLICK_AIR); }
    private void click(Action action) {
        caster.held = new ItemStack(Material.BLAZE_ROD);
        core.abilities().handleInteract(caster.player, new PlayerInteractEvent(caster.player, action, caster.held, null, BlockFace.SELF));
    }
    private EntityDamageByEntityEvent melee(Actor victim, double amount) {
        EntityDamageByEntityEvent event = attackEvent(caster.player, victim.player, amount);
        core.abilities().handleDamage(caster.player, victim.player, event);
        return event;
    }
    private EntityDamageByEntityEvent attackEvent(Player from, Player to, double amount) {
        return new EntityDamageByEntityEvent(from, to, EntityDamageEvent.DamageCause.ENTITY_ATTACK, amount);
    }
    private PlayerDeathEvent deathEvent(Player player) throws Exception {
        try {
            return PlayerDeathEvent.class.getConstructor(Player.class, List.class, int.class, String.class)
                .newInstance(player, new ArrayList<ItemStack>(), 0, "");
        } catch (NoSuchMethodException modern) {
            Class<?> sourceType = Class.forName("org.bukkit.damage.DamageSource");
            Object source = proxy(sourceType, (p, m, a) -> defaultValue(m.getReturnType()));
            return PlayerDeathEvent.class.getConstructor(Player.class, sourceType, List.class, int.class, String.class)
                .newInstance(player, source, new ArrayList<ItemStack>(), 0, "");
        }
    }
    private void advance(int ticks) {
        for (int i = 0; i < ticks; i++) {
            tick++;
            for (Integer id : new ArrayList<Integer>(tasks.keySet())) {
                Task task = tasks.get(id);
                if (task == null || task.due > tick) continue;
                if (task.period == 0) tasks.remove(id); else task.due += task.period;
                task.action.run();
            }
        }
    }
    private List<Player> players() {
        List<Player> result = new ArrayList<Player>();
        for (Actor actor : actors) if (actor.online) result.add(actor.player);
        return result;
    }
    private Block air(Location at) {
        return proxy(Block.class, (p, m, a) -> {
            switch (m.getName()) {
                // A synthetic world must return native materials, as CraftWorld would.
                case "getType": return Enum.valueOf(Material.class,
                    (safeFloor && at.getBlockY() == 159) || (anchorObstructed && at.getBlockX() == 1 && at.getBlockY() == 160) ? "STONE" : "AIR");
                case "getWorld": return world;
                case "getLocation": return at.clone();
                case "getX": return at.getBlockX();
                case "getY": return at.getBlockY();
                case "getZ": return at.getBlockZ();
                case "getRelative":
                    BlockFace face = (BlockFace) a[0]; int distance = a.length > 1 ? (Integer) a[1] : 1;
                    return air(at.clone().add(face.getModX() * distance, face.getModY() * distance, face.getModZ() * distance));
                default: return defaultValue(m.getReturnType());
            }
        });
    }

    private final class Actor {
        final UUID id = UUID.randomUUID();
        final String name;
        final Player player;
        final Map<PotionEffectType, PotionEffect> effects = new HashMap<PotionEffectType, PotionEffect>();
        Location location = new Location(world, 0, 160, 0);
        ItemStack held = new ItemStack(Material.BLAZE_ROD);
        ItemStack[] armor = new ItemStack[4];
        org.bukkit.util.Vector lastVelocity = new org.bukkit.util.Vector();
        boolean online = true, sneaking, lineOfSight = true, teleportAllowed = true, immune;
        int stones = 200, velocities, particles, food = 10, granted;
        double damage, health = 20;

        Actor(String name) {
            this.name = name;
            PlayerInventory inventory = proxy(PlayerInventory.class, (p, m, a) -> {
                if (m.getName().equals("getArmorContents")) return armor.clone();
                if (m.getName().equals("setArmorContents")) { armor = ((ItemStack[]) a[0]).clone(); return null; }
                if (m.getName().equals("addItem")) {
                    for (ItemStack item : (ItemStack[]) a[0]) granted += item.getAmount();
                    return new HashMap<Integer, ItemStack>();
                }
                if (m.getName().equals("contains")) return stones >= ((Number) a[1]).intValue();
                if (m.getName().equals("removeItem")) {
                    for (ItemStack item : (ItemStack[]) a[0]) stones -= item.getAmount();
                    return new HashMap<Integer, ItemStack>();
                }
                return defaultValue(m.getReturnType());
            });
            player = proxy(Player.class, (p, m, a) -> {
                switch (m.getName()) {
                    case "getUniqueId": return id;
                    case "getName": return name;
                    case "getWorld": return location.getWorld();
                    case "getLocation": return location.clone();
                    case "getEyeLocation": return location.clone().add(0, 1.6D, 0);
                    case "getEyeHeight": return 1.6D;
                    case "getInventory": return inventory;
                    case "getItemInHand": return held;
                    case "setItemInHand": held = (ItemStack) a[0]; return null;
                    case "getGameMode": return GameMode.SURVIVAL;
                    case "isOnline": return online;
                    case "isSneaking": return sneaking;
                    case "canSee": return true;
                    case "hasLineOfSight": return lineOfSight;
                    case "teleport": if (teleportAllowed) location = ((Location) a[0]).clone(); return teleportAllowed;
                    case "isDead": return health <= 0;
                    case "getFoodLevel": return food;
                    case "setFoodLevel": food = (Integer) a[0]; return null;
                    case "getHealth": return health;
                    case "getMaxHealth": return 20D;
                    case "setHealth": health = (Double) a[0]; return null;
                    case "getActivePotionEffects": return new ArrayList<PotionEffect>(effects.values());
                    case "addPotionEffect": PotionEffect effect = (PotionEffect) a[0]; effects.put(effect.getType(), effect); return true;
                    case "removePotionEffect": effects.remove(a[0]); return null;
                    case "hasPotionEffect": return effects.containsKey(a[0]);
                    case "setVelocity": velocities++; lastVelocity = ((org.bukkit.util.Vector) a[0]).clone(); return null;
                    case "spawnParticle": particles += ((Number) a[2]).intValue(); return null;
                    case "damage":
                        require(a.length == 2 && a[1] == caster.player, "Lost damage attribution");
                        EntityDamageByEntityEvent event = attackEvent(caster.player, (Player) p, ((Number) a[0]).doubleValue());
                        event.setCancelled(immune);
                        core.abilities().handleDamage(caster.player, (Player) p, event);
                        if (!event.isCancelled()) {
                            damage += event.getDamage();
                            health = Math.max(0, health - event.getFinalDamage());
                        }
                        return null;
                    default: return defaultValue(m.getReturnType());
                }
            });
            actors.add(this);
        }
    }
    private static final class Task {
        final Runnable action; long due; final long period;
        Task(Runnable action, long due, long period) { this.action = action; this.due = due; this.period = period; }
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
            if (m.getName().equals("toString")) return type.getSimpleName() + "VarietyFixture";
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
