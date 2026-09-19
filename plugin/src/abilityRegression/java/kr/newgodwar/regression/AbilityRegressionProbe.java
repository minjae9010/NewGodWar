package kr.newgodwar.regression;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.ability.AbilitySession;
import kr.newgodwar.ability.api.AbilityDefinition;
import kr.newgodwar.ability.api.AbilityPlayerContext;
import kr.newgodwar.ability.api.GodAbility;
import kr.newgodwar.ability.builtin.BaseAbility;
import kr.newgodwar.game.GameManager;
import kr.newgodwar.game.GameState;
import kr.newgodwar.game.GodTeam;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.lang.reflect.*;
import java.util.*;

/** Synthetic players/events on disposable Paper servers, using real ability and game logic. */
public final class AbilityRegressionProbe extends JavaPlugin {
    private NewGodWarPlugin core;
    private GameManager game;
    private final Map<Integer, Runnable> scheduled = new LinkedHashMap<Integer, Runnable>();
    private final List<String> messages = new ArrayList<String>();
    private Player caster;
    private Player enemy;
    private World world;
    private World enemyWorld;
    private Block sign;
    private Material signType;
    private GodAbility ability;
    private AbilityPlayerContext context;
    private Map<UUID, GodTeam> teams;
    private Map<UUID, AbilitySession> assignments;
    private int stones;
    private int pulses;
    private int taskId;
    private int removedSigns;
    private boolean enemyOnline;
    private Sign signState;
    private Server realServer;
    private ItemStack heldItem;

