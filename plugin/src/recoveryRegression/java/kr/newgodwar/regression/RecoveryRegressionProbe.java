package kr.newgodwar.regression;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.game.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Run in a disposable server with Test-GameRecovery.py; exercises real process restarts. */
public final class RecoveryRegressionProbe extends JavaPlugin {
    private static final UUID PLAYER = UUID.fromString("b0a42e68-7de5-4656-a003-50b8ae5bb880");
    private static final UUID OBSERVER = UUID.fromString("4409048e-c998-4f37-b9fa-2f8b463e227c");
    private NewGodWarPlugin core;
    private GameManager game;
    private File phaseFile;

    @Override public void onEnable() {
        Bukkit.getScheduler().runTaskLater(this, () -> runChecked(() -> runPhase()), 40L);
    }

    private void runPhase() throws Exception {
        core = (NewGodWarPlugin) Bukkit.getPluginManager().getPlugin("NewGodWar");
        game = core.game();
        getDataFolder().mkdirs();
        phaseFile = new File(getDataFolder(), "phase.txt");
        int phase = phaseFile.exists() ? Integer.parseInt(new String(Files.readAllBytes(phaseFile.toPath()), StandardCharsets.UTF_8).trim()) : 1;
        if (phase == 1) {
            require(!game.isRecovering(), "Initial recovery did not finish");
            World arena = Bukkit.createWorld(WorldBackupManager.creator("recovery-arena", "void"));
            arena.getBlockAt(1, 80, 1).setType(Material.STONE);
            core.getConfig().set("world.game-world", arena.getName());
            core.getConfig().set("world.reset-game-world-on-stop", true);
            Map<String, String> managed = new LinkedHashMap<String, String>();
            managed.put("name", arena.getName()); managed.put("type", "void");
            core.getConfig().set("world.managed-worlds", Collections.singletonList(managed));
            core.saveConfig();
            invoke(game, "prepareGameWorldSnapshot");
            seedPlayers();
            set(game, "state", GameState.RUNNING);
            set(game, "runningStartedAtMillis", System.currentTimeMillis() - 123000L);
            invoke(game, "applyWorldStartSettings");
            game.applyGameRules();
            arena.getBlockAt(1, 80, 1).setType(Material.DIAMOND_BLOCK);
            invoke(game, "saveCurrentWorlds");
            require((Boolean) invoke(game, "saveCheckpoint"), "Initial checkpoint failed");
            advance(2, "RECOVERY PHASE 1 READY");
        } else if (phase == 2) {
            verifyRunning(7, Material.DIAMOND_BLOCK);
            require(game.runningElapsedSeconds() >= 123 && game.runningElapsedSeconds() < 160, "Elapsed time was reset or included downtime");
            map(game, "kills").put(PLAYER, 8);
            Bukkit.getWorld("recovery-arena").getBlockAt(1, 80, 1).setType(Material.GOLD_BLOCK);
            // Do not invoke shutdown/checkpoint: wait for production periodic autosave, then kill the process.
            Bukkit.getScheduler().runTaskLater(this, () -> runChecked(() -> {
                YamlConfiguration saved = new GameSessionStore(core.getDataFolder()).load();
                require(saved.getInt("kills." + PLAYER) == 8, "Periodic checkpoint missed kills");
                advance(3, "RECOVERY PHASE 2 CRASH READY");
            }), 40L);
        } else if (phase == 3) {
            verifyRunning(8, Material.GOLD_BLOCK);
            game.stop(false);
            require(Bukkit.getWorld("recovery-arena").getBlockAt(1, 80, 1).getType() == Material.STONE,
                "Stop lost the original pre-game map: " + Bukkit.getWorld("recovery-arena").getBlockAt(1, 80, 1).getType());
            require(game.teamAssignments().isEmpty() && core.abilities().assignedAbilities().isEmpty(), "Explicit stop did not clear the match");
            invoke(game, "prepareGameWorldSnapshot");
            seedPlayers();
            set(game, "state", GameState.READY);
            set(game, "readySecondsRemaining", 4);
            map(game, "pendingSelection").put(PLAYER, 2);
            Bukkit.getWorld("recovery-arena").getBlockAt(1, 80, 1).setType(Material.OBSIDIAN);
            require((Boolean) invoke(game, "saveCheckpoint"), "READY checkpoint failed");
            advance(4, "RECOVERY PHASE 3 READY");
        } else if (phase == 4) {
            require(game.state() == GameState.READY, "Recovered READY was cancelled while players were offline");
            require(map(game, "pendingSelection").get(PLAYER).equals(2), "Reroll rights lost on restart");
            require(((Integer) get(game, "readySecondsRemaining")) == 4, "Countdown advanced without recovered players");
            // Simulate a process dying after the committed stop, before restoring the world.
            set(game, "state", GameState.ENDED);
            require((Boolean) invoke(game, "saveCheckpoint"), "Interrupted-stop checkpoint failed");
            advance(5, "RECOVERY PHASE 4 CRASH READY");
        } else if (phase == 5) {
            require(!game.isRecovering() && game.state() == GameState.ENDED, "Interrupted stop did not finish");
            require(Bukkit.getWorld("recovery-arena").getBlockAt(1, 80, 1).getType() == Material.STONE, "Interrupted reset lost original map");
            require(game.teamAssignments().isEmpty(), "Ended session resurrected participants");
            require(get(game, "activeGameWorldSnapshotName") == null, "Reset remained pending");
            advance(6, "RECOVERY PHASE 5 READY");
        } else {
            require(game.isRecovering(), "Corrupt checkpoint was silently ignored");
            try { game.start(); throw new AssertionError("New match overwrote corrupt checkpoint"); }
            catch (IllegalStateException expected) { }
            require(new String(Files.readAllBytes(new File(core.getDataFolder(), "game-session.yml").toPath()), StandardCharsets.UTF_8).equals("state: [broken"), "Corrupt checkpoint was overwritten");
            getLogger().info("RECOVERY REGRESSION PASS");
        }
    }

