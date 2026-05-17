package kr.newgodwar.command;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.ability.AbilityManager;
import kr.newgodwar.ability.api.AbilityDefinition;
import kr.newgodwar.game.GameManager;
import kr.newgodwar.game.GodTeam;
import kr.newgodwar.gui.AbilityGui;
import kr.newgodwar.gui.SettingsGui;
import kr.newgodwar.util.BukkitCompat;
import kr.newgodwar.util.GameTips;
import kr.newgodwar.util.PluginUpdater;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
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

public final class GodWarCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList(
        "help", "autoteam", "join", "leave", "settemple", "setspawn", "start", "stop", "status",
        "tips", "ability", "abilities", "assignedabilities", "assignments", "participants", "players", "rerolls", "skip", "skipseconds", "blacklist", "gamerule", "target", "setability", "spectate", "unspectate", "observer",
        "reload", "update", "gui", "settings", "test", "midjoin", "info", "yes", "no", "clear", "gamble", "gamblereward", "urf"
    );
    private static final int HELP_LINES_PER_PAGE = 7;

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
            help(sender, label, args);
            return true;
        }

        boolean themachyLabel = label.equalsIgnoreCase("t");
        if (themachyLabel && args[0].equalsIgnoreCase("help")) {
            help(sender, label, args);
            return true;
        }
        if (!themachyLabel && args[0].equalsIgnoreCase("help")) {
            help(sender, label, args);
            return true;
        }
        if (isThemachyAbilityCommand(label, args[0])) {
            setAbilityThemachy(sender, args, label);
            return true;
        }

        String sub = normalizeSubcommand(args[0]);
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
        if (sub.equals("setspawn")) {
            setSpawn(sender, args);
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
        if (sub.equals("observer")) {
            observer(sender, args);
            return true;
        }
        if (sub.equals("reload")) {
            plugin.reloadConfig();
            gameManager.reloadSettings();
            plugin.messages().send(sender, "&a설정을 다시 불러왔습니다.");
            return true;
        }
        if (sub.equals("update")) {
            update(sender, args);
            return true;
        }
        if (sub.equals("gui") || sub.equals("settings")) {
            openSettings(sender);
            return true;
        }

        plugin.messages().send(sender, "&c알 수 없는 명령어입니다. /" + label + " help");
        return true;
    }

    private void help(CommandSender sender, String label, String[] args) {
        int requestedPage = parseHelpPage(args);
        List<HelpEntry> entries = helpEntries(label, sender.hasPermission("newgodwar.admin"));
        int maxPage = Math.max(1, ((entries.size() - 1) / HELP_LINES_PER_PAGE) + 1);
        int page = Math.max(1, Math.min(maxPage, requestedPage));
        int start = (page - 1) * HELP_LINES_PER_PAGE;
        int end = Math.min(entries.size(), start + HELP_LINES_PER_PAGE);

        sender.sendMessage("");
        line(sender);
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + " NewGodWar"
            + ChatColor.DARK_GRAY + " | " + ChatColor.YELLOW + "신들의 전쟁 운영 메뉴"
            + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + page + "/" + maxPage);
        sender.sendMessage(ChatColor.GRAY + "  능력 확인: " + ChatColor.AQUA + "/a"
            + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + "설정: " + ChatColor.AQUA + "/" + label + " gui"
            + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + "다음: " + ChatColor.AQUA + "/" + label + " help " + nextPage(page, maxPage));
        line(sender);
        String section = "";
        for (int i = start; i < end; i++) {
            HelpEntry entry = entries.get(i);
            if (!entry.section.equals(section)) {
                section = entry.section;
                section(sender, section);
            }
            command(sender, label, entry.usage, entry.description);
        }
        line(sender);
        sender.sendMessage(ChatColor.GRAY + "  페이지 이동: " + ChatColor.AQUA + "/" + label + " help <1-" + maxPage + ">");
        line(sender);
    }

    private int parseHelpPage(String[] args) {
        if (args.length < 2) {
            return 1;
        }
        try {
            return Integer.parseInt(args[1]);
        } catch (NumberFormatException ignored) {
            String section = args[1].toLowerCase();
            if (section.equals("game") || section.equals("게임")) return 1;
            if (section.equals("team") || section.equals("팀")) return 2;
            if (section.equals("ability") || section.equals("능력")) return 3;
            if (section.equals("admin") || section.equals("관리")) return 4;
            return 1;
        }
    }

    private int nextPage(int page, int maxPage) {
        return page >= maxPage ? 1 : page + 1;
    }

    private List<HelpEntry> helpEntries(String label, boolean admin) {
        List<HelpEntry> entries = new ArrayList<HelpEntry>();
        entries.add(new HelpEntry("게임 진행", "gui", "관리자 설정 GUI 열기"));
        entries.add(new HelpEntry("게임 진행", "start", "게임 시작 및 능력 배정"));
        entries.add(new HelpEntry("게임 진행", "test [ability]", "혼자 능력 테스트 시작"));
        entries.add(new HelpEntry("게임 진행", "stop", "게임 종료"));
        entries.add(new HelpEntry("게임 진행", "status", "현재 상태 보기"));
        entries.add(new HelpEntry("게임 진행", "tips", "서버 플레이 팁 보기"));
        entries.add(new HelpEntry("게임 진행", "autoteam", "온라인 플레이어 자동 팀 배정"));
        entries.add(new HelpEntry("팀 / 신전", "join <team> [player]", "팀 수동 배정"));
        entries.add(new HelpEntry("팀 / 신전", "midjoin [player] [team|auto]", "진행 중인 게임에 중간 참여"));
        entries.add(new HelpEntry("팀 / 신전", "leave [player]", "팀 배정 해제"));
        entries.add(new HelpEntry("팀 / 신전", "setspawn <team>", "현재 위치를 팀 스폰으로 등록"));
        entries.add(new HelpEntry("팀 / 신전", "settemple <team>", "바라보는 다이아 블록을 심장으로 등록"));
        entries.add(new HelpEntry("팀 / 신전", "info [team]", "팀원 목록 확인"));
        entries.add(new HelpEntry("능력", "yes|no", "능력 재추첨 확정 / 다시 뽑기"));
        entries.add(new HelpEntry("능력", "abilities [검색어]", "능력 도감 검색"));
        entries.add(new HelpEntry("능력", "ability [player]", "현재 능력만 보기"));
        entries.add(new HelpEntry("능력", "target <player>", "타깃형 능력 대상 지정"));
        entries.add(new HelpEntry("능력", "clear [player]", "쿨타임 초기화"));
        entries.add(new HelpEntry("능력", "gamble", "도박 GUI 열기"));
        entries.add(new HelpEntry("Themachy 호환", "t <team> [player]", "팀 수동 배정"));
        entries.add(new HelpEntry("Themachy 호환", "spawn|s <team>", "팀 스폰 등록"));
        entries.add(new HelpEntry("Themachy 호환", "dia|d <team>", "다이아 심장 등록"));
        entries.add(new HelpEntry("Themachy 호환", "set", "설정 GUI 열기"));
        entries.add(new HelpEntry("Themachy 호환", "alist [검색어]", "플레이어별 배정 능력 확인"));
        entries.add(new HelpEntry("Themachy 호환", "a skip [초]", "능력 확정 대기 종료 및 시작 카운트다운 조정"));
        entries.add(new HelpEntry("Themachy 호환", "a <ability> <player>", "능력 수동 지정"));
        entries.add(new HelpEntry("Themachy 호환", "observer [list]", "옵저버 전환 / 목록"));
        entries.add(new HelpEntry("Themachy 호환", "con", "도박 GUI 열기"));
        if (admin) {
            entries.add(new HelpEntry("운영 설정", "setability <player> <ability>", "능력 수동 지정"));
            entries.add(new HelpEntry("운영 설정", "assignedabilities [검색어]", "플레이어별 배정 능력 확인"));
            entries.add(new HelpEntry("운영 설정", "participants [검색어|팀]", "참가자 팀/능력 현황 확인"));
            entries.add(new HelpEntry("운영 설정", "skip [초]", "능력 확정 대기 종료 및 시작 카운트다운 조정"));
            entries.add(new HelpEntry("운영 설정", "rerolls <횟수>", "능력 재추첨 가능 횟수 설정"));
            entries.add(new HelpEntry("운영 설정", "skipseconds <초>", "관리자 skip 기본 카운트다운 설정"));
            entries.add(new HelpEntry("운영 설정", "urf <on|off|toggle|80%>", "우르프 모드 및 쿨타임 감소율 설정"));
            entries.add(new HelpEntry("운영 설정", "gamblereward <normal|tajja> <번호|add> hand|message|<material> [값]", "도박 당첨 아이템/멘트 변경"));
            entries.add(new HelpEntry("운영 설정", "blacklist <list|add|remove|toggle> [ability]", "랜덤 제외 능력 관리"));
            entries.add(new HelpEntry("운영 설정", "gamerule <apply|restore>", "게임룰 수동 적용 / 복구"));
            entries.add(new HelpEntry("운영 설정", "update [check|download]", "최신 버전 확인 / 업데이트 jar 다운로드"));
            entries.add(new HelpEntry("운영 설정", "reload", "config.yml 다시 불러오기"));
            entries.add(new HelpEntry("운영 설정", "spectate|unspectate <player>", "관전 모드 전환"));
        }
        entries.add(new HelpEntry("단축 명령어", "/a", "내 능력 GUI 열기"));
        entries.add(new HelpEntry("단축 명령어", "/x <player>", "타깃형 능력 대상 빠른 지정"));
        return entries;
    }

    private void status(CommandSender sender) {
        sender.sendMessage("");
        line(sender);
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + " NewGodWar 상태");
        sender.sendMessage(ChatColor.GRAY + "  상태       " + ChatColor.YELLOW + gameManager.state());
        sender.sendMessage(ChatColor.GRAY + "  버전       " + ChatColor.YELLOW + plugin.versionSupport().minecraftVersion());
        sender.sendMessage(ChatColor.GRAY + "  팀킬       " + state(plugin.getConfig().getBoolean("game.friendly-fire", false)));
        sender.sendMessage(ChatColor.GRAY + "  우르프     " + urfStatus());
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
            plugin.messages().send(sender, "&e/godwar rerolls <횟수>");
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
            plugin.messages().send(sender, "&e/godwar skipseconds <초>");
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
        if (args.length < 2) {
            plugin.messages().send(sender, "&e/godwar join <team> [player]");
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

    private void midJoin(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        GodTeam team = null;
        if (args.length >= 2) {
            if (isTeamOrAuto(args[1])) {
                team = GodTeam.parse(args[1]);
                if (team != null && !gameManager.isTeamEnabled(team)) {
                    plugin.messages().send(sender, "&c비활성화된 팀에는 중간 참여할 수 없습니다.");
                    return;
                }
            } else {
                player = Bukkit.getPlayer(args[1]);
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
            }
        }
        if (player == null) {
            plugin.messages().send(sender, args.length >= 2 && !isTeamOrAuto(args[1])
                ? "&c대상 플레이어를 찾을 수 없습니다."
                : "&c플레이어만 사용할 수 있습니다.");
            return;
        }
        if (!player.equals(sender) && !sender.hasPermission("newgodwar.admin")) {
            plugin.messages().send(sender, "&c다른 플레이어의 중간 참여는 관리자만 처리할 수 있습니다.");
            return;
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
            plugin.messages().send(sender, "&e/godwar settemple <team>");
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
        }
    }

    private void setSpawn(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            plugin.messages().send(sender, "&c플레이어만 사용할 수 있습니다.");
            return;
        }
        if (args.length < 2) {
            plugin.messages().send(sender, "&e/godwar setspawn <team>");
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

    private void urf(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.messages().send(sender, "&e/godwar urf <on|off|toggle|20%>");
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
                plugin.messages().send(sender, "&e/godwar urf <on|off|toggle|20%>");
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
        if (args.length >= 2 && args[1].equalsIgnoreCase("list")) {
            listAbilities(sender, joinArguments(args, 2));
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
            + ChatColor.GRAY + " | 등급 " + ChatColor.YELLOW + ability.gradeText()
            + ChatColor.GRAY + " - " + ability.description());
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
        } else {
            Bukkit.broadcastMessage(ChatColor.DARK_AQUA + player.getName() + ChatColor.WHITE
                + " 님께서 능력을 다시 뽑았습니다. 남은 재추첨: " + ChatColor.YELLOW + remaining + "회");
        }
    }

    private void clearCooldowns(CommandSender sender, String[] args) {
        Player target = args.length >= 2 ? Bukkit.getPlayer(args[1]) : asPlayer(sender);
        if (target == null) {
            if (sender.hasPermission("newgodwar.admin")) {
                abilityManager.clearAllCooldowns();
                gameManager.refreshAllPlayerDisplays();
                plugin.messages().send(sender, "&a모든 능력 쿨타임을 초기화했습니다.");
                return;
            }
            plugin.messages().send(sender, "&c대상 플레이어를 찾을 수 없습니다.");
            return;
        }
        if (!target.equals(sender) && !sender.hasPermission("newgodwar.admin")) {
            plugin.messages().send(sender, "&c다른 플레이어의 쿨타임은 관리자만 초기화할 수 있습니다.");
            return;
        }
        abilityManager.clearCooldowns(target);
        gameManager.refreshPlayerDisplay(target);
        plugin.messages().send(sender, "&a쿨타임을 초기화했습니다.");
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
            plugin.messages().send(sender, "&e/godwar setability <player> <ability>");
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

    private void setAbilityThemachy(CommandSender sender, String[] args, String label) {
        if (!sender.hasPermission("newgodwar.admin")) {
            plugin.messages().send(sender, "&c권한이 없습니다.");
            return;
        }
        String usagePrefix = "/" + label + " " + args[0].toLowerCase(Locale.ROOT);
        if (args.length < 2 || args[1].equalsIgnoreCase("help")) {
            sender.sendMessage(ChatColor.YELLOW + usagePrefix + " help " + ChatColor.WHITE + "모든 능력 ID를 확인합니다.");
            sender.sendMessage(ChatColor.YELLOW + usagePrefix + " random " + ChatColor.WHITE + "온라인 참가자에게 랜덤 능력을 배정합니다.");
            sender.sendMessage(ChatColor.YELLOW + usagePrefix + " remove <player> " + ChatColor.WHITE + "해당 플레이어의 능력을 삭제합니다.");
            sender.sendMessage(ChatColor.YELLOW + usagePrefix + " reset " + ChatColor.WHITE + "모든 능력을 초기화합니다.");
            sender.sendMessage(ChatColor.YELLOW + usagePrefix + " skip [초] " + ChatColor.WHITE + "능력 확정을 강제로 넘기고 시작 카운트다운을 조정합니다.");
            sender.sendMessage(ChatColor.YELLOW + usagePrefix + " cutin [player] [team|auto] " + ChatColor.WHITE + "진행 중 게임에 중간 참여합니다.");
            sender.sendMessage(ChatColor.YELLOW + usagePrefix + " <ability|number> <player> " + ChatColor.WHITE + "능력을 지정합니다.");
            listAbilities(sender, null);
            return;
        }
        if (args[1].equalsIgnoreCase("random")) {
            for (Player player : BukkitCompat.onlinePlayers()) {
                if (gameManager.teamOf(player) != null && !gameManager.isObserver(player)) {
                    abilityManager.assignRandom(player);
                }
            }
            gameManager.refreshAllPlayerDisplays();
            Bukkit.broadcastMessage(plugin.messages().prefix() + ChatColor.AQUA + "관리자가 참가자 능력을 랜덤으로 배정했습니다.");
            return;
        }
        if (args[1].equalsIgnoreCase("reset")) {
            abilityManager.clear();
            gameManager.refreshAllPlayerDisplays();
            Bukkit.broadcastMessage(plugin.messages().prefix() + ChatColor.AQUA + "관리자가 모든 능력을 초기화했습니다.");
            return;
        }
        if (args[1].equalsIgnoreCase("skip")) {
            skipAbilitySelection(sender, args, 2);
            return;
        }
        if (args[1].equalsIgnoreCase("cutin")) {
            Player player = asPlayer(sender);
            GodTeam team = null;
            if (args.length >= 3) {
                if (isTeamOrAuto(args[2])) {
                    team = GodTeam.parse(args[2]);
                    if (team != null && !gameManager.isTeamEnabled(team)) {
                        plugin.messages().send(sender, "&c비활성화된 팀에는 중간 참여할 수 없습니다.");
                        return;
                    }
                } else {
                    player = Bukkit.getPlayer(args[2]);
                    if (args.length >= 4) {
                        if (!isTeamOrAuto(args[3])) {
                            plugin.messages().send(sender, "&c팀은 " + teamUsage() + ", auto 중 하나여야 합니다.");
                            return;
                        }
                        team = GodTeam.parse(args[3]);
                        if (team != null && !gameManager.isTeamEnabled(team)) {
                            plugin.messages().send(sender, "&c비활성화된 팀에는 중간 참여할 수 없습니다.");
                            return;
                        }
                    }
                }
            }
            if (player == null) {
                plugin.messages().send(sender, args.length >= 3 && !isTeamOrAuto(args[2])
                    ? "&c대상 플레이어를 찾을 수 없습니다."
                    : "&c플레이어만 사용할 수 있습니다.");
                return;
            }
            try {
                AbilityDefinition ability = gameManager.joinMidGame(player, team);
                plugin.messages().send(sender, "&a" + player.getName() + " 님이 "
                    + (team == null ? "&f자동 팀" : teamName(team) + " 팀")
                    + "으로 중간 참여했습니다. 능력: &f" + ability.name());
            } catch (IllegalStateException ex) {
                plugin.messages().send(sender, "&c" + ex.getMessage());
            }
            return;
        }
        if (args[1].equalsIgnoreCase("remove")) {
            if (args.length < 3) {
                plugin.messages().send(sender, "&e" + usagePrefix + " remove <player>");
                return;
            }
            Player target = Bukkit.getPlayer(args[2]);
            if (target == null || abilityManager.get(target) == null) {
                plugin.messages().send(sender, "&c플레이어 또는 능력을 찾을 수 없습니다.");
                return;
            }
            abilityManager.remove(target);
            plugin.messages().send(sender, "&a능력을 삭제했습니다.");
            return;
        }
        if (args.length < 3) {
            plugin.messages().send(sender, "&e" + usagePrefix + " <ability> <player>");
            return;
        }
        AbilityDefinition ability = abilityByToken(args[1]);
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

    private void openSettings(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) {
            plugin.messages().send(sender, "&c플레이어만 GUI를 열 수 있습니다.");
            return;
        }
        settingsGui.open(player);
    }

    private void update(CommandSender sender, String[] args) {
        String action = args.length < 2 ? "check" : args[1].toLowerCase(Locale.ROOT);
        boolean download = action.equals("download") || action.equals("install") || action.equals("auto") || action.equals("다운로드");
        if (!download
            && !action.equals("check")
            && !action.equals("status")
            && !action.equals("확인")) {
            plugin.messages().send(sender, "&e/godwar update [check|download]");
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

    private void gamblingReward(CommandSender sender, String[] args) {
        if (args.length < 4) {
            plugin.messages().send(sender, "&e/godwar gamblereward <normal|tajja> <번호|add> hand|<material> [수량]");
            return;
        }
        String path = gamblingRewardPath(args[1]);
        if (path == null) {
            plugin.messages().send(sender, "&c종류는 normal 또는 tajja 중 하나여야 합니다.");
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
                plugin.messages().send(sender, "&e/godwar gamblereward " + args[1] + " " + number + " message <멘트>");
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
        if (token.equalsIgnoreCase("tajja") || token.equalsIgnoreCase("타짜")) {
            return "gambling.rewards.tajja";
        }
        return null;
    }

    private String gamblingRewardLabel(String path) {
        return path.endsWith(".tajja") ? "타짜" : "일반";
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
        return !sub.equals("ability")
            && !sub.equals("abilities")
            && !sub.equals("target")
            && !sub.equals("status")
            && !sub.equals("tips")
            && !sub.equals("join")
            && !sub.equals("leave")
            && !sub.equals("midjoin")
            && !sub.equals("info")
            && !sub.equals("yes")
            && !sub.equals("no")
            && !sub.equals("gamble");
    }

    private boolean isThemachyAbilityCommand(String label, String sub) {
        if (sub == null) {
            return false;
        }
        if (label.equalsIgnoreCase("t")) {
            return sub.equalsIgnoreCase("ability") || sub.equalsIgnoreCase("a");
        }
        return sub.equalsIgnoreCase("a");
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
            values.add("alist");
            values.add("a");
            values.add("black");
            values.add("d");
            values.add("dia");
            values.add("info");
            values.add("observer");
            values.add("s");
            values.add("set");
            values.add("spawn");
            values.add("t");
            values.add("yes");
            values.add("no");
            values.add("clear");
            values.add("con");
            return startsWith(values, args[0]);
        }
        String sub = normalizeSubcommand(args[0]);
        if (args.length == 2 && sub.equals("help")) {
            return startsWith(Arrays.asList("1", "2", "3", "4", "5", "game", "team", "ability", "admin"), args[1]);
        }
        if (args.length == 2 && sub.equals("update")) {
            return startsWith(Arrays.asList("check", "download", "status"), args[1]);
        }
        if (isThemachyAbilityCommand(alias, args[0])) {
            if (args.length == 2) {
                List<String> values = new ArrayList<String>();
                values.addAll(Arrays.asList("help", "random", "remove", "reset", "skip", "cutin"));
                values.addAll(abilityIdSuggestions());
                return startsWith(values, args[1]);
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
            if (args.length == 3 && args[1].equalsIgnoreCase("remove")) {
                return startsWith(onlinePlayerNames(), args[2]);
            }
            if (args.length == 3 && args[1].equalsIgnoreCase("skip")) {
                return startsWith(Arrays.asList("0", "3", "5", "10", "15", "30"), args[2]);
            }
            if (args.length == 3 && !isThemachyAbilityAction(args[1])) {
                return startsWith(onlinePlayerNames(), args[2]);
            }
        }
        if (args.length == 2 && (sub.equals("join") || sub.equals("settemple") || sub.equals("setspawn") || sub.equals("info"))) {
            return startsWith(teamSuggestions(false), args[1]);
        }
        if (args.length == 3 && sub.equals("join")) {
            return startsWith(onlinePlayerNames(), args[2]);
        }
        if (args.length == 2 && sub.equals("midjoin")) {
            List<String> values = new ArrayList<String>();
            values.addAll(teamSuggestions(true));
            if (sender.hasPermission("newgodwar.admin")) {
                values.addAll(onlinePlayerNames());
            }
            return startsWith(values, args[1]);
        }
        if (args.length == 3 && sub.equals("midjoin")) {
            return startsWith(teamSuggestions(true), args[2]);
        }
        if (args.length == 2 && sub.equals("ability")) {
            List<String> values = new ArrayList<String>();
            values.add("list");
            values.addAll(onlinePlayerNames());
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
        if (args.length == 2 && sub.equals("urf")) {
            return startsWith(Arrays.asList("on", "off", "toggle", "20%", "50%", "100%", "percent"), args[1]);
        }
        if (args.length == 2 && sub.equals("gamblereward")) {
            return startsWith(Arrays.asList("normal", "tajja", "일반", "타짜"), args[1]);
        }
        if (args.length == 3 && sub.equals("gamblereward")) {
            return startsWith(gamblingRewardIndexes(args[1]), args[2]);
        }
        if (args.length == 4 && sub.equals("gamblereward")) {
            List<String> values = new ArrayList<String>();
            values.addAll(Arrays.asList("hand", "message", "item", "DIAMOND", "IRON_INGOT", "BLAZE_ROD", "OAK_LOG"));
            return startsWith(values, args[3]);
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
        if (args.length == 3 && sub.equals("urf")
            && (args[1].equalsIgnoreCase("percent") || args[1].equalsIgnoreCase("rate") || args[1].equalsIgnoreCase("배율"))) {
            return startsWith(Arrays.asList("0%", "20%", "50%", "100%"), args[2]);
        }
        if (args.length == 2 && sub.equals("test")) {
            return startsWith(abilityIdSuggestions(), args[1]);
        }
        if (args.length == 2 && (sub.equals("leave") || sub.equals("clear")
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

    private List<String> onlinePlayerNames() {
        List<String> players = new ArrayList<String>();
        for (Player player : BukkitCompat.onlinePlayers()) {
            players.add(player.getName());
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
                || token.equalsIgnoreCase("random")
                || token.equalsIgnoreCase("remove")
                || token.equalsIgnoreCase("reset")
                || token.equalsIgnoreCase("skip")
                || token.equalsIgnoreCase("cutin"));
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
        String lower = sub == null ? "" : sub.toLowerCase();
        if (lower.equals("assignments") || lower.equals("assigned")) {
            return "assignedabilities";
        }
        if (lower.equals("gamblerewards") || lower.equals("gamblerwd") || lower.equals("도박상품")) {
            return "gamblereward";
        }
        if (lower.equals("participant") || lower.equals("participants")
            || lower.equals("players") || lower.equals("users")
            || lower.equals("list") || lower.equals("참가자") || lower.equals("유저")) {
            return "participants";
        }
        if (lower.equals("reroll") || lower.equals("rerolls") || lower.equals("reassign")
            || lower.equals("재추첨") || lower.equals("재지정")) {
            return "rerolls";
        }
        if (lower.equals("skipsecond") || lower.equals("skipseconds")
            || lower.equals("skiptime") || lower.equals("스킵초") || lower.equals("스킵시간")) {
            return "skipseconds";
        }
        if (lower.equals("skip") || lower.equals("스킵")) {
            return "skip";
        }
        if (lower.equals("alist")) {
            return "assignedabilities";
        }
        if (lower.equals("plist") || lower.equals("players") || lower.equals("users")
            || lower.equals("참가자") || lower.equals("유저")) {
            return "participants";
        }
        if (lower.equals("black")) {
            return "blacklist";
        }
        if (lower.equals("dia") || lower.equals("d")) {
            return "settemple";
        }
        if (lower.equals("spawn") || lower.equals("s")) {
            return "setspawn";
        }
        if (lower.equals("info") || lower.equals("i")) {
            return "info";
        }
        if (lower.equals("tip") || lower.equals("tips") || lower.equals("팁")) {
            return "tips";
        }
        if (lower.equals("update") || lower.equals("updates") || lower.equals("업데이트")) {
            return "update";
        }
        if (lower.equals("clear") || lower.equals("c")) {
            return "clear";
        }
        if (lower.equals("con")) {
            return "gamble";
        }
        if (lower.equals("set")) {
            return "settings";
        }
        if (lower.equals("team") || lower.equals("t")) {
            return "join";
        }
        return lower;
    }

    private static final class HelpEntry {
        private final String section;
        private final String usage;
        private final String description;

        private HelpEntry(String section, String usage, String description) {
            this.section = section;
            this.usage = usage;
            this.description = description;
        }
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
