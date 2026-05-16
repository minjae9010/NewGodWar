package kr.newgodwar.game;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.ability.AbilityManager;
import kr.newgodwar.ability.api.AbilityDefinition;
import kr.newgodwar.nms.NmsAdapter;
import kr.newgodwar.util.BukkitCompat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class GameManager {

    private final NewGodWarPlugin plugin;
    private final AbilityManager abilityManager;
    private final NmsAdapter nmsAdapter;
    private final GameRuleController gameRuleController;
    private final Map<UUID, GodTeam> teams = new HashMap<UUID, GodTeam>();
    private final Map<GodTeam, TempleLocation> temples = new EnumMap<GodTeam, TempleLocation>(GodTeam.class);
    private final Set<GodTeam> eliminatedTeams = new HashSet<GodTeam>();
    private final Map<UUID, Integer> kills = new HashMap<UUID, Integer>();

    private GameState state = GameState.WAITING;
    private Scoreboard scoreboard;
    private int waterHealTask = -1;

    public GameManager(NewGodWarPlugin plugin, AbilityManager abilityManager, NmsAdapter nmsAdapter) {
        this.plugin = plugin;
        this.abilityManager = abilityManager;
        this.nmsAdapter = nmsAdapter;
        this.gameRuleController = new GameRuleController(plugin);
        loadTemples();
        setupScoreboard();
    }

    public void shutdown() {
        stop(false);
    }

    public GameState state() {
        return state;
    }

    public Map<GodTeam, TempleLocation> temples() {
        return Collections.unmodifiableMap(temples);
    }

    public void reloadSettings() {
        temples.clear();
        loadTemples();
        setupScoreboard();
    }

    public void applyGameRules() {
        gameRuleController.applyConfiguredRules();
    }

    public void restoreGameRules() {
        gameRuleController.restorePreviousRules();
    }

    public GodTeam teamOf(Player player) {
        return teams.get(player.getUniqueId());
    }

    public int killsOf(Player player) {
        Integer value = kills.get(player.getUniqueId());
        return value == null ? 0 : value;
    }

    public boolean isRunning() {
        return state == GameState.RUNNING;
    }

    public boolean isEliminated(GodTeam team) {
        return eliminatedTeams.contains(team);
    }

    public void assign(Player player, GodTeam team) {
        teams.put(player.getUniqueId(), team);
        addToScoreboard(player, team);
        nmsAdapter.sendActionBar(player, team.color() + team.defaultDisplayName() + " 팀에 배정되었습니다.");
    }

    public void leave(Player player) {
        teams.remove(player.getUniqueId());
        if (scoreboard != null) {
            for (Team team : scoreboard.getTeams()) {
                team.removeEntry(player.getName());
            }
        }
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public void autoBalance() {
        List<Player> players = new ArrayList<Player>();
        for (Player player : BukkitCompat.onlinePlayers()) {
            if (player.hasPermission("newgodwar.play")) {
                players.add(player);
            }
        }
        Collections.shuffle(players);
        GodTeam[] values = GodTeam.values();
        for (int i = 0; i < players.size(); i++) {
            assign(players.get(i), values[i % values.length]);
        }
    }

    public boolean setTemple(GodTeam team, Block block) {
        if (block == null || block.getType() != Material.DIAMOND_BLOCK) {
            return false;
        }
        TempleLocation location = TempleLocation.fromBlock(block);
        temples.put(team, location);
        plugin.getConfig().set("temples." + team.id(), location.serialize());
        plugin.saveConfig();
        return true;
    }

    public GodTeam templeTeam(Block block) {
        for (Map.Entry<GodTeam, TempleLocation> entry : temples.entrySet()) {
            if (entry.getValue().matches(block)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public boolean canDamage(Player attacker, Player victim) {
        if (!plugin.getConfig().getBoolean("game.friendly-fire", false)) {
            GodTeam attackerTeam = teamOf(attacker);
            GodTeam victimTeam = teamOf(victim);
            if (attackerTeam != null && attackerTeam == victimTeam) {
                return false;
            }
        }
        return true;
    }

    public void start() {
        if (plugin.getConfig().getBoolean("game.auto-balance-teams", true) && teams.isEmpty()) {
            autoBalance();
        }
        int minPlayers = plugin.getConfig().getInt("game.min-players", 2);
        if (teams.size() < minPlayers) {
            throw new IllegalStateException("최소 " + minPlayers + "명 이상 팀에 배정되어야 합니다.");
        }

        state = GameState.RUNNING;
        eliminatedTeams.clear();
        kills.clear();
        abilityManager.clear();
        gameRuleController.applyConfiguredRules();
        setupScoreboard();

        for (Player player : BukkitCompat.onlinePlayers()) {
            GodTeam team = teamOf(player);
            if (team == null) {
                continue;
            }
            addToScoreboard(player, team);
            AbilityDefinition ability = abilityManager.assignRandom(player);
            BukkitCompat.setSurvival(player);
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            if (plugin.getConfig().getBoolean("game.ability-roll-message", true)) {
                nmsAdapter.sendTitle(player, ability.name(), ability.description(), 10, 70, 20);
            }
        }

        String start = plugin.messages().get("game-start");
        Bukkit.broadcastMessage(plugin.messages().prefix() + start);
        startWaterHealTask();
    }

    public void stop(boolean announce) {
        state = GameState.ENDED;
        if (waterHealTask != -1) {
            Bukkit.getScheduler().cancelTask(waterHealTask);
            waterHealTask = -1;
        }
        if (announce) {
            Bukkit.broadcastMessage(plugin.messages().prefix() + plugin.messages().get("game-stop"));
        }
        gameRuleController.restorePreviousRules();
    }

    public void recordKill(Player killer) {
        if (killer == null) {
            return;
        }
        UUID uuid = killer.getUniqueId();
        Integer current = kills.get(uuid);
        kills.put(uuid, current == null ? 1 : current + 1);
        nmsAdapter.sendActionBar(killer, ChatColor.GOLD + "킬 수: " + killsOf(killer));
    }

    public void eliminate(GodTeam team, Player breaker) {
        if (team == null || eliminatedTeams.contains(team)) {
            return;
        }
        eliminatedTeams.add(team);
        String message = plugin.messages().get("team-eliminated").replace("{team}", team.coloredName());
        Bukkit.broadcastMessage(plugin.messages().prefix() + message);
        for (Player player : BukkitCompat.onlinePlayers()) {
            if (team == teamOf(player)) {
                setSpectator(player);
            }
        }
        checkWinner();
    }

    public void setSpectator(Player player) {
        BukkitCompat.setSpectatorOrAdventure(player);
        nmsAdapter.sendTitle(player, ChatColor.GRAY + "관전 모드", "팀이 탈락했거나 관리자가 관전으로 전환했습니다.", 10, 50, 10);
    }

    public void unsetSpectator(Player player) {
        BukkitCompat.setSurvival(player);
        nmsAdapter.sendActionBar(player, ChatColor.GREEN + "관전 모드가 해제되었습니다.");
    }

    public void sendTeamChat(Player sender, String message) {
        GodTeam team = teamOf(sender);
        if (team == null) {
            plugin.messages().send(sender, "&c팀에 소속되어 있지 않습니다.");
            return;
        }
        String formatted = team.color() + "[팀] " + sender.getName() + ": " + ChatColor.WHITE + message;
        for (Player player : BukkitCompat.onlinePlayers()) {
            if (team == teamOf(player)) {
                player.sendMessage(formatted);
            }
        }
    }

    public String statusLine() {
        return ChatColor.YELLOW + "상태: " + state
            + ChatColor.GRAY + " | " + ChatColor.YELLOW + "참가자: " + teams.size()
            + ChatColor.GRAY + " | " + ChatColor.YELLOW + "탈락팀: " + eliminatedTeams.size()
            + ChatColor.GRAY + " | " + ChatColor.YELLOW + plugin.versionSupport().minecraftVersion();
    }

    private void loadTemples() {
        for (GodTeam team : GodTeam.values()) {
            TempleLocation location = TempleLocation.deserialize(plugin.getConfig().getString("temples." + team.id()));
            if (location != null) {
                temples.put(team, location);
            }
        }
    }

    private void setupScoreboard() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            return;
        }
        scoreboard = manager.getNewScoreboard();
        for (GodTeam godTeam : GodTeam.values()) {
            Team team = scoreboard.registerNewTeam("gw_" + godTeam.id());
            team.setPrefix(godTeam.color().toString());
            team.setAllowFriendlyFire(plugin.getConfig().getBoolean("game.friendly-fire", false));
            BukkitCompat.setTeamColor(team, godTeam.color());
        }
        for (Player player : BukkitCompat.onlinePlayers()) {
            GodTeam team = teamOf(player);
            if (team != null) {
                addToScoreboard(player, team);
            }
        }
    }

    private void addToScoreboard(Player player, GodTeam godTeam) {
        if (scoreboard == null) {
            return;
        }
        Team team = scoreboard.getTeam("gw_" + godTeam.id());
        if (team != null) {
            team.addEntry(player.getName());
        }
        player.setScoreboard(scoreboard);
    }

    private void checkWinner() {
        Set<GodTeam> alive = new HashSet<GodTeam>();
        for (GodTeam team : teams.values()) {
            if (!eliminatedTeams.contains(team)) {
                alive.add(team);
            }
        }
        if (alive.size() == 1) {
            GodTeam winner = alive.iterator().next();
            String message = plugin.messages().get("winner").replace("{team}", winner.coloredName());
            Bukkit.broadcastMessage(plugin.messages().prefix() + message);
            for (Player player : BukkitCompat.onlinePlayers()) {
                if (winner == teamOf(player)) {
                    nmsAdapter.sendTitle(player, ChatColor.GOLD + "승리", winner.coloredName() + " 팀이 승리했습니다.", 10, 80, 20);
                    BukkitCompat.playLevelUp(player);
                }
            }
            stop(false);
        }
    }

    private void startWaterHealTask() {
        if (waterHealTask != -1) {
            Bukkit.getScheduler().cancelTask(waterHealTask);
        }
        long interval = Math.max(1L, plugin.getConfig().getLong("abilities.poseidon.water-heal-interval-seconds", 5L)) * 20L;
        waterHealTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (state == GameState.RUNNING) {
                    abilityManager.tick(BukkitCompat.onlinePlayers());
                }
            }
        }, interval, interval);
    }
}