    @Override public void onEnable() {
        Bukkit.getScheduler().runTask(this, () -> {
            try {
                core = (NewGodWarPlugin) Bukkit.getPluginManager().getPlugin("NewGodWar");
                game = core.game();
                core.getConfig().set("world.reset-game-world-on-stop", false);
                new ItemGrantRegressionChecks().run(core);
                new FeedbackRegressionChecks(core).run();
                runChecks();
                new AbilityVarietyChecks(core).run();
                getLogger().info("ABILITY REGRESSION PASS");
            } catch (Throwable ex) {
                getLogger().log(java.util.logging.Level.SEVERE, "ABILITY REGRESSION FAILED", ex);
            } finally {
                if (game != null) game.stop(false);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void runChecks() throws Exception {
        Server original = Bukkit.getServer();
        realServer = original;
        Field serverField = field(Bukkit.class, "server");
        world = original.getWorlds().get(0);
        // Legacy chunk loading calls CraftServer directly; load the sign fixture before installing the proxy.
        world.getChunkAt(0, 0).load();
        teams = (Map<UUID, GodTeam>) field(GameManager.class, "teams").get(game);
        assignments = (Map<UUID, AbilitySession>) field(core.abilities().getClass(), "assignments").get(core.abilities());
        caster = player("Caster", true);
        enemy = player("EnemyPlayer", false);
        signState = proxy(Sign.class, (p, m, a) -> defaultValue(m.getReturnType()));
        sign = proxy(Block.class, (p, m, a) -> {
            if (m.getName().equals("getType")) return signType;
            if (m.getName().equals("getState")) return signType == Material.STONE || signType == Material.AIR ? null : signState;
            if (m.getName().equals("breakNaturally")) { removedSigns++; signType = Material.AIR; return true; }
            return defaultValue(m.getReturnType());
        });
        // Paper's legacy bytecode converter needs the real CraftServer while these classes load.
        new AbilityPlayerContext(core, caster, core.abilities().registry().get("voodoo"));
        new AbilitySession(core.abilities().registry().get("voodoo"), core.abilities().registry().get("voodoo").create());
        new TargetAbility();
        new SignChangeEvent(sign, caster, new String[] {"", "", "", ""});
        new PlayerInteractEvent(caster, Action.LEFT_CLICK_BLOCK, null, sign, BlockFace.UP);
        game.killtimeMode();
        BukkitScheduler scheduler = proxy(BukkitScheduler.class, (p, m, a) -> {
            if (m.getName().equals("scheduleSyncDelayedTask")) {
                require(((Number) a[2]).longValue() == 140L, "Voodoo lifetime must be seven seconds");
                scheduled.put(++taskId, (Runnable) a[1]);
                return taskId;
            }
            if (m.getName().equals("cancelTask")) { scheduled.remove(a[0]); return null; }
            return invoke(original.getScheduler(), m, a);
        });
        Server lookup = proxy(Server.class, (p, m, a) -> {
            if (m.getName().equals("getScheduler")) return scheduler;
            if (m.getName().equals("getScoreboardManager")) return null;
            if (m.getName().equals("getPlayerExact") || m.getName().equals("getPlayer")) {
                for (Player candidate : Arrays.asList(caster, enemy)) {
                    if (!candidate.isOnline()) continue;
                    if (candidate.getUniqueId().equals(a[0]) || candidate.getName().equalsIgnoreCase(String.valueOf(a[0]))) return candidate;
                    // A fuzzy lookup must remain observable rather than accidentally behaving like an exact lookup.
                    if (m.getName().equals("getPlayer") && a[0] instanceof String
                        && candidate.getName().toLowerCase(Locale.ROOT).startsWith(((String) a[0]).toLowerCase(Locale.ROOT))) return candidate;
                }
                return null;
            }
            return invoke(original, m, a);
        });
        // Replace only during this synchronous fixture; always restore the real server before returning.
        try {
            serverField.set(null, lookup);
            reset();
            link("  enemyplayer  ");
            require(connected() && stones == 95 && ability.cooldownRemainingMillis(1) > 0L,
                "A complete enemy name with surrounding whitespace must connect and charge once");
            PlayerInteractEvent first = hit();
            require(first.isCancelled() && pulses == 1, "Linked sign must deal damage without being broken by the click");
            hit();
            require(pulses == 1, "Repeated clicks bypassed the configured pulse interval");
            core.getConfig().set("abilities.voodoo.hit-interval-millis", 0);
            hit();
            require(pulses == 2, "Configured zero interval did not allow another pulse");
            ability.clearCooldowns();
            messages.clear();
            link("EnemyPlayer");
            require(stones == 95 && !connected() && mentions("이미 연결"), "An active link must not be replaced or charged again");
            expire();
            require(removedSigns == 1, "Expired link did not remove its sign");
            getLogger().info("PASS voodoo: trimmed exact name, resource/cooldown, protected click, pulse interval, duplicate and expiry");

            reset(); reject("", "첫 줄");
            reset(); reject("Enemy", "정확한 이름");
            reset(); reject("MissingPlayer", "찾을 수 없");
            reset(); enemyOnline = false; reject("EnemyPlayer", "찾을 수 없");
            reset(); reject("Caster", "참가 중인 적");
            reset(); teams.put(enemy.getUniqueId(), GodTeam.RED); reject("EnemyPlayer", "참가 중인 적");
            reset(); teams.remove(enemy.getUniqueId()); reject("EnemyPlayer", "참가 중인 적");
            reset();
            Set<UUID> observers = (Set<UUID>) field(GameManager.class, "observers").get(game);
            observers.add(enemy.getUniqueId());
            try { reject("EnemyPlayer", "관전자"); } finally { observers.remove(enemy.getUniqueId()); }
            reset();
            Set<GodTeam> eliminated = (Set<GodTeam>) field(GameManager.class, "eliminatedTeams").get(game);
            eliminated.add(GodTeam.BLUE);
            try { reject("EnemyPlayer", "탈락자"); } finally { eliminated.remove(GodTeam.BLUE); }
            reset(); enemyWorld = proxy(World.class, (p, m, a) -> defaultValue(m.getReturnType()));
            reject("EnemyPlayer", "같은 월드");
            reset(); protectCombat(); reject("EnemyPlayer", "킬타임");
            reset(); protectCombat(); core.getConfig().set("game.killtime-mode", "core-only");
            link("EnemyPlayer");
            require(connected(), "Core-only killtime must still allow player abilities");
            reset();
            SignChangeEvent cancelled = new SignChangeEvent(sign, caster, new String[] {"EnemyPlayer", "", "", ""});
            cancelled.setCancelled(true);
            Bukkit.getPluginManager().callEvent(cancelled);
            require(!connected() && stones == 100, "Cancelled sign edits must not activate an ability");
            reset(); stones = 4; link("EnemyPlayer");
            require(stones == 4 && !connected() && mentions("부족"), "Insufficient resources must be explained without charging");
            getLogger().info("PASS voodoo: empty/partial/missing/offline/self/ally/outsider/observer/eliminated/world/killtime/resource failures");

            reset(); link("EnemyPlayer");
            teams.put(enemy.getUniqueId(), GodTeam.RED);
            hit();
            require(pulses == 0, "Link damaged a target that became an ally");
            signType = Material.STONE;
            expire();
            require(removedSigns == 0 && signType == Material.STONE, "Cleanup destroyed a replacement block");
            reset(); link("EnemyPlayer");
            ability.cancelScheduledTasks();
            require(scheduled.isEmpty() && removedSigns == 1, "Removing an ability must clean up its active link");

            reset();
            TargetAbility targeted = new TargetAbility();
            targeted.setTarget(context, caster, "Enemy");
            require(targeted.resolved() == null && mentions("정확한 이름"), "Command targeting accepted a partial name");
            targeted.setTarget(context, caster, " enemyplayer ");
            require(targeted.resolved() == enemy, "Command targeting rejected a trimmed exact name");
            messages.clear();
            protectCombat();
            require(targeted.validated(context) == null && mentions("킬타임"), "Targeted abilities silently fail during killtime");
            getLogger().info("PASS shared targets: exact names, whitespace and killtime explanation; voodoo revalidation and cleanup");
            checksRealSignVariants();
        } finally {
            try {
                if (ability != null) ability.cancelScheduledTasks();
            } finally {
                serverField.set(null, original);
                assignments.remove(caster.getUniqueId());
                teams.remove(caster.getUniqueId());
                teams.remove(enemy.getUniqueId());
                core.getConfig().set("game.killtime-seconds", 0);
            }
        }
    }

    private void checksRealSignVariants() throws Exception {
        Block syntheticSign = sign;
        Block placed = world.getBlockAt(10, 100, 10);
        Block realSign = proxy(Block.class, (p, m, a) -> {
            Server saved = Bukkit.getServer();
            Field serverField = field(Bukkit.class, "server");
            serverField.set(null, realServer);
            try { return invoke(placed, m, a); } finally { serverField.set(null, saved); }
        });
        try {
            for (String name : Arrays.asList("OAK_SIGN", "BIRCH_SIGN", "BIRCH_WALL_SIGN", "BIRCH_HANGING_SIGN", "BIRCH_WALL_HANGING_SIGN", "SIGN_POST")) {
                Material material;
                try { material = Enum.valueOf(Material.class, name); } catch (IllegalArgumentException unavailable) { continue; }
                reset();
                sign = realSign;
                if (!name.contains("WALL") && !name.equals("SIGN_POST")) {
                    heldItem = new ItemStack(material, 1);
                    PlayerInteractEvent ready = new PlayerInteractEvent(caster, Action.LEFT_CLICK_AIR, heldItem, null, BlockFace.SELF);
                    Bukkit.getPluginManager().callEvent(ready);
                    require(mentions("사용 할 수"), name + " was not recognized in the player's hand");
                }
                // Reflective call avoids the fixture's own legacy material translation.
                Block.class.getMethod("setType", Material.class, boolean.class).invoke(sign, material, false);
                require(sign.getState() instanceof Sign, name + " is not a sign state");
                link("EnemyPlayer");
                require(connected(), name + " did not deliver its sign edit through the registered listener");
                require(hit().isCancelled() && pulses == 1, name + " could not be hit after connecting");
                ability.cancelScheduledTasks();
                require(!(sign.getState() instanceof Sign), name + " was not removed during cleanup");
                // All sign variants must enforce the same resource check on placement.
                reset();
                Block.class.getMethod("setType", Material.class, boolean.class).invoke(sign, material, false);
                stones = 4;
                BlockPlaceEvent placement = new BlockPlaceEvent(sign, sign.getState(), sign.getRelative(BlockFace.DOWN), null, caster, true);
                Bukkit.getPluginManager().callEvent(placement);
                require(placement.isCancelled() && mentions("부족"), name + " bypassed the placement resource check");
                getLogger().info("PASS real sign variant: " + name);
            }
        } finally {
            realSign.setType(Material.AIR);
            sign = syntheticSign;
        }
    }

    private void reset() throws Exception {
        if (ability != null) ability.cancelScheduledTasks();
        scheduled.clear(); messages.clear();
        stones = 100; pulses = 0; removedSigns = 0; enemyOnline = true; enemyWorld = world;
        heldItem = null;
        signType = Material.SIGN_POST;
        core.getConfig().set("abilities.messages.enabled", true);
        core.getConfig().set("abilities.messages.failure", true);
        core.getConfig().set("abilities.voodoo.hit-interval-millis", 1000);
        core.getConfig().set("abilities.voodoo.damage", 0.5D);
        core.getConfig().set("game.killtime-seconds", 0);
        core.getConfig().set("game.urf.enabled", false);
        field(GameManager.class, "state").set(game, GameState.RUNNING);
        teams.put(caster.getUniqueId(), GodTeam.RED);
        teams.put(enemy.getUniqueId(), GodTeam.BLUE);
        AbilityDefinition definition = core.abilities().registry().get("voodoo");
        ability = definition.create();
        context = new AbilityPlayerContext(core, caster, definition);
        assignments.put(caster.getUniqueId(), new AbilitySession(definition, ability));
    }

    private void protectCombat() throws Exception {
        core.getConfig().set("game.killtime-seconds", 300);
        core.getConfig().set("game.killtime-mode", "player-combat");
        field(GameManager.class, "runningStartedAtMillis").set(game, System.currentTimeMillis());
    }

    private void link(String name) {
        Bukkit.getPluginManager().callEvent(new SignChangeEvent(sign, caster, new String[] {name, "", "", ""}));
    }

    private PlayerInteractEvent hit() {
        PlayerInteractEvent event = new PlayerInteractEvent(caster, Action.LEFT_CLICK_BLOCK, null, sign, BlockFace.UP);
        Bukkit.getPluginManager().callEvent(event);
        return event;
    }

    private void expire() {
        List<Runnable> tasks = new ArrayList<Runnable>(scheduled.values());
        scheduled.clear();
        for (Runnable task : tasks) task.run();
    }

    private boolean connected() { return mentions("연결시켰습니다"); }
    private boolean mentions(String text) { for (String message : messages) if (message.contains(text)) return true; return false; }
    private void reject(String name, String reason) {
        link(name);
        require(!connected() && mentions(reason) && stones == 100 && ability.cooldownRemainingMillis(1) == 0L,
            "Rejected target '" + name + "' must explain '" + reason + "' without charging: " + messages);
    }

    private Player player(String name, boolean owner) {
        UUID uuid = UUID.randomUUID();
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
                case "getUniqueId": return uuid;
                case "getName": return name;
                case "getWorld": return owner ? world : enemyWorld;
                case "getLocation": return world.getSpawnLocation();
                case "getInventory": return inventory;
                case "getItemInHand": return heldItem;
                case "getGameMode": return GameMode.SURVIVAL;
                case "isOnline": return owner || enemyOnline;
                case "sendMessage": if (owner && a[0] instanceof String) messages.add((String) a[0]); return null;
                case "damage":
                    require(!owner && ((Number) a[0]).doubleValue() == 0.5D && a[1] == caster, "Wrong pulse target, amount or attribution");
                    pulses++; return null;
                default: return defaultValue(m.getReturnType());
            }
        });
    }

    private static final class TargetAbility extends BaseAbility {
        @Override public boolean requiresTarget() { return true; }
        Player resolved() { return targetPlayer(); }
        Player validated(AbilityPlayerContext context) { return commandTargetPlayer(context, context.player(), false); }
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field result = type.getDeclaredField(name); result.setAccessible(true); return result;
    }
    private static Object invoke(Object target, Method method, Object[] args) throws Throwable {
        try { return method.invoke(target, args); } catch (InvocationTargetException ex) { throw ex.getCause(); }
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (p, m, a) -> {
            if (m.getName().equals("equals")) return p == a[0];
            if (m.getName().equals("hashCode")) return System.identityHashCode(p);
            if (m.getName().equals("toString")) return type.getSimpleName() + "Fixture";
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
