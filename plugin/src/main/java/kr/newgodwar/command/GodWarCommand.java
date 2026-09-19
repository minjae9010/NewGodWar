package kr.newgodwar.command;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.ability.AbilityManager;
import kr.newgodwar.ability.api.AbilityDefinition;
import kr.newgodwar.game.GameManager;
import kr.newgodwar.game.GodTeam;
import kr.newgodwar.game.StarterItems;
import kr.newgodwar.game.VoidWorldGenerator;
import kr.newgodwar.game.WorldBackupManager;
import kr.newgodwar.gui.AbilityGui;
import kr.newgodwar.gui.SettingsGui;
import kr.newgodwar.gui.SettingsPage;
import kr.newgodwar.gui.StarterItemsGui;
import kr.newgodwar.util.BukkitCompat;
import kr.newgodwar.util.GameTips;
import kr.newgodwar.util.PluginUpdater;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.io.File;
import java.io.IOException;

public final class GodWarCommand implements CommandExecutor, TabCompleter {

    private static final String PRIMARY_COMMAND_LABEL = "gw";

    private final NewGodWarPlugin plugin;
    private final GameManager gameManager;
    private final AbilityManager abilityManager;
    private final SettingsGui settingsGui;
    private final StarterItemsGui starterItemsGui;
    private final AbilityGui abilityGui;
    private final WorldBackupManager worldBackupManager;

    public GodWarCommand(NewGodWarPlugin plugin, GameManager gameManager, AbilityManager abilityManager, SettingsGui settingsGui, StarterItemsGui starterItemsGui, AbilityGui abilityGui, WorldBackupManager worldBackupManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.abilityManager = abilityManager;
        this.settingsGui = settingsGui;
        this.starterItemsGui = starterItemsGui;
        this.abilityGui = abilityGui;
        this.worldBackupManager = worldBackupManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("x")) {
            targetShortcut(sender, args, label);
            return true;
        }
        if (command.getName().equalsIgnoreCase("a")) {
            if (args.length == 0) {
                abilityShortcut(sender);
                return true;
            }
            args = prependSubcommand("ability", args);
        }
        args = CommandCatalog.expandShortcut(command.getName(), args);

        if (args.length == 0) {
            help(sender, label, args);
            return true;
        }

        boolean themachyLabel = isThemachyRoot(command, label);
        if (normalizeSubcommand(args[0]).equals("help")) {
            help(sender, label, args);
            return true;
        }
        if (themachyLabel && GodTeam.parse(args[0]) != null) {
            if (!sender.hasPermission("newgodwar.admin")) {
                plugin.messages().send(sender, "&c권한이 없습니다.");
                return true;
            }
            join(sender, prependSubcommand("join", args));
            return true;
        }
        CommandTree.Result route = CommandTree.resolve(args);
        if (route.help != null) {
            CommandHelp.group(sender, route.help, route.path, groupHelpArguments(args));
            return true;
        }
        args = route.args;
        if (route.abilityView) {
            ability(sender, args);
            return true;
        }
        if (isAbilityGroupRoot(themachyLabel, args[0])) {
            abilityGroup(sender, args, label, themachyLabel);
            return true;
        }

        String sub = normalizeSubcommand(args[0]);
        if (CommandCatalog.find(sub) == null) {
            plugin.messages().send(sender, "&c알 수 없는 명령어입니다. /gw help <검색어> 로 찾아보세요.");
            List<String> suggestions = CommandCatalog.suggest(sub, sender.hasPermission("newgodwar.admin"));
            if (!suggestions.isEmpty()) plugin.messages().send(sender, "&7추천 명령어: &f/gw " + join(suggestions));
            return true;
        }
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
        if (sub.equals("changeteam")) {
            changeTeam(sender, args);
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
        if (sub.equals("setspawn")) {
            setSpawn(sender, args);
            return true;
        }
        if (sub.equals("setlobby")) {
            setLobby(sender);
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
        if (sub.equals("tips")) {
            GameTips.send(sender, plugin);
            return true;
        }
        if (sub.equals("urf")) {
            urf(sender, args);
            return true;
        }
        if (sub.equals("world")) {
            world(sender, label, args);
            return true;
        }
        if (sub.equals("map")) {
            map(sender, label, args);
            return true;
        }
        if (sub.equals("info")) {
            teamInfo(sender, args);
            return true;
        }
        if (sub.equals("yes")) {
            confirmAbility(sender);
            return true;
        }
        if (sub.equals("no")) {
            rerollAbility(sender);
            return true;
        }
        if (sub.equals("clear")) {
            clearCooldowns(sender, args);
            return true;
        }
        if (sub.equals("gamble")) {
            plugin.getServer().dispatchCommand(sender, "gamble");
            return true;
        }
        if (sub.equals("gamblereward")) {
            gamblingReward(sender, args);
            return true;
        }
        if (sub.equals("defaultitems")) {
            defaultItems(sender, args);
            return true;
        }
        if (sub.equals("ability")) {
            ability(sender, args);
            return true;
        }
        if (sub.equals("abilities")) {
            listAbilities(sender, joinArguments(args, 1));
            return true;
        }
        if (sub.equals("assignedabilities")) {
            listAssignedAbilities(sender, joinArguments(args, 1));
            return true;
        }
        if (sub.equals("participants")) {
            listParticipants(sender, joinArguments(args, 1));
            return true;
        }
        if (sub.equals("rerolls")) {
            rerolls(sender, args);
            return true;
        }
        if (sub.equals("skip")) {
            skipAbilitySelection(sender, args, 1);
            return true;
        }
        if (sub.equals("skipseconds")) {
            skipSeconds(sender, args);
            return true;
        }
        if (sub.equals("pickaxe")) {
            pickaxe(sender, args);
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
        if (sub.equals("randomability")) {
            randomAbility(sender, args);
            return true;
        }
        if (sub.equals("removeability")) {
            removeAbility(sender, args);
            return true;
        }
        if (sub.equals("resetabilities")) {
            resetAbilities(sender, args);
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
        if (sub.equals("observer")) {
            observer(sender, args);
            return true;
        }
        if (sub.equals("reload")) {
            plugin.reloadConfig();
            boolean repairedTips = GameTips.repairLegacyConfiguredTips(plugin);
            boolean removedTajjaRewards = plugin.removeLegacyTajjaGamblingRewards();
            gameManager.reloadSettings();
            plugin.messages().send(sender, repairedTips || removedTajjaRewards
                ? "&a설정을 다시 불러왔습니다. 오래된 설정도 함께 정리했습니다."
                : "&a설정을 다시 불러왔습니다.");
            return true;
        }
        if (sub.equals("update")) {
            update(sender, args);
            return true;
        }
        if (sub.equals("gui") || sub.equals("settings")) {
            openSettings(sender, args);
            return true;
        }

        plugin.messages().send(sender, "&c알 수 없는 명령어입니다. /" + PRIMARY_COMMAND_LABEL + " help");
        return true;
    }

    public void registerShortcuts() {
        for (String name : CommandCatalog.shortcutCommands()) {
            org.bukkit.command.PluginCommand shortcut = plugin.getCommand(name);
            if (shortcut == null) throw new IllegalStateException("Missing command in plugin.yml: " + name);
            shortcut.setExecutor(this);
            shortcut.setTabCompleter(this);
        }
    }

    private void help(CommandSender sender, String label, String[] args) {
        CommandHelp.show(sender, Arrays.copyOfRange(args, Math.min(1, args.length), args.length));
    }

    private String[] groupHelpArguments(String[] args) {
        for (int i = 1; i < args.length; i++) {
            if (isHelpToken(args[i])) return Arrays.copyOfRange(args, i + 1, args.length);
        }
        return new String[0];
    }

    private boolean isHelpToken(String token) {
        return token.equalsIgnoreCase("help") || token.equalsIgnoreCase("h") || token.equals("?") || token.equals("도움말");
    }

    private void groupHelp(CommandSender sender, CommandTree.Group group, String path) {
        CommandHelp.group(sender, group, path, new String[0]);
    }

    private void status(CommandSender sender) {
        sender.sendMessage("");
        line(sender);
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + " NewGodWar 상태");
        sender.sendMessage(ChatColor.GRAY + "  상태       " + ChatColor.YELLOW + gameManager.state());
        sender.sendMessage(ChatColor.GRAY + "  진행 시간  " + ChatColor.YELLOW + runningElapsedText());
        sender.sendMessage(ChatColor.GRAY + "  버전       " + ChatColor.YELLOW + plugin.versionSupport().minecraftVersion());
        sender.sendMessage(ChatColor.GRAY + "  팀킬       " + state(plugin.getConfig().getBoolean("game.friendly-fire", false)));
        sender.sendMessage(ChatColor.GRAY + "  우르프     " + urfStatus());
        sender.sendMessage(ChatColor.GRAY + "  선택 맵    " + selectedMapStatus());
        sender.sendMessage(ChatColor.GRAY + "  재추첨     " + ChatColor.YELLOW
            + plugin.getConfig().getInt("game.ability-reroll-count", 1) + "회"
            + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + "자동 Skip "
            + ChatColor.YELLOW + plugin.getConfig().getInt("game.skip-ready-countdown-seconds", 5) + "초");
        sender.sendMessage(ChatColor.GRAY + "  종료 공개  " + state(plugin.getConfig().getBoolean("game.reveal-abilities-on-end", true)));
        sender.sendMessage(ChatColor.GRAY + "  게임룰     " + state(plugin.getConfig().getBoolean("gamerules.enabled", true)));
        sender.sendMessage(ChatColor.GRAY + "  플러그인   " + ChatColor.YELLOW + plugin.getDescription().getVersion() + updateStatusSuffix());
        sender.sendMessage(ChatColor.GRAY + "  블랙리스트 " + ChatColor.YELLOW + abilityManager.blacklistedAbilityIds().size() + ChatColor.GRAY + "개");
        sender.sendMessage(ChatColor.GRAY + "  원문       " + gameManager.statusLine());
        line(sender);
    }

    private String runningElapsedText() {
        if (!gameManager.isRunning()) {
            return "진행 중 아님";
        }
        return formatSeconds(gameManager.runningElapsedSeconds());
    }

    private void line(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "--------------------------------------------------");
    }

    private void section(CommandSender sender, String title) {
        sender.sendMessage(ChatColor.DARK_AQUA + "  [" + title + "]");
    }

    private void command(CommandSender sender, String label, String usage, String description) {
        String command = usage.startsWith("/") ? usage : "/" + label + " " + usage;
        sender.sendMessage(ChatColor.AQUA + "  " + command
            + ChatColor.DARK_GRAY + " - " + ChatColor.GRAY + description);
    }

    private String state(boolean enabled) {
        return enabled ? ChatColor.GREEN + "켜짐" : ChatColor.RED + "꺼짐";
    }

    private String urfStatus() {
        return state(abilityManager.urfEnabled()) + ChatColor.GRAY + " / 쿨타임 감소 "
            + ChatColor.YELLOW + abilityManager.urfCooldownPercent() + "%";
    }

    private String selectedMapStatus() {
        String map = selectedMapName();
        return map == null ? ChatColor.RED + "미선택" : ChatColor.AQUA + map;
    }

    private String updateStatusSuffix() {
        PluginUpdater.UpdateInfo info = plugin.updater().lastInfo();
        if (info == null || info.errorMessage() != null || !info.updateAvailable()) {
            return "";
        }
        String suffix = ChatColor.GRAY + " | 최신 " + ChatColor.GREEN + info.latestVersion();
        if (info.downloadedFile() != null) {
            suffix += ChatColor.GRAY + " | 재시작 대기";
        }
        return suffix;
    }

    private void rerolls(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.messages().send(sender, "&e/gw rerolls <횟수>");
            plugin.messages().send(sender, "&7현재 능력 재추첨 가능 횟수: &f"
                + plugin.getConfig().getInt("game.ability-reroll-count", 1) + "회");
            return;
        }
        Integer count = parseWholeNumber(args[1]);
        if (count == null || count.intValue() < 0) {
            plugin.messages().send(sender, "&c횟수는 0 이상의 숫자여야 합니다.");
            return;
        }
        plugin.getConfig().set("game.ability-reroll-count", Math.min(100, count.intValue()));
        plugin.saveConfig();
        plugin.messages().send(sender, "&a능력 재추첨 가능 횟수를 &f"
            + plugin.getConfig().getInt("game.ability-reroll-count", 1) + "회&a로 설정했습니다.");
    }