    private void seedPlayers() throws Exception {
        map(game, "teams").put(PLAYER, GodTeam.RED);
        map(game, "kills").put(PLAYER, 7);
        setOf(game, "eliminatedTeams").add(GodTeam.BLUE);
        setOf(game, "observers").add(OBSERVER);
        setOf(game, "preparedPlayerInventories").add(PLAYER);
        YamlConfiguration abilities = new YamlConfiguration();
        abilities.set(PLAYER + ".id", "archer");
        abilities.set(PLAYER + ".data.cooldowns.1", 900000L);
        core.abilities().loadSession(abilities);
    }

    private void verifyRunning(int kills, Material block) throws Exception {
        require(!game.isRecovering() && game.state() == GameState.RUNNING, "Running match did not resume");
        require(game.teamAssignments().get(PLAYER) == GodTeam.RED, "Team assignment lost");
        require(map(game, "kills").get(PLAYER).equals(kills), "Kill count lost");
        require(game.isEliminated(GodTeam.BLUE) && game.observers().contains(OBSERVER), "Eliminations/observers lost");
        require(setOf(game, "preparedPlayerInventories").contains(PLAYER), "Inventory preparation marker lost");
        require(core.abilities().assignedAbilities().get(PLAYER).id().equals("archer"), "Ability was rerolled/lost");
        YamlConfiguration abilities = new YamlConfiguration();
        core.abilities().saveSession(abilities);
        require(abilities.getLong(PLAYER + ".data.cooldowns.1") > 800000L, "Cooldown was reset");
        require(Bukkit.getWorld("recovery-arena").getBlockAt(1, 80, 1).getType() == block, "Running map was reset/lost");
        require(get(game, "activeGameWorldSnapshotName") != null, "Original snapshot reference lost");
    }

    private void advance(int phase, String marker) throws Exception {
        Files.write(phaseFile.toPath(), String.valueOf(phase).getBytes(StandardCharsets.UTF_8));
        getLogger().info(marker);
    }
    private void runChecked(Checked action) {
        try { action.run(); } catch (Throwable ex) { getLogger().log(java.util.logging.Level.SEVERE, "RECOVERY REGRESSION FAILED", ex); }
    }
    private interface Checked { void run() throws Exception; }
    private static Object get(Object object, String name) throws Exception { Field f = object.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(object); }
    private static void set(Object object, String name, Object value) throws Exception { Field f = object.getClass().getDeclaredField(name); f.setAccessible(true); f.set(object, value); }
    @SuppressWarnings("unchecked") private static Map<Object, Object> map(Object object, String name) throws Exception { return (Map<Object, Object>) get(object, name); }
    @SuppressWarnings("unchecked") private static Set<Object> setOf(Object object, String name) throws Exception { return (Set<Object>) get(object, name); }
    private static Object invoke(Object object, String name) throws Exception { Method m = object.getClass().getDeclaredMethod(name); m.setAccessible(true); return m.invoke(object); }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
