package kr.newgodwar.command;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.ability.AbilityManager;
import kr.newgodwar.ability.api.AbilityDefinition;
import kr.newgodwar.game.GameManager;
import kr.newgodwar.game.GodTeam;
import kr.newgodwar.gui.AbilityGui;
import kr.newgodwar.gui.SettingsGui;
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
        "ability", "abilities", "blacklist", "gamerule", "target", "setability", "spectate", "unspectate", "reload", "gui", "settings", "test", "midjoin"
    );

    private final NewGodWarPlugin plugin;
    private final GameManager gameManager;
    private final AbilityManager abilityManager;
    private final SettingsGui settingsGui;
    private final AbilityGui abilityGui;

    public GodWarCommand(NewGodWarPlugin plugin, GameManager gameManager, AbilityManager abilityManager, SettingsGui settingsGui, AbilityGui abilityGui) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.abilityManager = abilityManager;
        this.settingsGui = settingsGui;
        this.abilityGui = abilityGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("x")) {
            targetShortcut(sender, args, label);
            return true;
        }
        if (command.getName().equalsIgnoreCase("a")) {
            abilityShortcut(sender);
            return true;
        }

        if (args.length == 0) {
            help(sender, label);
            return true;
        }

        boolean themachyLabel = label.equalsIgnoreCase("t");
        if (themachyLabel && args[0].equalsIgnoreCase("help")) {
            abilityShortcut(sender);
            return true;
        }
        if (!themachyLabel && args[0].equalsIgnoreCase("help")) {
            help(sender, label);
            return true;
        }
        if (themachyLabel && (args[0].equalsIgnoreCase("ability") || args[0].equalsIgnoreCase("a"))) {
            setAbilityThemachy(sender, args);
            return true;
        }

        String sub = normalizeSubcommand(args[0], themachyLabel);
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
        if (sub.equals("midjoin")) {
            midJoin(sender, args);
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
        if (sub.equals("test")) {
            test(sender, args);
            return true;
        }
        if (sub.equals("stop")) {
            gameManager.stop(true);
            return true;
        }
        if (sub.equals("status")) {
            status(sender);
            return true;
        }
        if (sub.equals("ability")) {
            ability(sender, args);
            return true;
        }
        if (sub.equals("abilities")) {
            listAbilities(sender);
            return true;
        }
        if (sub.equals("assignedabilities")) {
            listAssignedAbilities(sender);
            return true;
        }
        if (sub.equals("blacklist")) {
            blacklist(sender, args);
            return true;
        }
        if (sub.equals("gamerule")) {
            gamerule(sender, args);
            return true;
        }
        if (sub.equals("target")) {
            target(sender, args);
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
            gameManager.reloadSettings();
            plugin.messages().send(sender, "&a설정을 다시 불러왔습니다.");
            return true;
        }
        if (sub.equals("gui") || sub.equals("settings")) {
            openSettings(sender);
            return true;
        }

        plugin.messages().send(sender, "&c알 수 없는 명령어입니다. /" + label + " help");
        return true;
    }

    private void help(CommandSender sender, String label) {
        sender.sendMessage("");
        line(sender);
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + " NewGodWar"
            + ChatColor.DARK_GRAY + " | " + ChatColor.YELLOW + "신들의 전쟁 운영 메뉴"
            + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + plugin.versionSupport().minecraftVersion());
        sender.sendMessage(ChatColor.GRAY + "  자주 쓰는 기능은 " + ChatColor.AQUA + "/" + label + " gui"
            + ChatColor.GRAY + " 에서 상자로 조작할 수 있습니다.");
        line(sender);
        section(sender, "게임 진행");
        command(sender, label, "gui", "관리자 설정 GUI 열기");
        command(sender, label, "start", "게임 시작 및 능력 배정");
        command(sender, label, "test [ability]", "혼자 능력 테스트 시작");
        command(sender, label, "stop", "게임 종료");
        command(sender, label, "status", "현재 상태 보기");
        command(sender, label, "autoteam", "온라인 플레이어 자동 팀 배정");
        section(sender, "팀 / 신전");
        command(sender, label, "join <red|blue|green> [player]", "팀 수동 배정");
        command(sender, label, "midjoin [red|blue|green]", "진행 중인 게임에 중간 참여");
        command(sender, label, "leave [player]", "팀 배정 해제");
        command(sender, label, "settemple <red|blue|green>", "바라보는 다이아 블록을 심장으로 등록");
        section(sender, "능력");
        sender.sendMessage(ChatColor.DARK_GRAY + "  /a" + ChatColor.GRAY + " 로 내 능력을 빠르게 확인할 수 있습니다.");
        sender.sendMessage(ChatColor.DARK_GRAY + "  /t help" + ChatColor.GRAY + " 도 Themachy처럼 내 능력을 보여줍니다.");
        command(sender, label, "abilities", "능력 도감 GUI");
        command(sender, label, "ability [player]", "현재 능력만 보기");
        command(sender, label, "setability <player> <ability>", "능력 수동 지정");
        command(sender, label, "target <player>", "타깃형 능력 대상 지정");
        sender.sendMessage(ChatColor.DARK_GRAY + "  /x <player>" + ChatColor.GRAY + " 로도 타깃을 빠르게 지정할 수 있습니다.");
        if (label.equalsIgnoreCase("t")) {
            section(sender, "Themachy 호환");
            command(sender, label, "t <red|blue|green> [player]", "팀 수동 배정");
            command(sender, label, "dia|d <red|blue|green>", "다이아 심장 등록");
            command(sender, label, "set", "설정 GUI 열기");
            command(sender, label, "black ...", "블랙리스트 관리");
            command(sender, label, "alist", "능력 도감 보기");
            command(sender, label, "a <ability> <player>", "능력 수동 지정");
        }
        if (sender.hasPermission("newgodwar.admin")) {
            section(sender, "운영 설정");
            command(sender, label, "blacklist <list|add|remove|toggle> [ability]", "랜덤 제외 능력 관리");
            command(sender, label, "gamerule <apply|restore>", "게임룰 수동 적용 / 복구");
            command(sender, label, "reload", "config.yml 다시 불러오기");
            command(sender, label, "spectate|unspectate <player>", "관전 모드 전환");
        }
        line(sender);
    }

    private void status(CommandSender sender) {
        sender.sendMessage("");
        line(sender);
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + " NewGodWar 상태");
        sender.sendMessage(ChatColor.GRAY + "  상태       " + ChatColor.YELLOW + gameManager.state());
        sender.sendMessage(ChatColor.GRAY + "  버전       " + ChatColor.YELLOW + plugin.versionSupport().minecraftVersion());
        sender.sendMessage(ChatColor.GRAY + "  팀킬       " + state(plugin.getConfig().getBoolean("game.friendly-fire", false)));
        sender.sendMessage(ChatColor.GRAY + "  우르프     " + state(abilityManager.urfEnabled()));
        sender.sendMessage(ChatColor.GRAY + "  게임룰     " + state(plugin.getConfig().getBoolean("gamerules.enabled", true)));
        sender.sendMessage(ChatColor.GRAY + "  블랙리스트 " + ChatColor.YELLOW + abilityManager.blacklistedAbilityIds().size() + ChatColor.GRAY + "개");
        sender.sendMessage(ChatColor.GRAY + "  원문       " + gameManager.statusLine());
        line(sender);
    }

    private void line(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "--------------------------------------------------");
    }

    private void section(CommandSender sender, String title) {
        sender.sendMessage(ChatColor.DARK_AQUA + "  [" + title + "]");
    }

    private void command(CommandSender sender, String label, String usage, String description) {
        sender.sendMessage(ChatColor.AQUA + "  /" + label + " " + usage
            + ChatColor.DARK_GRAY + " - " + ChatColor.GRAY + description);
    }

    private String state(boolean enabled) {
        return enabled ? ChatColor.GREEN + "켜짐" : ChatColor.RED + "꺼짐";
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
        if (!target.equals(sender) && !sender.hasPermission("newgodwar.admin")) {
            plugin.messages().send(sender, "&c다른 플레이어의 팀은 관리자만 변경할 수 있습니다.");
            return;
        }
        if (gameManager.isRunning()) {
            try {
                AbilityDefinition ability = gameManager.joinMidGame(target, team);
                plugin.messages().send(sender, "&a" + target.getName() + " 님이 " + team.coloredName()
                    + " 팀으로 중간 참여했습니다. 능력: &f" + ability.name());
            } catch (IllegalStateException ex) {
                plugin.messages().send(sender, "&c" + ex.getMessage());
            }
            return;
        }
        gameManager.assign(target, team);
        plugin.messages().send(sender, "&a" + target.getName() + " 님을 " + team.coloredName() + " 팀으로 배정했습니다.");
    }

    private void midJoin(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            plugin.messages().send(sender, "&c플레이어만 사용할 수 있습니다.");
            return;
        }
        GodTeam team = null;
        if (args.length >= 2) {
            team = GodTeam.parse(args[1]);
            if (team == null) {
                plugin.messages().send(sender, "&c팀은 red, blue, green 중 하나여야 합니다.");
                return;
            }
        }
        try {
            AbilityDefinition ability = gameManager.joinMidGame(player, team);
            plugin.messages().send(sender, "&a중간 참여했습니다. 능력: &f" + ability.name());
        } catch (IllegalStateException ex) {
            plugin.messages().send(sender, "&c" + ex.getMessage());
        }
    }

    private void leave(CommandSender sender, String[] args) {
        Player target = args.length >= 2 ? Bukkit.getPlayer(args[1]) : asPlayer(sender);
        if (target == null) {
            plugin.messages().send(sender, "&c대상 플레이어를 찾을 수 없습니다.");
            return;
        }
        if (!target.equals(sender) && !sender.hasPermission("newgodwar.admin")) {
            plugin.messages().send(sender, "&c다른 플레이어의 팀은 관리자만 변경할 수 있습니다.");
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

    private void test(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            plugin.messages().send(sender, "&c플레이어만 사용할 수 있습니다.");
            return;
        }
        AbilityDefinition ability = null;
        if (args.length >= 2) {
            ability = abilityManager.registry().get(args[1]);
            if (ability == null) {
                plugin.messages().send(sender, "&c능력을 찾을 수 없습니다.");
                return;
            }
        }
        try {
            AbilityDefinition assigned = gameManager.startTest(player, ability);
            plugin.messages().send(sender, "&a테스트 모드를 시작했습니다. 능력: &f" + assigned.name()
                + ChatColor.GRAY + " (" + assigned.id() + ")");
        } catch (IllegalStateException ex) {
            plugin.messages().send(sender, "&c" + ex.getMessage());
        }
    }

    private void ability(CommandSender sender, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("list")) {
            listAbilities(sender);
            return;
        }
        Player target = args.length >= 2 ? Bukkit.getPlayer(args[1]) : asPlayer(sender);
        if (target == null) {
            plugin.messages().send(sender, "&c대상 플레이어를 찾을 수 없습니다.");
            return;
        }
        Player viewer = asPlayer(sender);
        if (viewer != null) {
            abilityGui.open(viewer, target);
            return;
        }
        AbilityDefinition ability = abilityManager.get(target);
        if (ability == null) {
            plugin.messages().send(sender, "&e" + target.getName() + " 님은 아직 능력이 없습니다.");
            return;
        }
        plugin.messages().send(sender, "&a" + target.getName() + " 님의 능력: " + ability.name()
            + ChatColor.GRAY + " - " + ability.description());
    }

    private void abilityShortcut(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) {
            plugin.messages().send(sender, "&c플레이어만 사용할 수 있습니다.");
            return;
        }
        AbilityDefinition ability = abilityManager.get(player);
        if (ability == null) {
            plugin.messages().send(sender, "&e아직 능력이 없습니다.");
            return;
        }
        sender.sendMessage("");
        line(sender);
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "내 능력");
        sender.sendMessage(ChatColor.GRAY + "  이름   " + ChatColor.WHITE + ability.name()
            + ChatColor.DARK_GRAY + " (" + ability.id() + ")");
        sender.sendMessage(ChatColor.GRAY + "  설명   " + ChatColor.YELLOW + ability.description());
        sender.sendMessage(ChatColor.GRAY + "  일반   " + ChatColor.WHITE + ability.normalSkill()
            + ChatColor.DARK_GRAY + " / 조약돌 " + stoneCost(ability.normalStoneCost()));
        sender.sendMessage(ChatColor.GRAY + "  고급   " + ChatColor.WHITE + ability.advancedSkill()
            + ChatColor.DARK_GRAY + " / 조약돌 " + stoneCost(ability.advancedStoneCost()));
        sender.sendMessage(ChatColor.GRAY + "  패시브 " + ChatColor.WHITE + ability.passiveSkill());
        line(sender);
    }

    private void listAssignedAbilities(CommandSender sender) {
        boolean found = false;
        for (Player player : BukkitCompat.onlinePlayers()) {
            AbilityDefinition ability = abilityManager.get(player);
            if (ability == null) {
                continue;
            }
            if (!found) {
                sender.sendMessage(plugin.messages().prefix() + ChatColor.YELLOW + "적용된 능력");
                found = true;
            }
            sender.sendMessage(ChatColor.WHITE + player.getName() + ChatColor.GRAY + " : "
                + ChatColor.YELLOW + ability.name() + ChatColor.DARK_GRAY + " (" + ability.id() + ")");
        }
        if (!found) {
            plugin.messages().send(sender, "&e능력이 있는 플레이어가 없습니다.");
        }
    }

    private void listAbilities(CommandSender sender) {
        Player viewer = asPlayer(sender);
        if (viewer != null) {
            abilityGui.openList(viewer);
            return;
        }
        sender.sendMessage(plugin.messages().prefix() + ChatColor.YELLOW + "등록된 신의 능력");
        for (AbilityDefinition ability : abilityManager.registry().all()) {
            boolean enabled = abilityManager.isEnabled(ability);
            ChatColor stateColor = enabled ? ChatColor.GREEN : ChatColor.RED;
            String state = enabled ? "활성" : "비활성";
            sender.sendMessage(ChatColor.GOLD + ability.id() + ChatColor.GRAY + " | "
                + ChatColor.WHITE + ability.name() + ChatColor.GRAY + " | "
                + stateColor + state + ChatColor.GRAY + " - " + ability.description());
        }
    }

    private void setAbility(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.messages().send(sender, "&e/godwar setability <player> <ability>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        AbilityDefinition ability = abilityManager.registry().get(args[2]);
        if (target == null || ability == null) {
            plugin.messages().send(sender, "&c플레이어 또는 능력을 찾을 수 없습니다.");
            return;
        }
        abilityManager.set(target, ability);
        plugin.nms().sendTitle(target, ability.name(), ability.description(), 10, 60, 10);
        plugin.messages().send(sender, "&a능력을 지정했습니다.");
    }

    private void setAbilityThemachy(CommandSender sender, String[] args) {
        if (!sender.hasPermission("newgodwar.admin")) {
            plugin.messages().send(sender, "&c권한이 없습니다.");
            return;
        }
        if (args.length < 3) {
            plugin.messages().send(sender, "&e/t a <ability> <player>");
            return;
        }
        AbilityDefinition ability = abilityManager.registry().get(args[1]);
        Player target = Bukkit.getPlayer(args[2]);
        if (target == null || ability == null) {
            plugin.messages().send(sender, "&c플레이어 또는 능력을 찾을 수 없습니다.");
            return;
        }
        abilityManager.set(target, ability);
        plugin.nms().sendTitle(target, ability.name(), ability.description(), 10, 60, 10);
        plugin.messages().send(sender, "&a능력을 지정했습니다.");
    }

    private void blacklist(CommandSender sender, String[] args) {
        if (args.length < 2 || args[1].equalsIgnoreCase("list")) {
            List<String> ids = abilityManager.blacklistedAbilityIds();
            if (ids.isEmpty()) {
                plugin.messages().send(sender, "&e블랙리스트에 등록된 능력이 없습니다.");
                return;
            }
            plugin.messages().send(sender, "&e능력 블랙리스트: &f" + join(ids));
            return;
        }
        if (args.length < 3) {
            plugin.messages().send(sender, "&e/godwar blacklist <list|add|remove|toggle> [ability]");
            return;
        }

        String action = args[1].toLowerCase();
        AbilityDefinition ability = abilityManager.registry().get(args[2]);
        if (ability == null) {
            plugin.messages().send(sender, "&c능력을 찾을 수 없습니다.");
            return;
        }
        if (action.equals("add")) {
            abilityManager.setBlacklisted(ability.id(), true);
            plugin.messages().send(sender, "&a" + ability.name() + " 능력을 블랙리스트에 추가했습니다.");
            return;
        }
        if (action.equals("remove")) {
            abilityManager.setBlacklisted(ability.id(), false);
            plugin.messages().send(sender, "&a" + ability.name() + " 능력을 블랙리스트에서 제거했습니다.");
            return;
        }
        if (action.equals("toggle")) {
            abilityManager.toggleBlacklisted(ability.id());
            String state = abilityManager.isBlacklisted(ability) ? "추가" : "제거";
            plugin.messages().send(sender, "&a" + ability.name() + " 능력을 블랙리스트에서 " + state + "했습니다.");
            return;
        }
        plugin.messages().send(sender, "&e/godwar blacklist <list|add|remove|toggle> [ability]");
    }

    private void gamerule(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.messages().send(sender, "&e/godwar gamerule <apply|restore>");
            return;
        }
        if (args[1].equalsIgnoreCase("apply")) {
            gameManager.applyGameRules();
            plugin.messages().send(sender, "&a설정된 게임룰을 모든 월드에 적용했습니다.");
            return;
        }
        if (args[1].equalsIgnoreCase("restore")) {
            gameManager.restoreGameRules();
            plugin.messages().send(sender, "&a게임 시작 전 게임룰 값으로 복구했습니다.");
            return;
        }
        plugin.messages().send(sender, "&e/godwar gamerule <apply|restore>");
    }

    private void target(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            plugin.messages().send(sender, "&c플레이어만 사용할 수 있습니다.");
            return;
        }
        if (args.length < 2) {
            plugin.messages().send(sender, "&e/godwar target <player>");
            return;
        }
        abilityManager.setTarget(player, sender, args[1]);
    }

    private void targetShortcut(CommandSender sender, String[] args, String label) {
        Player player = asPlayer(sender);
        if (player == null) {
            plugin.messages().send(sender, "&c플레이어만 사용할 수 있습니다.");
            return;
        }
        if (args.length < 1) {
            plugin.messages().send(sender, "&e/" + label + " <player>");
            return;
        }
        abilityManager.setTarget(player, sender, args[0]);
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

    private void openSettings(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) {
            plugin.messages().send(sender, "&c플레이어만 GUI를 열 수 있습니다.");
            return;
        }
        settingsGui.open(player);
    }

    private boolean requiresAdmin(String sub) {
        return !sub.equals("ability")
            && !sub.equals("abilities")
            && !sub.equals("target")
            && !sub.equals("status")
            && !sub.equals("join")
            && !sub.equals("leave")
            && !sub.equals("midjoin");
    }

    private Player asPlayer(CommandSender sender) {
        return sender instanceof Player ? (Player) sender : null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("x") && args.length == 1) {
            List<String> values = new ArrayList<String>();
            for (Player player : BukkitCompat.onlinePlayers()) {
                values.add(player.getName());
            }
            return startsWith(values, args[0]);
        }
        if (command.getName().equalsIgnoreCase("a")) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            List<String> values = new ArrayList<String>(SUBCOMMANDS);
            if (alias.equalsIgnoreCase("t")) {
                values.add("alist");
                values.add("a");
                values.add("black");
                values.add("d");
                values.add("dia");
                values.add("s");
                values.add("set");
                values.add("t");
            }
            return startsWith(values, args[0]);
        }
        String sub = normalizeSubcommand(args[0], alias.equalsIgnoreCase("t"));
        if (alias.equalsIgnoreCase("t") && (args[0].equalsIgnoreCase("a") || args[0].equalsIgnoreCase("ability"))) {
            if (args.length == 2) {
                List<String> abilities = new ArrayList<String>();
                for (String id : abilityManager.registry().ids()) {
                    abilities.add(id);
                }
                return startsWith(abilities, args[1]);
            }
            if (args.length == 3) {
                List<String> players = new ArrayList<String>();
                for (Player player : BukkitCompat.onlinePlayers()) {
                    players.add(player.getName());
                }
                return startsWith(players, args[2]);
            }
        }
        if (args.length == 2 && (sub.equals("join") || sub.equals("settemple"))) {
            return startsWith(Arrays.asList("red", "blue", "green"), args[1]);
        }
        if (args.length == 2 && sub.equals("midjoin")) {
            return startsWith(Arrays.asList("red", "blue", "green"), args[1]);
        }
        if (args.length == 2 && sub.equals("ability")) {
            List<String> values = new ArrayList<String>();
            values.add("list");
            for (Player player : BukkitCompat.onlinePlayers()) {
                values.add(player.getName());
            }
            return startsWith(values, args[1]);
        }
        if (args.length == 2 && sub.equals("target")) {
            List<String> values = new ArrayList<String>();
            for (Player player : BukkitCompat.onlinePlayers()) {
                values.add(player.getName());
            }
            return startsWith(values, args[1]);
        }
        if (args.length == 2 && sub.equals("blacklist")) {
            return startsWith(Arrays.asList("list", "add", "remove", "toggle"), args[1]);
        }
        if (args.length == 3 && sub.equals("blacklist")) {
            List<String> abilities = new ArrayList<String>();
            for (String id : abilityManager.registry().ids()) {
                abilities.add(id);
            }
            return startsWith(abilities, args[2]);
        }
        if (args.length == 2 && sub.equals("gamerule")) {
            return startsWith(Arrays.asList("apply", "restore"), args[1]);
        }
        if (args.length == 2 && sub.equals("test")) {
            List<String> abilities = new ArrayList<String>();
            for (String id : abilityManager.registry().ids()) {
                abilities.add(id);
            }
            return startsWith(abilities, args[1]);
        }
        if (args.length == 3 && sub.equals("setability")) {
            List<String> abilities = new ArrayList<String>();
            for (String id : abilityManager.registry().ids()) {
                abilities.add(id);
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

    private String join(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(values.get(i));
        }
        return builder.toString();
    }

    private String stoneCost(int cost) {
        return cost <= 0 ? "없음" : cost + "개";
    }

    private String normalizeSubcommand(String sub, boolean themachyLabel) {
        String lower = sub == null ? "" : sub.toLowerCase();
        if (!themachyLabel) {
            return lower;
        }
        if (lower.equals("alist")) {
            return "assignedabilities";
        }
        if (lower.equals("black")) {
            return "blacklist";
        }
        if (lower.equals("dia") || lower.equals("d")) {
            return "settemple";
        }
        if (lower.equals("set")) {
            return "settings";
        }
        if (lower.equals("team") || lower.equals("t")) {
            return "join";
        }
        return lower;
    }
}
