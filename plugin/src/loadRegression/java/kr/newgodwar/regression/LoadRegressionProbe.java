package kr.newgodwar.regression;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.game.GodTeam;
import kr.newgodwar.game.GameState;
import kr.newgodwar.util.InventoryItems;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.*;
import java.lang.management.*;
import java.lang.reflect.*;
import java.util.*;

/** Real socket clients drive the events; this fixture only arranges and measures the match. */
public final class LoadRegressionProbe extends JavaPlugin implements Listener {
    private NewGodWarPlugin core;
    private World world;
    private String phase;
    private long started, gcStart, moves, interactions, commands, joins, quits, spent, damageEvents;
    private long peakHeap;
    private int peakEntities, peakTasks;
    private final List<Double> ticks = new ArrayList<Double>();
    private final Map<UUID, Integer> stones = new HashMap<UUID, Integer>();
    private static final String[] ABILITIES = {"zeus", "frost", "athena", "demeter", "hephaestus", "thor", "poseidon", "blacksmith"};

    @Override public void onEnable() {
        if (!getServer().getIp().equals("127.0.0.1")) throw new IllegalStateException("Load fixture requires a localhost-only server");
        core = (NewGodWarPlugin) Bukkit.getPluginManager().getPlugin("NewGodWar");
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTask(this, () -> {
            try {
                exportProtocol();
                registerTickMonitor();
                world = Bukkit.getWorlds().get(0);
                for (int x = 0; x < 48; x++) for (int z = 0; z < 48; z++) world.getBlockAt(x, 64, z).setType(Material.STONE);
                world.setSpawnLocation(8, 65, 8);
                core.game().setLobby(new Location(world, 8.5, 65, 8.5));
                int index = 0;
                for (GodTeam team : core.game().activeTeams()) {
                    core.game().setSpawn(team, new Location(world, 8.5 + index * 10, 65, 8.5));
                    org.bukkit.block.Block block = world.getBlockAt(3 + index++ * 10, 65, 3);
                    block.setType(Material.DIAMOND_BLOCK);
                    core.game().setTemple(team, block);
                }
                Bukkit.getScheduler().runTaskTimer(this, () -> sample(), 20L, 20L);
                getLogger().info("LOAD READY");
            } catch (Throwable ex) { fail(ex); }
        });
    }

    @SuppressWarnings("unchecked") private void registerTickMonitor() throws Exception {
        Class<? extends Event> type = (Class<? extends Event>) Class.forName("com.destroystokyo.paper.event.server.ServerTickEndEvent");
        Method duration = type.getMethod("getTickDuration");
        Bukkit.getPluginManager().registerEvent(type, this, EventPriority.MONITOR, (listener, event) -> {
            if (phase != null) try { ticks.add(((Number) duration.invoke(event)).doubleValue()); }
            catch (ReflectiveOperationException ex) { throw new EventException(ex); }
        }, this);
    }

