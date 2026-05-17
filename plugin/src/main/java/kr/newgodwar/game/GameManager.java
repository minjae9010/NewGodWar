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
import org.bukkit.World;
import org.bukkit.block.Block;
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
    private final Map<UUID, Integer> pendingSelection = new HashMap<UUID, Integer>();
    private final Map<UUID, Integer> kills = new HashMap<UUID, Integer>();
    private final Map<UUID, Scoreboard> playerScoreboards = new HashMap<UUID, Scoreboard>();
    private final Map<String, WorldSnapshot> worldSnapshots = new HashMap<String, WorldSnapshot>();

    private GameState state = GameState.WAITING;
    private int waterHealTask = -1;
    private int abilityNoticeTask = -1;
    private int readyTask = -1;
    private int readySecondsRemaining = 0;
    private int readyReminder = 0;

    public GameManager(NewGodWarPlugin plugin, AbilityManager abilityManager, NmsAdapter nmsAdapter) {
        this.plugin = plugin;
        this.abilityManager = abilityManager;
        this.nmsAdapter = nmsAdapter;
        this.gameRuleController = new GameRuleController(plugin);
        GodTeam.reload(plugin.getConfig());
        loadTemples();
        loadSpawns();
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

    public void leave(Player player) {
        teams.remove(player.getUniqueId());
        playerScoreboards.remove(player.getUniqueId());
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            player.setScoreboard(manager.getMainScoreboard());
        }
        resetPlayerListName(player);
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
        plugin.getConfig().set("spawns." + team.id(), spawn.serialize());
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

        state = GameState.READY;
        eliminatedTeams.clear();
        kills.clear();
        pendingSelection.clear();
        clearPotionEffects(participants);
        abilityManager.clear();
        setupScoreboard();

        Bukkit.broadcastMessage(plugin.messages().prefix() + ChatColor.GOLD + "게임 시작 준비를 시작합니다.");
        broadcastStartSettings();
        GameTips.broadcast(plugin);

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
        }
        startReadyTask();
    }

    public AbilityDefinition startTest(Player player, AbilityDefinition preferredAbility) {
        if (player == null) {
            throw new IllegalStateException("테스트할 플레이어를 찾을 수 없습니다.");
        }

        state = GameState.RUNNING;
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
        refreshAllPlayerDisplays();
        startWaterHealTask();
        return ability;
    }

    public AbilityDefinition joinMidGame(Player player, GodTeam requestedTeam) {
        if (state != GameState.RUNNING) {
            throw new IllegalStateException("게임 진행 중에만 중간 참여를 사용할 수 있습니다.");
        }
        if (!plugin.getConfig().getBoolean("game.allow-mid-join", true)) {
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

        observers.remove(player.getUniqueId());
        assign(player, team);
        preparePlayerForGame(player);
        teleportToTeamSpawn(player, team);
        AbilityDefinition ability = abilityManager.assignRandom(player);
        BukkitCompat.setSurvival(player);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        nmsAdapter.sendTitle(player, ChatColor.GREEN + "중간 참여", ability.name(), 10, 60, 10);
        Bukkit.broadcastMessage(plugin.messages().prefix() + teamColoredName(team) + ChatColor.YELLOW
            + " 팀에 " + player.getName() + " 님이 중간 참여했습니다.");
        refreshAllPlayerDisplays();
        return ability;
    }

    public void stop(boolean announce) {
        state = GameState.ENDED;
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
        pendingSelection.clear();
        abilityManager.clear();
        clearPotionEffects(BukkitCompat.onlinePlayers());
        if (announce) {
            Bukkit.broadcastMessage(plugin.messages().prefix() + plugin.messages().get("game-stop"));
        }
        gameRuleController.restorePreviousRules();
        restoreWorldSettings();
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
        String message = plugin.messages().get("team-eliminated").replace("{team}", teamColoredName(team));
        Bukkit.broadcastMessage(plugin.messages().prefix() + message);
        for (Player player : BukkitCompat.onlinePlayers()) {
            if (team.equals(teamOf(player))) {
                setSpectator(player);
            }
        }
        refreshAllPlayerDisplays();
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

    public boolean toggleObserver(Player player) {
        UUID uuid = player.getUniqueId();
        boolean enabled;
        if (observers.contains(uuid)) {
            observers.remove(uuid);
            unsetSpectator(player);
            enabled = false;
        } else {
            observers.add(uuid);
            setSpectator(player);
            enabled = true;
        }
        refreshAllPlayerDisplays();
        return enabled;
    }

    public boolean confirmAbility(Player player) {
        return pendingSelection.remove(player.getUniqueId()) != null;
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

    public String statusLine() {
        return ChatColor.YELLOW + "상태: " + state
            + ChatColor.GRAY + " | " + ChatColor.YELLOW + "참가자: " + teams.size()
            + ChatColor.GRAY + " | " + ChatColor.YELLOW + "옵저버: " + observers.size()
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

    private void loadSpawns() {
        for (GodTeam team : GodTeam.values()) {
            GameLocation location = GameLocation.deserialize(plugin.getConfig().getString("spawns." + team.id()));
            if (location != null) {
                spawns.put(team, location);
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

    public void refreshPlayerDisplay(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null || player == null) {
            return;
        }
        Scoreboard board = manager.getNewScoreboard();
        registerTeams(board);
        if (plugin.getConfig().getBoolean("scoreboard.enabled", true)) {
            fillSidebar(player, board);
        }
        playerScoreboards.put(player.getUniqueId(), board);
        player.setScoreboard(board);
        updatePlayerListName(player);
    }

    public void forgetPlayer(Player player) {
        if (player == null) {
            return;
        }
        playerScoreboards.remove(player.getUniqueId());
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

    private void fillSidebar(Player player, Scoreboard board) {
        Objective objective = board.registerNewObjective("gw_status", "dummy");
        objective.setDisplayName(ChatColor.GOLD + "신들의 전쟁");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        AbilityDefinition ability = abilityManager.get(player);
        GodTeam team = teamOf(player);
        int score = 15;
        setLine(objective, score--, ChatColor.YELLOW + "상태 " + ChatColor.WHITE + stateLabel());
        setLine(objective, score--, ChatColor.YELLOW + "팀 " + (team == null ? ChatColor.GRAY + "미참가" : teamColoredName(team)));
        setLine(objective, score--, ChatColor.YELLOW + "능력 " + ChatColor.WHITE + (ability == null ? "없음" : ability.name()));
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

    private void finishStart() {
        if (readyTask != -1) {
            Bukkit.getScheduler().cancelTask(readyTask);
            readyTask = -1;
        }
        state = GameState.RUNNING;
        gameRuleController.applyConfiguredRules();
        applyWorldStartSettings();
        clearConfiguredEntities();

        for (Player player : participants()) {
            preparePlayerForGame(player);
            teleportToTeamSpawn(player, teamOf(player));
            abilityManager.reapply(player);
        }
        refreshAllPlayerDisplays();
        Bukkit.broadcastMessage(plugin.messages().prefix() + plugin.messages().get("game-start"));
        startWaterHealTask();
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
        Bukkit.broadcastMessage(ChatColor.WHITE + "도박 : " + state(plugin.getConfig().getBoolean("gambling.enabled", true)));
        Bukkit.broadcastMessage(ChatColor.WHITE + "코어 폭파 보호 : " + state(plugin.getConfig().getBoolean("core.protect-diamond-from-explosion", true)));
        Bukkit.broadcastMessage(ChatColor.WHITE + "코어 맨손 파괴 : " + state(plugin.getConfig().getBoolean("core.require-empty-hand", true)));
        Bukkit.broadcastMessage(ChatColor.WHITE + "다이아 곡괭이 금지 : " + state(plugin.getConfig().getBoolean("core.forbid-diamond-pickaxe", true)));
        Bukkit.broadcastMessage(ChatColor.GREEN + "***************************");
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
        player.getInventory().addItem(new ItemStack(Material.CHEST, 1));
        player.getInventory().addItem(new ItemStack(Material.LAVA_BUCKET, 2));
        player.getInventory().addItem(new ItemStack(material("ICE", "ICE"), 2));
        player.getInventory().addItem(new ItemStack(material("OAK_SAPLING", "SAPLING"), 1));
        player.getInventory().addItem(new ItemStack(material("BONE_MEAL", "INK_SACK"), 1, (short) 15));
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
        Set<GodTeam> alive = new HashSet<GodTeam>();
        for (GodTeam team : activeTeams()) {
            if (!eliminatedTeams.contains(team)) {
                alive.add(team);
            }
        }
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
