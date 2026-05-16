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
            plugin.messages().send(sender, "&e/" + label + " <message>");
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
