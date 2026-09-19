package kr.newgodwar.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;
import kr.newgodwar.gui.SettingsPage;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;

import java.io.InputStreamReader;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.*;

public final class CommandRoutingTest {
    private final GodWarCommand executor = new GodWarCommand(null, null, null, null, null, null, null);
    private final Command root = new Command("godwar") {
        @Override public boolean execute(CommandSender sender, String label, String[] args) { return true; }
    };

    @Test public void structuredAndKoreanRoutesReachExistingHandlers() {
        route("start", "game", "start");
        route("start", "게임", "시작");
        route("join red Steve", "team", "join", "red", "Steve");
        route("join red Steve", "team", "red", "Steve");
        route("changeteam Steve blue", "팀", "변경", "Steve", "blue");
        route("clear Steve", "ability", "cooldown", "reset", "Steve");
        route("clear all", "a", "cd", "clear", "all");
        route("clear 전체", "능력", "쿨타임", "초기화", "전체");
        route("clear self", "cooldown", "reset", "self");
        route("defaultitems set 1 ICE 2", "settings", "items", "set", "1", "ICE", "2");
        route("setspawn red", "setup", "spawn", "red");
        route("reload", "server", "reload");
        route("randomability Steve", "ability", "random", "Steve");
        route("ability zeus Steve", "a", "zeus", "Steve");
        route("setability Steve zeus", "a", "assign", "Steve", "zeus");
        route("ability Steve", "a", "show", "Steve");
    }

    @Test public void incompleteAndUnknownGroupActionsOnlyShowHelp() {
        for (String[] args : Arrays.asList(new String[] {"game"}, new String[] {"game", "strat"},
            new String[] {"a", "cooldown"}, new String[] {"a", "cooldown", "all"},
            new String[] {"settings", "typo"})) {
            assertNotNull(Arrays.toString(args), CommandTree.resolve(args).help);
        }
        assertArrayEquals(new String[] {"settings"}, CommandTree.resolve(new String[] {"settings"}).args);
    }

    @Test public void explicitAbilityViewDoesNotReinterpretPlayerNamesAsActions() {
        for (String player : Arrays.asList("random", "reset", "list", "cooldown", "help")) {
            CommandTree.Result result = CommandTree.resolve(new String[] {"ability", "show", player});
            assertTrue(player, result.abilityView);
            assertArrayEquals(new String[] {"ability", player}, result.args);
        }
    }

    @Test public void legacyAliasesRetainTheirMeanings() {
        assertEquals("setspawn", CommandCatalog.normalize("s"));
        assertEquals("settemple", CommandCatalog.normalize("d"));
        assertEquals("join", CommandCatalog.normalize("t"));
        assertEquals("join", CommandCatalog.normalize("team"));
        assertEquals("setlobby", CommandCatalog.normalize("lobby"));
        assertEquals("rerolls", CommandCatalog.normalize("reroll"));
        assertEquals("rerolls", CommandCatalog.normalize("재추첨"));
        assertEquals("no", CommandCatalog.normalize("rr"));
        assertEquals("clear", CommandCatalog.normalize("c"));
        assertEquals("settings", CommandCatalog.normalize("gui"));
        assertEquals("participants", CommandCatalog.normalize("list"));
    }

    @Test public void helpSearchFiltersByCategoryAndPermission() {
        for (CommandCatalog.Entry entry : CommandCatalog.helpEntries(false, "")) assertFalse(entry.admin);
        for (CommandCatalog.Entry entry : CommandCatalog.helpEntries(true, "game")) assertEquals("game", entry.category);
        assertEquals("clear", CommandCatalog.helpEntries(true, "쿨타임 초기화").get(0).name);
        assertTrue(CommandCatalog.helpEntries(false, "쿨타임 초기화").isEmpty());
        assertTrue(CommandCatalog.suggest("star", false).isEmpty());
        assertTrue(CommandCatalog.suggest("star", true).contains("start"));
        assertEquals("randomability", CommandCatalog.helpEntries(true, "random").get(0).name);
        assertEquals("clear", CommandCatalog.helpEntries(true, "cooldown").get(0).name);
    }

