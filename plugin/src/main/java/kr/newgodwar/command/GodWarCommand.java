package kr.newgodwar.command;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.ability.AbilityManager;
import kr.newgodwar.ability.AbilityType;
import kr.newgodwar.game.GameManager;
import kr.newgodwar.game.GodTeam;
import kr.newgodwar.util.BukkitCompat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class GodWarCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList(
        "help", "autoteam", "join", "leave", "settemple", "start", "stop", "status",
        "ability", "setability", "spectate", "unspectate", "reload"
    );

    private final NewGodWarPlugin plugin;
    private final GameManager gameManager;
    private final AbilityManager abilityManager;

    public GodWarCommand(NewGodWarPlugin plugin, GameManager gameManager, AbilityManager abilityManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.abilityManager = abilityManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            help(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase();
        if (requiresAdmin(sub) && !sender.hasPermission("newgodwar.admin")) {
            plugin.messages().send(sender, "&c권한이 없습니다.");
            return true;
        }

        if (sub.equals("autoteam")) {
            gameManager.autoBalance();
            plugin.messages().send(sender, "&a온라인 플레이어를 자동으로 팀 배정했습니다.");
            return true;
        }
        if (sub.equals("join")) {
            join(sender, args);
            return true;
        }
        if (sub.equals("leave")) {
            leave(sender, args);
            return true;
        }
        if (sub.equals("settemple")) {
            setTemple(sender, args);
            return true;
        }
        if (sub.equals("start")) {
            start(sender);
            return true;
        }
        if (sub.equals("stop")) {
            gameManager.stop(true);
            return true;
        }
        if (sub.equals("status")) {
            sender.sendMessage(plugin.messages().prefix() + gameManager.statusLine());
            return true;
        }
        if (sub.equals("ability")) {
            ability(sender, args);
            return true;
        }
        if (sub.equals("setability")) {
            setAbility(sender, args);
            return true;
        }
        if (sub.equals("spectate")) {
            spectate(sender, args, true);
            return true;
        }
        if (sub.equals("unspectate")) {
            spectate(sender, args, false);
            return true;
        }
        if (sub.equals("reload")) {
            plugin.reloadConfig();
            plugin.messages().send(sender, "&a설정을 다시 불러왔습니다.");
            return true;
        }

        plugin.messages().send(sender, "&c알 수 없는 명령어입니다. /" + label + " help");
        return true;
    }

    private void help(CommandSender sender, String label) {
        sender.sendMessage(plugin.messages().prefix() + ChatColor.YELLOW + "명령어");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " autoteam");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " join <red|blue|green> [player]");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " settemple <red|blue|green>");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " start | stop | status");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " ability [player]");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " setability <player> <ability>");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " spectate|unspectate <player>");
    }

    private void join(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.messages().send(sender, "&e/godwar join <red|blue|green> [player]");
            return;
        }
        GodTeam team = GodTeam.parse(args[1]);
        if (team == null) {
            plugin.messages().send(sender, "&c팀은 red, blue, green 중 하나여야 합니다.");
            return;
        }
        Player target = args.length >= 3 ? Bukkit.getPlayer(args[2]) : asPlayer(sender);
        if (target == null) {
            plugin.messages().send(sender, "&c대상 플레이어를 찾을 수 없습니다.");
            return;
        }
        gameManager.assign(target, team);
        plugin.messages().send(sender, "&a" + target.getName() + " 님을 " + team.coloredName() + " 팀으로 배정했습니다.");
    }

    private void leave(CommandSender sender, String[] args) {
        Player target = args.length >= 2 ? Bukkit.getPlayer(args[1]) : asPlayer(sender);
        if (target == null) {
            plugin.messages().send(sender, "&c대상 플레이어를 찾을 수 없습니다.");
            return;
        }
        gameManager.leave(target);
        plugin.messages().send(sender, "&a" + target.getName() + " 님의 팀 배정을 해제했습니다.");
    }

    private void setTemple(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            plugin.messages().send(sender, "&c플레이어만 사용할 수 있습니다.");
            return;
        }
        if (args.length < 2) {
            plugin.messages().send(sender, "&e/godwar settemple <red|blue|green>");
            return;
        }
        GodTeam team = GodTeam.parse(args[1]);
        if (team == null) {
            plugin.messages().send(sender, "&c팀은 red, blue, green 중 하나여야 합니다.");
            return;
        }
        Block block = BukkitCompat.getTargetBlock(player, 8);
        if (block == null || block.getType() != Material.DIAMOND_BLOCK) {
            plugin.messages().send(sender, "&c바라보는 블록이 다이아몬드 블록이어야 합니다.");
            return;
        }
        if (gameManager.setTemple(team, block)) {
            plugin.messages().send(sender, "&a" + team.coloredName() + " 팀의 다이아 심장을 등록했습니다.");
        }
    }

    private void start(CommandSender sender) {
        try {
            gameManager.start();
        } catch (IllegalStateException ex) {
            plugin.messages().send(sender, "&c" + ex.getMessage());
        }
    }

    private void ability(CommandSender sender, String[] args) {
        Player target = args.length >= 2 ? Bukkit.getPlayer(args[1]) : asPlayer(sender);
        if (target == null) {
            plugin.messages().send(sender, "&c대상 플레이어를 찾을 수 없습니다.");
            return;
        }
        AbilityType ability = abilityManager.get(target);
        if (ability == null) {
            plugin.messages().send(sender, "&e" + target.getName() + " 님은 아직 능력이 없습니다.");
            return;
        }
        plugin.messages().send(sender, "&a" + target.getName() + " 님의 능력: " + ability.displayName()
            + ChatColor.GRAY + " - " + ability.description());
    }

    private void setAbility(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.messages().send(sender, "&e/godwar setability <player> <ability>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        AbilityType ability = AbilityType.parse(args[2]);
        if (target == null || ability == null) {
            plugin.messages().send(sender, "&c플레이어 또는 능력을 찾을 수 없습니다.");
            return;
        }
        abilityManager.set(target, ability);
        plugin.nms().sendTitle(target, ability.displayName(), ability.description(), 10, 60, 10);
        plugin.messages().send(sender, "&a능력을 지정했습니다.");
    }

    private void spectate(CommandSender sender, String[] args, boolean spectate) {
        if (args.length < 2) {
            plugin.messages().send(sender, "&e/godwar " + (spectate ? "spectate" : "unspectate") + " <player>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            plugin.messages().send(sender, "&c대상 플레이어를 찾을 수 없습니다.");
            return;
        }
        if (spectate) {
            gameManager.setSpectator(target);
        } else {
            gameManager.unsetSpectator(target);
        }
        plugin.messages().send(sender, "&a처리했습니다.");
    }

    private boolean requiresAdmin(String sub) {
        return !sub.equals("ability") && !sub.equals("status");
    }

    private Player asPlayer(CommandSender sender) {
        return sender instanceof Player ? (Player) sender : null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return startsWith(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("join") || args[0].equalsIgnoreCase("settemple"))) {
            return startsWith(Arrays.asList("red", "blue", "green"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("setability")) {
            List<String> abilities = new ArrayList<String>();
            for (AbilityType type : AbilityType.values()) {
                abilities.add(type.id());
            }
            return startsWith(abilities, args[2]);
        }
        return Collections.emptyList();
    }

    private List<String> startsWith(List<String> values, String prefix) {
        List<String> result = new ArrayList<String>();
        String lower = prefix == null ? "" : prefix.toLowerCase();
        for (String value : values) {
            if (value.toLowerCase().startsWith(lower)) {
                result.add(value);
            }
        }
        return result;
    }
}
