package kr.newgodwar.regression;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.game.*;
import kr.newgodwar.listener.GameListener;
import kr.newgodwar.nms.NmsAdapter;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import java.lang.reflect.*;
import java.util.*;

/** Runs on a disposable Paper server; exercises the production game/event handlers. */
public final class CoreRegressionProbe extends JavaPlugin {
    private NewGodWarPlugin core;
    private GameManager game;
    private GameListener listener;
    private Player player;
    private final UUID playerId = UUID.randomUUID();
    private final List<GameState> eliminationStates = new ArrayList<GameState>();
    private final List<Set<GodTeam>> terminalEliminations = new ArrayList<Set<GodTeam>>();
    private Map<GodTeam, TempleLocation> temples;
    private Map<UUID, GodTeam> teams;
    private Set<UUID> observers;
    private Set<GodTeam> eliminated;
    private Block red;
    private Block blue;

    @Override public void onEnable() {
        Bukkit.getScheduler().runTask(this, () -> {
            try {
                initialize();
                rejectsIneligibleBreakers();
                rejectsDuplicateTemples();
                batchesExplosionsBeforeVictory();
                cancelsUnderpopulatedStarts();
                getLogger().info("CORE REGRESSION PASS");
            } catch (Throwable ex) {
                getLogger().log(java.util.logging.Level.SEVERE, "CORE REGRESSION FAILED", ex);
            } finally {
                if (game != null) game.stop(false);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void initialize() throws Exception {
        core = (NewGodWarPlugin) Bukkit.getPluginManager().getPlugin("NewGodWar");
        game = core.game();
        listener = new GameListener(core, game, core.abilities(), (NmsAdapter) get(core, "nmsAdapter"));
        core.getConfig().set("game.killtime-seconds", 0);
        core.getConfig().set("world.reset-game-world-on-stop", false);
        core.getConfig().set("core.protect-diamond-from-explosion", false);
        core.getConfig().set("game.reveal-abilities-on-end", false);
        core.getConfig().set("game.remove-entities", false);
        core.getConfig().set("game.min-players", 2);
        teams = (Map<UUID, GodTeam>) get(game, "teams");
        temples = (Map<GodTeam, TempleLocation>) get(game, "temples");
        observers = (Set<UUID>) get(game, "observers");
        eliminated = new HashSet<GodTeam>() {
            @Override public boolean add(GodTeam team) {
                boolean changed = super.add(team);
                if (changed) eliminationStates.add(game.state());
                return changed;
            }
            @Override public void clear() {
                if (!isEmpty()) terminalEliminations.add(new HashSet<GodTeam>(this));
                super.clear();
            }
        };
        set(game, "eliminatedTeams", eliminated);
        PlayerInventory bag = (PlayerInventory) Proxy.newProxyInstance(PlayerInventory.class.getClassLoader(),
            new Class<?>[] {PlayerInventory.class}, (proxy, method, args) -> defaultValue(method.getReturnType()));
        player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[] {Player.class}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "getUniqueId": return playerId;
                case "getName": return "CoreRegressionPlayer";
                case "getInventory": return bag;
                case "getGameMode": return GameMode.SURVIVAL;
                case "getWorld": return Bukkit.getWorlds().get(0);
                case "getLocation": return Bukkit.getWorlds().get(0).getSpawnLocation();
                case "isOnline": case "hasPermission": return true;
                case "hashCode": return playerId.hashCode();
                case "equals": return proxy == args[0];
                default: return defaultValue(method.getReturnType());
            }
        });
        red = Bukkit.getWorlds().get(0).getBlockAt(0, 100, 0);
        blue = Bukkit.getWorlds().get(0).getBlockAt(1, 100, 0);
        reset();
    }

    private void reset() throws Exception {
        game.stop(false);
        for (GodTeam team : GodTeam.values()) core.getConfig().set("teams." + team.id() + ".enabled", true);
        temples.clear();
        red.setType(Material.DIAMOND_BLOCK);
        blue.setType(Material.DIAMOND_BLOCK);
        temples.put(GodTeam.RED, TempleLocation.fromBlock(red));
        temples.put(GodTeam.BLUE, TempleLocation.fromBlock(blue));
        eliminationStates.clear();
        terminalEliminations.clear();
        set(game, "state", GameState.RUNNING);
    }

    private void rejectsIneligibleBreakers() throws Exception {
        checkBreakBlocked("unassigned");
        teams.put(playerId, GodTeam.BLUE);
        observers.add(playerId);
        checkBreakBlocked("observer");
        observers.clear();
        eliminated.add(GodTeam.BLUE);
        checkBreakBlocked("eliminated");
        eliminated.clear();
        core.getConfig().set("teams.blue.enabled", false);
        checkBreakBlocked("disabled team");
        core.getConfig().set("teams.blue.enabled", true);
        teams.put(playerId, GodTeam.RED);
        checkBreakBlocked("own temple");
        teams.put(playerId, GodTeam.BLUE);
        BlockBreakEvent legal = new BlockBreakEvent(red, player);
        listener.onBlockBreak(legal);
        require(!legal.isCancelled() && game.isEliminated(GodTeam.RED), "Legal enemy core break rejected");
        getLogger().info("PASS core eligibility: outsider, observer, eliminated, disabled, own team and legal enemy");
        reset();
    }

    private void checkBreakBlocked(String scenario) {
        BlockBreakEvent event = new BlockBreakEvent(red, player);
        listener.onBlockBreak(event);
        require(event.isCancelled() && !game.isEliminated(GodTeam.RED), "Core break allowed for " + scenario);
    }

    private void rejectsDuplicateTemples() throws Exception {
        require(game.setTemple(GodTeam.RED, red), "Own temple registration must remain idempotent");
        require(!game.setTemple(GodTeam.BLUE, red), "Duplicate temple accepted");
        require(game.templeTeam(blue).equals(GodTeam.BLUE), "Rejected update changed the existing temple");
        temples.put(GodTeam.BLUE, TempleLocation.fromBlock(red));
        try {
            invoke(game, "validateStartSettings", new Class<?>[0]);
            throw new AssertionError("Legacy duplicate config accepted");
        } catch (InvocationTargetException ex) {
            require(ex.getCause() instanceof IllegalStateException && ex.getCause().getMessage().contains("중복"),
                "Wrong duplicate validation failure: " + ex.getCause());
        }
        getLogger().info("PASS duplicate temples: registration and legacy config validation");
        reset();
    }

    private void batchesExplosionsBeforeVictory() throws Exception {
        core.getConfig().set("teams.green.enabled", false);
        explode(Arrays.asList(red, blue, red));
        require(eliminationStates.equals(Arrays.asList(GameState.RUNNING, GameState.RUNNING)), "Eliminated after game end or twice");
        require(game.state() == GameState.ENDED, "All-team destruction did not end game");
        require(terminalEliminations.size() == 1 && terminalEliminations.get(0).equals(new HashSet<GodTeam>(Arrays.asList(GodTeam.RED, GodTeam.BLUE))),
            "Victory evaluated before all destroyed teams were eliminated");
        game.eliminate(GodTeam.RED, null);
        require(eliminated.isEmpty() && terminalEliminations.size() == 1, "Late event mutated an ended game");
        reset();
        // Third team survives: both destroyed teams must be recorded in the one terminal decision.
        explode(Arrays.asList(blue, red));
        require(game.state() == GameState.ENDED && terminalEliminations.size() == 1 && terminalEliminations.get(0).size() == 2,
            "Three-team batch winner was evaluated incorrectly");
        reset();
        // An ordinary single elimination still leaves a three-team game running.
        explode(Collections.singletonList(red));
        require(game.state() == GameState.RUNNING && game.isEliminated(GodTeam.RED), "Single elimination ended game too early");
        getLogger().info("PASS explosion batches: simultaneous final cores, remaining winner, duplicate and late events");
        reset();
    }

    private void explode(List<Block> blocks) throws Exception {
        invoke(listener, "eliminateExplodedTempleDiamonds", new Class<?>[] {List.class}, blocks);
    }

    private void cancelsUnderpopulatedStarts() throws Exception {
        set(game, "state", GameState.READY);
        int task = Bukkit.getScheduler().scheduleSyncRepeatingTask(core, () -> { }, 1000L, 1000L);
        set(game, "readyTask", task);
        invoke(game, "finishStart", new Class<?>[0]);
        require(game.state() == GameState.ENDED && ((Integer) get(game, "readyTask")) == -1,
            "Zero participants started a minimum-two game or left the ready timer running");
        invoke(game, "finishStart", new Class<?>[0]);
        require(game.state() == GameState.ENDED, "A stale ready callback restarted the game");
        getLogger().info("PASS start revalidation: insufficient participants, timer cleanup and stale callbacks");
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == double.class) return 0.0D;
        if (type == float.class) return 0.0F;
        if (type == long.class) return 0L;
        return null;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static Field field(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static Object get(Object object, String name) throws Exception { return field(object, name).get(object); }
    private static void set(Object object, String name, Object value) throws Exception { field(object, name).set(object, value); }
    private static Object invoke(Object object, String name, Class<?>[] types, Object... args) throws Exception {
        Method method = object.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(object, args);
    }
}
