package kr.newgodwar.regression;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.game.GameManager;
import kr.newgodwar.game.GameState;
import kr.newgodwar.game.GodTeam;
import kr.newgodwar.listener.GameListener;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Scoreboard;
import java.lang.reflect.*;
import java.util.*;

/** Checks the other viewer's board when a participant reconnects. */
final class RejoinDisplayRegression {
    @SuppressWarnings("unchecked") static void run(NewGodWarPlugin core) throws Exception {
        GameManager game = core.game();
        Server original = Bukkit.getServer();
        Field serverField = field(Bukkit.class, "server"), stateField = field(GameManager.class, "state");
        Object originalState = stateField.get(game);
        Map<UUID, GodTeam> teams = (Map<UUID, GodTeam>) field(GameManager.class, "teams").get(game);
        List<Player> online = new ArrayList<Player>();
        Scoreboard[] returningBoard = {original.getScoreboardManager().getNewScoreboard()};
        Scoreboard[] viewerBoard = {original.getScoreboardManager().getNewScoreboard()};
        Player returning = player("RejoinReturn", returningBoard, online);
        Player viewer = player("RejoinViewer", viewerBoard, online);
        teams.put(returning.getUniqueId(), GodTeam.RED);
        teams.put(viewer.getUniqueId(), GodTeam.BLUE);
        stateField.set(game, GameState.RUNNING);
        game.refreshPlayerDisplay(viewer); // Resolve adapters before replacing CraftServer.
        Server fixture = (Server) Proxy.newProxyInstance(Server.class.getClassLoader(), new Class<?>[]{Server.class}, (p,m,a) -> {
            if (m.getName().equals("getOnlinePlayers")) return online;
            if (m.getName().equals("getPlayer") || m.getName().equals("getPlayerExact")) {
                for (Player candidate : online) if (candidate.getUniqueId().equals(a[0]) || candidate.getName().equals(a[0])) return candidate;
                return null;
            }
            try { return m.invoke(original, a); }
            catch (InvocationTargetException ex) { throw ex.getCause(); }
        });
        try {
            serverField.set(null, fixture);
            online.add(viewer);
            game.refreshPlayerDisplay(viewer);
            Scoreboard before = viewerBoard[0];
            before.registerNewObjective("external", "dummy").getScore("keep").setScore(9);
            require(before.getEntryTeam(returning.getName()) == null, "Fixture did not start with an offline participant");
            online.add(returning);
            GameListener listener = new GameListener(core, game, core.abilities(), core.nms());
            listener.onJoin(new PlayerJoinEvent(returning, null));
            require(before.getTeam("gw_red").hasEntry(returning.getName()), "Reconnect did not add the participant to the existing viewer's team");
            // Also reconcile an outdated team entry without rebuilding the board.
            before.getTeam("gw_blue").addEntry(returning.getName());
            listener.onJoin(new PlayerJoinEvent(returning, null));
            for (int i=0; i<3; i++) game.refreshAllPlayerSidebars();
            require(viewerBoard[0] == before && before.getObjective("external").getScore("keep").getScore() == 9,
                "Reconnect replaced the other viewer's board or removed its objective");
            require(before.getTeam("gw_red").hasEntry(returning.getName()) && !before.getTeam("gw_blue").hasEntry(returning.getName()),
                "Reconnect kept an outdated team entry");
            require(returningBoard[0].getTeam("gw_red").hasEntry(returning.getName()), "Own board lost the reconnected team");
            core.getLogger().info("PASS rejoin display: other viewer roster, stale team, own board and external objective");
        } finally {
            serverField.set(null, original);
            stateField.set(game, originalState);
            teams.remove(returning.getUniqueId()); teams.remove(viewer.getUniqueId());
        }
    }

    private static Player player(String name, Scoreboard[] board, List<Player> online) {
        UUID id = UUID.randomUUID();
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class}, (p,m,a) -> {
            switch(m.getName()) {
                case "getUniqueId": return id;
                case "getName": case "getDisplayName": return name;
                case "getScoreboard": return board[0];
                case "setScoreboard": board[0]=(Scoreboard)a[0]; return null;
                case "getActivePotionEffects": return Collections.emptyList();
                case "getGameMode": return org.bukkit.GameMode.SURVIVAL;
                case "isOnline": return online.contains(p);
                case "hashCode": return id.hashCode();
                case "equals": return p==a[0];
                default:
                    Class<?> type = m.getReturnType();
                    if (type == boolean.class) return false;
                    if (type == int.class) return 0;
                    if (type == long.class) return 0L;
                    if (type == double.class) return 0D;
                    if (type == float.class) return 0F;
                    return null;
            }
        });
    }
    private static Field field(Class<?> owner, String name) throws Exception {
        Field field = owner.getDeclaredField(name); field.setAccessible(true); return field;
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
