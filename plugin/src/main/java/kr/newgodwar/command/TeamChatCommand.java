package kr.newgodwar.command;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.game.GameManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class TeamChatCommand implements CommandExecutor {

    private final NewGodWarPlugin plugin;
    private final GameManager gameManager;

    public TeamChatCommand(NewGodWarPlugin plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.messages().send(sender, "&c플레이어만 사용할 수 있습니다.");
            return true;
        }
        if (args.length == 0) {
            Player player = (Player) sender;
            if (gameManager.teamOf(player) == null) {
                plugin.messages().send(sender, "&c팀에 소속되어 있지 않습니다.");
                return true;
            }
            boolean enabled = gameManager.toggleTeamChatMode(player);
            if (enabled) {
                plugin.messages().send(sender, "&b팀 채팅 모드가 켜졌습니다. 이제 일반 채팅이 팀챗으로 전송됩니다.");
            } else {
                plugin.messages().send(sender, "&e팀 채팅 모드가 꺼졌습니다.");
            }
            return true;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        gameManager.sendTeamChat((Player) sender, builder.toString());
        return true;
    }
}