    private void exportProtocol() throws Exception {
        Properties properties = new Properties();
        Object version = Class.forName("net.minecraft.SharedConstants").getMethod("getCurrentVersion").invoke(null);
        properties.setProperty("version", String.valueOf(Class.forName("net.minecraft.WorldVersion").getMethod("protocolVersion").invoke(version)));
        Class<?> provider = Class.forName("net.minecraft.network.ProtocolInfo$DetailsProvider");
        Class<?> detailsType = Class.forName("net.minecraft.network.ProtocolInfo$Details");
        Class<?> visitor = Class.forName("net.minecraft.network.ProtocolInfo$Details$PacketVisitor");
        for (String state : Arrays.asList("login", "configuration", "game")) {
            String title = Character.toUpperCase(state.charAt(0)) + state.substring(1);
            Class<?> protocols = Class.forName("net.minecraft.network.protocol." + state + "." + title + "Protocols");
            for (String direction : Arrays.asList("SERVERBOUND", "CLIENTBOUND")) {
                Object template = protocols.getField(direction + "_TEMPLATE").get(null);
                Object details = provider.getMethod("details").invoke(template);
                Object receiver = Proxy.newProxyInstance(visitor.getClassLoader(), new Class<?>[]{visitor}, (proxy, method, args) -> {
                    if (method.getName().equals("accept")) {
                        String id = String.valueOf(args[0].getClass().getMethod("id").invoke(args[0]));
                        properties.setProperty(state + "." + direction.toLowerCase(Locale.ROOT) + "." + id.replace("minecraft:", ""), String.valueOf(args[1]));
                    }
                    return null;
                });
                detailsType.getMethod("listPackets", visitor).invoke(details, receiver);
            }
        }
        try (OutputStream out = new FileOutputStream("load-protocol.properties")) { properties.store(out, "Packet IDs read from the running Paper server"); }
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equals("loadping")) {
            if (sender instanceof Player && args.length == 1) sender.sendMessage("LOADPONG:" + args[0] + ":" + System.currentTimeMillis());
            return true;
        }
        if (!(sender instanceof ConsoleCommandSender)) return true;
        try {
            String action = args[0];
            if (action.equals("start")) {
                core.getConfig().set("game.urf.enabled", args.length > 1 && args[1].equals("urf"));
                List<Player> players = players();
                for (int i = 0; i < players.size(); i++) core.game().assign(players.get(i), GodTeam.values()[i % core.game().activeTeams().size()]);
                core.game().start();
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    try {
                        require(core.game().state() == GameState.RUNNING, "Match did not reach RUNNING");
                        for (Player player : players()) {
                            int i = Integer.parseInt(player.getName().substring(4));
                            player.teleport(new Location(world, 8.5 + (i % 8) * 3, 65, 8.5 + (i / 8) * 3, i % 2 == 0 ? -90 : 90, 0));
                            player.setMaxHealth(200); player.setHealth(200);
                            core.abilities().set(player, core.abilities().registry().get(ABILITIES[i % ABILITIES.length]));
                            player.getInventory().clear();
                            player.getInventory().setItem(0, new ItemStack(Material.BLAZE_ROD));
                            player.getInventory().setHeldItemSlot(0);
                            refill(player);
                        }
                        getLogger().info("LOAD MATCH READY " + players.size());
                    } catch (Throwable ex) { fail(ex); }
                }, 100L);
            } else if (action.equals("begin")) {
                phase = args[1]; ticks.clear(); moves = interactions = commands = joins = quits = spent = damageEvents = 0;
                peakHeap = 0; peakEntities = peakTasks = 0; stones.clear();
                for (Player player : players()) stones.put(player.getUniqueId(), InventoryItems.count(player.getInventory(), Material.COBBLESTONE));
                started = System.nanoTime(); gcStart = gcMillis();
                getLogger().info("LOAD MEASURE " + phase);
            } else if (action.equals("end")) {
                writeResult(); phase = null;
            } else if (action.equals("stop")) {
                core.game().stop(false);
                verifyClean(false);
                getLogger().info("LOAD STOP CLEAN " + players().size());
            } else if (action.equals("verify")) {
                require(players().size() == Integer.parseInt(args[1]), "Unexpected online count");
                verifyClean(true);
                getLogger().info("LOAD CLEANUP PASS " + players().size());
            } else if (action.equals("online")) {
                require(players().size() == Integer.parseInt(args[1]), "Unexpected online count " + players().size());
                getLogger().info("LOAD ONLINE " + players().size());
            }
        } catch (Throwable ex) { fail(ex); }
        return true;
    }

    private void verifyClean(boolean allReturned) throws Exception {
        require(core.game().state() == GameState.ENDED, "Match did not end");
        for (Player player : players()) {
            require(player.getGameMode() == org.bukkit.GameMode.SURVIVAL, "Spectator leaked");
            for (ItemStack item : player.getInventory().getContents()) require(item == null || item.getType() == Material.AIR, "Inventory leaked for " + player.getName());
            require(core.game().teamOf(player) == null && core.abilities().get(player) == null, "Team/ability leaked");
        }
        if (allReturned) {
            Field field = core.game().getClass().getDeclaredField("pendingPlayerCleanup"); field.setAccessible(true);
            require(((Map<?, ?>) field.get(core.game())).isEmpty(), "Pending cleanup leaked");
            Field parts = Class.forName("kr.newgodwar.ability.feedback.ObjectEffects").getDeclaredField("totalParts"); parts.setAccessible(true);
            require(parts.getInt(null) == 0, "Ability display entities leaked after stop");
        }
    }

    private void sample() {
        if (phase != null) {
            Runtime runtime = Runtime.getRuntime();
            peakHeap = Math.max(peakHeap, runtime.totalMemory() - runtime.freeMemory());
            peakEntities = Math.max(peakEntities, world.getEntities().size());
            int tasks = 0;
            for (org.bukkit.scheduler.BukkitTask task : Bukkit.getScheduler().getPendingTasks()) if (task.getOwner() == core) tasks++;
            peakTasks = Math.max(peakTasks, tasks);
        }
        if (!core.game().isRunning()) return;
        for (Player player : players()) {
            if (player.isDead()) continue;
            player.setHealth(player.getMaxHealth()); player.setFoodLevel(20); player.setFireTicks(0);
            int current = InventoryItems.count(player.getInventory(), Material.COBBLESTONE);
            Integer previous = stones.get(player.getUniqueId());
            if (phase != null && previous != null) spent += Math.max(0, previous - current);
            if (current < 128) { refill(player); current = InventoryItems.count(player.getInventory(), Material.COBBLESTONE); }
            stones.put(player.getUniqueId(), current);
        }
    }

    private void refill(Player player) {
        ItemStack item = new ItemStack(Material.COBBLESTONE, 64);
        ItemMeta meta = item.getItemMeta(); meta.setDisplayName("Load-test resource"); item.setItemMeta(meta);
        for (int slot = 9; slot < 18; slot++) player.getInventory().setItem(slot, item.clone());
    }

    private List<Player> players() {
        List<Player> result = new ArrayList<Player>();
        for (Player player : Bukkit.getOnlinePlayers()) if (player.getName().startsWith("Load")) result.add(player);
        result.sort(Comparator.comparing(Player::getName)); return result;
    }
    @EventHandler public void onMove(PlayerMoveEvent event) { if (phase != null) moves++; }
    @EventHandler public void onInteract(PlayerInteractEvent event) { if (phase != null) interactions++; }
    @EventHandler public void onCommand(PlayerCommandPreprocessEvent event) { if (phase != null) commands++; }
    @EventHandler public void onJoin(PlayerJoinEvent event) { if (phase != null) joins++; }
    @EventHandler public void onQuit(PlayerQuitEvent event) { if (phase != null) quits++; }
    @EventHandler public void onDamage(EntityDamageByEntityEvent event) { if (phase != null) damageEvents++; }

    private void writeResult() throws Exception {
        double seconds = (System.nanoTime() - started) / 1e9;
        List<Double> sorted = new ArrayList<Double>(ticks); Collections.sort(sorted);
        double sum = 0; int slow = 0; for (double tick : ticks) { sum += tick; if (tick > 50) slow++; }
        Properties result = new Properties();
        result.setProperty("phase", phase);
        result.setProperty("players", "" + players().size());
        result.setProperty("seconds", "" + seconds);
        result.setProperty("ticks", "" + ticks.size());
        result.setProperty("tps", "" + Math.min(20, ticks.size() / seconds));
        result.setProperty("tick_ms_mean", "" + sum / Math.max(1, ticks.size()));
        result.setProperty("tick_ms_p95", "" + percentile(sorted, .95));
        result.setProperty("tick_ms_p99", "" + percentile(sorted, .99));
        result.setProperty("tick_ms_max", "" + percentile(sorted, 1));
        result.setProperty("ticks_over_50ms", "" + slow);
        result.setProperty("peak_heap_mb", "" + peakHeap / 1048576.0);
        result.setProperty("gc_ms", "" + (gcMillis() - gcStart));
        result.setProperty("peak_entities", "" + peakEntities);
        result.setProperty("peak_plugin_tasks", "" + peakTasks);
        result.setProperty("moves", "" + moves); result.setProperty("interactions", "" + interactions);
        result.setProperty("commands", "" + commands); result.setProperty("joins", "" + joins); result.setProperty("quits", "" + quits);
        result.setProperty("resource_spent", "" + spent);
        result.setProperty("damage_events", "" + damageEvents);
        try (OutputStream out = new FileOutputStream("load-result-" + phase + ".properties")) { result.store(out, "Measured real client load"); }
        getLogger().info("LOAD RESULT " + phase + " players=" + players().size() + " tps=" + result.getProperty("tps") + " p95=" + result.getProperty("tick_ms_p95"));
    }
    private static double percentile(List<Double> sorted, double p) { return sorted.isEmpty() ? 0 : sorted.get(Math.min(sorted.size() - 1, (int) Math.ceil(sorted.size() * p) - 1)); }
    private static long gcMillis() { long total = 0; for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) total += Math.max(0, gc.getCollectionTime()); return total; }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private void fail(Throwable ex) { getLogger().log(java.util.logging.Level.SEVERE, "LOAD FAILED", ex); }
}
