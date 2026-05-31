package kr.newgodwar.game;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.ability.AbilityManager;
import kr.newgodwar.ability.api.AbilityDefinition;
import kr.newgodwar.nms.NmsAdapter;
import kr.newgodwar.util.BukkitCompat;
import kr.newgodwar.util.GameTips;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class GameManager {

    private final NewGodWarPlugin plugin;
    private final AbilityManager abilityManager;
    private final NmsAdapter nmsAdapter;
    private final GameRuleController gameRuleController;
    private final Map<UUID, GodTeam> teams = new HashMap<UUID, GodTeam>();
    private final Map<GodTeam, TempleLocation> temples = new LinkedHashMap<GodTeam, TempleLocation>();
    private final Map<GodTeam, GameLocation> spawns = new LinkedHashMap<GodTeam, GameLocation>();
    private final Set<GodTeam> eliminatedTeams = new HashSet<GodTeam>();
    private final Set<UUID> observers = new HashSet<UUID>();
    private final Set<UUID> teamChatModePlayers = Collections.synchronizedSet(new HashSet<UUID>());
    private final Map<UUID, Integer> pendingSelection = new HashMap<UUID, Integer>();
    private final Map<UUID, Integer> kills = new HashMap<UUID, Integer>();
    private final Map<UUID, Scoreboard> playerScoreboards = new HashMap<UUID, Scoreboard>();
    private final Map<String, WorldSnapshot> worldSnapshots = new HashMap<String, WorldSnapshot>();
    private final Set<String> announcedPickaxeUnlocks = new HashSet<String>();
    private static final String CORE_EXPLOSION_UNLOCK_SECONDS_PATH = "core.explosion-unlock-seconds";
    private static final String SIDEBAR_OBJECTIVE_NAME = "gw_status";
    private static final PickaxeUnlockNotice[] PICKAXE_UNLOCK_NOTICES = new PickaxeUnlockNotice[] {
        new PickaxeUnlockNotice("나무", "core.pickaxe-unlock.wooden-seconds"),
        new PickaxeUnlockNotice("돌", "core.pickaxe-unlock.stone-seconds"),
        new PickaxeUnlockNotice("철", "core.pickaxe-unlock.iron-seconds"),
        new PickaxeUnlockNotice("다이아", "core.pickaxe-unlock.diamond-seconds")
    };

    private GameState state = GameState.WAITING;
    private int waterHealTask = -1;
    private int abilityNoticeTask = -1;
    private int gameTimerTask = -1;
    private int pickaxeUnlockNoticeTask = -1;
    private int readyTask = -1;
    private int gameTipTask = -1;
    private int readySecondsRemaining = 0;
    private int readyReminder = 0;
    private int nextGameTipIndex = 0;
    private long runningStartedAtMillis = 0L;
    private BossBar gameTimerBar;
    private boolean killtimeEndAnnounced = false;
    private boolean coreExplosionUnlockAnnounced = false;
    private boolean abilitySelectionWaitEnded = false;
    private GameLocation lobbyLocation;
    private String activeGameWorldName;
    private String activeGameWorldSnapshotName;
    private String activeGameWorldType;

    public GameManager(NewGodWarPlugin plugin, AbilityManager abilityManager, NmsAdapter nmsAdapter) {
        this.plugin = plugin;
        this.abilityManager = abilityManager;
        this.nmsAdapter = nmsAdapter;
        this.gameRuleController = new GameRuleController(plugin);
        GodTeam.reload(plugin.getConfig());
        loadTemples();
        loadSpawns();
        loadLobby();
        setupScoreboard();
    }

    public void shutdown() {
        stop(false, false);
    }

    public GameState state() {
        return state;
    }

    public Map<GodTeam, TempleLocation> temples() {
        return Collections.unmodifiableMap(temples);
    }

    public Map<GodTeam, GameLocation> spawns() {
        return Collections.unmodifiableMap(spawns);
    }

    public void reloadSettings() {
        GodTeam.reload(plugin.getConfig());
        cleanupRemovedTeams();
        temples.clear();
        spawns.clear();
        loadTemples();
        loadSpawns();
        loadLobby();
        setupScoreboard();
    }

    public void cleanupRemovedTeams() {
        Set<String> knownIds = new HashSet<String>(GodTeam.ids());
        List<UUID> removedPlayers = new ArrayList<UUID>();
        for (Map.Entry<UUID, GodTeam> entry : teams.entrySet()) {
            if (!knownIds.contains(entry.getValue().id()) || !isTeamEnabled(entry.getValue())) {
                removedPlayers.add(entry.getKey());
            }
        }
        for (UUID uuid : removedPlayers) {
            teams.remove(uuid);
            teamChatModePlayers.remove(uuid);
        }
        List<GodTeam> removedEliminated = new ArrayList<GodTeam>();
        for (GodTeam team : eliminatedTeams) {
            if (!knownIds.contains(team.id()) || !isTeamEnabled(team)) {
                removedEliminated.add(team);
            }
        }
        eliminatedTeams.removeAll(removedEliminated);
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

    public Map<UUID, GodTeam> teamAssignments() {
        return Collections.unmodifiableMap(teams);
    }

    public boolean isObserver(Player player) {
        return player != null && observers.contains(player.getUniqueId());
    }

    public Set<UUID> observers() {
        return Collections.unmodifiableSet(observers);
    }

    public int killsOf(Player player) {
        Integer value = kills.get(player.getUniqueId());
        return value == null ? 0 : value;
    }

    public boolean isRunning() {
        return state == GameState.RUNNING;
    }

    public long runningElapsedSeconds() {
        if (state != GameState.RUNNING || runningStartedAtMillis <= 0L) {
            return 0L;
        }
        return Math.max(0L, (System.currentTimeMillis() - runningStartedAtMillis) / 1000L);
    }

    public long killtimeRemainingSeconds() {
        if (state != GameState.RUNNING) {
            return 0L;
        }
        return Math.max(0L, killtimeDurationSeconds() - runningElapsedSeconds());
    }

    public boolean isCoreExplosionProtected() {
        if (!plugin.getConfig().getBoolean("core.protect-diamond-from-explosion", true)) {
            return false;
        }
        int unlockSeconds = coreExplosionUnlockSeconds();
        if (unlockSeconds < 0 || state != GameState.RUNNING) {
            return true;
        }
        return runningElapsedSeconds() < unlockSeconds;
    }

    public void refreshGameTimerBar() {
        if (state != GameState.RUNNING) {
            cancelGameTimerTask();
            return;
        }
        if (gameTimerBossBarEnabled()) {
            startGameTimerTask();
        } else {
            cancelGameTimerTask();
        }
    }

    public boolean isEliminated(GodTeam team) {
        return eliminatedTeams.contains(team);
    }

    public boolean isTeamEnabled(GodTeam team) {
        return team != null && plugin.getConfig().getBoolean("teams." + team.id() + ".enabled", true);
    }

    public List<GodTeam> activeTeams() {
        List<GodTeam> active = new ArrayList<GodTeam>();
        for (GodTeam team : GodTeam.values()) {
            if (isTeamEnabled(team)) {
                active.add(team);
            }
        }
        return active;
    }

    public void assign(Player player, GodTeam team) {
        if (!isTeamEnabled(team)) {
            throw new IllegalStateException("비활성화된 팀에는 배정할 수 없습니다.");
        }
        teams.put(player.getUniqueId(), team);
        refreshAllPlayerDisplays();
        nmsAdapter.sendActionBar(player, teamColoredName(team) + " 팀에 배정되었습니다.");
    }

    public void changeTeam(Player player, GodTeam team) {
        if (!isTeamEnabled(team)) {
            throw new IllegalStateException("비활성화된 팀에는 변경할 수 없습니다.");
        }
        if (state == GameState.RUNNING && eliminatedTeams.contains(team)) {
            throw new IllegalStateException("탈락한 팀으로는 변경할 수 없습니다.");
        }
        teams.put(player.getUniqueId(), team);
        if (state == GameState.RUNNING) {
            observers.remove(player.getUniqueId());
            BukkitCompat.setSurvival(player);
            if (abilityManager.get(player) != null) {
                abilityManager.reapply(player);
            }
        }
        refreshAllPlayerDisplays();
        nmsAdapter.sendActionBar(player, teamColoredName(team) + " 팀으로 변경되었습니다.");
    }

    public void leave(Player player) {
        boolean removedPendingSelection = pendingSelection.remove(player.getUniqueId()) != null;
        teams.remove(player.getUniqueId());
        teamChatModePlayers.remove(player.getUniqueId());
        playerScoreboards.remove(player.getUniqueId());
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            player.setScoreboard(manager.getMainScoreboard());
        }
        resetPlayerListName(player);
        if (removedPendingSelection) {
            completeAbilitySelectionIfReady();
        }
        refreshAllPlayerDisplays();
    }

    public void autoBalance() {
        List<Player> players = new ArrayList<Player>();
        for (Player player : BukkitCompat.onlinePlayers()) {
            if (player.hasPermission("newgodwar.play") && !isObserver(player)) {
                players.add(player);
            }
        }
        Collections.shuffle(players);
        List<GodTeam> values = activeTeams();
        if (values.isEmpty()) {
            return;
        }
        for (int i = 0; i < players.size(); i++) {
            assign(players.get(i), values.get(i % values.size()));
        }
    }

    public boolean setTemple(GodTeam team, Block block) {
        if (block == null || block.getType() != Material.DIAMOND_BLOCK) {
            return false;
        }
        TempleLocation location = TempleLocation.fromBlock(block);
        temples.put(team, location);
        saveMapTempleIfSelected(team, location.serialize(), block.getWorld().getName());
        plugin.getConfig().set("temples." + team.id(), location.serialize());
        plugin.saveConfig();
        return true;
    }

    public boolean setSpawn(GodTeam team, Location location) {
        if (team == null || location == null) {
            return false;
        }
        GameLocation spawn = GameLocation.from(location);
        spawns.put(team, spawn);
        saveMapSpawnIfSelected(team, spawn.serialize(), location.getWorld().getName());
        plugin.getConfig().set("spawns." + team.id(), spawn.serialize());
        plugin.saveConfig();
        return true;
    }

    public boolean setLobby(Location location) {
        if (location == null) {
            return false;
        }
        lobbyLocation = GameLocation.from(location);
        plugin.getConfig().set("lobby.location", lobbyLocation.serialize());
        plugin.saveConfig();
        return true;
    }

    public Location lobbyLocation() {
        return lobbyLocation == null ? null : lobbyLocation.toLocation();
    }

    public boolean hasLobbyLocation() {
        return lobbyLocation() != null;
    }

    public boolean teleportToLobby(Player player) {
        if (player == null) {
            return false;
        }
        Location location = lobbyLocation();
        if (location == null) {
            return false;
        }
        player.teleport(location);
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
            if (attackerTeam != null && attackerTeam.equals(victimTeam)) {
                return false;
            }
        }
        return true;
    }

    public Location respawnLocation(Player player, boolean bedSpawn) {
        if (player == null || state != GameState.RUNNING) {
            return null;
        }
        if (!plugin.getConfig().getBoolean("game.ignore-bed", true) && bedSpawn) {
            return null;
        }
        GodTeam team = teamOf(player);
        if (team == null) {
            return null;
        }
        GameLocation spawn = spawns.get(team);
        return spawn == null ? null : spawn.toLocation();
    }

    public void start() {
        if (state == GameState.READY || state == GameState.RUNNING) {
            throw new IllegalStateException("게임이 이미 시작 준비 중이거나 진행 중입니다.");
        }
        if (plugin.getConfig().getBoolean("game.auto-balance-teams", true) && teams.isEmpty()) {
            autoBalance();
        }
        int minPlayers = plugin.getConfig().getInt("game.min-players", 2);
        List<Player> participants = participants();
        if (participants.size() < minPlayers) {
            throw new IllegalStateException("최소 " + minPlayers + "명 이상 팀에 배정되어야 합니다.");
        }
        restoreTempleBlocks();
        validateStartSettings();
        prepareGameWorldSnapshot();

        state = GameState.READY;
        eliminatedTeams.clear();
        kills.clear();
        pendingSelection.clear();
        abilitySelectionWaitEnded = false;
        clearPotionEffects(participants);
        abilityManager.clear();
        setupScoreboard();

        Bukkit.broadcastMessage(plugin.messages().prefix() + ChatColor.GOLD + "게임 시작 준비를 시작합니다.");
        broadcastStartSettings();
        nextGameTipIndex = GameTips.broadcastOnStart(plugin);

        int rerollCount = abilityRerollCount();
        for (Player player : participants) {
            AbilityDefinition ability = abilityManager.assignRandom(player);
            if (plugin.getConfig().getBoolean("game.select-right", true) && rerollCount > 0) {
                pendingSelection.put(player.getUniqueId(), rerollCount);
            }
            if (plugin.getConfig().getBoolean("game.ability-roll-message", true)) {
                nmsAdapter.sendTitle(player, ability.name(), ability.description(), 10, 70, 20);
            }
        }
        refreshAllPlayerDisplays();

        if (!pendingSelection.isEmpty()) {
            Bukkit.broadcastMessage(ChatColor.YELLOW + "능력 재추첨 기회가 주어졌습니다. "
                + ChatColor.AQUA + "/t yes" + ChatColor.WHITE + " 또는 " + ChatColor.RED + "/t no"
                + ChatColor.WHITE + " 로 능력을 확정해주세요.");
        } else {
            abilitySelectionWaitEnded = true;
        }
        startReadyTask();
    }

    public AbilityDefinition startTest(Player player, AbilityDefinition preferredAbility) {
        if (player == null) {
            throw new IllegalStateException("테스트할 플레이어를 찾을 수 없습니다.");
        }

        state = GameState.RUNNING;
        runningStartedAtMillis = System.currentTimeMillis();
        killtimeEndAnnounced = false;
        coreExplosionUnlockAnnounced = false;
        eliminatedTeams.clear();
        kills.clear();
        abilityManager.clear();
        BukkitCompat.clearPotionEffects(player);
        gameRuleController.applyConfiguredRules();
        setupScoreboard();

        GodTeam team = teamOf(player);
        if (team == null) {
            List<GodTeam> teams = activeTeams();
            team = teams.isEmpty() ? GodTeam.RED : teams.get(0);
            assign(player, team);
        }
        AbilityDefinition ability = preferredAbility == null ? abilityManager.assignRandom(player) : preferredAbility;
        if (preferredAbility != null) {
            abilityManager.set(player, preferredAbility);
        }
        BukkitCompat.setSurvival(player);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        nmsAdapter.sendTitle(player, ChatColor.GOLD + "능력 테스트", ability.name(), 10, 60, 10);
        startGameTimerTask();
        startPickaxeUnlockNoticeTask();
        refreshAllPlayerDisplays();
        startWaterHealTask();
        return ability;
    }

    public AbilityDefinition joinMidGame(Player player, GodTeam requestedTeam) {
        return joinMidGame(player, requestedTeam, true);
    }

    private AbilityDefinition joinMidGame(Player player, GodTeam requestedTeam, boolean requireEnabled) {
        if (state != GameState.RUNNING) {
            throw new IllegalStateException("게임 진행 중에만 중간 참여를 사용할 수 있습니다.");
        }
        if (requireEnabled && !plugin.getConfig().getBoolean("game.allow-mid-join", true)) {
            throw new IllegalStateException("현재 설정에서 중간 참여가 꺼져 있습니다.");
        }
        GodTeam currentTeam = teamOf(player);
        boolean activeParticipant = currentTeam != null
            && abilityManager.get(player) != null
            && !eliminatedTeams.contains(currentTeam)
            && !isObserver(player);
        if (activeParticipant) {
            throw new IllegalStateException("이미 게임에 참여 중입니다.");
        }

        GodTeam team = requestedTeam == null ? smallestJoinableTeam() : requestedTeam;
        if (team == null || eliminatedTeams.contains(team)) {
            throw new IllegalStateException("참여 가능한 팀이 없습니다.");
        }

        AbilityDefinition existingAbility = abilityManager.get(player);
        observers.remove(player.getUniqueId());
        assign(player, team);
        preparePlayerForGame(player);
        teleportToTeamSpawn(player, team);
        AbilityDefinition ability;
        if (existingAbility == null) {
            ability = abilityManager.assignRandom(player);
        } else {
            ability = existingAbility;
            abilityManager.reapply(player);
            player.sendMessage(ChatColor.GREEN + "기존 능력을 유지한 채 중간 참여했습니다.");
        }
        BukkitCompat.setSurvival(player);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        nmsAdapter.sendTitle(player, ChatColor.GREEN + "중간 참여", ability.name(), 10, 60, 10);
        Bukkit.broadcastMessage(plugin.messages().prefix() + teamColoredName(team) + ChatColor.YELLOW
            + " 팀에 " + player.getName() + " 님이 중간 참여했습니다.");
        refreshAllPlayerDisplays();
        return ability;
    }

    private AbilityDefinition joinEliminatedMidGame(Player player, GodTeam preferredTeam) {
        if (state != GameState.RUNNING) {
            throw new IllegalStateException("게임 진행 중에만 중간 참여를 사용할 수 있습니다.");
        }

        GodTeam team = smallestJoinableTeam(preferredTeam);
        if (team == null || eliminatedTeams.contains(team)) {
            throw new IllegalStateException("참여 가능한 팀이 없습니다.");
        }

        AbilityDefinition ability = abilityManager.get(player);
        observers.remove(player.getUniqueId());
        assign(player, team);
        teleportToTeamSpawn(player, team);
        if (ability == null) {
            ability = abilityManager.assignRandom(player);
        }
        BukkitCompat.setSurvival(player);
        nmsAdapter.sendTitle(player, ChatColor.GREEN + "중간 참여", ability.name(), 10, 60, 10);
        Bukkit.broadcastMessage(plugin.messages().prefix() + teamColoredName(team) + ChatColor.YELLOW
            + " 팀에 " + player.getName() + " 님이 중간 참여했습니다.");
        refreshAllPlayerDisplays();
        return ability;
    }

    public void stop(boolean announce) {
        stop(announce, true);
    }

    private void stop(boolean announce, boolean resetGameWorld) {
        List<Player> endingPlayers = endingPlayers();
        state = GameState.ENDED;
        runningStartedAtMillis = 0L;
        killtimeEndAnnounced = false;
        if (readyTask != -1) {
            Bukkit.getScheduler().cancelTask(readyTask);
            readyTask = -1;
        }
        if (waterHealTask != -1) {
            Bukkit.getScheduler().cancelTask(waterHealTask);
            waterHealTask = -1;
        }
        if (abilityNoticeTask != -1) {
            Bukkit.getScheduler().cancelTask(abilityNoticeTask);
            abilityNoticeTask = -1;
        }
        cancelPickaxeUnlockNoticeTask();
        cancelGameTimerTask();
        if (gameTipTask != -1) {
            Bukkit.getScheduler().cancelTask(gameTipTask);
            gameTipTask = -1;
        }
        pendingSelection.clear();
        abilitySelectionWaitEnded = false;
        teamChatModePlayers.clear();
        if (announce) {
            Bukkit.broadcastMessage(plugin.messages().prefix() + plugin.messages().get("game-stop"));
        }
        revealAbilitiesOnEnd();
        abilityManager.clear();
        clearPotionEffects(BukkitCompat.onlinePlayers());
        gameRuleController.restorePreviousRules();
        restoreWorldSettings();
        clearEndingInventories(endingPlayers);
        teleportEndingPlayersToLobby(endingPlayers);
        if (resetGameWorld) {
            resetConfiguredGameWorld();
        }
        clearGameParticipation();
        refreshAllPlayerDisplays();
    }

    public void recordKill(Player killer) {
        if (killer == null) {
            return;
        }
        UUID uuid = killer.getUniqueId();
        Integer current = kills.get(uuid);
        kills.put(uuid, current == null ? 1 : current + 1);
        nmsAdapter.sendActionBar(killer, ChatColor.GOLD + "킬 수: " + killsOf(killer));
        refreshPlayerDisplay(killer);
    }

    public void eliminate(GodTeam team, Player breaker) {
        if (team == null || eliminatedTeams.contains(team)) {
            return;
        }
        eliminatedTeams.add(team);
        GodTeam breakerTeam = breaker == null ? null : teamOf(breaker);
        String message = plugin.messages().get("team-eliminated").replace("{team}", teamColoredName(team));
        Bukkit.broadcastMessage(plugin.messages().prefix() + message);
        List<Player> eliminatedPlayers = new ArrayList<Player>();
        for (Player player : BukkitCompat.onlinePlayers()) {
            if (team.equals(teamOf(player))) {
                eliminatedPlayers.add(player);
            }
        }
        for (Player player : eliminatedPlayers) {
            handleEliminatedPlayer(player, team, breakerTeam);
        }
        refreshAllPlayerDisplays();
        checkWinner();
    }

    public boolean handleEliminatedJoin(Player player) {
        GodTeam team = teamOf(player);
        if (team == null || !eliminatedTeams.contains(team)) {
            return false;
        }
        if ("none".equals(eliminatedPlayerAction())) {
            return false;
        }
        handleEliminatedPlayer(player, team, null);
        return true;
    }

    private void handleEliminatedPlayer(Player player, GodTeam team, GodTeam breakerTeam) {
        String action = eliminatedPlayerAction();
        if ("none".equals(action)) {
            return;
        }
        if ("kick".equals(action)) {
            kickEliminatedPlayer(player, team);
            return;
        }
        if ("midjoin".equals(action) && aliveTeamCount() > 1) {
            try {
                joinEliminatedMidGame(player, breakerTeam);
                return;
            } catch (IllegalStateException ex) {
                player.sendMessage(ChatColor.RED + "자동 중간 참여 실패: " + ex.getMessage());
            }
        }
        setSpectator(player);
    }

    private void kickEliminatedPlayer(Player player, GodTeam team) {
        String message = plugin.messages().get("team-eliminated-kick").replace("{team}", teamColoredName(team));
        player.kickPlayer(message);
    }

    private String eliminatedPlayerAction() {
        String action = plugin.getConfig().getString("game.eliminated-player-action", "spectator");
        if (action == null) {
            return "spectator";
        }
        action = action.toLowerCase(Locale.ROOT).trim();
        if ("kick".equals(action) || "midjoin".equals(action) || "none".equals(action) || "spectator".equals(action)) {
            return action;
        }
        return "spectator";
    }

    public void setSpectator(Player player) {
        BukkitCompat.setSpectatorOrAdventure(player);
        nmsAdapter.sendTitle(player, ChatColor.GRAY + "관전 모드", "팀이 탈락했거나 관리자가 관전으로 전환했습니다.", 10, 50, 10);
    }

    public void unsetSpectator(Player player) {
        BukkitCompat.setSurvival(player);
        nmsAdapter.sendActionBar(player, ChatColor.GREEN + "관전 모드가 해제되었습니다.");
    }

    public boolean toggleObserver(Player player) {
        UUID uuid = player.getUniqueId();
        boolean enabled;
        if (observers.contains(uuid)) {
            observers.remove(uuid);
            unsetSpectator(player);
            enabled = false;
        } else {
            observers.add(uuid);
            pendingSelection.remove(uuid);
            setSpectator(player);
            enabled = true;
        }
        completeAbilitySelectionIfReady();
        refreshAllPlayerDisplays();
        return enabled;
    }

    public boolean confirmAbility(Player player) {
        return player != null && pendingSelection.remove(player.getUniqueId()) != null;
    }

    public AbilityDefinition rerollAbility(Player player) {
        Integer remaining = pendingSelection.get(player.getUniqueId());
        if (remaining == null || remaining.intValue() <= 0) {
            return null;
        }
        AbilityDefinition ability = abilityManager.assignRandom(player);
        if (remaining.intValue() > 1) {
            pendingSelection.put(player.getUniqueId(), Integer.valueOf(remaining.intValue() - 1));
        } else {
            pendingSelection.remove(player.getUniqueId());
        }
        nmsAdapter.sendTitle(player, ability.name(), ability.description(), 10, 70, 20);
        refreshPlayerDisplay(player);
        return ability;
    }

    public int remainingAbilityRerolls(Player player) {
        Integer remaining = pendingSelection.get(player.getUniqueId());
        return remaining == null ? 0 : remaining.intValue();
    }

    public int skipAbilitySelection() {
        return skipAbilitySelection(plugin.getConfig().getInt("game.skip-ready-countdown-seconds", 5));
    }

    public int skipAbilitySelection(int countdownSeconds) {
        int count = pendingSelection.size();
        pendingSelection.clear();
        if (state == GameState.READY) {
            abilitySelectionWaitEnded = true;
            readySecondsRemaining = Math.max(0, countdownSeconds);
            if (readySecondsRemaining <= 0) {
                finishStart();
            }
        }
        return count;
    }

    public boolean hasPendingAbilitySelection() {
        return !pendingSelection.isEmpty();
    }

    public boolean completeAbilitySelectionIfReady() {
        if (state != GameState.READY || abilitySelectionWaitEnded || !pendingSelection.isEmpty()) {
            return false;
        }
        abilitySelectionWaitEnded = true;
        readyReminder = 0;
        int countdown = Math.max(0, plugin.getConfig().getInt("game.skip-ready-countdown-seconds", 5));
        readySecondsRemaining = Math.min(readySecondsRemaining, countdown);
        Bukkit.broadcastMessage(plugin.messages().prefix() + ChatColor.GREEN
            + "모든 플레이어가 능력을 확정했습니다. 확정 대기를 종료합니다.");
        refreshAllPlayerDisplays();
        if (readySecondsRemaining <= 0) {
            finishStart();
        }
        return true;
    }

    public void sendTeamChat(Player sender, String message) {
        GodTeam team = teamOf(sender);
        if (team == null) {
            plugin.messages().send(sender, "&c팀에 소속되어 있지 않습니다.");
            return;
        }
        String formatted = teamColor(team) + "[팀] " + sender.getName() + ": " + ChatColor.WHITE + message;
        for (Player player : BukkitCompat.onlinePlayers()) {
            if (team.equals(teamOf(player))) {
                player.sendMessage(formatted);
            }
        }
    }

    public boolean isTeamChatMode(Player player) {
        return player != null && teamChatModePlayers.contains(player.getUniqueId());
    }

    public boolean toggleTeamChatMode(Player player) {
        if (player == null || teamOf(player) == null) {
            return false;
        }
        UUID uuid = player.getUniqueId();
        synchronized (teamChatModePlayers) {
            if (teamChatModePlayers.contains(uuid)) {
                teamChatModePlayers.remove(uuid);
                return false;
            }
            teamChatModePlayers.add(uuid);
            return true;
        }
    }

    public String statusLine() {
        return ChatColor.YELLOW + "상태: " + state
            + ChatColor.GRAY + " | " + ChatColor.YELLOW + "참가자: " + teams.size()
            + ChatColor.GRAY + " | " + ChatColor.YELLOW + "옵저버: " + observers.size()
            + ChatColor.GRAY + " | " + ChatColor.YELLOW + "탈락팀: " + eliminatedTeams.size()
            + ChatColor.GRAY + " | " + ChatColor.YELLOW + plugin.versionSupport().minecraftVersion();
    }

    private void loadTemples() {
        String worldName = configuredGameWorldName();
        boolean migrated = false;
        for (GodTeam team : GodTeam.values()) {
            String mapPath = activeMapTemplePath(team);
            TempleLocation location = TempleLocation.deserialize(plugin.getConfig().getString(mapPath));
            if (location == null) {
                String legacy = plugin.getConfig().getString("temples." + team.id());
                if (worldName == null || locationValueMatchesWorld(legacy, worldName)) {
                    location = TempleLocation.deserialize(legacy);
                    if (worldName != null && location != null) {
                        plugin.getConfig().set(mapPath, legacy);
                        migrated = true;
                    }
                }
            }
            if (location != null) {
                temples.put(team, location);
            }
        }
        if (migrated) {
            plugin.saveConfig();
            plugin.getLogger().info("Migrated legacy temple locations into selected map '" + worldName + "'.");
        }
    }

    private void loadSpawns() {
        String worldName = configuredGameWorldName();
        boolean migrated = false;
        for (GodTeam team : GodTeam.values()) {
            String mapPath = activeMapSpawnPath(team);
            GameLocation location = GameLocation.deserialize(plugin.getConfig().getString(mapPath));
            if (location == null) {
                String legacy = plugin.getConfig().getString("spawns." + team.id());
                if (worldName == null || locationValueMatchesWorld(legacy, worldName)) {
                    location = GameLocation.deserialize(legacy);
                    if (worldName != null && location != null) {
                        plugin.getConfig().set(mapPath, legacy);
                        migrated = true;
                    }
                }
            }
            if (location != null) {
                spawns.put(team, location);
            }
        }
        if (migrated) {
            plugin.saveConfig();
            plugin.getLogger().info("Migrated legacy spawn locations into selected map '" + worldName + "'.");
        }
    }

    private String activeMapTemplePath(GodTeam team) {
        String worldName = configuredGameWorldName();
        return worldName == null ? "temples." + team.id() : mapTemplePath(worldName, team);
    }

    private String activeMapSpawnPath(GodTeam team) {
        String worldName = configuredGameWorldName();
        return worldName == null ? "spawns." + team.id() : mapSpawnPath(worldName, team);
    }

    private String mapTemplePath(String worldName, GodTeam team) {
        return "maps." + worldName + ".temples." + team.id();
    }

    private String mapSpawnPath(String worldName, GodTeam team) {
        return "maps." + worldName + ".spawns." + team.id();
    }

    private void saveMapTempleIfSelected(GodTeam team, String value, String locationWorldName) {
        String worldName = configuredGameWorldName();
        if (worldName != null && worldName.equalsIgnoreCase(locationWorldName)) {
            plugin.getConfig().set(mapTemplePath(worldName, team), value);
        }
    }

    private void saveMapSpawnIfSelected(GodTeam team, String value, String locationWorldName) {
        String worldName = configuredGameWorldName();
        if (worldName != null && worldName.equalsIgnoreCase(locationWorldName)) {
            plugin.getConfig().set(mapSpawnPath(worldName, team), value);
        }
    }

    private boolean locationValueMatchesWorld(String value, String worldName) {
        return value != null && value.regionMatches(true, 0, worldName + ",", 0, worldName.length() + 1);
    }

    private void loadLobby() {
        lobbyLocation = GameLocation.deserialize(plugin.getConfig().getString("lobby.location"));
    }

    private void prepareGameWorldSnapshot() {
        if (!gameWorldResetEnabled()) {
            clearActiveGameWorldSnapshot();
            return;
        }
        String worldName = configuredGameWorldName();
        if (worldName == null) {
            clearActiveGameWorldSnapshot();
            return;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            throw new IllegalStateException("게임 월드가 로드되어 있지 않습니다: " + worldName);
        }
        if (isLobbyWorld(world)) {
            throw new IllegalStateException("로비 월드는 게임 월드 자동 초기화 대상으로 사용할 수 없습니다.");
        }
        String activeWorldName = world.getName();
        String snapshotName = "active-game-world";
        String worldType = managedWorldType(activeWorldName);
        try {
            plugin.worldBackups().saveWorldSnapshot(world, snapshotName);
            activeGameWorldName = activeWorldName;
            activeGameWorldSnapshotName = snapshotName;
            activeGameWorldType = worldType;
            plugin.getLogger().info("Saved game world snapshot for '" + activeWorldName + "'.");
        } catch (IOException ex) {
            clearActiveGameWorldSnapshot();
            throw new IllegalStateException("게임 월드 백업 생성 실패: " + ex.getMessage());
        }
    }

    private void resetConfiguredGameWorld() {
        if (activeGameWorldName == null || activeGameWorldSnapshotName == null) {
            clearActiveGameWorldSnapshot();
            return;
        }
        if (!gameWorldResetEnabled()) {
            clearActiveGameWorldSnapshot();
            return;
        }
        String worldName = activeGameWorldName;
        try {
            if (!unloadActiveGameWorld(worldName)) {
                return;
            }
            plugin.worldBackups().restoreWorldSnapshot(worldName, activeGameWorldSnapshotName);
            World reloaded = Bukkit.createWorld(WorldBackupManager.creator(worldName, activeGameWorldType));
            if (reloaded == null) {
                plugin.getLogger().warning("Restored game world folder, but Bukkit did not load it: " + worldName);
            } else {
                plugin.getLogger().info("Reset and reloaded game world: " + worldName);
            }
        } catch (IOException ex) {
            plugin.getLogger().warning("Game world reset failed for '" + worldName + "': " + ex.getMessage());
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Game world reload failed for '" + worldName + "': " + ex.getMessage());
        } finally {
            clearActiveGameWorldSnapshot();
        }
    }

    private boolean unloadActiveGameWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return true;
        }
        if (isLobbyWorld(world)) {
            plugin.getLogger().warning("Skipping game world reset because it points at the lobby world: " + worldName);
            return false;
        }
        if (!world.getPlayers().isEmpty()) {
            plugin.getLogger().warning("Skipping game world reset because players are still in world '" + worldName + "'. Set a lobby with /godwar setlobby first.");
            return false;
        }
        if (!Bukkit.unloadWorld(world, false)) {
            plugin.getLogger().warning("Could not unload game world for reset: " + worldName);
            return false;
        }
        return true;
    }

    private void clearActiveGameWorldSnapshot() {
        activeGameWorldName = null;
        activeGameWorldSnapshotName = null;
        activeGameWorldType = null;
    }

    private boolean gameWorldResetEnabled() {
        return plugin.getConfig().getBoolean("world.reset-game-world-on-stop", true);
    }

    private String configuredGameWorldName() {
        String worldName = plugin.getConfig().getString("world.game-world", "");
        if (worldName == null || worldName.trim().length() == 0) {
            return null;
        }
        return worldName.trim();
    }

    private boolean isLobbyWorld(World world) {
        Location lobby = lobbyLocation();
        return lobby != null && lobby.getWorld() != null && lobby.getWorld().equals(world);
    }

    private String managedWorldType(String worldName) {
        for (Map<?, ?> entry : plugin.getConfig().getMapList("world.managed-worlds")) {
            Object name = entry.get("name");
            if (name != null && name.toString().equalsIgnoreCase(worldName)) {
                Object type = entry.get("type");
                String value = type == null ? "normal" : type.toString().toLowerCase(Locale.ROOT);
                return value.equals("void") || value.equals("flat") ? value : "normal";
            }
        }
        return "normal";
    }

    private List<Player> endingPlayers() {
        List<Player> players = new ArrayList<Player>();
        for (Player player : BukkitCompat.onlinePlayers()) {
            if (teamOf(player) != null || isObserver(player)) {
                players.add(player);
            }
        }
        return players;
    }

    private void teleportEndingPlayersToLobby(List<Player> players) {
        if (!plugin.getConfig().getBoolean("lobby.teleport-on-game-stop", true)) {
            return;
        }
        Location location = lobbyLocation();
        if (location == null) {
            return;
        }
        for (Player player : players) {
            BukkitCompat.setSurvival(player);
            player.teleport(location);
        }
    }

    private void clearEndingInventories(List<Player> players) {
        if (!plugin.getConfig().getBoolean("game.clear-inventory", true)
            || !plugin.getConfig().getBoolean("game.clear-inventory-on-stop", true)) {
            return;
        }
        for (Player player : players) {
            player.getInventory().clear();
            player.getInventory().setHelmet(null);
            player.getInventory().setChestplate(null);
            player.getInventory().setLeggings(null);
            player.getInventory().setBoots(null);
            try {
                player.getInventory().setItemInOffHand(null);
            } catch (Throwable ignored) {
            }
            player.setItemOnCursor(null);
            player.updateInventory();
        }
    }

    private void clearGameParticipation() {
        teams.clear();
        observers.clear();
        eliminatedTeams.clear();
        kills.clear();
        pendingSelection.clear();
        teamChatModePlayers.clear();
        playerScoreboards.clear();
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            for (Player player : BukkitCompat.onlinePlayers()) {
                player.setScoreboard(manager.getMainScoreboard());
                resetPlayerListName(player);
            }
        }
    }

    private void setupScoreboard() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            return;
        }
        playerScoreboards.clear();
        for (Player player : BukkitCompat.onlinePlayers()) {
            refreshPlayerDisplay(player);
        }
    }

    public void refreshAllPlayerDisplays() {
        for (Player player : BukkitCompat.onlinePlayers()) {
            refreshPlayerDisplay(player);
        }
    }

    public void refreshAllPlayerSidebars() {
        for (Player player : BukkitCompat.onlinePlayers()) {
            refreshPlayerSidebar(player);
        }
    }

    public void refreshPlayerDisplay(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null || player == null) {
            return;
        }
        Scoreboard board = manager.getNewScoreboard();
        registerTeams(board);
        refreshSidebar(player, board);
        playerScoreboards.put(player.getUniqueId(), board);
        player.setScoreboard(board);
        updatePlayerListName(player);
        syncGameTimerBarPlayer(player);
    }

    public void refreshPlayerSidebar(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null || player == null) {
            return;
        }
        Scoreboard board = playerScoreboards.get(player.getUniqueId());
        if (board == null || player.getScoreboard() != board) {
            refreshPlayerDisplay(player);
            return;
        }
        refreshSidebar(player, board);
        updatePlayerListName(player);
        syncGameTimerBarPlayer(player);
    }

    public void forgetPlayer(Player player) {
        if (player == null) {
            return;
        }
        playerScoreboards.remove(player.getUniqueId());
        if (gameTimerBar != null) {
            gameTimerBar.removePlayer(player);
        }
        resetPlayerListName(player);
        refreshAllPlayerDisplays();
    }

    private void registerTeams(Scoreboard board) {
        for (GodTeam godTeam : GodTeam.values()) {
            Team team = board.registerNewTeam("gw_" + godTeam.id());
            team.setAllowFriendlyFire(plugin.getConfig().getBoolean("game.friendly-fire", false));
            BukkitCompat.setTeamColor(team, teamColor(godTeam));
            team.setPrefix(teamPrefix(godTeam));
        }
        for (Map.Entry<UUID, GodTeam> entry : teams.entrySet()) {
            Player member = Bukkit.getPlayer(entry.getKey());
            if (member == null) {
                continue;
            }
            Team team = board.getTeam("gw_" + entry.getValue().id());
            if (team != null) {
                team.addEntry(member.getName());
            }
        }
    }

    private void refreshSidebar(Player player, Scoreboard board) {
        Objective previous = board.getObjective(SIDEBAR_OBJECTIVE_NAME);
        if (previous != null) {
            previous.unregister();
        }
        if (!plugin.getConfig().getBoolean("scoreboard.enabled", true)) {
            return;
        }
        fillSidebar(player, board);
    }

    private void fillSidebar(Player player, Scoreboard board) {
        Objective objective = board.registerNewObjective(SIDEBAR_OBJECTIVE_NAME, "dummy");
        objective.setDisplayName(ChatColor.GOLD + "신들의 전쟁");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        AbilityDefinition ability = abilityManager.get(player);
        GodTeam team = teamOf(player);
        int score = 15;
        setLine(objective, score--, ChatColor.YELLOW + "상태 " + ChatColor.WHITE + stateLabel());
        setLine(objective, score--, ChatColor.YELLOW + "팀 " + (team == null ? ChatColor.GRAY + "미참가" : teamColoredName(team)));
        setLine(objective, score--, ChatColor.YELLOW + "능력 " + ChatColor.WHITE + (ability == null ? "없음" : ability.name()));
        if (ability != null) {
            setLine(objective, score--, ChatColor.YELLOW + "등급 " + ChatColor.WHITE + ability.grade().symbol());
        }
        if (hasSkill(ability == null ? null : ability.normalSkill())) {
            setLine(objective, score--, ChatColor.AQUA + "일반 " + cooldownStatus(player, ability, 1));
        }
        if (hasSkill(ability == null ? null : ability.advancedSkill())) {
            setLine(objective, score--, ChatColor.RED + "고급 " + cooldownStatus(player, ability, 2));
        }
        List<String> timers = abilityManager.activeTimerLines(player);
        if (!timers.isEmpty()) {
            setLine(objective, score--, ChatColor.LIGHT_PURPLE + "타이머 " + timers.get(0));
        }
        if (timers.size() > 1) {
            setLine(objective, score--, ChatColor.LIGHT_PURPLE + "타이머 " + timers.get(1));
        }
        long killtimeRemaining = killtimeRemainingSeconds();
        if (killtimeRemaining > 0L) {
            setLine(objective, score--, ChatColor.RED + "공격금지 " + ChatColor.WHITE + formatClock(killtimeRemaining));
        }
        setLine(objective, score--, ChatColor.YELLOW + "킬 " + ChatColor.WHITE + killsOf(player));
        setLine(objective, score--, ChatColor.YELLOW + "도박 " + state(plugin.getConfig().getBoolean("gambling.enabled", true)));
        if (abilityManager.urfEnabled()) {
            setLine(objective, score, ChatColor.YELLOW + "우르프 " + state(true)
                + ChatColor.GRAY + " 감소 " + abilityManager.urfCooldownPercent() + "%");
        }
    }

    private void setLine(Objective objective, int score, String text) {
        String line = text.length() > 32 ? text.substring(0, 32) : text;
        objective.getScore(line).setScore(score);
    }

    private String teamPrefix(GodTeam team) {
        if (!plugin.getConfig().getBoolean("scoreboard.team-prefixes", true)) {
            return teamColor(team).toString();
        }
        String displayName = teamDisplayName(team);
        if (displayName.length() > 6) {
            displayName = displayName.substring(0, 6);
        }
        return teamColor(team) + "[" + displayName + "] " + ChatColor.RESET;
    }

    private void updatePlayerListName(Player player) {
        GodTeam team = teamOf(player);
        if (team == null || !plugin.getConfig().getBoolean("scoreboard.team-prefixes", true)) {
            resetPlayerListName(player);
            return;
        }
        try {
            player.setPlayerListName(teamPrefix(team) + player.getName());
        } catch (IllegalArgumentException ex) {
            player.setPlayerListName(player.getName());
        }
    }

    private void resetPlayerListName(Player player) {
        try {
            player.setPlayerListName(player.getName());
        } catch (IllegalArgumentException ignored) {
        }
    }

    public String teamDisplayName(GodTeam team) {
        return plugin.getConfig().getString("teams." + team.id() + ".display-name", team.defaultDisplayName());
    }

    public ChatColor teamColor(GodTeam team) {
        if (team == null) {
            return ChatColor.WHITE;
        }
        String configured = plugin.getConfig().getString("teams." + team.id() + ".color", team.color().name());
        if (configured != null) {
            try {
                ChatColor color = ChatColor.valueOf(configured.trim().toUpperCase(Locale.ROOT));
                if (color.isColor()) {
                    return color;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return team.color();
    }

    public String teamColoredName(GodTeam team) {
        if (team == null) {
            return ChatColor.GRAY + "미배정" + ChatColor.RESET;
        }
        return teamColor(team) + teamDisplayName(team) + ChatColor.RESET;
    }

    public String playerColoredName(Player player) {
        if (player == null) {
            return ChatColor.GRAY + "알 수 없음" + ChatColor.RESET;
        }
        return teamColor(teamOf(player)) + player.getName() + ChatColor.RESET;
    }

    private String stateLabel() {
        if (state == GameState.RUNNING) {
            return "진행중";
        }
        if (state == GameState.READY) {
            return "준비중";
        }
        if (state == GameState.ENDED) {
            return "종료";
        }
        return "대기";
    }

    private String cooldownStatus(Player player, int slot) {
        long millis = abilityManager.cooldownRemainingMillis(player, slot);
        return cooldownText(millis);
    }

    private String cooldownStatus(Player player, AbilityDefinition ability, int slot) {
        long millis = abilityManager.cooldownRemainingMillis(player, slot);
        if (millis <= 0L && ability != null) {
            long shared = abilityManager.cooldownRemainingMillis(player, 0);
            if (slot == 1 && shared > 0L && hasCooldown(ability.normalCooldown()) && !hasCooldown(ability.advancedCooldown())) {
                millis = shared;
            } else if (slot == 2 && shared > 0L && hasCooldown(ability.advancedCooldown()) && !hasCooldown(ability.normalCooldown())) {
                millis = shared;
            }
        }
        return cooldownText(millis);
    }

    private String cooldownText(long millis) {
        if (millis <= 0L) {
            return ChatColor.DARK_AQUA + "사용 가능!";
        }
        long seconds = (millis + 999L) / 1000L;
        return ChatColor.WHITE + "" + (seconds / 60L) + "분 " + (seconds % 60L) + "초";
    }

    private boolean hasCooldown(String cooldown) {
        return cooldown != null && cooldown.trim().length() > 0 && !"없음".equals(cooldown.trim());
    }

    private boolean hasSkill(String skill) {
        return skill != null && skill.trim().length() > 0 && !"없음".equals(skill.trim());
    }

    private String state(boolean enabled) {
        return enabled ? ChatColor.GREEN + "켜짐" : ChatColor.RED + "꺼짐";
    }

    private GodTeam smallestJoinableTeam() {
        return smallestJoinableTeam(null);
    }

    private GodTeam smallestJoinableTeam(GodTeam preferredTeam) {
        GodTeam selected = null;
        int selectedCount = Integer.MAX_VALUE;
        for (GodTeam team : activeTeams()) {
            if (eliminatedTeams.contains(team)) {
                continue;
            }
            int count = 0;
            for (GodTeam assigned : teams.values()) {
                if (team.equals(assigned)) {
                    count++;
                }
            }
            if (count < selectedCount) {
                selected = team;
                selectedCount = count;
            } else if (count == selectedCount && team.equals(preferredTeam)) {
                selected = team;
            }
        }
        return selected;
    }

    private List<Player> participants() {
        List<Player> players = new ArrayList<Player>();
        for (Player player : BukkitCompat.onlinePlayers()) {
            if (teamOf(player) != null && !isObserver(player)) {
                players.add(player);
            }
        }
        return players;
    }

    private void validateStartSettings() {
        List<String> missing = new ArrayList<String>();
        for (GodTeam team : activeTeams()) {
            GameLocation spawn = spawns.get(team);
            if (spawn == null || spawn.toLocation() == null) {
                missing.add(teamDisplayName(team) + " 팀 스폰");
            }

            TempleLocation temple = temples.get(team);
            Location templeLocation = temple == null ? null : temple.toLocation();
            if (templeLocation == null || templeLocation.getBlock().getType() != Material.DIAMOND_BLOCK) {
                missing.add(teamDisplayName(team) + " 팀 다이아 심장");
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("시작 설정이 완료되지 않았습니다: " + joinLabels(missing)
                + ". /godwar settings 또는 /godwar setspawn, /godwar settemple로 먼저 설정해주세요.");
        }
    }

    private void restoreTempleBlocks() {
        for (GodTeam team : activeTeams()) {
            TempleLocation temple = temples.get(team);
            Location location = temple == null ? null : temple.toLocation();
            if (location != null && location.getBlock().getType() != Material.DIAMOND_BLOCK) {
                location.getBlock().setType(Material.DIAMOND_BLOCK);
            }
        }
    }

    private String joinLabels(List<String> labels) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < labels.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(labels.get(i));
        }
        return builder.toString();
    }

    private void startReadyTask() {
        if (readyTask != -1) {
            Bukkit.getScheduler().cancelTask(readyTask);
        }
        readySecondsRemaining = plugin.getConfig().getBoolean("game.fast-start", true)
            ? plugin.getConfig().getInt("game.fast-ready-countdown-seconds", 5)
            : plugin.getConfig().getInt("game.ready-countdown-seconds", 40);
        readySecondsRemaining = Math.max(1, readySecondsRemaining);
        readyReminder = 0;
        readyTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> tickReady(), 20L, 20L);
    }

    private void tickReady() {
        if (state != GameState.READY) {
            if (readyTask != -1) {
                Bukkit.getScheduler().cancelTask(readyTask);
                readyTask = -1;
            }
            return;
        }
        pruneInactivePendingSelections();
        completeAbilitySelectionIfReady();
        if (!pendingSelection.isEmpty()) {
            readyReminder++;
            if (readyReminder == 1 || readyReminder % 15 == 0) {
                broadcastPendingSelection();
            }
            int autoSkipSeconds = abilitySelectionAutoSkipSeconds();
            if (autoSkipSeconds > 0 && readyReminder >= autoSkipSeconds) {
                int skipped = skipAbilitySelection(readySecondsRemaining);
                Bukkit.broadcastMessage(plugin.messages().prefix() + ChatColor.YELLOW
                    + "능력 확정 대기를 자동으로 종료했습니다. 대상: " + skipped + "명");
            }
            refreshAllPlayerDisplays();
            return;
        }
        if (readySecondsRemaining == 5 || readySecondsRemaining == 4 || readySecondsRemaining == 3
            || readySecondsRemaining == 2 || readySecondsRemaining == 1 || readySecondsRemaining % 10 == 0) {
            Bukkit.broadcastMessage(ChatColor.RED + "게임 시작 " + readySecondsRemaining + "초 전");
        }
        readySecondsRemaining--;
        if (readySecondsRemaining <= 0) {
            finishStart();
        }
    }

    private void pruneInactivePendingSelections() {
        if (pendingSelection.isEmpty()) {
            return;
        }
        List<UUID> inactivePlayers = new ArrayList<UUID>();
        for (UUID uuid : pendingSelection.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline() || teamOf(player) == null || isObserver(player)) {
                inactivePlayers.add(uuid);
            }
        }
        if (inactivePlayers.isEmpty()) {
            return;
        }
        for (UUID uuid : inactivePlayers) {
            pendingSelection.remove(uuid);
        }
        plugin.getLogger().info("Removed " + inactivePlayers.size() + " inactive player(s) from ability selection wait.");
    }

    private void finishStart() {
        if (readyTask != -1) {
            Bukkit.getScheduler().cancelTask(readyTask);
            readyTask = -1;
        }
        state = GameState.RUNNING;
        runningStartedAtMillis = System.currentTimeMillis();
        killtimeEndAnnounced = false;
        coreExplosionUnlockAnnounced = false;
        gameRuleController.applyConfiguredRules();
        applyWorldStartSettings();
        clearConfiguredEntities();

        for (Player player : participants()) {
            preparePlayerForGame(player);
            teleportToTeamSpawn(player, teamOf(player));
            abilityManager.reapply(player);
        }
        startGameTimerTask();
        refreshAllPlayerDisplays();
        Bukkit.broadcastMessage(plugin.messages().prefix() + plugin.messages().get("game-start"));
        if (gameTimerBossBarEnabled() && killtimeDurationSeconds() > 0) {
            Bukkit.broadcastMessage(plugin.messages().prefix() + ChatColor.AQUA
                + "킬타임 공격 금지 타이머가 시작되었습니다. "
                + ChatColor.YELLOW + formatSeconds(killtimeDurationSeconds()) + ChatColor.AQUA + " 동안 공격하지 마세요.");
        }
        startWaterHealTask();
        startPickaxeUnlockNoticeTask();
        startGameTipTask();
    }

    private void broadcastPendingSelection() {
        Bukkit.broadcastMessage(ChatColor.DARK_AQUA + "능력을 확정하지 않은 플레이어 목록");
        for (Map.Entry<UUID, Integer> entry : pendingSelection.entrySet()) {
            UUID uuid = entry.getKey();
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                Bukkit.broadcastMessage(ChatColor.GOLD + "  " + player.getName()
                    + ChatColor.GRAY + " (재추첨 " + entry.getValue() + "회 남음)");
            }
        }
        Bukkit.broadcastMessage(ChatColor.WHITE + "능력을 확정하려면 " + ChatColor.AQUA + "/t yes"
            + ChatColor.WHITE + " 또는 " + ChatColor.RED + "/t no" + ChatColor.WHITE + " 를 입력하세요.");
        sendAdminPendingSkipTip();
    }

    private int abilityRerollCount() {
        return Math.max(0, plugin.getConfig().getInt("game.ability-reroll-count", 1));
    }

    private int abilitySelectionAutoSkipSeconds() {
        return Math.max(0, plugin.getConfig().getInt("game.skip-ready-countdown-seconds", 5));
    }

    private void sendAdminPendingSkipTip() {
        for (Player player : BukkitCompat.onlinePlayers()) {
            if (player.hasPermission("newgodwar.admin")) {
                player.sendMessage(ChatColor.GRAY + "관리자: " + ChatColor.YELLOW + "/t skip [초]"
                    + ChatColor.GRAY + " 로 남은 능력 확정 대기를 종료할 수 있습니다.");
            }
        }
    }

    private void broadcastStartSettings() {
        Bukkit.broadcastMessage(ChatColor.GREEN + "****** 서버 세팅상태 ******");
        Bukkit.broadcastMessage(ChatColor.WHITE + "인벤토리 클리어 : " + state(plugin.getConfig().getBoolean("game.clear-inventory", true)));
        Bukkit.broadcastMessage(ChatColor.WHITE + "기본 아이템 지급 : " + state(plugin.getConfig().getBoolean("game.give-skyblock-items", true)));
        Bukkit.broadcastMessage(ChatColor.WHITE + "엔티티 삭제 : " + state(plugin.getConfig().getBoolean("game.remove-entities", true)));
        Bukkit.broadcastMessage(ChatColor.WHITE + "침대 무시 : " + state(plugin.getConfig().getBoolean("game.ignore-bed", true)));
        Bukkit.broadcastMessage(ChatColor.WHITE + "종료 후 능력 공개 : " + state(plugin.getConfig().getBoolean("game.reveal-abilities-on-end", true)));
        Bukkit.broadcastMessage(ChatColor.WHITE + "킬타임 보스바 : " + state(gameTimerBossBarEnabled()));
        Bukkit.broadcastMessage(ChatColor.WHITE + "킬타임 공격 금지 : " + ChatColor.YELLOW + formatSeconds(killtimeDurationSeconds()));
        Bukkit.broadcastMessage(ChatColor.WHITE + "도박 : " + state(plugin.getConfig().getBoolean("gambling.enabled", true)));
        Bukkit.broadcastMessage(ChatColor.WHITE + "코어 폭파 보호 : " + coreExplosionProtectionSummary());
        Bukkit.broadcastMessage(ChatColor.WHITE + "코어 맨손 파괴 : " + state(plugin.getConfig().getBoolean("core.require-empty-hand", true)));
        Bukkit.broadcastMessage(ChatColor.WHITE + "다이아 곡괭이 금지 : " + state(plugin.getConfig().getBoolean("core.forbid-diamond-pickaxe", true)));
        Bukkit.broadcastMessage(ChatColor.WHITE + "곡괭이 시간 해제 : " + pickaxeUnlockSummary());
        Bukkit.broadcastMessage(ChatColor.GREEN + "***************************");
    }

    private String coreExplosionProtectionSummary() {
        if (!plugin.getConfig().getBoolean("core.protect-diamond-from-explosion", true)) {
            return state(false);
        }
        int seconds = coreExplosionUnlockSeconds();
        if (seconds < 0) {
            return state(true);
        }
        return state(true) + ChatColor.GRAY + " / " + ChatColor.YELLOW + formatSeconds(seconds)
            + ChatColor.GRAY + " 후 폭파 허용";
    }

    private String pickaxeUnlockSummary() {
        String summary = pickaxeUnlockLabel("나무", "core.pickaxe-unlock.wooden-seconds")
            + ChatColor.GRAY + " / " + pickaxeUnlockLabel("돌", "core.pickaxe-unlock.stone-seconds")
            + ChatColor.GRAY + " / " + pickaxeUnlockLabel("철", "core.pickaxe-unlock.iron-seconds")
            + ChatColor.GRAY + " / " + pickaxeUnlockLabel("다이아", "core.pickaxe-unlock.diamond-seconds");
        return summary;
    }

    private String pickaxeUnlockLabel(String name, String path) {
        int seconds = plugin.getConfig().getInt(path, -1);
        if (seconds < 0) {
            return ChatColor.DARK_GRAY + name + " 꺼짐";
        }
        if (seconds == 0) {
            return ChatColor.GREEN + name + " 즉시";
        }
        return ChatColor.YELLOW + name + " " + formatSeconds(seconds);
    }

    private String formatSeconds(int seconds) {
        int minutes = seconds / 60;
        int remain = seconds % 60;
        if (minutes <= 0) {
            return remain + "초";
        }
        if (remain == 0) {
            return minutes + "분";
        }
        return minutes + "분 " + remain + "초";
    }

    private String formatClock(long seconds) {
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long remain = seconds % 60L;
        if (hours > 0L) {
            return String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, remain);
        }
        return String.format(Locale.ROOT, "%02d:%02d", minutes, remain);
    }

    private void revealAbilitiesOnEnd() {
        if (!plugin.getConfig().getBoolean("game.reveal-abilities-on-end", true)) {
            return;
        }
        Map<UUID, AbilityDefinition> assignments = abilityManager.assignedAbilities();
        if (assignments.isEmpty()) {
            return;
        }

        List<UUID> players = new ArrayList<UUID>(assignments.keySet());
        sortAbilityRevealPlayers(players);
        Bukkit.broadcastMessage(plugin.messages().prefix() + ChatColor.GOLD + "게임 종료 능력 공개");
        for (UUID uuid : players) {
            AbilityDefinition ability = assignments.get(uuid);
            if (ability == null) {
                continue;
            }
            GodTeam team = teams.get(uuid);
            Bukkit.broadcastMessage(ChatColor.GRAY + "- " + teamColor(team) + playerName(uuid)
                + ChatColor.DARK_GRAY + " [" + revealTeamName(team) + "] "
                + ChatColor.WHITE + ability.name()
                + ChatColor.DARK_GRAY + " (" + ability.id() + ")");
        }
    }

    private void sortAbilityRevealPlayers(List<UUID> players) {
        for (int i = 1; i < players.size(); i++) {
            UUID current = players.get(i);
            int cursor = i - 1;
            while (cursor >= 0 && compareAbilityRevealPlayers(players.get(cursor), current) > 0) {
                players.set(cursor + 1, players.get(cursor));
                cursor--;
            }
            players.set(cursor + 1, current);
        }
    }

    private int compareAbilityRevealPlayers(UUID left, UUID right) {
        GodTeam leftTeam = teams.get(left);
        GodTeam rightTeam = teams.get(right);
        int leftTeamOrder = leftTeam == null ? GodTeam.values().length : leftTeam.ordinal();
        int rightTeamOrder = rightTeam == null ? GodTeam.values().length : rightTeam.ordinal();
        if (leftTeamOrder != rightTeamOrder) {
            return leftTeamOrder - rightTeamOrder;
        }
        return playerName(left).compareToIgnoreCase(playerName(right));
    }

    private String playerName(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        String name = offline == null ? null : offline.getName();
        return name == null ? uuid.toString() : name;
    }

    private String revealTeamName(GodTeam team) {
        return team == null ? "미배정" : teamDisplayName(team);
    }

    private void preparePlayerForGame(Player player) {
        BukkitCompat.setSurvival(player);
        BukkitCompat.clearPotionEffects(player);
        if (plugin.getConfig().getBoolean("game.clear-inventory", true)) {
            player.getInventory().clear();
            player.getInventory().setHelmet(null);
            player.getInventory().setChestplate(null);
            player.getInventory().setLeggings(null);
            player.getInventory().setBoots(null);
        }
        if (plugin.getConfig().getBoolean("game.give-skyblock-items", true)) {
            giveSkyblockItems(player);
        }
        player.setFoodLevel(20);
        player.setSaturation(10.0F);
        player.setExhaustion(0.0F);
        player.setLevel(0);
        player.setExp(0.0F);
        player.setHealth(player.getMaxHealth());
    }

    private void giveSkyblockItems(Player player) {
        for (Map<?, ?> entry : StarterItems.configuredEntries(plugin.getConfig())) {
            ItemStack item = StarterItems.toItemStack(entry);
            if (item != null) {
                giveStarterItem(player, item);
            }
        }
    }

    private void giveStarterItem(Player player, ItemStack item) {
        int remaining = item.getAmount();
        int maxStack = Math.max(1, item.getMaxStackSize());
        while (remaining > 0) {
            ItemStack stack = item.clone();
            stack.setAmount(Math.min(maxStack, remaining));
            player.getInventory().addItem(stack);
            remaining -= stack.getAmount();
        }
    }

    private Material material(String modernName, String legacyName) {
        Material modern = Material.matchMaterial(modernName);
        if (modern != null) {
            return modern;
        }
        Material legacy = Material.matchMaterial(legacyName);
        return legacy == null ? Material.STONE : legacy;
    }

    private void teleportToTeamSpawn(Player player, GodTeam team) {
        Location location = null;
        if (team != null) {
            GameLocation spawn = spawns.get(team);
            if (spawn != null) {
                location = spawn.toLocation();
            }
        }
        if (location == null) {
            location = player.getWorld().getSpawnLocation();
            player.sendMessage(ChatColor.RED + "팀 스폰지역이 설정되지 않아 기본 스폰지역으로 이동합니다.");
        }
        player.teleport(location);
    }

    private void applyWorldStartSettings() {
        for (World world : Bukkit.getWorlds()) {
            if (!worldSnapshots.containsKey(world.getName())) {
                worldSnapshots.put(world.getName(), new WorldSnapshot(world));
            }
            world.setPVP(true);
            world.setAutoSave(plugin.getConfig().getBoolean("world.autosave", true));
            world.setSpawnFlags(plugin.getConfig().getBoolean("world.spawn-monsters", false), plugin.getConfig().getBoolean("world.spawn-animals", false));
            world.setDifficulty(difficulty());
            setWorldTime(world, plugin.getConfig().getLong("world.start-time", 6000L));
        }
    }

    private void setWorldTime(World world, long time) {
        try {
            world.setTime(time);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Skipping time change for world '" + world.getName() + "': " + ex.getMessage());
        }
    }

    private void restoreWorldSettings() {
        for (World world : Bukkit.getWorlds()) {
            WorldSnapshot snapshot = worldSnapshots.get(world.getName());
            if (snapshot != null) {
                snapshot.restore(world);
            }
        }
        worldSnapshots.clear();
    }

    private Difficulty difficulty() {
        String configured = plugin.getConfig().getString("world.difficulty", "EASY");
        try {
            return Difficulty.valueOf(configured.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return Difficulty.EASY;
        }
    }

    private void clearConfiguredEntities() {
        if (!plugin.getConfig().getBoolean("game.remove-entities", true)) {
            return;
        }
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Item || entity instanceof Monster || entity instanceof Animals) {
                    entity.remove();
                }
            }
        }
    }

    private void clearPotionEffects(Iterable<? extends Player> players) {
        for (Player player : players) {
            BukkitCompat.clearPotionEffects(player);
        }
    }

    private void checkWinner() {
        Set<GodTeam> alive = aliveTeams();
        if (alive.size() == 1) {
            GodTeam winner = alive.iterator().next();
            String message = plugin.messages().get("winner").replace("{team}", teamColoredName(winner));
            Bukkit.broadcastMessage(plugin.messages().prefix() + message);
            for (Player player : BukkitCompat.onlinePlayers()) {
                if (winner.equals(teamOf(player))) {
                    nmsAdapter.sendTitle(player, ChatColor.GOLD + "승리", teamColoredName(winner) + " 팀이 승리했습니다.", 10, 80, 20);
                    BukkitCompat.playLevelUp(player);
                }
            }
            stop(false);
        }
    }

    private int aliveTeamCount() {
        return aliveTeams().size();
    }

    private Set<GodTeam> aliveTeams() {
        Set<GodTeam> alive = new HashSet<GodTeam>();
        for (GodTeam team : activeTeams()) {
            if (!eliminatedTeams.contains(team)) {
                alive.add(team);
            }
        }
        return alive;
    }

    private void startGameTimerTask() {
        cancelGameTimerTask();
        if (!gameTimerBossBarEnabled() || killtimeDurationSeconds() <= 0 || killtimeRemainingSeconds() <= 0L) {
            return;
        }
        gameTimerBar = Bukkit.createBossBar(timerBarTitle(), BarColor.YELLOW, BarStyle.SOLID);
        gameTimerBar.setProgress(timerBarProgress());
        syncGameTimerBarPlayers();
        gameTimerTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> tickGameTimerBar(), 0L, 20L);
    }

    private void tickGameTimerBar() {
        if (state != GameState.RUNNING) {
            cancelGameTimerTask();
            return;
        }
        if (killtimeRemainingSeconds() <= 0L) {
            announceKilltimeEnded();
            cancelGameTimerTask();
            refreshAllPlayerDisplays();
            return;
        }
        if (gameTimerBar == null) {
            gameTimerBar = Bukkit.createBossBar(timerBarTitle(), BarColor.YELLOW, BarStyle.SOLID);
        }
        gameTimerBar.setTitle(timerBarTitle());
        gameTimerBar.setProgress(timerBarProgress());
        syncGameTimerBarPlayers();
    }

    private String timerBarTitle() {
        return ChatColor.RED + "킬타임 공격 금지 " + ChatColor.WHITE + formatClock(killtimeRemainingSeconds());
    }

    private boolean gameTimerBossBarEnabled() {
        return plugin.getConfig().getBoolean("game.killtime-bossbar", false);
    }

    private int killtimeDurationSeconds() {
        return Math.max(0, plugin.getConfig().getInt("game.killtime-seconds", 300));
    }

    private int coreExplosionUnlockSeconds() {
        return plugin.getConfig().getInt(CORE_EXPLOSION_UNLOCK_SECONDS_PATH, -1);
    }

    private double timerBarProgress() {
        int duration = killtimeDurationSeconds();
        if (duration <= 0) {
            return 0.0D;
        }
        double progress = killtimeRemainingSeconds() / (double) duration;
        return Math.max(0.0D, Math.min(1.0D, progress));
    }

    private void announceKilltimeEnded() {
        if (killtimeEndAnnounced || killtimeDurationSeconds() <= 0) {
            return;
        }
        killtimeEndAnnounced = true;
        Bukkit.broadcastMessage(plugin.messages().prefix() + ChatColor.GREEN
            + "킬타임이 종료되었습니다. 이제 공격할 수 있습니다.");
    }

    private void syncGameTimerBarPlayers() {
        if (gameTimerBar == null) {
            return;
        }
        for (Player player : new ArrayList<Player>(gameTimerBar.getPlayers())) {
            if (player == null || !player.isOnline() || state != GameState.RUNNING) {
                gameTimerBar.removePlayer(player);
            }
        }
        if (state == GameState.RUNNING) {
            for (Player player : BukkitCompat.onlinePlayers()) {
                syncGameTimerBarPlayer(player);
            }
        }
    }

    private void syncGameTimerBarPlayer(Player player) {
        if (gameTimerBar == null || player == null) {
            return;
        }
        if (state == GameState.RUNNING && player.isOnline()) {
            gameTimerBar.addPlayer(player);
        } else {
            gameTimerBar.removePlayer(player);
        }
    }

    private void cancelGameTimerTask() {
        if (gameTimerTask != -1) {
            Bukkit.getScheduler().cancelTask(gameTimerTask);
            gameTimerTask = -1;
        }
        if (gameTimerBar != null) {
            for (Player player : new ArrayList<Player>(gameTimerBar.getPlayers())) {
                gameTimerBar.removePlayer(player);
            }
            gameTimerBar = null;
        }
    }

    private void startPickaxeUnlockNoticeTask() {
        cancelPickaxeUnlockNoticeTask();
        announcedPickaxeUnlocks.clear();
        coreExplosionUnlockAnnounced = false;
        tickPickaxeUnlockNotices();
        if (state == GameState.RUNNING) {
            pickaxeUnlockNoticeTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> tickPickaxeUnlockNotices(), 20L, 20L);
        }
    }

    private void tickPickaxeUnlockNotices() {
        if (state != GameState.RUNNING) {
            cancelPickaxeUnlockNoticeTask();
            return;
        }
        long elapsed = runningElapsedSeconds();
        for (PickaxeUnlockNotice notice : PICKAXE_UNLOCK_NOTICES) {
            int seconds = plugin.getConfig().getInt(notice.path, -1);
            if (seconds < 0) {
                announcedPickaxeUnlocks.remove(notice.path);
                continue;
            }
            if (elapsed >= seconds) {
                if (announcedPickaxeUnlocks.add(notice.path)) {
                    announcePickaxeUnlocked(notice);
                }
            } else {
                announcedPickaxeUnlocks.remove(notice.path);
            }
        }
        tickCoreExplosionUnlockNotice(elapsed);
    }

    private void tickCoreExplosionUnlockNotice(long elapsed) {
        if (!plugin.getConfig().getBoolean("core.protect-diamond-from-explosion", true)) {
            coreExplosionUnlockAnnounced = false;
            return;
        }
        int seconds = coreExplosionUnlockSeconds();
        if (seconds < 0) {
            coreExplosionUnlockAnnounced = false;
            return;
        }
        if (elapsed >= seconds) {
            if (!coreExplosionUnlockAnnounced) {
                coreExplosionUnlockAnnounced = true;
                announceCoreExplosionUnlocked();
            }
        } else {
            coreExplosionUnlockAnnounced = false;
        }
    }

    private void announceCoreExplosionUnlocked() {
        String title = ChatColor.RED + "코어 폭파 허용";
        String subtitle = ChatColor.WHITE + "이제 폭발로 코어를 파괴할 수 있습니다.";
        Bukkit.broadcastMessage(plugin.messages().prefix() + title + ChatColor.WHITE
            + " - 이제 폭발로 코어를 파괴할 수 있습니다.");
        for (Player player : BukkitCompat.onlinePlayers()) {
            nmsAdapter.sendTitle(player, title, subtitle, 10, 60, 10);
            BukkitCompat.playLevelUp(player);
        }
    }

    private void announcePickaxeUnlocked(PickaxeUnlockNotice notice) {
        String title = ChatColor.GREEN + notice.name + " 곡괭이 해제";
        String subtitle = ChatColor.WHITE + "이제 코어 파괴에 사용할 수 있습니다.";
        Bukkit.broadcastMessage(plugin.messages().prefix() + title + ChatColor.WHITE
            + " - 이제 코어 파괴에 사용할 수 있습니다.");
        for (Player player : BukkitCompat.onlinePlayers()) {
            nmsAdapter.sendTitle(player, title, subtitle, 10, 60, 10);
            BukkitCompat.playLevelUp(player);
        }
    }

    private void cancelPickaxeUnlockNoticeTask() {
        if (pickaxeUnlockNoticeTask != -1) {
            Bukkit.getScheduler().cancelTask(pickaxeUnlockNoticeTask);
            pickaxeUnlockNoticeTask = -1;
        }
        announcedPickaxeUnlocks.clear();
        coreExplosionUnlockAnnounced = false;
    }

    private void startWaterHealTask() {
        if (waterHealTask != -1) {
            Bukkit.getScheduler().cancelTask(waterHealTask);
        }
        startAbilityNoticeTask();
        long interval = Math.max(1L, plugin.getConfig().getLong("game.ability-tick-interval-seconds", 1L)) * 20L;
        waterHealTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (state == GameState.RUNNING) {
                abilityManager.tick(BukkitCompat.onlinePlayers());
                refreshAllPlayerDisplays();
            }
        }, interval, interval);
    }

    private void startAbilityNoticeTask() {
        if (abilityNoticeTask != -1) {
            Bukkit.getScheduler().cancelTask(abilityNoticeTask);
        }
        abilityNoticeTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (state == GameState.RUNNING) {
                abilityManager.tickCountdownAlerts(BukkitCompat.onlinePlayers());
            }
        }, 20L, 20L);
    }

    private void startGameTipTask() {
        if (gameTipTask != -1) {
            Bukkit.getScheduler().cancelTask(gameTipTask);
            gameTipTask = -1;
        }
        if (!GameTips.timedTipsEnabled(plugin) || GameTips.count(plugin) == 0) {
            return;
        }
        if (!GameTips.repeatTimedTips(plugin) && nextGameTipIndex >= GameTips.count(plugin)) {
            return;
        }

        long initialDelay = GameTips.timedInitialDelaySeconds(plugin) * 20L;
        long interval = GameTips.timedIntervalSeconds(plugin) * 20L;
        gameTipTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (state != GameState.RUNNING || !GameTips.timedTipsEnabled(plugin) || GameTips.count(plugin) == 0) {
                cancelGameTipTask();
                return;
            }
            if (!GameTips.repeatTimedTips(plugin) && nextGameTipIndex >= GameTips.count(plugin)) {
                cancelGameTipTask();
                return;
            }
            nextGameTipIndex = GameTips.broadcastTip(plugin, nextGameTipIndex);
            if (!GameTips.repeatTimedTips(plugin) && nextGameTipIndex >= GameTips.count(plugin)) {
                cancelGameTipTask();
            }
        }, initialDelay, interval);
    }

    private void cancelGameTipTask() {
        if (gameTipTask != -1) {
            Bukkit.getScheduler().cancelTask(gameTipTask);
            gameTipTask = -1;
        }
    }

    private static final class PickaxeUnlockNotice {
        private final String name;
        private final String path;

        private PickaxeUnlockNotice(String name, String path) {
            this.name = name;
            this.path = path;
        }
    }

    private static final class WorldSnapshot {
        private final boolean pvp;
        private final boolean allowMonsters;
        private final boolean allowAnimals;
        private final boolean autoSave;
        private final Difficulty difficulty;

        private WorldSnapshot(World world) {
            this.pvp = world.getPVP();
            this.allowMonsters = world.getAllowMonsters();
            this.allowAnimals = world.getAllowAnimals();
            this.autoSave = world.isAutoSave();
            this.difficulty = world.getDifficulty();
        }

        private void restore(World world) {
            world.setPVP(pvp);
            world.setSpawnFlags(allowMonsters, allowAnimals);
            world.setAutoSave(autoSave);
            world.setDifficulty(difficulty);
        }
    }
}