    @Test public void everyAdvertisedPathAndAliasReachesItsCommand() {
        for (CommandCatalog.Entry entry : CommandCatalog.helpEntries(true, "")) {
            for (String alias : entry.names()) assertEquals(entry.name, CommandCatalog.normalize(alias));
            String[] path = CommandTree.preferredPath(entry.name).split(" ");
            CommandTree.Result result = CommandTree.resolve(path);
            assertNull(entry.name, result.help);
            assertEquals(entry.name, CommandCatalog.normalize(result.args[0]));
        }
    }

    @Test public void shortcutsAreRegisteredWithMatchingPermissionsAndPreserveArguments() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(
            getClass().getResourceAsStream("/plugin.yml"), StandardCharsets.UTF_8));
        for (String name : CommandCatalog.shortcutCommands()) {
            assertTrue(name, yaml.isConfigurationSection("commands." + name));
            String[] args = new String[] {"Steve", "all"};
            String[] result = CommandCatalog.expandShortcut(name, args);
            assertEquals("Steve", result[1]);
            assertEquals("all", result[2]);
            assertArrayEquals(new String[] {"Steve", "all"}, args);
            assertEquals(name, CommandCatalog.requiresAdmin(result[0]), "newgodwar.admin".equals(yaml.getString("commands." + name + ".permission")));
        }
        for (String name : yaml.getConfigurationSection("commands").getKeys(false)) {
            if (!Arrays.asList("godwar", "t", "teamchat", "x", "a", "gamble").contains(name)) {
                assertTrue(name, CommandCatalog.shortcutCommands().contains(name));
            }
        }
    }

    @Test public void completionFollowsEachStageAndHidesAdminActions() {
        assertTrue(complete(true, "ability", "cooldown", "").contains("reset"));
        assertTrue(complete(false, "ability", "cooldown", "").isEmpty());
        assertTrue(complete(false, "a", "cd", "reset", "").isEmpty());
        assertEquals(Arrays.asList("0", "3", "5", "10", "15", "30"), complete(true, "game", "skip", ""));
        assertEquals(Arrays.asList("0", "1", "2", "3", "5"), complete(true, "settings", "rerolls", ""));
        assertTrue(complete(false, "game", "").contains("status"));
        assertFalse(complete(false, "game", "").contains("start"));
        assertFalse(complete(false, "").contains("gcd"));
        assertFalse(complete(false, "").contains("cd"));
        assertTrue(complete(true, "설").contains("설정"));
    }

    @Test public void helpAndGroupHelpWorkForConsoleWithoutGameDependencies() {
        List<String> messages = new ArrayList<String>();
        assertTrue(executor.onCommand(sender(true, messages), root, "gw", new String[] {"도움말", "game"}));
        assertTrue(messages.toString().contains("/gw game start"));
        assertFalse(messages.toString().contains("/gw team join"));
        messages.clear();
        executor.onCommand(sender(false, messages), root, "gw", new String[] {"game", "help"});
        assertTrue(messages.toString().contains("/gw game status"));
        assertFalse(messages.toString().contains("/gw game start"));
        messages.clear();
        executor.onCommand(sender(false, messages), root, "gw", new String[] {"a", "cooldown"});
        assertTrue(messages.toString().contains("관리자 권한"));
    }

    @Test public void aliasesAreIndependentOfServerLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            assertEquals("participants", CommandCatalog.normalize("LIST"));
            route("skip 3", "GAME", "SKIP", "3");
        } finally { Locale.setDefault(previous); }
    }

    @Test public void guiRoutesPreserveTheSelectedScreenAndTeam() {
        route("settings game", "settings", "open", "game");
        route("settings team red", "settings", "open", "team", "red");
        route("settings team red", "setting", "team", "red");
        route("settings 게임", "설정", "열기", "게임");
        assertArrayEquals(new String[] {"gui", "rewards"}, CommandCatalog.expandShortcut("gmenu", new String[] {"rewards"}));
        // Existing settings actions still edit values; explicit open always navigates.
        route("urf 80%", "settings", "urf", "80%");
        route("settings urf", "settings", "open", "urf");
        route("gamblereward normal add hand", "settings", "rewards", "normal", "add", "hand");
        assertEquals(SettingsPage.PROTECTION, SettingsPage.parse("곡괭이"));
        assertEquals(SettingsPage.DISPLAY, SettingsPage.parse("urf"));
        assertNull(SettingsPage.parse("stop_confirm"));
    }

    @Test public void guiAndHelpCompleteAtTheCorrectDepth() {
        assertTrue(complete(true, "gui", "").contains("game"));
        assertTrue(complete(true, "settings", "open", "").contains("protection"));
        assertTrue(complete(true, "gui", "보").contains("보호"));
        assertTrue(complete(false, "gui", "").isEmpty());
        assertEquals(Arrays.asList("1", "2", "3"), complete(true, "ability", "help", ""));
        assertEquals(Arrays.asList("1", "2", "3"), complete(true, "world", "help", ""));
        assertEquals(Arrays.asList("1", "2"), complete(true, "gui", "help", ""));
    }

    @Test public void helpUsesAnOverviewAndBoundedPagesWithMultiwordSearch() {
        List<String> messages = new ArrayList<String>();
        CommandHelp.show(sender(true, messages), new String[0]);
        assertTrue(messages.toString().contains("/gw help gui"));
        assertFalse(messages.toString().contains("/gw game start"));
        messages.clear();
        CommandHelp.show(sender(false, messages), new String[0]);
        assertFalse(messages.toString().contains("/gw help gui"));
        messages.clear();
        CommandHelp.show(sender(true, messages), new String[] {"ability", "2"});
        assertTrue(messages.toString().contains("2/3"));
        assertTrue(messages.toString().contains("ability set"));
        assertFalse(messages.toString().contains("ability show"));
        assertEquals(5, messages.stream().filter(line -> line.startsWith(" /gw")).count());
        messages.clear();
        CommandHelp.show(sender(true, messages), new String[] {"world", "backup"});
        assertTrue(messages.toString().contains("world backup load"));
        messages.clear();
        CommandHelp.show(sender(true, messages), new String[] {"ability", "cooldown"});
        assertTrue(messages.toString().contains("ability cooldown reset"));
        assertNull(CommandHelp.Query.parse(new String[] {"game", "0"}));
        assertNull(CommandHelp.Query.parse(new String[] {"game", "99999999999999999999"}));
    }

    @Test public void helpCommandLinksSuggestInputAndOnlyNavigationLinksRun() {
        BaseComponent[] command = HelpChat.link("§b/gw game stop", "/gw game stop ", "게임 종료", false);
        for (BaseComponent part : command) assertEquals(ClickEvent.Action.SUGGEST_COMMAND, part.getClickEvent().getAction());
        BaseComponent[] page = HelpChat.link("§b[다음]", "/gw help ability 2", "다음 페이지", true);
        for (BaseComponent part : page) assertEquals(ClickEvent.Action.RUN_COMMAND, part.getClickEvent().getAction());
    }

    private void route(String expected, String... input) {
        CommandTree.Result result = CommandTree.resolve(input);
        assertNull(result.help);
        assertArrayEquals(expected.split(" "), result.args);
    }

    private List<String> complete(boolean admin, String... args) {
        return executor.onTabComplete(sender(admin, new ArrayList<String>()), root, "gw", args);
    }

    private CommandSender sender(boolean admin, List<String> messages) {
        return (CommandSender) Proxy.newProxyInstance(CommandSender.class.getClassLoader(), new Class<?>[] {CommandSender.class}, (proxy, method, args) -> {
            if (method.getName().equals("hasPermission")) return admin;
            if (method.getName().equals("sendMessage")) {
                if (args[0] instanceof String[]) for (String message : (String[]) args[0]) messages.add(ChatColor.stripColor(message));
                else messages.add(ChatColor.stripColor((String) args[0]));
                return null;
            }
            throw new AssertionError(method.getName());
        });
    }
}
