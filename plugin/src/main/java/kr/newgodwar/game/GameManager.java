package kr.newgodwar.game;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.ability.AbilityManager;
import kr.newgodwar.ability.api.AbilityDefinition;
import kr.newgodwar.nms.NmsAdapter;
import kr.newgodwar.util.BukkitCompat;
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
    private final Map<GodTeam, GameLocation> spawns = new EnumMap<GodTeam, GameLocation>(GodTeam.class);
    private final Set<GodTeam> eliminatedTeams = new HashSet<GodTeam>();
    private final Set<UUID> observers = new HashSet<UUID>();
    private final Set<UUID> pendingSelection = new HashSet<UUID>();
    private final Map<UUID, Integer> kills = new HashMap<UUID, Integer>();
    private final Map<UUID, Scoreboard> playerScoreboards = new HashMap<UUID, Scoreboard>();
    private final Map<String, WorldSnapshot> worldSnapshots = new HashMap<String, WorldSnapshot>();

    private GameState state = GameState.WAITING;
    private int waterHealTask = -1;
    private int readyTask = -1;
    private int readySecondsRemaining = 0;
    private int readyReminder = 0;

    public GameManager(NewGodWarPlugin plugin, AbilityManager abilityManager, NmsAdapter nmsAdapter) {
        this.plugin = plugin;
        this.abilityManager = abilityManager;
        this.nmsAdapter = nmsAdapter;
        this.gameRuleController = new GameRuleController(plugin);
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

    public void reloadSettings() {
        temples.clear();
        spawns.clear();
        loadTemples();
        loadSpawns();
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

    public void assign(Player player, GodTeam team) {
        teams.put(player.getUniqueId(), team);
        refreshAllPlayerDisplays();
        nmsAdapter.sendActionBar(player, team.color() + team.defaultDisplayName() + " 팀에 배정되었습니다.");
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
            if (attackerTeam != null && attackerTeam == victimTeam) {
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

        state = GameState.READY;
        eliminatedTeams.clear();
        kills.clear();
        pendingSelection.clear();
        abilityManager.clear();
        setupScoreboard();

        for (Player player : participants) {
            AbilityDefinition ability = abilityManager.assignRandom(player);
            if (plugin.getConfig().getBoolean("game.select-right", true)) {
                pendingSelection.add(player.getUniqueId());
            }
            if (plugin.getConfig().getBoolean("game.ability-roll-message", true)) {
                nmsAdapter.sendTitle(player, ability.name(), ability.description(), 10, 70, 20);
            }
        }
        refreshAllPlayerDisplays();

        Bukkit.broadcastMessage(plugin.messages().prefix() + ChatColor.GOLD + "게임 시작 준비를 시작합니다.");
        broadcastStartSettings();
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
        gameRuleController.applyConfiguredRules();
        setupScoreboard();

        GodTeam team = teamOf(player);
        if (team == null) {
            team = GodTeam.RED;
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
        if (teamOf(player) != null && abilityManager.get(player) != null) {
            throw new IllegalStateException("이미 게임에 참여 중입니다.");
        }

        GodTeam team = requestedTeam == null ? smallestJoinableTeam() : requestedTeam;
        if (team == null || eliminatedTeams.contains(team)) {
            throw new IllegalStateException("참여 가능한 팀이 없습니다.");
        }

        assign(player, team);
        preparePlayerForGame(player);
        teleportToTeamSpawn(player, team);
        AbilityDefinition ability = abilityManager.assignRandom(player);
        BukkitCompat.setSurvival(player);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        nmsAdapter.sendTitle(player, ChatColor.GREEN + "중간 참여", ability.name(), 10, 60, 10);
        Bukkit.broadcastMessage(plugin.messages().prefix() + team.coloredName() + ChatColor.YELLOW
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
        pendingSelection.clear();
        abilityManager.clear();
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
        String message = plugin.messages().get("team-eliminated").replace("{team}", team.coloredName());
        Bukkit.broadcastMessage(plugin.messages().prefix() + message);
        for (Player player : BukkitCompat.onlinePlayers()) {
            if (team == teamOf(player)) {
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
        return pendingSelection.remove(player.getUniqueId());
    }

    public AbilityDefinition rerollAbility(Player player) {
        if (!pendingSelection.remove(player.getUniqueId())) {
            return null;
        }
        AbilityDefinition ability = abilityManager.assignRandom(player);
        nmsAdapter.sendTitle(player, ability.name(), ability.description(), 10, 70, 20);
        refreshPlayerDisplay(player);
        return ability;
    }

    public int skipAbilitySelection() {
        int count = pendingSelection.size();
        pendingSelection.clear();
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
            team.setPrefix(teamPrefix(godTeam));
            team.setAllowFriendlyFire(plugin.getConfig().getBoolean("game.friendly-fire", false));
            BukkitCompat.setTeamColor(team, godTeam.color());
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
        setLine(objective, 10, ChatColor.YELLOW + "상태 " + ChatColor.WHITE + stateLabel());
        setLine(objective, 9, ChatColor.YELLOW + "팀 " + (team == null ? ChatColor.GRAY + "미참가" : team.coloredName()));
        setLine(objective, 8, ChatColor.YELLOW + "능력 " + ChatColor.WHITE + (ability == null ? "없음" : ability.name()));
        setLine(objective, 7, ChatColor.DARK_GRAY + " ");
        setLine(objective, 6, ChatColor.AQUA + "일반 " + cooldownStatus(player, 1));
        setLine(objective, 5, ChatColor.RED + "고급 " + cooldownStatus(player, 2));
        setLine(objective, 4, ChatColor.GOLD + "능력 " + cooldownStatus(player, 0));
        setLine(objective, 3, ChatColor.DARK_GRAY + "  ");
        setLine(objective, 2, ChatColor.YELLOW + "킬 " + ChatColor.WHITE + killsOf(player));
        setLine(objective, 1, ChatColor.YELLOW + "우르프 " + state(abilityManager.urfEnabled())
            + ChatColor.GRAY + " " + abilityManager.urfCooldownPercent() + "%");
    }

    private void setLine(Objective objective, int score, String text) {
        String line = text.length() > 32 ? text.substring(0, 32) : text;
        objective.getScore(line).setScore(score);
    }

    private String teamPrefix(GodTeam team) {
        if (!plugin.getConfig().getBoolean("scoreboard.team-prefixes", true)) {
            return team.color().toString();
        }
        String displayName = teamDisplayName(team);
        if (displayName.length() > 6) {
            displayName = displayName.substring(0, 6);
        }
        return team.color() + "[" + displayName + "] " + ChatColor.RESET;
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

    private String teamDisplayName(GodTeam team) {
        return plugin.getConfig().getString("teams." + team.id() + ".display-name", team.defaultDisplayName());
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
        if (millis <= 0L) {
            return ChatColor.DARK_AQUA + "사용 가능!";
        }
        long seconds = (millis + 999L) / 1000L;
        return ChatColor.WHITE + "" + (seconds / 60L) + "분 " + (seconds % 60L) + "초";
    }

    private String state(boolean enabled) {
        return enabled ? ChatColor.GREEN + "켜짐" : ChatColor.RED + "꺼짐";
    }

    private GodTeam smallestJoinableTeam() {
        GodTeam selected = null;
        int selectedCount = Integer.MAX_VALUE;
        for (GodTeam team : GodTeam.values()) {
            if (eliminatedTeams.contains(team)) {
                continue;
            }
            int count = 0;
            for (GodTeam assigned : teams.values()) {
                if (assigned == team) {
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

    private void startReadyTask() {
        if (readyTask != -1) {
            Bukkit.getScheduler().cancelTask(readyTask);
        }
        readySecondsRemaining = plugin.getConfig().getBoolean("game.fast-start", true)
            ? plugin.getConfig().getInt("game.fast-ready-countdown-seconds", 5)
            : plugin.getConfig().getInt("game.ready-countdown-seconds", 40);
        readySecondsRemaining = Math.max(1, readySecondsRemaining);
        readyReminder = 0;
        readyTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
                tickReady();
            }
        }, 20L, 20L);
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
        for (UUID uuid : pendingSelection) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                Bukkit.broadcastMessage(ChatColor.GOLD + "  " + player.getName());
            }
        }
        Bukkit.broadcastMessage(ChatColor.WHITE + "능력을 확정하려면 " + ChatColor.AQUA + "/t yes"
            + ChatColor.WHITE + " 또는 " + ChatColor.RED + "/t no" + ChatColor.WHITE + " 를 입력하세요.");
    }

    private void broadcastStartSettings() {
        Bukkit.broadcastMessage(ChatColor.GREEN + "****** 서버 세팅상태 ******");
        Bukkit.broadcastMessage(ChatColor.WHITE + "인벤토리 클리어 : " + state(plugin.getConfig().getBoolean("game.clear-inventory", true)));
        Bukkit.broadcastMessage(ChatColor.WHITE + "기본 아이템 지급 : " + state(plugin.getConfig().getBoolean("game.give-skyblock-items", true)));
        Bukkit.broadcastMessage(ChatColor.WHITE + "엔티티 삭제 : " + state(plugin.getConfig().getBoolean("game.remove-entities", true)));
        Bukkit.broadcastMessage(ChatColor.WHITE + "침대 무시 : " + state(plugin.getConfig().getBoolean("game.ignore-bed", true)));
        Bukkit.broadcastMessage(ChatColor.WHITE + "도박 : " + state(plugin.getConfig().getBoolean("gambling.enabled", true)));
        Bukkit.broadcastMessage(ChatColor.WHITE + "코어 폭파 보호 : " + state(plugin.getConfig().getBoolean("core.protect-diamond-from-explosion", true)));
        Bukkit.broadcastMessage(ChatColor.WHITE + "다이아 곡괭이 금지 : " + state(plugin.getConfig().getBoolean("core.forbid-diamond-pickaxe", true)));
        Bukkit.broadcastMessage(ChatColor.GREEN + "***************************");
    }

    private void preparePlayerForGame(Player player) {
        BukkitCompat.setSurvival(player);
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
            world.setTime(plugin.getConfig().getLong("world.start-time", 6000L));
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
        long interval = Math.max(1L, plugin.getConfig().getLong("game.ability-tick-interval-seconds", 1L)) * 20L;
        waterHealTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (state == GameState.RUNNING) {
                    abilityManager.tick(BukkitCompat.onlinePlayers());
                }
            }
        }, interval, interval);
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
