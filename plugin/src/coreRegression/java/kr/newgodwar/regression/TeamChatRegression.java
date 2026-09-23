package kr.newgodwar.regression;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.game.GameManager;
import kr.newgodwar.game.GameState;
import kr.newgodwar.game.GodTeam;
import kr.newgodwar.listener.GameListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.HashSet;
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
