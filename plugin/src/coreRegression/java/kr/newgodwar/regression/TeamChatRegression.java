package kr.newgodwar.regression;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.ability.AbilitySession;
import kr.newgodwar.ability.api.AbilityDefinition;
import kr.newgodwar.ability.api.AbilityPlayerContext;
import kr.newgodwar.ability.builtin.BaseAbility;
import kr.newgodwar.game.GameManager;
import kr.newgodwar.game.GameState;
import kr.newgodwar.game.GodTeam;
import kr.newgodwar.listener.GameListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.scheduler.BukkitScheduler;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Exercises offline scoreboard cleanup and the actual public/team chat handler. */
final class TeamChatRegression {
    static void run(NewGodWarPlugin core) throws Exception {
        GameManager game = core.game();
        GameListener listener = new GameListener(core, game, core.abilities(), core.nms());
        UUID id = UUID.randomUUID();
        Scoreboard[] board = {Bukkit.getScoreboardManager().getMainScoreboard()};
        String[] listName = {"OfflineTeamPlayer"};
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(),
            new Class<?>[] {Player.class}, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "getUniqueId": return id;
                    case "getActivePotionEffects": return java.util.Collections.emptyList();
                    case "getName": case "getDisplayName": return "OfflineTeamPlayer";
                    case "getScoreboard": return board[0];
                    case "setScoreboard": board[0] = (Scoreboard) args[0]; return null;
                    case "getPlayerListName": return listName[0];
                    case "setPlayerListName": listName[0] = (String) args[0]; return null;
                    case "hashCode": return id.hashCode();
                    case "equals": return proxy == args[0];
                    default:
                        if (method.getReturnType() == boolean.class) return false;
                        if (method.getReturnType() == int.class) return 0;
                        if (method.getReturnType() == long.class) return 0L;
                        if (method.getReturnType() == double.class) return 0D;
                        if (method.getReturnType() == float.class) return 0F;
                        return null;
                }
            });
        Object originalName = core.getConfig().get("teams.red.display-name");
        Object originalColor = core.getConfig().get("teams.red.color");
        Object originalTeleport = core.getConfig().get("lobby.teleport-on-join");
        try {
            game.stop(false);
            core.getConfig().set("lobby.teleport-on-join", false);
            core.getConfig().set("teams.red.display-name", "긴이름의 100% 팀");
            core.getConfig().set("teams.red.color", "GOLD");
            game.assign(player, GodTeam.RED);
            checkPublicChat(listener, player, ChatColor.GOLD + "[긴이름의 100% 팀] " + ChatColor.RESET);

            game.changeTeam(player, GodTeam.BLUE);
            checkPublicChat(listener, player, game.teamColor(GodTeam.BLUE) + "[" + game.teamDisplayName(GodTeam.BLUE) + "] " + ChatColor.RESET);
            game.changeTeam(player, GodTeam.RED);
            core.getConfig().set("teams.red.display-name", "새 팀명");
            game.reloadSettings();
            checkPublicChat(listener, player, ChatColor.GOLD + "[새 팀명] " + ChatColor.RESET);

            require(game.toggleTeamChatMode(player), "Team chat mode did not enable");
            AsyncPlayerChatEvent privateChat = chat(player);
            listener.onChat(privateChat);
            require(privateChat.isCancelled() && privateChat.getFormat().equals("<%1$s> %2$s"),
                "Team chat leaked to public chat or received a public prefix");
            game.toggleTeamChatMode(player);

            Field state = GameManager.class.getDeclaredField("state");
            state.setAccessible(true);
            state.set(game, GameState.RUNNING);
            checkPublicChat(listener, player, ChatColor.GOLD + "[새 팀명] " + ChatColor.RESET);
            game.refreshPlayerDisplay(player);
            checkSidebarUpdates(core, player, board);
            // Proxy players are absent from Bukkit's online list, like a disconnected player.
            Team assigned = board[0].getTeam("gw_red");
            assigned.addEntry(player.getName());
            listener.onQuit(new PlayerQuitEvent(player, null));
            require(game.teamOf(player) == GodTeam.RED, "Disconnect lost a running game's team");
            listener.onJoin(new PlayerJoinEvent(player, null));
            require(game.teamOf(player) == GodTeam.RED && listName[0].contains("새 팀명"),
                "Reconnect during the same round failed to restore the team display");

            board[0].getTeam("gw_red").addEntry(player.getName());
            Scoreboard offlineBoard = board[0];
            listener.onQuit(new PlayerQuitEvent(player, null));
            game.stop(false);
            require(game.teamOf(player) == null && game.teamAssignments().isEmpty(), "Stop retained offline assignments");
            require(offlineBoard.getEntryTeam(player.getName()) == null && offlineBoard.getTeam("gw_red") == null,
                "Stop retained an offline player's scoreboard team");
            require(offlineBoard.getObjective("gw_status") == null, "Stop retained an offline player's team sidebar");
            listener.onJoin(new PlayerJoinEvent(player, null));
            require(listName[0].equals(player.getName()), "Reconnect after stop retained the tab prefix");
            require(board[0].getEntryTeam(player.getName()) == null, "Reconnect after stop restored an old team");
            checkPublicChat(listener, player, "");
            game.stop(false);
            checkPublicChat(listener, player, "");

            game.assign(player, GodTeam.BLUE);
            game.leave(player);
            checkPublicChat(listener, player, "");
            core.getLogger().info("PASS offline teams: running reconnect, stop cleanup, post-stop reconnect and repeated stop");
            core.getLogger().info("PASS public chat: team colors, long/percent names, rename, reassignment, unassigned and team chat");
        } finally {
            core.getConfig().set("teams.red.display-name", originalName);
            core.getConfig().set("teams.red.color", originalColor);
            core.getConfig().set("lobby.teleport-on-join", originalTeleport);
            game.stop(false);
            game.reloadSettings();
        }
    }

    private static AsyncPlayerChatEvent chat(Player player) {
        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(true, player, "hello 100% %s", new HashSet<Player>());
        event.setFormat("<%1$s> %2$s");
        return event;
    }

    @SuppressWarnings("unchecked")
    private static void checkSidebarUpdates(NewGodWarPlugin core, Player player, Scoreboard[] board) throws Exception {
        GameManager game = core.game();
        Scoreboard initial = board[0];
        initial.registerNewObjective("external", "dummy").getScore("preserved").setScore(7);
        AbilityDefinition definition = core.abilities().registry().get("thor");
        SidebarProbe probe = new SidebarProbe();
        Field assignmentsField = core.abilities().getClass().getDeclaredField("assignments");
        assignmentsField.setAccessible(true);
        Map<UUID, AbilitySession> assignments = (Map<UUID, AbilitySession>) assignmentsField.get(core.abilities());
        assignments.put(player.getUniqueId(), new AbilitySession(definition, probe));
        Object enabled = core.getConfig().get("scoreboard.enabled");
        Server original = Bukkit.getServer();
        Field serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        List<Runnable> periodic = new ArrayList<Runnable>();
        BukkitScheduler scheduler = (BukkitScheduler) Proxy.newProxyInstance(BukkitScheduler.class.getClassLoader(),
            new Class<?>[] {BukkitScheduler.class}, (p, m, a) -> {
                if (m.getName().equals("scheduleSyncRepeatingTask")) { periodic.add((Runnable) a[1]); return -1; }
                return invoke(original.getScheduler(), m, a);
            });
        Server fixture = (Server) Proxy.newProxyInstance(Server.class.getClassLoader(), new Class<?>[] {Server.class}, (p, m, a) -> {
            if (m.getName().equals("getOnlinePlayers")) return Collections.singletonList(player);
            if (m.getName().equals("getScheduler")) return scheduler;
            return invoke(original, m, a);
        });
        try {
            core.getConfig().set("scoreboard.enabled", true);
            // Resolve legacy scoreboard adapters before temporarily replacing CraftServer.
            game.refreshPlayerSidebar(player);
            probe.cooldown(new AbilityPlayerContext(core, player, definition));
            require(board[0] == initial, "Cooldown start replaced the player's scoreboard");
            require(initial.getEntries().stream().anyMatch(line -> line.contains("일반 ") && !line.contains("준비")),
                "Cooldown start did not refresh its sidebar line");
            serverField.set(null, fixture);
            Method start = GameManager.class.getDeclaredMethod("startWaterHealTask");
            start.setAccessible(true);
            start.invoke(game);
            require(periodic.size() == 2, "Ability maintenance timers were not scheduled");
            for (int i = 0; i < 3; i++) periodic.get(1).run();
            require(board[0] == initial && initial.getObjective("external").getScore("preserved").getScore() == 7,
                "Periodic ability tick replaced the scoreboard or removed another objective");
            core.getConfig().set("scoreboard.enabled", false);
            game.refreshPlayerSidebar(player);
            require(board[0] == initial && initial.getObjective("gw_status") == null,
                "Disabling the sidebar replaced the board or left its objective");
            core.getConfig().set("scoreboard.enabled", true);
            game.refreshPlayerSidebar(player);
            require(board[0] == initial && initial.getObjective("gw_status") != null,
                "Re-enabling the sidebar failed");
            board[0] = original.getScoreboardManager().getNewScoreboard();
            game.refreshPlayerSidebar(player);
            require(board[0] != initial && board[0].getObjective("gw_status") != null
                && board[0].getTeam("gw_red") != null, "Missing player display was not rebuilt");
            core.getLogger().info("PASS sidebar refresh: cooldown, periodic tick, external objective, disable/enable and fallback rebuild");
        } finally {
            serverField.set(null, original);
            assignments.remove(player.getUniqueId());
            core.getConfig().set("scoreboard.enabled", enabled);
        }
    }

    private static Object invoke(Object target, Method method, Object[] args) throws Throwable {
        try { return method.invoke(target, args); }
        catch (InvocationTargetException ex) { throw ex.getCause(); }
    }

    private static final class SidebarProbe extends BaseAbility {
        void cooldown(AbilityPlayerContext context) { setCooldown(context, 1, 30); }
    }

    private static void checkPublicChat(GameListener listener, Player player, String prefix) {
        AsyncPlayerChatEvent event = chat(player);
        listener.onChat(event);
        require(!event.isCancelled(), "Public chat was cancelled");
        String rendered = String.format(event.getFormat(), player.getDisplayName(), event.getMessage());
        require(rendered.equals(prefix + "<" + player.getDisplayName() + "> " + event.getMessage()),
            "Unexpected public chat format: " + rendered);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