    private void skipSeconds(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.messages().send(sender, "&e/gw skipseconds <초>");
            plugin.messages().send(sender, "&7현재 능력 확정 자동/관리자 skip 초: &f"
                + plugin.getConfig().getInt("game.skip-ready-countdown-seconds", 5) + "초");
            return;
        }
        Integer seconds = parseWholeNumber(args[1]);
        if (seconds == null || seconds.intValue() < 0) {
            plugin.messages().send(sender, "&c초는 0 이상의 숫자여야 합니다.");
            return;
        }
        plugin.getConfig().set("game.skip-ready-countdown-seconds", Math.min(600, seconds.intValue()));
        plugin.saveConfig();
        plugin.messages().send(sender, "&a능력 확정 자동/관리자 skip 초를 &f"
            + plugin.getConfig().getInt("game.skip-ready-countdown-seconds", 5) + "초&a로 설정했습니다.");
    }

    private void pickaxe(CommandSender sender, String[] args) {
        if (args.length < 2 || args[1].equalsIgnoreCase("status") || args[1].equalsIgnoreCase("list")
            || args[1].equalsIgnoreCase("상태") || args[1].equalsIgnoreCase("목록")) {
            pickaxeStatus(sender);
            return;
        }
        PickaxeUnlockTarget target = pickaxeUnlockTarget(args[1]);
        if (target == null) {
            plugin.messages().send(sender, "&c곡괭이는 wooden, stone, iron, diamond, all 중 하나여야 합니다.");
            return;
        }
        if (args.length < 3) {
            plugin.messages().send(sender, "&e/gw pickaxe <wooden|stone|iron|diamond|all> <open|off|분>");
            return;
        }
        Integer seconds = parsePickaxeUnlockSeconds(args[2]);
        if (seconds == null) {
            plugin.messages().send(sender, "&c시간은 open, off, 0 이상의 분 또는 초로 입력하세요. 예: open, off, 10, 300초");
            return;
        }
        for (PickaxeUnlockTarget selected : pickaxeUnlockTargets(target)) {
            plugin.getConfig().set(selected.path, seconds.intValue());
        }
        plugin.saveConfig();
        plugin.messages().send(sender, "&a곡괭이 허용 시간을 변경했습니다: &f"
            + pickaxeTargetLabel(target) + " &7-> &f" + pickaxeUnlockText(seconds.intValue()));
        pickaxeStatus(sender);
    }

    private void pickaxeStatus(CommandSender sender) {
        plugin.messages().send(sender, "&e/gw pickaxe 상태");
        plugin.messages().send(sender, "&7진행 시간: &f" + runningElapsedText());
        for (PickaxeUnlockTarget target : pickaxeUnlockTargets(PickaxeUnlockTarget.ALL)) {
            int seconds = plugin.getConfig().getInt(target.path, -1);
            plugin.messages().send(sender, "&7" + target.label + ": &f" + pickaxeUnlockStateText(seconds));
        }
    }

    private void skipAbilitySelection(CommandSender sender, String[] args, int secondsIndex) {
        Integer seconds = null;
        if (args.length > secondsIndex) {
            seconds = parseWholeNumber(args[secondsIndex]);
            if (seconds == null || seconds.intValue() < 0) {
                plugin.messages().send(sender, "&c초는 0 이상의 숫자여야 합니다.");
                return;
            }
        }
        int countdown = seconds == null
            ? plugin.getConfig().getInt("game.skip-ready-countdown-seconds", 5)
            : Math.min(600, seconds.intValue());
        int skipped = gameManager.skipAbilitySelection(countdown);
        Bukkit.broadcastMessage(plugin.messages().prefix() + ChatColor.YELLOW
            + "능력 확정 대기를 종료했습니다. 대상: " + skipped + "명, 시작까지 " + countdown + "초");
    }

    private void join(CommandSender sender, String[] args) {
        if (!sender.hasPermission("newgodwar.admin")) {
            plugin.messages().send(sender, "&c권한이 없습니다.");
            return;
        }
        if (args.length < 3) {
            plugin.messages().send(sender, "&e/gw join <team> <player>");
            plugin.messages().send(sender, "&7가능한 팀: &f" + teamUsage());
            return;
        }
        GodTeam team = GodTeam.parse(args[1]);
        if (team == null) {
            plugin.messages().send(sender, "&c팀은 " + teamUsage() + " 중 하나여야 합니다.");
            return;
        }
        if (!gameManager.isTeamEnabled(team)) {
            plugin.messages().send(sender, "&c비활성화된 팀에는 배정할 수 없습니다.");
            return;
        }
        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            plugin.messages().send(sender, "&c대상 플레이어를 찾을 수 없습니다.");
            return;
        }
        if (gameManager.isRunning()) {
            try {
                AbilityDefinition ability = gameManager.joinMidGame(target, team);
                plugin.messages().send(sender, "&a" + target.getName() + " 님이 " + teamName(team)
                    + " 팀으로 중간 참여했습니다. 능력: &f" + ability.name());
            } catch (IllegalStateException ex) {
                plugin.messages().send(sender, "&c" + ex.getMessage());
            }
            return;
        }
        gameManager.assign(target, team);
        plugin.messages().send(sender, "&a" + target.getName() + " 님을 " + teamName(team) + " 팀으로 배정했습니다.");
    }

    private void changeTeam(CommandSender sender, String[] args) {
        if (!sender.hasPermission("newgodwar.admin")) {
            plugin.messages().send(sender, "&c권한이 없습니다.");
            return;
        }
        if (args.length < 3) {
            plugin.messages().send(sender, "&e/gw changeteam <player> <team>");
            plugin.messages().send(sender, "&7또는: &f/gw changeteam <team> <player>");
            plugin.messages().send(sender, "&7가능한 팀: &f" + teamUsage());
            return;
        }
        GodTeam firstTeam = GodTeam.parse(args[1]);
        GodTeam team = firstTeam == null ? GodTeam.parse(args[2]) : firstTeam;
        Player target = Bukkit.getPlayer(firstTeam == null ? args[1] : args[2]);
        if (team == null) {
            plugin.messages().send(sender, "&c팀은 " + teamUsage() + " 중 하나여야 합니다.");
            return;
        }
        if (target == null) {
            plugin.messages().send(sender, "&c대상 플레이어를 찾을 수 없습니다.");
            return;
        }
        try {
            gameManager.changeTeam(target, team);
            plugin.messages().send(sender, "&a" + target.getName() + " 님의 팀을 "
                + teamName(team) + " 팀으로 변경했습니다.");
        } catch (IllegalStateException ex) {
            plugin.messages().send(sender, "&c" + ex.getMessage());
        }
    }

    private void midJoin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("newgodwar.admin")) {
            plugin.messages().send(sender, "&c권한이 없습니다.");
            return;
        }
        if (args.length < 2) {
            plugin.messages().send(sender, "&e/gw midjoin <player> [team|auto]");
            return;
        }
        Player player = Bukkit.getPlayer(args[1]);
        GodTeam team = null;
        if (player == null) {
            plugin.messages().send(sender, "&c대상 플레이어를 찾을 수 없습니다.");
            return;
        }
        if (args.length >= 3) {
            if (!isTeamOrAuto(args[2])) {
                plugin.messages().send(sender, "&c팀은 " + teamUsage() + ", auto 중 하나여야 합니다.");
                return;
            }
            team = GodTeam.parse(args[2]);
            if (team != null && !gameManager.isTeamEnabled(team)) {
                plugin.messages().send(sender, "&c비활성화된 팀에는 중간 참여할 수 없습니다.");
                return;
            }
        }
        try {
            AbilityDefinition ability = gameManager.joinMidGame(player, team);
            plugin.messages().send(sender, "&a" + player.getName() + " 님이 "
                + (team == null ? "&f자동 팀" : teamName(team) + " 팀")
                + "으로 중간 참여했습니다. 능력: &f" + ability.name());
        } catch (IllegalStateException ex) {
            plugin.messages().send(sender, "&c" + ex.getMessage());
        }
    }

    private void leave(CommandSender sender, String[] args) {
        if (!sender.hasPermission("newgodwar.admin")) {
            plugin.messages().send(sender, "&c권한이 없습니다.");
            return;
        }
        if (args.length < 2) {
            plugin.messages().send(sender, "&e/gw leave <player>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
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
            plugin.messages().send(sender, "&e/gw settemple <team>");
            plugin.messages().send(sender, "&7가능한 팀: &f" + teamUsage());
            return;
        }
        GodTeam team = GodTeam.parse(args[1]);
        if (team == null) {
            plugin.messages().send(sender, "&c팀은 " + teamUsage() + " 중 하나여야 합니다.");
            return;
        }
        Block block = BukkitCompat.getTargetBlock(player, 8);
        if (block == null || block.getType() != Material.DIAMOND_BLOCK) {
            plugin.messages().send(sender, "&c바라보는 블록이 다이아몬드 블록이어야 합니다.");
            return;
        }
        if (gameManager.setTemple(team, block)) {
            plugin.messages().send(sender, "&a" + teamName(team) + " 팀의 다이아 심장을 등록했습니다.");
        } else {
            plugin.messages().send(sender, "&c이미 다른 팀의 심장으로 등록된 블록입니다. 다른 다이아몬드 블록을 선택하세요.");
        }
    }

    private void setSpawn(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            plugin.messages().send(sender, "&c플레이어만 사용할 수 있습니다.");
            return;
        }
        if (args.length < 2) {
            plugin.messages().send(sender, "&e/gw setspawn <team>");
            plugin.messages().send(sender, "&7가능한 팀: &f" + teamUsage());
            return;
        }
        GodTeam team = GodTeam.parse(args[1]);
        if (team == null) {
            plugin.messages().send(sender, "&c팀은 " + teamUsage() + " 중 하나여야 합니다.");
            return;
        }
        gameManager.setSpawn(team, player.getLocation());
        plugin.messages().send(sender, "&a현재 위치를 " + teamName(team) + " 팀 스폰으로 등록했습니다.");
    }

    private void setLobby(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) {
            plugin.messages().send(sender, "&c플레이어만 사용할 수 있습니다.");
            return;
        }
        if (gameManager.setLobby(player.getLocation())) {
            plugin.messages().send(sender, "&a현재 위치를 접속/게임 종료 로비로 등록했습니다.");
        }
    }

    private void urf(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.messages().send(sender, "&e/gw urf <on|off|toggle|20%>");
            plugin.messages().send(sender, "&7현재 우르프: " + urfStatus());
            return;
        }

        String value = args[1];
        if (value.equalsIgnoreCase("on") || value.equalsIgnoreCase("enable") || value.equalsIgnoreCase("true")) {
            plugin.getConfig().set("game.urf.enabled", true);
            plugin.saveConfig();
        } else if (value.equalsIgnoreCase("off") || value.equalsIgnoreCase("disable") || value.equalsIgnoreCase("false")) {
            plugin.getConfig().set("game.urf.enabled", false);
            plugin.saveConfig();
        } else if (value.equalsIgnoreCase("toggle")) {
            plugin.getConfig().set("game.urf.enabled", !abilityManager.urfEnabled());
            plugin.saveConfig();
        } else {
            String percentText = value;
            if ((value.equalsIgnoreCase("percent") || value.equalsIgnoreCase("rate") || value.equalsIgnoreCase("배율"))
                && args.length >= 3) {
                percentText = args[2];
            }
            Integer percent = parsePercent(percentText);
            if (percent == null) {
                plugin.messages().send(sender, "&e/gw urf <on|off|toggle|20%>");
                return;
            }
            abilityManager.setUrfCooldownPercent(percent.intValue());
        }

        gameManager.refreshAllPlayerDisplays();
        plugin.messages().send(sender, "&a우르프 설정: " + urfStatus());
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
            ability = abilityByToken(args[1]);
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
        Player target = args.length >= 2 ? Bukkit.getPlayer(args[1]) : asPlayer(sender);
        if (target == null) {
            plugin.messages().send(sender, "&c대상 플레이어를 찾을 수 없습니다.");
            return;
        }
        if (!canViewAbility(sender, target)) {
            plugin.messages().send(sender, "&c상대 팀 플레이어의 능력은 볼 수 없습니다.");
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
            + ChatColor.GRAY + " | 등급 " + ChatColor.YELLOW + ability.gradeText()
            + ChatColor.GRAY + " - " + ability.description());
    }

    private boolean canViewAbility(CommandSender sender, Player target) {
        if (sender.hasPermission("newgodwar.admin")) {
            return true;
        }
        Player viewer = asPlayer(sender);
        if (viewer == null) {
            return false;
        }
        if (viewer.equals(target)) {
            return true;
        }
        GodTeam viewerTeam = gameManager.teamOf(viewer);
        return viewerTeam != null && viewerTeam.equals(gameManager.teamOf(target));
    }

    private void abilityShortcut(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) {
            plugin.messages().send(sender, "&c플레이어만 사용할 수 있습니다.");
            return;
        }
        abilityGui.openCurrent(player, player);
    }

    private void teamInfo(CommandSender sender, String[] args) {
        GodTeam team = null;
        if (args.length >= 2) {
            team = GodTeam.parse(args[1]);
            if (team == null) {
                plugin.messages().send(sender, "&c팀은 " + teamUsage() + " 중 하나여야 합니다.");
                return;
            }
        } else {
            Player player = asPlayer(sender);
            if (player != null) {
                team = gameManager.teamOf(player);
            }
        }
        if (team == null) {
            plugin.messages().send(sender, "&e소속 팀이 없습니다.");
            return;
        }
        sender.sendMessage(ChatColor.GREEN + "======  " + teamName(team) + ChatColor.GREEN + "  ======");
        int count = 0;
        for (Player player : BukkitCompat.onlinePlayers()) {
            if (team.equals(gameManager.teamOf(player))) {
                count++;
                sender.sendMessage(ChatColor.YELLOW + String.valueOf(count) + ". " + ChatColor.GOLD + player.getName());
            }
        }
        if (count == 0) {
            plugin.messages().send(sender, "&e해당 팀에 온라인 팀원이 없습니다.");
        }
    }

    private void confirmAbility(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) {
            plugin.messages().send(sender, "&c플레이어만 사용할 수 있습니다.");
            return;
        }
        if (gameManager.confirmAbility(player)) {
            AbilityDefinition ability = abilityManager.get(player);
            plugin.messages().send(sender, "&a능력을 확정했습니다: &f" + (ability == null ? "없음" : ability.name()));
            Bukkit.broadcastMessage(ChatColor.DARK_AQUA + player.getName() + ChatColor.WHITE + " 님께서 능력을 확정하셨습니다.");
            gameManager.completeAbilitySelectionIfReady();
            return;
        }
        plugin.messages().send(sender, "&e현재 확정할 능력이 없습니다.");
    }

    private void rerollAbility(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) {
            plugin.messages().send(sender, "&c플레이어만 사용할 수 있습니다.");
            return;
        }
        AbilityDefinition ability = gameManager.rerollAbility(player);
        if (ability == null) {
            plugin.messages().send(sender, "&e현재 다시 뽑을 능력이 없습니다.");
            return;
        }
        int remaining = gameManager.remainingAbilityRerolls(player);
        plugin.messages().send(sender, "&a능력을 새로 뽑았습니다: &f" + ability.name()
            + ChatColor.GRAY + " | 남은 재추첨: " + ChatColor.YELLOW + remaining + "회");
        if (remaining <= 0) {
            Bukkit.broadcastMessage(ChatColor.DARK_AQUA + player.getName() + ChatColor.WHITE + " 님께서 능력을 확정하셨습니다.");
            gameManager.completeAbilitySelectionIfReady();
        } else {
            Bukkit.broadcastMessage(ChatColor.DARK_AQUA + player.getName() + ChatColor.WHITE
                + " 님께서 능력을 다시 뽑았습니다. 남은 재추첨: " + ChatColor.YELLOW + remaining + "회");
        }
    }

    private void clearCooldowns(CommandSender sender, String[] args) {
        if (args.length > 2) {
            plugin.messages().send(sender, "&e/gw ability cooldown reset [player|self|all]");
            return;
        }
        String targetName = args.length == 2 ? args[1] : "self";
        if (targetName.equalsIgnoreCase("all") || targetName.equals("전체") || targetName.equals("*")) {
            abilityManager.clearAllCooldowns();
            gameManager.refreshAllPlayerDisplays();
            plugin.messages().send(sender, "&a모든 능력 쿨타임을 초기화했습니다.");
            return;
        }
        boolean self = targetName.equalsIgnoreCase("self") || targetName.equals("본인");
        Player target = self ? asPlayer(sender) : Bukkit.getPlayerExact(targetName);
        if (target == null) {
            plugin.messages().send(sender, self
                ? "&e콘솔에서는 대상을 지정하세요: /gw ability cooldown reset <player|all>"
                : "&c대상 플레이어를 찾을 수 없습니다: " + targetName);
            return;
        }
        abilityManager.clearCooldowns(target);
        gameManager.refreshPlayerDisplay(target);
        plugin.messages().send(sender, "&a" + target.getName() + " 님의 능력 쿨타임을 초기화했습니다.");
    }

    private void listAssignedAbilities(CommandSender sender, String query) {
        List<AssignedAbilityView> assigned = assignedAbilityViews(query);
        if (assigned.isEmpty()) {
            plugin.messages().send(sender, hasQuery(query) ? "&e검색 결과에 맞는 배정 능력이 없습니다." : "&e능력이 있는 플레이어가 없습니다.");
            return;
        }

        sender.sendMessage(plugin.messages().prefix() + ChatColor.YELLOW + "배정된 능력"
            + (hasQuery(query) ? ChatColor.GRAY + " | 검색: " + ChatColor.WHITE + query : ""));
        for (AssignedAbilityView view : assigned) {
            sender.sendMessage(ChatColor.WHITE + view.playerName + ChatColor.GRAY + " : "
                + ChatColor.YELLOW + view.ability.name() + ChatColor.DARK_GRAY + " (" + view.ability.id() + ")"
                + ChatColor.GRAY + " | 등급 " + ChatColor.YELLOW + view.ability.grade().symbol()
                + (view.online ? "" : ChatColor.DARK_GRAY + " [오프라인]"));
        }
    }

    private void listParticipants(CommandSender sender, String query) {
        List<ParticipantView> participants = participantViews(query);
        if (participants.isEmpty()) {
            plugin.messages().send(sender, hasQuery(query) ? "&e검색 결과에 맞는 참가자가 없습니다." : "&e참가자가 없습니다.");
            return;
        }

        sender.sendMessage(plugin.messages().prefix() + ChatColor.YELLOW + "참가자 / 능력 현황"
            + (hasQuery(query) ? ChatColor.GRAY + " | 검색: " + ChatColor.WHITE + query : ""));
        int total = 0;
        for (GodTeam team : GodTeam.values()) {
            int count = 0;
            for (ParticipantView view : participants) {
                if (team.equals(view.team)) {
                    if (count == 0) {
                        sender.sendMessage(teamName(team) + ChatColor.GRAY + " 팀");
                    }
                    count++;
                    total++;
                    sendParticipantLine(sender, count, view);
                }
            }
        }

        int noTeamCount = 0;
        for (ParticipantView view : participants) {
            if (view.team == null) {
                if (noTeamCount == 0) {
                    sender.sendMessage(ChatColor.GRAY + "미배정");
                }
                noTeamCount++;
                total++;
                sendParticipantLine(sender, noTeamCount, view);
            }
        }
        sender.sendMessage(ChatColor.DARK_GRAY + "총 " + total + "명");
    }

    private void sendParticipantLine(CommandSender sender, int count, ParticipantView view) {
        sender.sendMessage(ChatColor.YELLOW + "  " + count + ". " + ChatColor.WHITE + view.playerName
            + ChatColor.GRAY + " : " + abilityText(view.ability)
            + ChatColor.DARK_GRAY + " | " + (view.online ? ChatColor.GREEN + "온라인" : ChatColor.DARK_GRAY + "오프라인")
            + (view.observer ? ChatColor.DARK_GRAY + " | 옵저버" : "")
            + (view.kills > 0 ? ChatColor.DARK_GRAY + " | " + ChatColor.GOLD + view.kills + "킬" : ""));
    }

    private String abilityText(AbilityDefinition ability) {
        if (ability == null) {
            return ChatColor.GRAY + "미배정";
        }
        return ChatColor.YELLOW + ability.name() + ChatColor.DARK_GRAY + " (" + ability.id() + ")"
            + ChatColor.GRAY + " [" + ability.grade().symbol() + "]";
    }

    private String teamName(GodTeam team) {
        return gameManager.teamColoredName(team);
    }

    private void listAbilities(CommandSender sender, String query) {
        Player viewer = asPlayer(sender);
        if (viewer != null) {
            abilityGui.openList(viewer, query);
            return;
        }
        List<AbilityDefinition> abilities = filteredAbilities(query);
        if (abilities.isEmpty()) {
            plugin.messages().send(sender, "&e검색 결과에 맞는 능력이 없습니다.");
            return;
        }
        sender.sendMessage(plugin.messages().prefix() + ChatColor.YELLOW + "등록된 신의 능력"
            + (hasQuery(query) ? ChatColor.GRAY + " | 검색: " + ChatColor.WHITE + query : ""));
        for (AbilityDefinition ability : abilities) {
            boolean enabled = abilityManager.isEnabled(ability);
            ChatColor stateColor = enabled ? ChatColor.GREEN : ChatColor.RED;
            String state = enabled ? "활성" : "비활성";
            sender.sendMessage(ChatColor.GOLD + ability.id() + ChatColor.GRAY + " | "
                + ChatColor.WHITE + ability.name() + ChatColor.GRAY + " | "
                + ChatColor.YELLOW + ability.gradeText() + ChatColor.GRAY + " | "
                + stateColor + state + ChatColor.GRAY + " - " + ability.description());
        }
    }

    private void setAbility(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.messages().send(sender, "&e/gw setability <player> <ability>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        AbilityDefinition ability = abilityByToken(args[2]);
        if (target == null || ability == null) {
            plugin.messages().send(sender, "&c플레이어 또는 능력을 찾을 수 없습니다.");
            return;
        }
        abilityManager.set(target, ability);
        plugin.nms().sendTitle(target, ability.name(), ability.description(), 10, 60, 10);
        plugin.messages().send(sender, "&a능력을 지정했습니다.");
    }

    private void randomAbility(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                plugin.messages().send(sender, "&c대상 플레이어를 찾을 수 없습니다.");
                return;
            }
            AbilityDefinition ability = abilityManager.assignRandom(target);
            if (ability == null) {
                plugin.messages().send(sender, "&c배정할 수 있는 능력이 없습니다.");
                return;
            }
            plugin.messages().send(sender, "&a" + target.getName() + " 님에게 랜덤 능력 &f" + ability.name() + "&a을 배정했습니다.");
            return;
        }

        int count = 0;
        for (Player player : BukkitCompat.onlinePlayers()) {
            if (gameManager.teamOf(player) != null && !gameManager.isObserver(player)) {
                AbilityDefinition ability = abilityManager.assignRandom(player);
                if (ability != null) {
                    count++;
                }
            }
        }
        gameManager.refreshAllPlayerDisplays();
        Bukkit.broadcastMessage(plugin.messages().prefix() + ChatColor.AQUA
            + "관리자가 참가자 " + count + "명의 능력을 랜덤으로 배정했습니다.");
    }

    private void removeAbility(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.messages().send(sender, "&e/gw a remove <player>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !abilityManager.remove(target)) {
            plugin.messages().send(sender, "&c플레이어 또는 능력을 찾을 수 없습니다.");
            return;
        }
        plugin.messages().send(sender, "&a" + target.getName() + " 님의 능력을 삭제했습니다.");
    }

    private void resetAbilities(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null || !abilityManager.remove(target)) {
                plugin.messages().send(sender, "&c플레이어 또는 능력을 찾을 수 없습니다.");
                return;
            }
            plugin.messages().send(sender, "&a" + target.getName() + " 님의 능력을 초기화했습니다.");
            return;
        }
        abilityManager.clear();
        gameManager.refreshAllPlayerDisplays();
        Bukkit.broadcastMessage(plugin.messages().prefix() + ChatColor.AQUA + "관리자가 모든 능력을 초기화했습니다.");
    }

    private void abilityGroup(CommandSender sender, String[] args, String label, boolean themachyRoot) {
        String usagePrefix = "/" + PRIMARY_COMMAND_LABEL + " " + args[0].toLowerCase(Locale.ROOT);
        if (args.length < 2) {
            ability(sender, args);
            return;
        }
        if (args[1].equalsIgnoreCase("help")) {
            sendAbilityGroupHelp(sender, usagePrefix, themachyRoot, sender.hasPermission("newgodwar.admin"));
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        if (requiresAbilityGroupAdmin(action, themachyRoot) && !sender.hasPermission("newgodwar.admin")) {
            plugin.messages().send(sender, "&c권한이 없습니다.");
            return;
        }
        if (isAbilityListAction(action)) {
            listAssignedAbilities(sender, joinArguments(args, 2));
            return;
        }
        if (isAbilityCatalogAction(action)) {
            listAbilities(sender, joinArguments(args, 2));
            return;
        }
        if (action.equals("random")) {
            randomAbility(sender, prependSubcommand("randomability", args, 2));
            return;
        }
        if (action.equals("reset")) {
            resetAbilities(sender, prependSubcommand("resetabilities", args, 2));
            return;
        }
        if (action.equals("skip")) {
            skipAbilitySelection(sender, args, 2);
            return;
        }
        if (action.equals("cutin")) {
            midJoin(sender, prependSubcommand("midjoin", args, 2));
            return;
        }
        if (action.equals("remove")) {
            removeAbility(sender, prependSubcommand("removeability", args, 2));
            return;
        }
        if (action.equals("set") || action.equals("assign")) {
            if (args.length < 4) {
                plugin.messages().send(sender, "&e" + usagePrefix + " set <player> <ability>");
                return;
            }
            setAbility(sender, new String[] {"setability", args[2], args[3]});
            return;
        }
        if (args.length >= 3 && abilityByToken(args[1]) != null) {
            if (!sender.hasPermission("newgodwar.admin")) {
                plugin.messages().send(sender, "&c권한이 없습니다.");
                return;
            }
            setAbility(sender, new String[] {"setability", args[2], args[1]});
            return;
        }
        if (!themachyRoot) {
            ability(sender, args);
            return;
        }
        if (args.length < 3) {
            ability(sender, args);
            return;
        }
        if (!sender.hasPermission("newgodwar.admin")) {
            plugin.messages().send(sender, "&c권한이 없습니다.");
            return;
        }
        setAbility(sender, new String[] {"setability", args[2], args[1]});
    }

    private void sendAbilityGroupHelp(CommandSender sender, String usagePrefix, boolean themachyRoot, boolean admin) {
        CommandTree.Result route = CommandTree.resolve(new String[] {"ability", "help"});
        groupHelp(sender, route.help, route.path);
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
            plugin.messages().send(sender, "&e/gw blacklist <list|add|remove|toggle> [ability]");
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
        plugin.messages().send(sender, "&e/gw blacklist <list|add|remove|toggle> [ability]");
    }

    private void gamerule(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.messages().send(sender, "&e/gw gamerule <apply|restore>");
            return;
        }
        if (!plugin.versionSupport().paperServer()) {
            plugin.messages().send(sender, "&c현재 서버는 Paper가 아닙니다. 오류 방지를 위해 게임룰 작업을 건너뛰었습니다. Paper 서버로 실행해 주세요.");
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
        plugin.messages().send(sender, "&e/gw gamerule <apply|restore>");
    }

    private void world(CommandSender sender, String label, String[] args) {
        if (args.length < 2 || args[1].equalsIgnoreCase("list") || args[1].equalsIgnoreCase("목록")) {
            listWorlds(sender);
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        if (action.equals("help") || action.equals("도움말") || action.equals("?")) {
            CommandHelp.show(sender, prependSubcommand("world", args, 2));
            return;
        }
        if (action.equals("gui") || action.equals("settings") || action.equals("설정")) {
            openWorldSettings(sender);
            return;
        }
        if (action.equals("create") || action.equals("new") || action.equals("생성")) {
            createWorld(sender, args);
            return;
        }
        if (action.equals("game") || action.equals("gameworld") || action.equals("게임")) {
            gameWorld(sender, args);
            return;
        }
        if (action.equals("load") || action.equals("로드")) {
            loadWorld(sender, args);
            return;
        }
        if (action.equals("copy") || action.equals("clone") || action.equals("복사")) {
            copyWorld(sender, args);
            return;
        }
        if (action.equals("unload") || action.equals("언로드")) {
            unloadWorld(sender, args);
            return;
        }
        if (action.equals("delete") || action.equals("remove") || action.equals("삭제")) {
            deleteWorld(sender, args);
            return;
        }
        if (action.equals("backup") || action.equals("backups") || action.equals("백업")) {
            worldBackup(sender, args);
            return;
        }
        if (action.equals("tp") || action.equals("teleport") || action.equals("move") || action.equals("이동")) {
            if (args.length < 3) {
                plugin.messages().send(sender, "&e/gw world tp <world> [player]");
                return;
            }
            Player target = args.length >= 4 ? Bukkit.getPlayer(args[3]) : asPlayer(sender);
            if (target == null) {
                plugin.messages().send(sender, "&c이동할 플레이어를 찾을 수 없습니다.");
                return;
            }
            World world = worldByName(args[2]);
            if (world == null) {
                plugin.messages().send(sender, "&c월드를 찾을 수 없습니다: " + args[2]);
                return;
            }
            target.teleport(world.getSpawnLocation());
            plugin.messages().send(sender, "&a" + target.getName() + " 님을 &f" + world.getName() + "&a 월드로 이동했습니다.");
            return;
        }
        if (action.equals("lobby") || action.equals("로비")) {
            Player target = args.length >= 3 ? Bukkit.getPlayer(args[2]) : asPlayer(sender);
            if (target == null) {
                plugin.messages().send(sender, "&c이동할 플레이어를 찾을 수 없습니다.");
                return;
            }
            if (!gameManager.teleportToLobby(target)) {
                plugin.messages().send(sender, "&c로비 위치가 설정되지 않았습니다. /gw setlobby 를 먼저 실행하세요.");
                return;
            }
            plugin.messages().send(sender, "&a" + target.getName() + " 님을 로비로 이동했습니다.");
            return;
        }
        World directWorld = worldByName(args[1]);
        if (directWorld != null) {
            Player target = args.length >= 3 ? Bukkit.getPlayer(args[2]) : asPlayer(sender);
            if (target == null) {
                plugin.messages().send(sender, "&c이동할 플레이어를 찾을 수 없습니다.");
                return;
            }
            target.teleport(directWorld.getSpawnLocation());
            plugin.messages().send(sender, "&a" + target.getName() + " 님을 &f" + directWorld.getName() + "&a 월드로 이동했습니다.");
            return;
        }
        plugin.messages().send(sender, "&e/gw world <list|game|create|load|copy|tp|lobby|unload|delete|backup>");
    }

    private void map(CommandSender sender, String label, String[] args) {
        if (args.length < 2 || args[1].equalsIgnoreCase("list") || args[1].equalsIgnoreCase("목록")) {
            listMaps(sender, label);
            return;
        }
        if (args[1].equalsIgnoreCase("help") || args[1].equalsIgnoreCase("도움말") || args[1].equals("?")) {
            CommandHelp.show(sender, prependSubcommand("map", args, 2));
            return;
        }
        if (args[1].equalsIgnoreCase("clear") || args[1].equalsIgnoreCase("none") || args[1].equalsIgnoreCase("해제")) {
            clearSelectedMap(sender);
            return;
        }
        if ((args[1].equalsIgnoreCase("select") || args[1].equalsIgnoreCase("선택")) && args.length >= 3) {
            selectMap(sender, args[2], true);
            return;
        }
        selectMap(sender, args[1], true);
    }



    private void listMaps(CommandSender sender, String label) {
        String selected = selectedMapName();
        sender.sendMessage(plugin.messages().prefix() + ChatColor.YELLOW + "게임 맵 목록");
        sender.sendMessage(ChatColor.GRAY + "현재 선택: " + selectedMapStatus());
        Set<String> listed = new HashSet<String>();
        for (World world : Bukkit.getWorlds()) {
            listed.add(world.getName().toLowerCase(Locale.ROOT));
            sender.sendMessage(mapListLine(world.getName(), true, selected));
        }
        for (String worldName : unloadedWorldNameSuggestions()) {
            if (!listed.contains(worldName.toLowerCase(Locale.ROOT))) {
                sender.sendMessage(mapListLine(worldName, false, selected));
            }
        }
        sender.sendMessage(ChatColor.GRAY + "선택: " + ChatColor.AQUA + "/" + label + " map <world>"
            + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + "해제: " + ChatColor.AQUA + "/" + label + " map clear");
    }

    private String mapListLine(String worldName, boolean loaded, String selected) {
        boolean active = selected != null && selected.equalsIgnoreCase(worldName);
        return ChatColor.GRAY + "- " + (active ? ChatColor.GREEN : ChatColor.WHITE) + worldName
            + ChatColor.DARK_GRAY + " | " + (loaded ? ChatColor.GRAY + "로드됨" : ChatColor.YELLOW + "폴더만 있음")
            + (active ? ChatColor.DARK_GRAY + " | " + ChatColor.GREEN + "선택됨" : "");
    }



    private void openWorldSettings(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) {
            plugin.messages().send(sender, "&c플레이어만 GUI를 열 수 있습니다.");
            return;
        }
        settingsGui.openWorld(player);
    }

    private void gameWorld(CommandSender sender, String[] args) {
        if (args.length < 3) {
            String configured = plugin.getConfig().getString("world.game-world", "");
            if (configured == null || configured.trim().length() == 0) {
                plugin.messages().send(sender, "&e게임 월드가 지정되어 있지 않습니다. 자동 월드 초기화는 작동하지 않습니다.");
            } else {
                plugin.messages().send(sender, "&a현재 게임 월드: &f" + configured);
            }
            plugin.messages().send(sender, "&e/gw world game <world|clear>");
            return;
        }
        if (args[2].equalsIgnoreCase("clear") || args[2].equalsIgnoreCase("none") || args[2].equalsIgnoreCase("해제")) {
            clearSelectedMap(sender);
            return;
        }
        selectMap(sender, args[2], false);
    }

    private void selectMap(CommandSender sender, String requestedWorldName, boolean loadIfNeeded) {
        World world = worldByName(requestedWorldName);
        if (world == null) {
            if (!loadIfNeeded) {
                plugin.messages().send(sender, "&c로드된 월드를 찾을 수 없습니다: " + requestedWorldName);
                return;
            }
            world = loadMapWorld(sender, requestedWorldName);
            if (world == null) {
                return;
            }
        }
        org.bukkit.Location lobby = gameManager.lobbyLocation();
        if (lobby != null && lobby.getWorld() != null && lobby.getWorld().equals(world)) {
            plugin.messages().send(sender, "&c로비 월드는 게임 맵으로 선택할 수 없습니다.");
            return;
        }
        plugin.getConfig().set("world.game-world", world.getName());
        plugin.saveConfig();
        gameManager.reloadSettings();
        plugin.messages().send(sender, "&a게임 맵을 선택했습니다: &f" + world.getName()
            + "&7 | 시작 시 백업, 종료 시 자동 초기화됩니다.");
        plugin.messages().send(sender, "&7맵별 스폰/심장 설정: &e/gw setspawn <team>&7, &e/gw settemple <team>");
    }

    private World loadMapWorld(CommandSender sender, String requestedWorldName) {
        String name = sanitizeWorldName(requestedWorldName);
        if (name == null) {
            plugin.messages().send(sender, "&c월드 이름은 영문, 숫자, 점, 밑줄, 하이픈만 사용할 수 있습니다.");
            return null;
        }
        if (!worldBackupManager.hasWorldFolder(name)) {
            plugin.messages().send(sender, "&c로드된 월드나 월드 폴더를 찾을 수 없습니다: " + name);
            return null;
        }
        World loaded = Bukkit.createWorld(WorldBackupManager.creator(name, managedWorldType(name)));
        if (loaded == null) {
            plugin.messages().send(sender, "&c맵 월드를 로드하지 못했습니다: " + name);
            return null;
        }
        saveManagedWorld(loaded.getName(), managedWorldType(loaded.getName()));
        plugin.messages().send(sender, "&a맵 월드를 로드했습니다: &f" + loaded.getName());
        return loaded;
    }

    private void clearSelectedMap(CommandSender sender) {
        plugin.getConfig().set("world.game-world", "");
        plugin.saveConfig();
        gameManager.reloadSettings();
        plugin.messages().send(sender, "&a게임 맵 선택을 해제했습니다. 자동 월드 초기화는 작동하지 않습니다.");
    }

    private String selectedMapName() {
        String configured = plugin.getConfig().getString("world.game-world", "");
        return configured == null || configured.trim().length() == 0 ? null : configured.trim();
    }

    private void createWorld(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.messages().send(sender, "&e/gw world create <world> [normal|flat|void]");
            return;
        }
        String name = sanitizeWorldName(args[2]);
        if (name == null) {
            plugin.messages().send(sender, "&c월드 이름은 영문, 숫자, 점, 밑줄, 하이픈만 사용할 수 있습니다.");
            return;
        }
        if (worldByName(name) != null || new File(Bukkit.getWorldContainer(), name).exists()) {
            plugin.messages().send(sender, "&c이미 같은 이름의 월드가 있습니다: " + name);
            return;
        }
        WorldCreator creator = new WorldCreator(name);
        creator.environment(World.Environment.NORMAL);
        if (args.length >= 4 && (args[3].equalsIgnoreCase("flat") || args[3].equalsIgnoreCase("평지"))) {
            creator.type(WorldType.FLAT);
        } else if (args.length >= 4 && (args[3].equalsIgnoreCase("void") || args[3].equalsIgnoreCase("empty") || args[3].equalsIgnoreCase("공허"))) {
            creator.generator(new VoidWorldGenerator());
            creator.generateStructures(false);
        }
        World created = Bukkit.createWorld(creator);
        if (created == null) {
            plugin.messages().send(sender, "&c월드를 생성하지 못했습니다: " + name);
            return;
        }
        created.save();
        saveManagedWorld(created.getName(), worldTypeToken(args, 3));
        plugin.messages().send(sender, "&a월드를 생성하고 로드했습니다: &f" + created.getName());
    }

    private void loadWorld(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.messages().send(sender, "&e/gw world load <world> [normal|flat|void]");
            return;
        }
        String name = sanitizeWorldName(args[2]);
        if (name == null) {
            plugin.messages().send(sender, "&c월드 이름은 영문, 숫자, 점, 밑줄, 하이픈만 사용할 수 있습니다.");
            return;
        }
        if (worldByName(name) != null) {
            plugin.messages().send(sender, "&e이미 로드된 월드입니다: &f" + name);
            return;
        }
        if (!worldBackupManager.hasWorldFolder(name)) {
            plugin.messages().send(sender, "&c월드 폴더를 찾을 수 없습니다: " + name);
            return;
        }
        String type = args.length >= 4 ? worldTypeToken(args, 3) : managedWorldType(name);
        World loaded = Bukkit.createWorld(WorldBackupManager.creator(name, type));
        if (loaded == null) {
            plugin.messages().send(sender, "&c월드를 로드하지 못했습니다: " + name);
            return;
        }
        saveManagedWorld(loaded.getName(), type);
        plugin.messages().send(sender, "&a월드를 로드했습니다: &f" + loaded.getName());
    }

    private void copyWorld(CommandSender sender, String[] args) {
        if (args.length < 4) {
            plugin.messages().send(sender, "&e/gw world copy <sourceWorld> <newWorld> [normal|flat|void]");
            return;
        }
        String sourceName = sanitizeWorldName(args[2]);
        String targetName = sanitizeWorldName(args[3]);
        if (sourceName == null || targetName == null) {
            plugin.messages().send(sender, "&c월드 이름은 영문, 숫자, 점, 밑줄, 하이픈만 사용할 수 있습니다.");
            return;
        }
        if (worldByName(targetName) != null || worldBackupManager.hasWorldFolder(targetName)) {
            plugin.messages().send(sender, "&c이미 같은 이름의 월드가 있습니다: " + targetName);
            return;
        }
        try {
            String type = args.length >= 5 ? worldTypeToken(args, 4) : inferWorldType(sourceName);
            World copied = worldBackupManager.copyWorld(sourceName, targetName, type);
            saveManagedWorld(copied.getName(), type);
            plugin.messages().send(sender, "&a월드를 복사하고 로드했습니다: &f" + sourceName + " &7-> &f" + copied.getName());
        } catch (IOException ex) {
            plugin.messages().send(sender, "&c월드 복사 실패: " + ex.getMessage());
        }
    }

    private void unloadWorld(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.messages().send(sender, "&e/gw world unload <world> [save]");
            return;
        }
        World world = worldByName(args[2]);
        if (world == null) {
            plugin.messages().send(sender, "&c로드된 월드를 찾을 수 없습니다: " + args[2]);
            return;
        }
        if (isProtectedWorld(world)) {
            plugin.messages().send(sender, "&c기본 월드나 로비 월드는 언로드할 수 없습니다.");
            return;
        }
        if (!world.getPlayers().isEmpty()) {
            plugin.messages().send(sender, "&c플레이어가 있는 월드는 언로드할 수 없습니다. 먼저 다른 월드로 이동시키세요.");
            return;
        }
        boolean save = args.length < 4 || !args[3].equalsIgnoreCase("false");
        if (!Bukkit.unloadWorld(world, save)) {
            plugin.messages().send(sender, "&c월드를 언로드하지 못했습니다: " + world.getName());
            return;
        }
        plugin.messages().send(sender, "&a월드를 언로드했습니다: &f" + world.getName());
    }

    private void deleteWorld(CommandSender sender, String[] args) {
        if (args.length < 4 || !args[3].equalsIgnoreCase("confirm")) {
            plugin.messages().send(sender, "&e/gw world delete <world> confirm");
            plugin.messages().send(sender, "&c주의: 월드 폴더를 영구 삭제합니다. 먼저 백업을 권장합니다.");
            return;
        }
        String name = sanitizeWorldName(args[2]);
        if (name == null) {
            plugin.messages().send(sender, "&c월드 이름은 영문, 숫자, 점, 밑줄, 하이픈만 사용할 수 있습니다.");
            return;
        }
        World loaded = worldByName(name);
        File folder = loaded == null ? null : loaded.getWorldFolder();
        if (loaded != null) {
            if (isProtectedWorld(loaded)) {
                plugin.messages().send(sender, "&c기본 월드나 로비 월드는 삭제할 수 없습니다.");
                return;
            }
            if (isSharedWorldFolder(loaded, folder)) {
                plugin.messages().send(sender, "&c다른 로드된 월드와 같은 폴더를 쓰는 월드는 삭제할 수 없습니다: " + folder.getPath());
                return;
            }
            if (!loaded.getPlayers().isEmpty()) {
                plugin.messages().send(sender, "&c플레이어가 있는 월드는 삭제할 수 없습니다. 먼저 다른 월드로 이동시키세요.");
                return;
            }
            if (!Bukkit.unloadWorld(loaded, false)) {
                plugin.messages().send(sender, "&c삭제 전 월드를 언로드하지 못했습니다: " + loaded.getName());
                return;
            }
        }
        List<File> folders = worldBackupManager.existingWorldFolders(name);
        if (folders.isEmpty()) {
            removeManagedWorld(name);
            plugin.getConfig().set("world.game-world", gameWorldNameMatches(name) ? "" : plugin.getConfig().getString("world.game-world", ""));
            plugin.saveConfig();
            plugin.messages().send(sender, "&e월드는 언로드했지만 삭제할 폴더를 찾지 못했습니다: &f" + name);
            return;
        }
        for (File worldFolder : folders) {
            if (!isInsideWorldContainer(worldFolder)) {
                plugin.messages().send(sender, "&c서버 월드 폴더 밖의 경로는 삭제할 수 없습니다: " + worldFolder.getPath());
                return;
            }
        }
        try {
            for (File worldFolder : folders) {
                deleteDirectory(worldFolder);
            }
            removeManagedWorld(name);
            if (gameWorldNameMatches(name)) {
                plugin.getConfig().set("world.game-world", "");
                plugin.saveConfig();
            }
            plugin.messages().send(sender, "&a월드 폴더를 삭제했습니다: &f" + name + " &7(" + folders.size() + "개 경로)");
        } catch (IOException ex) {
            plugin.messages().send(sender, "&c월드 삭제 실패: " + ex.getMessage());
        }
    }

    private void worldBackup(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.messages().send(sender, "&e/gw world backup <create|list|load> [이름] [로드월드이름]");
            plugin.messages().send(sender, "&7백업은 plugins/NewGodWar/world-backups/ 아래에 저장됩니다.");
            return;
        }
        String action = args[2].toLowerCase(Locale.ROOT);
        if (action.equals("list") || action.equals("목록")) {
            worldBackupManager.sendBackupList(sender);
            return;
        }
        if (action.equals("create") || action.equals("save") || action.equals("생성") || action.equals("저장")) {
            try {
                WorldBackupManager.BackupResult result = worldBackupManager.createBackup(args.length >= 4 ? args[3] : null);
                plugin.messages().send(sender, "&a월드 백업을 생성했습니다: &f" + result.name()
                    + " &7(" + result.worlds().size() + "개 월드)");
            } catch (IOException ex) {
                plugin.messages().send(sender, "&c월드 백업 생성 실패: " + ex.getMessage());
            }
            return;
        }
        if (action.equals("load") || action.equals("import") || action.equals("로드") || action.equals("불러오기")) {
            if (args.length < 4) {
                plugin.messages().send(sender, "&e/gw world backup load <백업이름> [로드월드이름]");
                return;
            }
            try {
                WorldBackupManager.LoadResult result = worldBackupManager.loadBackup(args[3], args.length >= 5 ? args[4] : null);
                for (String worldName : result.worlds()) {
                    saveManagedWorld(worldName, "normal");
                }
                plugin.messages().send(sender, "&a월드 백업을 새 월드로 로드했습니다: &f" + result.backupName()
                    + " &7-> &f" + join(result.worlds()));
            } catch (IOException ex) {
                plugin.messages().send(sender, "&c월드 백업 로드 실패: " + ex.getMessage());
            }
            return;
        }
        plugin.messages().send(sender, "&e/gw world backup <create|list|load> [이름] [로드월드이름]");
    }

    private void listWorlds(CommandSender sender) {
        sender.sendMessage(plugin.messages().prefix() + ChatColor.YELLOW + "로드된 월드 목록");
        Set<String> listed = new HashSet<String>();
        for (World world : Bukkit.getWorlds()) {
            listed.add(world.getName().toLowerCase(Locale.ROOT));
            sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.WHITE + world.getName()
                + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + world.getEnvironment().name()
                + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + world.getPlayers().size() + "명"
                + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + world.getWorldFolder().getName());
        }
        List<String> unmanagedFolders = unloadedWorldNameSuggestions();
        if (!unmanagedFolders.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "로드되지 않은 월드 폴더");
            for (String worldName : unmanagedFolders) {
                if (!listed.contains(worldName.toLowerCase(Locale.ROOT))) {
                    sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.WHITE + worldName
                        + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + "/gw world load " + worldName);
                }
            }
        }
        if (gameManager.lobbyLocation() != null) {
            sender.sendMessage(ChatColor.GRAY + "로비 이동: " + ChatColor.AQUA + "/gw world lobby");
        } else {
            sender.sendMessage(ChatColor.GRAY + "로비 위치 없음: " + ChatColor.AQUA + "/gw setlobby");
        }
    }

    private boolean isProtectedWorld(World world) {
        if (world == null) {
            return false;
        }
        List<World> worlds = Bukkit.getWorlds();
        if (!worlds.isEmpty() && worlds.get(0).equals(world)) {
            return true;
        }
        org.bukkit.Location lobby = gameManager.lobbyLocation();
        return lobby != null && lobby.getWorld() != null && lobby.getWorld().equals(world);
    }

    private String sanitizeWorldName(String name) {
        if (name == null || name.trim().length() == 0) {
            return null;
        }
        String trimmed = name.trim();
        if (!trimmed.matches("[A-Za-z0-9._-]{1,64}") || ".".equals(trimmed) || "..".equals(trimmed)) {
            return null;
        }
        return trimmed;
    }

    private String worldTypeToken(String[] args, int index) {
        if (args.length <= index) {
            return "normal";
        }
        String token = args[index].toLowerCase(Locale.ROOT);
        if (token.equals("void") || token.equals("empty") || token.equals("공허")) {
            return "void";
        }
        if (token.equals("flat") || token.equals("평지")) {
            return "flat";
        }
        return "normal";
    }

    private void saveManagedWorld(String worldName, String type) {
        List<Map<?, ?>> source = plugin.getConfig().getMapList("world.managed-worlds");
        List<Map<String, Object>> updated = new ArrayList<Map<String, Object>>();
        boolean replaced = false;
        for (Map<?, ?> entry : source) {
            String name = entry.get("name") == null ? null : entry.get("name").toString();
            if (name == null || name.trim().length() == 0) {
                continue;
            }
            Map<String, Object> copied = new LinkedHashMap<String, Object>();
            copied.put("name", name);
            copied.put("type", name.equalsIgnoreCase(worldName) ? type : managedTypeValue(entry));
            if (name.equalsIgnoreCase(worldName)) {
                replaced = true;
            }
            updated.add(copied);
        }
        if (!replaced) {
            Map<String, Object> entry = new LinkedHashMap<String, Object>();
            entry.put("name", worldName);
            entry.put("type", type);
            updated.add(entry);
        }
        plugin.getConfig().set("world.managed-worlds", updated);
        plugin.saveConfig();
    }

    private void removeManagedWorld(String worldName) {
        List<Map<?, ?>> source = plugin.getConfig().getMapList("world.managed-worlds");
        List<Map<String, Object>> updated = new ArrayList<Map<String, Object>>();
        for (Map<?, ?> entry : source) {
            String name = entry.get("name") == null ? null : entry.get("name").toString();
            if (name == null || name.equalsIgnoreCase(worldName)) {
                continue;
            }
            Map<String, Object> copied = new LinkedHashMap<String, Object>();
            copied.put("name", name);
            copied.put("type", managedTypeValue(entry));
            updated.add(copied);
        }
        plugin.getConfig().set("world.managed-worlds", updated);
        plugin.saveConfig();
    }

    private String managedWorldType(String worldName) {
        for (Map<?, ?> entry : plugin.getConfig().getMapList("world.managed-worlds")) {
            Object name = entry.get("name");
            if (name != null && name.toString().equalsIgnoreCase(worldName)) {
                return managedTypeValue(entry);
            }
        }
        return "normal";
    }

    private String inferWorldType(String worldName) {
        String managed = managedWorldType(worldName);
        if (!"normal".equals(managed)) {
            return managed;
        }
        World world = worldByName(worldName);
        if (world == null) {
            return managed;
        }
        if (world.getGenerator() instanceof VoidWorldGenerator
            || (world.getGenerator() != null && world.getGenerator().getClass().getName().toLowerCase(Locale.ROOT).contains("void"))) {
            return "void";
        }
        try {
            if (world.getWorldType() == WorldType.FLAT) {
                return "flat";
            }
        } catch (Throwable ignored) {
        }
        return managed;
    }

    private String managedTypeValue(Map<?, ?> entry) {
        Object type = entry.get("type");
        String value = type == null ? "normal" : type.toString().toLowerCase(Locale.ROOT);
        return value.equals("void") || value.equals("flat") ? value : "normal";
    }

    private boolean isInsideWorldContainer(File folder) {
        try {
            File container = Bukkit.getWorldContainer().getCanonicalFile();
            File target = folder.getCanonicalFile();
            while (target != null) {
                if (target.equals(container)) {
                    return true;
                }
                target = target.getParentFile();
            }
        } catch (IOException ex) {
            return false;
        }
        return false;
    }

    private boolean isSharedWorldFolder(World world, File folder) {
        for (World other : Bukkit.getWorlds()) {
            if (other.equals(world)) {
                continue;
            }
            if (sameFile(other.getWorldFolder(), folder)) {
                return true;
            }
        }
        return false;
    }

    private boolean sameFile(File left, File right) {
        try {
            return left != null && right != null && left.getCanonicalFile().equals(right.getCanonicalFile());
        } catch (IOException ex) {
            return false;
        }
    }

    private boolean gameWorldNameMatches(String worldName) {
        String configured = plugin.getConfig().getString("world.game-world", "");
        return configured != null && configured.equalsIgnoreCase(worldName);
    }

    private void deleteDirectory(File directory) throws IOException {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else if (!file.delete()) {
                    throw new IOException("파일을 삭제하지 못했습니다: " + file.getPath());
                }
            }
        }
        if (!directory.delete()) {
            throw new IOException("폴더를 삭제하지 못했습니다: " + directory.getPath());
        }
    }

    private World worldByName(String name) {
        if (name == null) {
            return null;
        }
        World exact = Bukkit.getWorld(name);
        if (exact != null) {
            return exact;
        }
        for (World world : Bukkit.getWorlds()) {
            if (world.getName().equalsIgnoreCase(name)) {
                return world;
            }
        }
        return null;
    }

    private void target(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            plugin.messages().send(sender, "&c플레이어만 사용할 수 있습니다.");
            return;
        }
        if (args.length < 2) {
            plugin.messages().send(sender, "&e/gw target <player>");
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
            plugin.messages().send(sender, "&e/gw " + (spectate ? "spectate" : "unspectate") + " <player>");
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

    private void observer(CommandSender sender, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("list")) {
            sender.sendMessage(ChatColor.DARK_AQUA + "옵저버 목록");
            boolean found = false;
            for (Player player : BukkitCompat.onlinePlayers()) {
                if (gameManager.isObserver(player)) {
                    found = true;
                    sender.sendMessage(ChatColor.GRAY + "  " + player.getName());
                }
            }
            if (!found) {
                plugin.messages().send(sender, "&e아무도 옵저버가 아닙니다.");
            }
            return;
        }
        Player player = asPlayer(sender);
        if (player == null) {
            plugin.messages().send(sender, "&c플레이어만 사용할 수 있습니다.");
            return;
        }
        boolean enabled = gameManager.toggleObserver(player);
        plugin.messages().send(sender, enabled ? "&a옵저버 모드가 켜졌습니다." : "&a옵저버 모드가 해제되었습니다.");
    }

    private void openSettings(CommandSender sender, String[] args) {
        if (args.length >= 2 && isHelpToken(args[1])) {
            CommandHelp.gui(sender, Arrays.copyOfRange(args, 2, args.length));
            return;
        }
        SettingsPage page = args.length >= 2 ? SettingsPage.parse(args[1]) : SettingsPage.MAIN;
        if (page == null) {
            plugin.messages().send(sender, "&c설정 화면을 찾을 수 없습니다: " + args[1] + " &7(/gw gui help)");
            return;
        }
        if (args.length > 3 || (args.length == 3 && page != SettingsPage.TEAM)) {
            plugin.messages().send(sender, "&e/gw gui <화면> &7또는 &e/gw gui team [team]");
            return;
        }
        GodTeam team = args.length == 3 ? GodTeam.parse(args[2]) : null;
        if (args.length == 3 && team == null) {
            plugin.messages().send(sender, "&c팀을 찾을 수 없습니다: " + args[2] + " &7가능한 팀: " + teamUsage());
            return;
        }
        Player player = asPlayer(sender);
        if (player == null) {
            plugin.messages().send(sender, "&c설정 화면은 플레이어만 열 수 있습니다. &7/gw gui help 로 화면 목록을 확인하세요.");
            return;
        }
        settingsGui.openPage(player, page, team);
    }

    private void openDefaultItems(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) {
            listDefaultItems(sender);
            return;
        }
        starterItemsGui.open(player);
    }

    private void update(CommandSender sender, String[] args) {
        String action = args.length < 2 ? "check" : args[1].toLowerCase(Locale.ROOT);
        boolean download = action.equals("download") || action.equals("install") || action.equals("auto") || action.equals("다운로드");
        if (!download
            && !action.equals("check")
            && !action.equals("status")
            && !action.equals("확인")) {
            plugin.messages().send(sender, "&e/gw update [check|download]");
            return;
        }

        plugin.messages().send(sender, download
            ? "&7최신 릴리즈를 확인하고 업데이트 jar를 다운로드합니다..."
            : "&7최신 릴리즈를 확인합니다...");

        boolean started = plugin.updater().checkNow(download, info -> plugin.updater().sendStatus(sender, info));
        if (!started) {
            plugin.messages().send(sender, "&e이미 업데이트 확인이 진행 중입니다. 잠시 후 다시 시도하세요.");
        }
    }

    private void defaultItems(CommandSender sender, String[] args) {
        if (args.length < 2 || isOpenDefaultItemsAction(args[1])) {
            openDefaultItems(sender);
            return;
        }
        if (isListDefaultItemsAction(args[1])) {
            listDefaultItems(sender);
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        if (action.equals("add") || action.equals("append") || action.equals("추가")) {
            if (args.length < 3) {
                plugin.messages().send(sender, "&e/gw defaultitems add hand|<material> [수량]");
                return;
            }
            ItemStack item = defaultItemStack(sender, args, 2);
            if (item == null) {
                return;
            }
            List<Map<String, Object>> entries = StarterItems.copiedConfiguredEntries(plugin.getConfig());
            entries.add(defaultItemEntry(args[2], item));
            saveDefaultItems(entries);
            plugin.messages().send(sender, "&a기본 지급 아이템 #" + entries.size() + "에 &f"
                + item.getType().name() + " " + item.getAmount() + "개&a를 추가했습니다.");
            return;
        }
        if (action.equals("set") || action.equals("replace") || action.equals("변경") || action.equals("교체")) {
            if (args.length < 4) {
                plugin.messages().send(sender, "&e/gw defaultitems set <번호> hand|<material> [수량]");
                return;
            }
            Integer number = parsePositiveInt(args[2]);
            if (number == null) {
                plugin.messages().send(sender, "&c번호는 1 이상의 숫자여야 합니다.");
                return;
            }
            List<Map<String, Object>> entries = StarterItems.copiedConfiguredEntries(plugin.getConfig());
            int index = number.intValue() - 1;
            if (index < 0 || index >= entries.size()) {
                plugin.messages().send(sender, "&c번호는 1-" + entries.size() + " 사이여야 합니다.");
                return;
            }
            ItemStack item = defaultItemStack(sender, args, 3);
            if (item == null) {
                return;
            }
            entries.set(index, defaultItemEntry(args[3], item));
            saveDefaultItems(entries);
            plugin.messages().send(sender, "&a기본 지급 아이템 #" + number + "을 &f"
                + item.getType().name() + " " + item.getAmount() + "개&a로 변경했습니다.");
            return;
        }
        if (action.equals("remove") || action.equals("delete") || action.equals("rm") || action.equals("삭제")) {
            if (args.length < 3) {
                plugin.messages().send(sender, "&e/gw defaultitems remove <번호>");
                return;
            }
            Integer number = parsePositiveInt(args[2]);
            if (number == null) {
                plugin.messages().send(sender, "&c번호는 1 이상의 숫자여야 합니다.");
                return;
            }
            List<Map<String, Object>> entries = StarterItems.copiedConfiguredEntries(plugin.getConfig());
            int index = number.intValue() - 1;
            if (index < 0 || index >= entries.size()) {
                plugin.messages().send(sender, "&c번호는 1-" + entries.size() + " 사이여야 합니다.");
                return;
            }
            String removed = StarterItems.displayName(entries.remove(index));
            saveDefaultItems(entries);
            plugin.messages().send(sender, "&a기본 지급 아이템 #" + number + "을 삭제했습니다: &f" + removed);
            return;
        }
        if (action.equals("clear") || action.equals("empty") || action.equals("비우기")) {
            saveDefaultItems(new ArrayList<Map<String, Object>>());
            plugin.messages().send(sender, "&a기본 지급 아이템 목록을 비웠습니다.");
            return;
        }
        if (action.equals("reset") || action.equals("default") || action.equals("초기화")) {
            saveDefaultItems(StarterItems.defaultEntries());
            plugin.messages().send(sender, "&a기본 지급 아이템을 기본 스카이블럭 세트로 초기화했습니다.");
            return;
        }
        plugin.messages().send(sender, "&e/gw defaultitems [gui|list|add|set|remove|clear|reset]");
    }

    private void listDefaultItems(CommandSender sender) {
        List<Map<?, ?>> entries = StarterItems.configuredEntries(plugin.getConfig());
        sender.sendMessage(ChatColor.GOLD + "기본 지급 아이템");
        for (int i = 0; i < entries.size(); i++) {
            sender.sendMessage(ChatColor.GRAY + "  #" + (i + 1) + " " + ChatColor.WHITE + StarterItems.displayName(entries.get(i)));
        }
        sender.sendMessage(ChatColor.DARK_GRAY + "  " + StarterItems.PATH);
    }

    private ItemStack defaultItemStack(CommandSender sender, String[] args, int tokenIndex) {
        String token = args[tokenIndex];
        if (token.equalsIgnoreCase("hand") || token.equalsIgnoreCase("held") || token.equalsIgnoreCase("item")
            || token.equalsIgnoreCase("손") || token.equalsIgnoreCase("손아이템")) {
            Player player = asPlayer(sender);
            if (player == null) {
                plugin.messages().send(sender, "&c콘솔에서는 hand 대신 <material> [수량]을 사용하세요.");
                return null;
            }
            ItemStack hand = player.getItemInHand();
            if (hand == null || hand.getType() == Material.AIR || hand.getAmount() <= 0) {
                plugin.messages().send(sender, "&c손에 든 아이템이 없습니다.");
                return null;
            }
            return hand.clone();
        }

        Material material = StarterItems.matchMaterial(token);
        if (material == null || material == Material.AIR) {
            plugin.messages().send(sender, "&c알 수 없는 아이템입니다: " + token);
            return null;
        }
        int amount = 1;
        if (args.length > tokenIndex + 1) {
            Integer parsedAmount = parsePositiveInt(args[tokenIndex + 1]);
            if (parsedAmount == null) {
                plugin.messages().send(sender, "&c수량은 숫자여야 합니다.");
                return null;
            }
            amount = Math.min(2304, parsedAmount.intValue());
        }
        return new ItemStack(material, amount);
    }

    private Map<String, Object> defaultItemEntry(String token, ItemStack item) {
        if (token.equalsIgnoreCase("hand") || token.equalsIgnoreCase("held") || token.equalsIgnoreCase("item")
            || token.equalsIgnoreCase("손") || token.equalsIgnoreCase("손아이템")) {
            return StarterItems.fromItem(item);
        }
        return StarterItems.fromMaterial(item.getType(), item.getAmount());
    }

    private boolean isListDefaultItemsAction(String token) {
        return token.equalsIgnoreCase("list")
            || token.equalsIgnoreCase("show")
            || token.equalsIgnoreCase("목록");
    }

    private boolean isOpenDefaultItemsAction(String token) {
        return token == null
            || token.equalsIgnoreCase("gui")
            || token.equalsIgnoreCase("open")
            || token.equalsIgnoreCase("chest")
            || token.equalsIgnoreCase("warehouse")
            || token.equalsIgnoreCase("창고")
            || token.equalsIgnoreCase("열기");
    }

    private void saveDefaultItems(List<Map<String, Object>> entries) {
        plugin.getConfig().set(StarterItems.PATH, entries);
        plugin.saveConfig();
    }

    private void gamblingReward(CommandSender sender, String[] args) {
        if (args.length < 4) {
            plugin.messages().send(sender, "&e/gw gamblereward <normal> <번호|add> hand|message|<material> [값]");
            return;
        }
        String path = gamblingRewardPath(args[1]);
        if (path == null) {
            plugin.messages().send(sender, "&c종류는 normal 또는 일반이어야 합니다.");
            return;
        }
        boolean add = isAddRewardToken(args[2]);
        Integer number = null;
        int index = -1;
        int size = plugin.getConfig().getMapList(path).size();
        if (!add) {
            number = parsePositiveInt(args[2]);
            if (number == null) {
                plugin.messages().send(sender, "&c상품 번호는 숫자이거나 add여야 합니다.");
                return;
            }
            index = number.intValue() - 1;
            if (index < 0 || index >= size) {
                plugin.messages().send(sender, "&c상품 번호는 1-" + size + " 사이여야 합니다.");
                return;
            }
        }

        String action = args[3].toLowerCase(Locale.ROOT);
        if (action.equals("message") || action.equals("msg") || action.equals("ment") || action.equals("멘트") || action.equals("문구")) {
            if (add) {
                plugin.messages().send(sender, "&c멘트는 먼저 상품을 추가한 뒤 상품 번호로 수정하세요.");
                return;
            }
            if (args.length < 5) {
                plugin.messages().send(sender, "&e/gw gamblereward " + args[1] + " " + number + " message <멘트>");
                return;
            }
            String message = joinArguments(args, 4);
            setGamblingRewardMessage(path, index, message);
            plugin.messages().send(sender, "&a도박 " + gamblingRewardLabel(path) + " 상품 #" + number + " 멘트를 변경했습니다: &f" + message);
            return;
        }
        if (action.equals("hand") || action.equals("held") || action.equals("sethand")
            || action.equals("item") || action.equals("손") || action.equals("손아이템")) {
            Player player = asPlayer(sender);
            if (player == null) {
                plugin.messages().send(sender, "&c콘솔에서는 hand 대신 <material> [수량]을 사용하세요.");
                return;
            }
            ItemStack hand = player.getItemInHand();
            if (hand == null || hand.getType() == Material.AIR || hand.getAmount() <= 0) {
                plugin.messages().send(sender, "&c손에 든 아이템이 없습니다.");
                return;
            }
            if (add) {
                int added = addGamblingRewardItem(path, hand);
                plugin.messages().send(sender, "&a도박 " + gamblingRewardLabel(path) + " 상품 #" + (added + 1) + "을 손 아이템으로 추가했습니다.");
                return;
            }
            setGamblingRewardItem(path, index, hand);
            plugin.messages().send(sender, "&a도박 " + gamblingRewardLabel(path) + " 상품 #" + number + " 아이템을 손 아이템으로 변경했습니다.");
            return;
        }

        Material material = matchRewardMaterial(args[3]);
        if (material == null || material == Material.AIR) {
            plugin.messages().send(sender, "&c알 수 없는 아이템입니다: " + args[3]);
            return;
        }
        int amount = 1;
        if (args.length >= 5) {
            Integer parsedAmount = parsePositiveInt(args[4]);
            if (parsedAmount == null) {
                plugin.messages().send(sender, "&c수량은 숫자여야 합니다.");
                return;
            }
            amount = Math.min(2304, parsedAmount.intValue());
        }
        if (add) {
            int added = addGamblingRewardMaterial(path, material, amount);
            plugin.messages().send(sender, "&a도박 " + gamblingRewardLabel(path) + " 상품 #" + (added + 1) + "을 "
                + material.name() + " " + amount + "개로 추가했습니다.");
            return;
        }
        setGamblingRewardMaterial(path, index, material, amount);
        plugin.messages().send(sender, "&a도박 " + gamblingRewardLabel(path) + " 상품 #" + number + " 아이템을 "
            + material.name() + " " + amount + "개로 변경했습니다.");
    }

    private String gamblingRewardPath(String token) {
        if (token == null) {
            return null;
        }
        if (token.equalsIgnoreCase("normal") || token.equalsIgnoreCase("일반")) {
            return "gambling.rewards.normal";
        }
        return null;
    }

    private String gamblingRewardLabel(String path) {
        return "일반";
    }

    private Material matchRewardMaterial(String token) {
        if (token == null) {
            return null;
        }
        Material material = Material.matchMaterial(token.toUpperCase(Locale.ROOT));
        if (material == null && token.equalsIgnoreCase("OAK_LOG")) {
            material = Material.matchMaterial("LOG");
        }
        return material;
    }

    private boolean isAddRewardToken(String token) {
        return token != null
            && (token.equalsIgnoreCase("add")
                || token.equalsIgnoreCase("append")
                || token.equalsIgnoreCase("new")
                || token.equalsIgnoreCase("추가"));
    }

    private void setGamblingRewardItem(String path, int index, ItemStack item) {
        ItemStack saved = item.clone();
        updateGamblingReward(path, index, saved, saved.getType(), saved.getAmount());
    }

    private void setGamblingRewardMaterial(String path, int index, Material material, int amount) {
        updateGamblingReward(path, index, null, material, amount);
    }

    private void updateGamblingReward(String path, int index, ItemStack item, Material material, int amount) {
        FileConfiguration config = plugin.getConfig();
        List<Map<?, ?>> source = config.getMapList(path);
        List<Map<String, Object>> updated = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < source.size(); i++) {
            Map<String, Object> copied = copyReward(source.get(i));
            if (i == index) {
                if (item == null) {
                    copied.remove("item");
                } else {
                    copied.put("item", item);
                }
                copied.put("material", material.name());
                copied.put("amount", amount);
                copied.remove("legacy-material");
            }
            updated.add(copied);
        }
        config.set(path, updated);
        plugin.saveConfig();
    }

    private void setGamblingRewardMessage(String path, int index, String message) {
        FileConfiguration config = plugin.getConfig();
        List<Map<?, ?>> source = config.getMapList(path);
        List<Map<String, Object>> updated = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < source.size(); i++) {
            Map<String, Object> copied = copyReward(source.get(i));
            if (i == index) {
                copied.put("message", message);
                copied.remove("messages");
            }
            updated.add(copied);
        }
        config.set(path, updated);
        plugin.saveConfig();
    }

    private int addGamblingRewardItem(String path, ItemStack item) {
        ItemStack saved = item.clone();
        return appendGamblingReward(path, saved, saved.getType(), saved.getAmount());
    }

    private int addGamblingRewardMaterial(String path, Material material, int amount) {
        return appendGamblingReward(path, null, material, amount);
    }

    private int appendGamblingReward(String path, ItemStack item, Material material, int amount) {
        FileConfiguration config = plugin.getConfig();
        List<Map<?, ?>> source = config.getMapList(path);
        List<Map<String, Object>> updated = new ArrayList<Map<String, Object>>();
        for (Map<?, ?> reward : source) {
            updated.add(copyReward(reward));
        }
        Map<String, Object> reward = new LinkedHashMap<String, Object>();
        reward.put("chance", 1);
        if (item != null) {
            reward.put("item", item);
        }
        reward.put("material", material.name());
        reward.put("amount", amount);
        reward.put("message", "&a도박 상품에 당첨되었습니다!");
        updated.add(reward);
        config.set(path, updated);
        plugin.saveConfig();
        return updated.size() - 1;
    }

    private Map<String, Object> copyReward(Map<?, ?> source) {
        Map<String, Object> copied = new LinkedHashMap<String, Object>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() != null) {
                copied.put(entry.getKey().toString(), entry.getValue());
            }
        }
        return copied;
    }

    private Integer parsePositiveInt(String text) {
        try {
            int value = Integer.parseInt(text);
            return value <= 0 ? null : Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean requiresAdmin(String sub) {
        return CommandCatalog.requiresAdmin(sub);
    }

    private boolean isThemachyRoot(Command command, String label) {
        return command.getName().equalsIgnoreCase("t") || label.equalsIgnoreCase("t");
    }

    private boolean isAbilityGroupRoot(boolean themachyRoot, String sub) {
        if (sub == null) {
            return false;
        }
        return normalizeSubcommand(sub).equals("ability");
    }

    private boolean requiresAbilityGroupAdmin(String action, boolean themachyRoot) {
        return isAbilityListAction(action)
            || action.equalsIgnoreCase("random")
            || action.equalsIgnoreCase("remove")
            || action.equalsIgnoreCase("reset")
            || action.equalsIgnoreCase("skip")
            || action.equalsIgnoreCase("cutin")
            || action.equalsIgnoreCase("set")
            || action.equalsIgnoreCase("assign");
    }

    private Player asPlayer(CommandSender sender) {
        return sender instanceof Player ? (Player) sender : null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("x")) {
            return args.length == 1 ? startsWith(onlinePlayerNames(), args[0]) : Collections.<String>emptyList();
        }
        if (command.getName().equalsIgnoreCase("a")) {
            args = prependSubcommand("ability", args);
        }
        args = CommandCatalog.expandShortcut(command.getName(), args);
        if (args.length == 0) return Collections.emptyList();
        if (args.length == 1) {
            List<String> values = firstLevelSuggestions(sender, command, alias);
            return startsWith(values, args[0]);
        }
        boolean admin = sender.hasPermission("newgodwar.admin");
        CommandTree.Result helpRoute = CommandTree.resolve(args);
        if (helpRoute.help != null && args.length >= 3 && isHelpToken(args[args.length - 2])) {
            return startsWith(CommandHelp.groupPages(helpRoute.help, helpRoute.path, admin), args[args.length - 1]);
        }
        List<String> grouped = CommandTree.complete(args, admin);
        // Legacy ability-first assignment and /t <team> <player> keep their argument completion.
        if (args.length == 2 && isAbilityGroupRoot(false, args[0])) {
            if (grouped == null) grouped = new ArrayList<String>();
            grouped.addAll(abilityTargetNames(sender));
            if (admin) grouped.addAll(abilityIdSuggestions());
        }
        if (args.length == 2 && isThemachyRoot(command, alias) && GodTeam.parse(args[0]) != null) {
            return admin ? startsWith(onlinePlayerNames(), args[1]) : Collections.<String>emptyList();
        }
        if (grouped != null) {
            return startsWith(grouped, args[args.length - 1]);
        }
        CommandTree.Result route = CommandTree.resolve(args);
        if (route.help != null) return Collections.emptyList();
        args = route.args;
        if (route.abilityView) {
            return args.length == 2 ? startsWith(abilityTargetNames(sender), args[1]) : Collections.<String>emptyList();
        }
        String sub = normalizeSubcommand(args[0]);
        if (isAbilityGroupRoot(isThemachyRoot(command, alias), args[0])) {
            return abilityGroupTabComplete(sender, command, alias, args);
        }
        if (sub.equals("help")) {
            return startsWith(CommandHelp.complete(Arrays.copyOfRange(args, 1, args.length), admin), args[args.length - 1]);
        }
        if (requiresAdmin(sub) && !sender.hasPermission("newgodwar.admin")) {
            return Collections.emptyList();
        }
        if (args.length == 3 && (sub.equals("world") || sub.equals("map")) && isHelpToken(args[1])) {
            return startsWith(CommandHelp.complete(new String[] {sub, args[2]}, admin), args[2]);
        }
        if (sub.equals("settings")) {
            if (args.length == 2) {
                List<String> pages = new ArrayList<String>(SettingsPage.suggestions());
                pages.add("help");
                return startsWith(pages, args[1]);
            }
            if (args.length == 3 && SettingsPage.parse(args[1]) == SettingsPage.TEAM) return startsWith(teamSuggestions(false), args[2]);
            if (args.length == 3 && isHelpToken(args[1])) return startsWith(CommandHelp.complete(new String[] {"gui", args[2]}, admin), args[2]);
            return Collections.emptyList();
        }
        if (args.length == 2 && sub.equals("update")) {
            return startsWith(Arrays.asList("check", "download", "status"), args[1]);
        }
        if (args.length == 2 && (sub.equals("join") || sub.equals("settemple") || sub.equals("setspawn") || sub.equals("info"))) {
            return startsWith(teamSuggestions(false), args[1]);
        }
        if (args.length == 3 && sub.equals("join")) {
            return startsWith(onlinePlayerNames(), args[2]);
        }
        if (args.length == 2 && sub.equals("changeteam")) {
            List<String> values = new ArrayList<String>();
            values.addAll(onlinePlayerNames());
            values.addAll(teamSuggestions(false));
            return startsWith(values, args[1]);
        }
        if (args.length == 3 && sub.equals("changeteam")) {
            if (GodTeam.parse(args[1]) != null) {
                return startsWith(onlinePlayerNames(), args[2]);
            }
            return startsWith(teamSuggestions(false), args[2]);
        }
        if (args.length == 2 && sub.equals("midjoin")) {
            return startsWith(onlinePlayerNames(), args[1]);
        }
        if (args.length == 3 && sub.equals("midjoin")) {
            return startsWith(teamSuggestions(true), args[2]);
        }
        if (args.length == 2 && sub.equals("ability")) {
            List<String> values = new ArrayList<String>();
            values.add("list");
            values.addAll(abilityTargetNames(sender));
            return startsWith(values, args[1]);
        }
        if (args.length == 2 && sub.equals("abilities")) {
            return startsWith(abilitySuggestions(), args[1]);
        }
        if (args.length == 2 && sub.equals("assignedabilities")) {
            return startsWith(assignedAbilitySuggestions(), args[1]);
        }
        if (args.length == 2 && sub.equals("participants")) {
            return startsWith(participantSuggestions(), args[1]);
        }
        if (args.length == 2 && sub.equals("target")) {
            return startsWith(onlinePlayerNames(), args[1]);
        }
        if (args.length == 2 && sub.equals("blacklist")) {
            return startsWith(Arrays.asList("list", "add", "remove", "toggle"), args[1]);
        }
        if (args.length == 3 && sub.equals("blacklist")) {
            return startsWith(abilityIdSuggestions(), args[2]);
        }
        if (args.length == 2 && sub.equals("gamerule")) {
            return startsWith(Arrays.asList("apply", "restore"), args[1]);
        }
        if (args.length == 2 && sub.equals("world")) {
            List<String> values = new ArrayList<String>();
            values.addAll(Arrays.asList("help", "gui", "settings", "list", "game", "create", "load", "copy", "tp", "lobby", "unload", "delete", "backup"));
            values.addAll(worldNameSuggestions());
            return startsWith(values, args[1]);
        }
        if (args.length == 2 && sub.equals("map")) {
            List<String> values = new ArrayList<String>();
            values.addAll(Arrays.asList("help", "list", "clear", "select"));
            values.addAll(mapNameSuggestions());
            return startsWith(values, args[1]);
        }
        if (args.length == 3 && sub.equals("map")
            && (args[1].equalsIgnoreCase("select") || args[1].equalsIgnoreCase("선택"))) {
            return startsWith(mapNameSuggestions(), args[2]);
        }
        if (args.length == 3 && sub.equals("world")) {
            if (args[1].equalsIgnoreCase("tp") || args[1].equalsIgnoreCase("teleport") || args[1].equalsIgnoreCase("이동")) {
                return startsWith(worldNameSuggestions(), args[2]);
            }
            if (args[1].equalsIgnoreCase("load") || args[1].equalsIgnoreCase("로드")) {
                return startsWith(unloadedWorldNameSuggestions(), args[2]);
            }
            if (args[1].equalsIgnoreCase("copy") || args[1].equalsIgnoreCase("clone") || args[1].equalsIgnoreCase("복사")) {
                return startsWith(worldNameSuggestions(), args[2]);
            }
            if (args[1].equalsIgnoreCase("create") || args[1].equalsIgnoreCase("new") || args[1].equalsIgnoreCase("생성")) {
                return startsWith(Arrays.asList("game", "lobby", "arena"), args[2]);
            }
            if (args[1].equalsIgnoreCase("game") || args[1].equalsIgnoreCase("gameworld") || args[1].equalsIgnoreCase("게임")) {
                List<String> values = new ArrayList<String>();
                values.add("clear");
                values.addAll(worldNameSuggestions());
                return startsWith(values, args[2]);
            }
            if (args[1].equalsIgnoreCase("unload") || args[1].equalsIgnoreCase("delete") || args[1].equalsIgnoreCase("remove")
                || args[1].equalsIgnoreCase("언로드") || args[1].equalsIgnoreCase("삭제")) {
                return startsWith(worldNameSuggestions(), args[2]);
            }
            if (args[1].equalsIgnoreCase("backup") || args[1].equalsIgnoreCase("backups") || args[1].equalsIgnoreCase("백업")) {
                return startsWith(Arrays.asList("create", "list", "load"), args[2]);
            }
            if (args[1].equalsIgnoreCase("lobby") || worldByName(args[1]) != null) {
                return startsWith(onlinePlayerNames(), args[2]);
            }
        }
        if (args.length == 4 && sub.equals("world")
            && (args[1].equalsIgnoreCase("tp") || args[1].equalsIgnoreCase("teleport") || args[1].equalsIgnoreCase("이동"))) {
            return startsWith(onlinePlayerNames(), args[3]);
        }
        if (args.length == 4 && sub.equals("world")
            && (args[1].equalsIgnoreCase("create") || args[1].equalsIgnoreCase("new") || args[1].equalsIgnoreCase("생성"))) {
            return startsWith(Arrays.asList("normal", "flat", "void"), args[3]);
        }
        if (args.length == 4 && sub.equals("world")
            && (args[1].equalsIgnoreCase("load") || args[1].equalsIgnoreCase("로드"))) {
            return startsWith(Arrays.asList("normal", "flat", "void"), args[3]);
        }
        if (args.length == 5 && sub.equals("world")
            && (args[1].equalsIgnoreCase("copy") || args[1].equalsIgnoreCase("clone") || args[1].equalsIgnoreCase("복사"))) {
            return startsWith(Arrays.asList("normal", "flat", "void"), args[4]);
        }
        if (args.length == 4 && sub.equals("world")
            && (args[1].equalsIgnoreCase("delete") || args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("삭제"))) {
            return startsWith(Arrays.asList("confirm"), args[3]);
        }
        if (args.length == 4 && sub.equals("world")
            && (args[1].equalsIgnoreCase("backup") || args[1].equalsIgnoreCase("backups") || args[1].equalsIgnoreCase("백업"))
            && (args[2].equalsIgnoreCase("load") || args[2].equalsIgnoreCase("로드"))) {
            return startsWith(backupNameSuggestions(), args[3]);
        }
        if (args.length == 2 && sub.equals("urf")) {
            return startsWith(Arrays.asList("on", "off", "toggle", "20%", "50%", "100%", "percent"), args[1]);
        }
        if (args.length == 2 && sub.equals("gamblereward")) {
            return startsWith(Arrays.asList("normal", "일반"), args[1]);
        }
        if (args.length == 3 && sub.equals("gamblereward")) {
            return startsWith(gamblingRewardIndexes(args[1]), args[2]);
        }
        if (args.length == 4 && sub.equals("gamblereward")) {
            List<String> values = new ArrayList<String>();
            values.addAll(Arrays.asList("hand", "message", "item", "DIAMOND", "IRON_INGOT", "BLAZE_ROD", "OAK_LOG"));
            return startsWith(values, args[3]);
        }
        if (args.length == 2 && sub.equals("defaultitems")) {
            return startsWith(Arrays.asList("gui", "list", "add", "set", "remove", "clear", "reset"), args[1]);
        }
        if (args.length == 3 && sub.equals("defaultitems")
            && (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("추가"))) {
            return startsWith(defaultItemMaterialSuggestions(), args[2]);
        }
        if (args.length == 3 && sub.equals("defaultitems")
            && (args[1].equalsIgnoreCase("set") || args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("삭제"))) {
            return startsWith(defaultItemIndexes(), args[2]);
        }
        if (args.length == 4 && sub.equals("defaultitems") && args[1].equalsIgnoreCase("set")) {
            return startsWith(defaultItemMaterialSuggestions(), args[3]);
        }
        if (args.length == 2 && sub.equals("rerolls")) {
            return startsWith(Arrays.asList("0", "1", "2", "3", "5"), args[1]);
        }
        if (args.length == 2 && sub.equals("skip")) {
            return startsWith(Arrays.asList("0", "3", "5", "10", "15", "30"), args[1]);
        }
        if (args.length == 2 && sub.equals("skipseconds")) {
            return startsWith(Arrays.asList("0", "3", "5", "10", "15", "30"), args[1]);
        }
        if (args.length == 2 && sub.equals("pickaxe")) {
            return startsWith(Arrays.asList("status", "wooden", "stone", "iron", "diamond", "all"), args[1]);
        }
        if (args.length == 3 && sub.equals("pickaxe")) {
            return startsWith(Arrays.asList("open", "off", "0", "5", "10", "15", "20", "30", "300초"), args[2]);
        }
        if (args.length == 3 && sub.equals("urf")
            && (args[1].equalsIgnoreCase("percent") || args[1].equalsIgnoreCase("rate") || args[1].equalsIgnoreCase("배율"))) {
            return startsWith(Arrays.asList("0%", "20%", "50%", "100%"), args[2]);
        }
        if (args.length == 2 && sub.equals("test")) {
            return startsWith(abilityIdSuggestions(), args[1]);
        }
        if (args.length == 2 && sub.equals("clear")) {
            List<String> values = new ArrayList<String>(Arrays.asList("self", "all", "본인", "전체"));
            values.addAll(onlinePlayerNames());
            return startsWith(values, args[1]);
        }
        if (args.length == 2 && sub.equals("observer")) {
            return startsWith(Arrays.asList("list"), args[1]);
        }
        if (args.length == 2 && (sub.equals("leave") || sub.equals("randomability")
            || sub.equals("removeability") || sub.equals("resetabilities")
            || sub.equals("spectate") || sub.equals("unspectate"))) {
            return startsWith(onlinePlayerNames(), args[1]);
        }
        if (args.length == 2 && sub.equals("setability")) {
            return startsWith(onlinePlayerNames(), args[1]);
        }
        if (args.length == 3 && sub.equals("setability")) {
            return startsWith(abilityIdSuggestions(), args[2]);
        }
        return Collections.emptyList();
    }

    private List<String> firstLevelSuggestions(CommandSender sender, Command command, String alias) {
        boolean admin = sender.hasPermission("newgodwar.admin");
        List<String> values = new ArrayList<String>(CommandTree.roots(admin));
        values.addAll(CommandCatalog.names(admin));
        if (admin && isThemachyRoot(command, alias)) values.addAll(teamSuggestions(false));
        return values;
    }

    private List<String> abilityGroupTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        boolean themachyRoot = isThemachyRoot(command, alias);
        boolean admin = sender.hasPermission("newgodwar.admin");
        if (args.length == 2) {
            List<String> values = new ArrayList<String>();
            values.addAll(Arrays.asList("help", "catalog"));
            if (admin) {
                values.addAll(Arrays.asList("list", "random", "remove", "reset", "skip", "cutin", "set"));
            }
            if (themachyRoot) {
                values.addAll(abilityTargetNames(sender));
                if (admin) {
                    values.addAll(abilityIdSuggestions());
                }
            } else {
                values.addAll(admin ? onlinePlayerNames() : abilityTargetNames(sender));
                if (admin) {
                    values.addAll(abilityIdSuggestions());
                }
            }
            return startsWith(values, args[1]);
        }
        if (!admin && !isAbilityCatalogAction(args[1])) {
            return Collections.emptyList();
        }
        if (args.length == 3 && isAbilityListAction(args[1])) {
            return startsWith(assignedAbilitySuggestions(), args[2]);
        }
        if (args.length == 3 && isAbilityCatalogAction(args[1])) {
            return startsWith(abilitySuggestions(), args[2]);
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("cutin")) {
            List<String> values = new ArrayList<String>();
            values.addAll(onlinePlayerNames());
            values.addAll(teamSuggestions(true));
            return startsWith(values, args[2]);
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("cutin")) {
            return startsWith(teamSuggestions(true), args[3]);
        }
        if (args.length == 3 && (args[1].equalsIgnoreCase("random")
            || args[1].equalsIgnoreCase("remove")
            || args[1].equalsIgnoreCase("reset")
            || args[1].equalsIgnoreCase("set"))) {
            return startsWith(onlinePlayerNames(), args[2]);
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("set")) {
            return startsWith(abilityIdSuggestions(), args[3]);
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("skip")) {
            return startsWith(Arrays.asList("0", "3", "5", "10", "15", "30"), args[2]);
        }
        if (args.length == 3 && !isThemachyAbilityAction(args[1]) && abilityByToken(args[1]) != null) {
            return startsWith(onlinePlayerNames(), args[2]);
        }
        return Collections.emptyList();
    }

    private List<String> startsWith(List<String> values, String prefix) {
        List<String> result = new ArrayList<String>();
        String lower = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lower) && !result.contains(value)) {
                result.add(value);
            }
        }
        return result;
    }

    private List<String> onlinePlayerNames() {
        List<String> players = new ArrayList<String>();
        for (Player player : BukkitCompat.onlinePlayers()) {
            players.add(player.getName());
        }
        return players;
    }

    private List<String> abilityTargetNames(CommandSender sender) {
        if (sender.hasPermission("newgodwar.admin")) {
            return onlinePlayerNames();
        }
        Player viewer = asPlayer(sender);
        if (viewer == null) {
            return Collections.emptyList();
        }
        GodTeam viewerTeam = gameManager.teamOf(viewer);
        List<String> players = new ArrayList<String>();
        for (Player player : BukkitCompat.onlinePlayers()) {
            if (player.equals(viewer) || (viewerTeam != null && viewerTeam.equals(gameManager.teamOf(player)))) {
                players.add(player.getName());
            }
        }
        return players;
    }

    private List<String> abilityIdSuggestions() {
        List<String> abilities = new ArrayList<String>();
        for (String id : abilityManager.registry().ids()) {
            abilities.add(id);
        }
        return abilities;
    }

    private List<String> teamSuggestions(boolean includeAuto) {
        List<String> teams = new ArrayList<String>();
        for (GodTeam team : GodTeam.values()) {
            teams.add(team.id());
            teams.add(gameManager.teamDisplayName(team));
        }
        if (includeAuto) {
            teams.add("auto");
            teams.add("자동");
        }
        return teams;
    }

    private String teamUsage() {
        return join(GodTeam.ids());
    }

    private List<String> gamblingRewardIndexes(String token) {
        String path = gamblingRewardPath(token);
        if (path == null) {
            return Collections.emptyList();
        }
        int size = plugin.getConfig().getMapList(path).size();
        List<String> indexes = new ArrayList<String>();
        indexes.add("add");
        for (int i = 1; i <= size; i++) {
            indexes.add(String.valueOf(i));
        }
        return indexes;
    }

    private List<String> defaultItemIndexes() {
        int size = StarterItems.configuredEntries(plugin.getConfig()).size();
        List<String> indexes = new ArrayList<String>();
        for (int i = 1; i <= size; i++) {
            indexes.add(String.valueOf(i));
        }
        return indexes;
    }

    private List<String> defaultItemMaterialSuggestions() {
        return Arrays.asList("hand", "LAVA_BUCKET", "ICE", "OAK_SAPLING", "BONE_MEAL", "CHEST", "COOKED_BEEF");
    }

    private List<String> backupNameSuggestions() {
        List<String> backups = new ArrayList<String>();
        for (WorldBackupManager.BackupInfo backup : worldBackupManager.listBackups()) {
            backups.add(backup.name());
        }
        return backups;
    }

    private List<String> worldNameSuggestions() {
        List<String> worlds = new ArrayList<String>();
        for (World world : Bukkit.getWorlds()) {
            worlds.add(world.getName());
        }
        return worlds;
    }

    private List<String> mapNameSuggestions() {
        List<String> worlds = worldNameSuggestions();
        for (String unloaded : unloadedWorldNameSuggestions()) {
            if (!containsIgnoreCase(worlds, unloaded)) {
                worlds.add(unloaded);
            }
        }
        return worlds;
    }

    private boolean containsIgnoreCase(List<String> values, String target) {
        for (String value : values) {
            if (value.equalsIgnoreCase(target)) {
                return true;
            }
        }
        return false;
    }

    private List<String> unloadedWorldNameSuggestions() {
        List<String> worlds = new ArrayList<String>();
        File[] folders = Bukkit.getWorldContainer().listFiles(File::isDirectory);
        if (folders == null) {
            return worlds;
        }
        for (File folder : folders) {
            if (Bukkit.getWorld(folder.getName()) == null && new File(folder, "level.dat").isFile()) {
                worlds.add(folder.getName());
            }
        }
        return worlds;
    }

    private boolean isTeamOrAuto(String token) {
        return GodTeam.parse(token) != null || isAutoTeamToken(token);
    }

    private boolean isAutoTeamToken(String token) {
        return token != null
            && (token.equalsIgnoreCase("auto")
                || token.equalsIgnoreCase("random")
                || token.equalsIgnoreCase("balanced")
                || token.equalsIgnoreCase("자동"));
    }

    private boolean isThemachyAbilityAction(String token) {
        return token != null
            && (token.equalsIgnoreCase("help")
                || isAbilityListAction(token)
                || isAbilityCatalogAction(token)
                || token.equalsIgnoreCase("random")
                || token.equalsIgnoreCase("remove")
                || token.equalsIgnoreCase("reset")
                || token.equalsIgnoreCase("skip")
                || token.equalsIgnoreCase("cutin")
                || token.equalsIgnoreCase("set"));
    }

    private boolean isAbilityListAction(String token) {
        return token != null
            && (token.equalsIgnoreCase("list")
                || token.equalsIgnoreCase("assigned")
                || token.equalsIgnoreCase("assignments")
                || token.equalsIgnoreCase("배정"));
    }

    private boolean isAbilityCatalogAction(String token) {
        return token != null
            && (token.equalsIgnoreCase("catalog")
                || token.equalsIgnoreCase("book")
                || token.equalsIgnoreCase("all")
                || token.equalsIgnoreCase("도감"));
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

    private String joinArguments(String[] args, int start) {
        if (args.length <= start) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i > start) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        return builder.toString();
    }

    private String[] prependSubcommand(String sub, String[] args) {
        return prependSubcommand(sub, args, 0);
    }

    private String[] prependSubcommand(String sub, String[] args, int start) {
        int length = Math.max(0, args.length - start);
        String[] result = new String[length + 1];
        result[0] = sub;
        for (int i = 0; i < length; i++) {
            result[i + 1] = args[start + i];
        }
        return result;
    }

    private String stoneCost(int cost) {
        return cost <= 0 ? "없음" : cost + "개";
    }

    private Integer parsePercent(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.trim();
        if (normalized.endsWith("%")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        try {
            return Integer.valueOf((int) Math.round(Double.parseDouble(normalized)));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer parseWholeNumber(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.trim();
        if (normalized.endsWith("초") || normalized.endsWith("회")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        try {
            return Integer.valueOf(Integer.parseInt(normalized));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer parsePickaxeUnlockSeconds(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("open") || normalized.equals("now") || normalized.equals("on")
            || normalized.equals("열기") || normalized.equals("해제") || normalized.equals("켜기")) {
            return Integer.valueOf(0);
        }
        if (normalized.equals("off") || normalized.equals("disable") || normalized.equals("닫기")
            || normalized.equals("꺼짐") || normalized.equals("끄기")) {
            return Integer.valueOf(-1);
        }
        int multiplier = 60;
        if (normalized.endsWith("seconds")) {
            multiplier = 1;
            normalized = normalized.substring(0, normalized.length() - "seconds".length()).trim();
        } else if (normalized.endsWith("second")) {
            multiplier = 1;
            normalized = normalized.substring(0, normalized.length() - "second".length()).trim();
        } else if (normalized.endsWith("secs")) {
            multiplier = 1;
            normalized = normalized.substring(0, normalized.length() - "secs".length()).trim();
        } else if (normalized.endsWith("sec")) {
            multiplier = 1;
            normalized = normalized.substring(0, normalized.length() - "sec".length()).trim();
        } else if (normalized.endsWith("s") || normalized.endsWith("초")) {
            multiplier = 1;
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        } else if (normalized.endsWith("minutes")) {
            normalized = normalized.substring(0, normalized.length() - "minutes".length()).trim();
        } else if (normalized.endsWith("minute")) {
            normalized = normalized.substring(0, normalized.length() - "minute".length()).trim();
        } else if (normalized.endsWith("mins")) {
            normalized = normalized.substring(0, normalized.length() - "mins".length()).trim();
        } else if (normalized.endsWith("min")) {
            normalized = normalized.substring(0, normalized.length() - "min".length()).trim();
        } else if (normalized.endsWith("m") || normalized.endsWith("분")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        try {
            int value = Integer.parseInt(normalized);
            if (value < 0) {
                return null;
            }
            long seconds = (long) value * (long) multiplier;
            return Integer.valueOf((int) Math.min(86400L, seconds));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private PickaxeUnlockTarget pickaxeUnlockTarget(String token) {
        if (token == null) {
            return null;
        }
        String normalized = token.toLowerCase(Locale.ROOT).trim();
        if (normalized.equals("wood") || normalized.equals("wooden") || normalized.equals("나무")) {
            return PickaxeUnlockTarget.WOODEN;
        }
        if (normalized.equals("stone") || normalized.equals("돌")) {
            return PickaxeUnlockTarget.STONE;
        }
        if (normalized.equals("iron") || normalized.equals("철")) {
            return PickaxeUnlockTarget.IRON;
        }
        if (normalized.equals("diamond") || normalized.equals("dia") || normalized.equals("다이아")) {
            return PickaxeUnlockTarget.DIAMOND;
        }
        if (normalized.equals("all") || normalized.equals("전체")) {
            return PickaxeUnlockTarget.ALL;
        }
        return null;
    }

    private List<PickaxeUnlockTarget> pickaxeUnlockTargets(PickaxeUnlockTarget target) {
        if (target == PickaxeUnlockTarget.ALL) {
            return Arrays.asList(
                PickaxeUnlockTarget.WOODEN,
                PickaxeUnlockTarget.STONE,
                PickaxeUnlockTarget.IRON,
                PickaxeUnlockTarget.DIAMOND);
        }
        return Collections.singletonList(target);
    }

    private String pickaxeTargetLabel(PickaxeUnlockTarget target) {
        return target == PickaxeUnlockTarget.ALL ? "전체" : target.label;
    }

    private String pickaxeUnlockStateText(int seconds) {
        if (seconds < 0) {
            return "자동 해제 안 함";
        }
        if (!gameManager.isRunning()) {
            return pickaxeUnlockText(seconds);
        }
        long elapsed = gameManager.runningElapsedSeconds();
        if (elapsed >= seconds) {
            return pickaxeUnlockText(seconds) + ChatColor.GREEN + " (열림)";
        }
        return pickaxeUnlockText(seconds) + ChatColor.GRAY + " (남은 시간 " + formatSeconds(seconds - elapsed) + ")";
    }

    private String pickaxeUnlockText(int seconds) {
        if (seconds < 0) {
            return "자동 해제 안 함";
        }
        if (seconds == 0) {
            return "즉시 열림";
        }
        return formatSeconds(seconds);
    }

    private String formatSeconds(long seconds) {
        long safeSeconds = Math.max(0L, seconds);
        long minutes = safeSeconds / 60L;
        long remain = safeSeconds % 60L;
        if (minutes <= 0L) {
            return remain + "초";
        }
        if (remain == 0L) {
            return minutes + "분";
        }
        return minutes + "분 " + remain + "초";
    }

    private enum PickaxeUnlockTarget {
        WOODEN("나무", "core.pickaxe-unlock.wooden-seconds"),
        STONE("돌", "core.pickaxe-unlock.stone-seconds"),
        IRON("철", "core.pickaxe-unlock.iron-seconds"),
        DIAMOND("다이아", "core.pickaxe-unlock.diamond-seconds"),
        ALL("전체", null);

        private final String label;
        private final String path;

        PickaxeUnlockTarget(String label, String path) {
            this.label = label;
            this.path = path;
        }
    }

    private AbilityDefinition abilityByToken(String token) {
        AbilityDefinition byId = abilityManager.registry().get(token);
        if (byId != null) {
            return byId;
        }
        try {
            int code = Integer.parseInt(token);
            int index = 1;
            for (AbilityDefinition ability : abilityManager.registry().all()) {
                if (index == code) {
                    return ability;
                }
                index++;
            }
        } catch (NumberFormatException ignored) {
        }
        for (AbilityDefinition ability : abilityManager.registry().all()) {
            if (ability.name().equalsIgnoreCase(token)) {
                return ability;
            }
        }
        return null;
    }

    private List<AbilityDefinition> filteredAbilities(String query) {
        List<AbilityDefinition> abilities = new ArrayList<AbilityDefinition>();
        for (AbilityDefinition ability : abilityManager.registry().all()) {
            if (!hasQuery(query) || abilityMatches(ability, query)) {
                abilities.add(ability);
            }
        }
        return abilities;
    }

    private boolean abilityMatches(AbilityDefinition ability, String query) {
        String normalized = query.toLowerCase(Locale.ROOT).trim();
        return contains(ability.id(), normalized)
            || contains(ability.name(), normalized)
            || contains(ability.description(), normalized)
            || contains(ability.normalSkill(), normalized)
            || contains(ability.advancedSkill(), normalized)
            || contains(ability.passiveSkill(), normalized)
            || contains(ability.author(), normalized)
            || contains(ability.grade().symbol(), normalized)
            || contains(ability.grade().label(), normalized);
    }

    private boolean contains(String text, String query) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(query);
    }

    private boolean hasQuery(String query) {
        return query != null && query.trim().length() > 0;
    }

    private List<String> abilitySuggestions() {
        List<String> suggestions = new ArrayList<String>();
        for (AbilityDefinition ability : abilityManager.registry().all()) {
            suggestions.add(ability.id());
            suggestions.add(ability.name());
        }
        return suggestions;
    }

    private List<String> assignedAbilitySuggestions() {
        List<String> suggestions = abilitySuggestions();
        for (Map.Entry<UUID, AbilityDefinition> entry : abilityManager.assignedAbilities().entrySet()) {
            suggestions.add(playerName(entry.getKey()));
        }
        return suggestions;
    }

    private List<String> participantSuggestions() {
        List<String> suggestions = assignedAbilitySuggestions();
        suggestions.addAll(teamSuggestions(false));
        for (Map.Entry<UUID, GodTeam> entry : gameManager.teamAssignments().entrySet()) {
            suggestions.add(playerName(entry.getKey()));
        }
        return suggestions;
    }

    private List<AssignedAbilityView> assignedAbilityViews(String query) {
        List<AssignedAbilityView> views = new ArrayList<AssignedAbilityView>();
        for (Map.Entry<UUID, AbilityDefinition> entry : abilityManager.assignedAbilities().entrySet()) {
            String playerName = playerName(entry.getKey());
            AbilityDefinition ability = entry.getValue();
            if (hasQuery(query) && !contains(playerName, query.toLowerCase(Locale.ROOT).trim()) && !abilityMatches(ability, query)) {
                continue;
            }
            views.add(new AssignedAbilityView(playerName, Bukkit.getPlayer(entry.getKey()) != null, ability));
        }
        for (int i = 1; i < views.size(); i++) {
            AssignedAbilityView current = views.get(i);
            int cursor = i - 1;
            while (cursor >= 0 && views.get(cursor).playerName.compareToIgnoreCase(current.playerName) > 0) {
                views.set(cursor + 1, views.get(cursor));
                cursor--;
            }
            views.set(cursor + 1, current);
        }
        return views;
    }

    private List<ParticipantView> participantViews(String query) {
        Map<UUID, GodTeam> teams = gameManager.teamAssignments();
        Map<UUID, AbilityDefinition> abilities = abilityManager.assignedAbilities();
        Set<UUID> uuids = new HashSet<UUID>();
        uuids.addAll(teams.keySet());
        uuids.addAll(abilities.keySet());

        List<ParticipantView> views = new ArrayList<ParticipantView>();
        for (UUID uuid : uuids) {
            String playerName = playerName(uuid);
            GodTeam team = teams.get(uuid);
            AbilityDefinition ability = abilities.get(uuid);
            Player online = Bukkit.getPlayer(uuid);
            boolean observer = online != null && gameManager.isObserver(online);
            int kills = online == null ? 0 : gameManager.killsOf(online);
            if (hasQuery(query) && !participantMatches(playerName, team, ability, query)) {
                continue;
            }
            views.add(new ParticipantView(playerName, online != null, observer, team, ability, kills));
        }
        sortParticipantViews(views);
        return views;
    }

    private boolean participantMatches(String playerName, GodTeam team, AbilityDefinition ability, String query) {
        String normalized = query.toLowerCase(Locale.ROOT).trim();
        return contains(playerName, normalized)
            || (team != null && (contains(team.id(), normalized) || contains(gameManager.teamDisplayName(team), normalized)))
            || (ability != null && abilityMatches(ability, query));
    }

    private void sortParticipantViews(List<ParticipantView> views) {
        for (int i = 1; i < views.size(); i++) {
            ParticipantView current = views.get(i);
            int cursor = i - 1;
            while (cursor >= 0 && compareParticipantViews(views.get(cursor), current) > 0) {
                views.set(cursor + 1, views.get(cursor));
                cursor--;
            }
            views.set(cursor + 1, current);
        }
    }

    private int compareParticipantViews(ParticipantView left, ParticipantView right) {
        int leftTeam = left.team == null ? GodTeam.values().length : left.team.ordinal();
        int rightTeam = right.team == null ? GodTeam.values().length : right.team.ordinal();
        if (leftTeam != rightTeam) {
            return leftTeam - rightTeam;
        }
        return left.playerName.compareToIgnoreCase(right.playerName);
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

    private String normalizeSubcommand(String sub) {
        return CommandCatalog.normalize(sub);
    }

    private static final class AssignedAbilityView {
        private final String playerName;
        private final boolean online;
        private final AbilityDefinition ability;

        private AssignedAbilityView(String playerName, boolean online, AbilityDefinition ability) {
            this.playerName = playerName;
            this.online = online;
            this.ability = ability;
        }
    }

    private static final class ParticipantView {
        private final String playerName;
        private final boolean online;
        private final boolean observer;
        private final GodTeam team;
        private final AbilityDefinition ability;
        private final int kills;

        private ParticipantView(String playerName, boolean online, boolean observer, GodTeam team, AbilityDefinition ability, int kills) {
            this.playerName = playerName;
            this.online = online;
            this.observer = observer;
            this.team = team;
            this.ability = ability;
            this.kills = kills;
        }
    }

}
