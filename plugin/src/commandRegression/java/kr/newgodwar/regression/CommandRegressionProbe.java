package kr.newgodwar.regression;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.ability.AbilitySession;
import kr.newgodwar.ability.api.AbilityDefinition;
import kr.newgodwar.ability.api.GodAbility;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import kr.newgodwar.gui.SettingsPage;
import net.md_5.bungee.api.chat.BaseComponent;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Run only on an isolated Paper server; verifies actual command registration and effects. */
public final class CommandRegressionProbe extends JavaPlugin {
    private NewGodWarPlugin core;
    private Map<UUID, AbilitySession> sessions;
    private final UUID first = UUID.randomUUID();
    private final UUID second = UUID.randomUUID();
    private final Counter firstAbility = new Counter();
    private final Counter secondAbility = new Counter();
    private final List<String> messages = new ArrayList<String>();
    private Inventory displayed;
    private int componentMessages;

    @Override public void onEnable() {
        Bukkit.getScheduler().runTask(this, () -> {
            try {
                initialize();
                verifiesResetScopes();
                verifiesPermissionBoundaries();
                verifiesHelpAndCompletion();
                verifiesSettingsDestinations();
                TrainingDummyChecks.run(this, core);
            } catch (Throwable error) {
                getLogger().log(java.util.logging.Level.SEVERE, "COMMAND REGRESSION FAILED", error);
            } finally {
                if (sessions != null) { sessions.remove(first); sessions.remove(second); }
            }
        });
    }

    @SuppressWarnings("unchecked") private void initialize() throws Exception {
        core = (NewGodWarPlugin) Bukkit.getPluginManager().getPlugin("NewGodWar");
        core.getConfig().set("scoreboard.enabled", false);
        core.getConfig().set("scoreboard.team-prefixes", false);
        Field field = core.abilities().getClass().getDeclaredField("assignments");
        field.setAccessible(true);
        sessions = (Map<UUID, AbilitySession>) field.get(core.abilities());
        AbilityDefinition definition = core.abilities().registry().all().iterator().next();
        sessions.put(first, new AbilitySession(definition, firstAbility));
        sessions.put(second, new AbilitySession(definition, secondAbility));
    }

    private void verifiesResetScopes() {
        CommandSender console = sender(true, false);
        for (String command : new String[] {
            "gw clear __missing__", "gw clear", "gcd", "gcd __missing__",
            "gw ability cooldown reset __missing__", "gw ability cooldown reset",
            "gw ability cooldown reset all extra", "gw ability cooldown all",
            "gw ability show random", "gw ability show reset", "a show random"
        }) {
            dispatch(console, command);
            check(firstAbility.resets == 0 && secondAbility.resets == 0, "invalid reset changed abilities: " + command);
            check(sessions.containsKey(first) && sessions.containsKey(second), "view command removed ability assignments: " + command);
        }
        for (String command : new String[] {"gw ability cooldown reset all", "gcd all", "gw 능력 쿨타임 초기화 전체", "a cd reset all"}) {
            int before = firstAbility.resets;
            dispatch(console, command);
            check(firstAbility.resets == before + 1 && secondAbility.resets == before + 1, "all reset: " + command);
        }
        int otherResets = secondAbility.resets;
        dispatch(sender(true, true), "gw ability cooldown reset self");
        check(firstAbility.resets == otherResets + 1 && secondAbility.resets == otherResets, "self reset leaked to other player");
        getLogger().info("PASS cooldown typo, missing console target, extra arguments, self/all scopes");
    }

    private void verifiesPermissionBoundaries() {
        int before = firstAbility.resets;
        for (String command : new String[] {"gw clear all", "gw cd all", "gw ability cooldown reset all", "gw 능력 쿨타임 초기화 전체", "a cd reset all", "gcd all",
            "gw dummy", "gw game dummy", "gw 더미"}) {
            messages.clear();
            dispatch(sender(false, false), command);
            check(firstAbility.resets == before, "permission bypass: " + command);
            check(messages.toString().contains("권한"), "missing permission response: " + command);
            check(core.trainingDummies().players().isEmpty(), "unprivileged command spawned dummy: " + command);
        }
        getLogger().info("PASS administrator permission across structured, Korean and shortcut commands");
    }

    private void verifiesHelpAndCompletion() {
        CommandSender admin = sender(true, false);
        PluginCommand command = core.getCommand("godwar");
        check(command.getTabCompleter().onTabComplete(admin, command, "gw", new String[] {"ability", "cooldown", "reset", ""}).contains("all"), "target completion");
        check(command.getTabCompleter().onTabComplete(sender(false, false), command, "gw", new String[] {"ability", "cooldown", ""}).isEmpty(), "private completion");
        messages.clear();
        dispatch(admin, "ghelp 쿨타임");
        check(messages.toString().contains("ability cooldown reset"), "searchable structured help");
        for (String name : new String[] {"ghelp", "gstatus", "gconfirm", "greroll", "gstart", "gstop", "gskip", "gautoteam", "gjoin", "gplayers", "gmap", "gworld", "gcd", "gmenu", "gkit"}) {
            check(core.getCommand(name) != null && core.getCommand(name).getExecutor() == command.getExecutor(), "shortcut registration: " + name);
        }
        getLogger().info("PASS shortcut registration, help search and stage completion");
    }

    private void dispatch(CommandSender sender, String input) {
        String[] parts = input.split(" ");
        PluginCommand command = core.getCommand(parts[0].equals("gw") ? "godwar" : parts[0]);
        check(command != null, "missing command " + parts[0]);
        command.getExecutor().onCommand(sender, command, parts[0], java.util.Arrays.copyOfRange(parts, 1, parts.length));
    }

    @SuppressWarnings("unchecked") private void verifiesSettingsDestinations() throws Exception {
        Object gui = field(core, "settingsGui");
        Map<UUID, Object> views = (Map<UUID, Object>) field(gui, "openViews");
        Map<UUID, String> selected = (Map<UUID, String>) field(gui, "selectedTeamIds");
        Map<UUID, String> renames = (Map<UUID, String>) field(gui, "pendingTeamRenameIds");
        CommandSender player = sender(true, true);
        String[] expected = {"MAIN", "GAME", "TEAM", "WORLD", "WORLD_CORE", "PICKAXE_UNLOCK", "DISPLAY", "GAMBLING", "GAMBLING_NORMAL"};
        int index = 0;
        for (SettingsPage page : SettingsPage.values()) {
            displayed = null;
            dispatch(player, "gw gui " + page.id());
            check(displayed != null, "GUI did not open: " + page.id());
            if (page != SettingsPage.ITEMS) {
                check(expected[index++].equals(views.get(first).toString()), "wrong settings screen: " + page.id());
                ItemStack heading = displayed.getItem(4);
                check(heading.getItemMeta().getLore().toString().contains("/gw gui"), "missing GUI shortcut hint: " + page.id());
            }
        }
        for (String command : new String[] {"gw gui team red", "gmenu team red", "gw settings open team red", "gw 설정 열기 팀 red"}) {
            renames.put(first, "blue");
            dispatch(player, command);
            check("TEAM_DETAIL".equals(views.get(first).toString()) && "red".equals(selected.get(first)), "team detail route: " + command);
            check(!renames.containsKey(first), "stale rename survived direct navigation");
        }
        dispatch(player, "gmenu rewards");
        check("GAMBLING_NORMAL".equals(views.get(first).toString()), "gmenu rewards must open a screen");
        dispatch(player, "gw settings open urf");
        check("DISPLAY".equals(views.get(first).toString()), "URF screen alias");
        dispatch(player, "gw help");
        dispatch(player, "gw help ability 2");
        check(componentMessages > 0, "interactive help was not delivered as chat components");
        for (String command : new String[] {"gw gui typo", "gw gui team __missing__", "gw gui game extra", "gw gui stop_confirm"}) {
            displayed = null;
            dispatch(player, command);
            check(displayed == null, "invalid destination opened a GUI: " + command);
        }
        displayed = null;
        dispatch(sender(false, true), "gmenu game");
        check(displayed == null, "non-admin opened settings");
        dispatch(sender(true, false), "gw gui game");
        check(displayed == null, "console opened settings");
        views.remove(first);
        selected.remove(first);
        ((java.util.Set<UUID>) field(gui, "openViewers")).remove(first);
        getLogger().info("PASS all 10 GUI destinations, team detail, aliases, invalid inputs and permissions");
    }

    private Object field(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    private CommandSender sender(boolean admin, boolean player) {
        Class<?> type = player ? Player.class : CommandSender.class;
        return (CommandSender) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "hasPermission": case "isOp": return admin;
                case "getUniqueId": return first;
                case "getName": case "getDisplayName": return "CommandProbe";
                case "getWorld": return Bukkit.getWorlds().get(0);
                case "getLocation": return Bukkit.getWorlds().get(0).getSpawnLocation();
                case "openInventory": displayed = (Inventory) args[0]; return null;
                case "getServer": return Bukkit.getServer();
                case "spigot": return new Player.Spigot() {
                    @Override public void sendMessage(BaseComponent... components) { captureComponents(components); }
                    @Override public void sendMessage(BaseComponent component) { captureComponents(new BaseComponent[] {component}); }
                };
                case "sendMessage":
                    for (Object arg : args) {
                        if (arg instanceof String) messages.add(ChatColor.stripColor((String) arg));
                        if (arg instanceof String[]) for (String message : (String[]) arg) messages.add(ChatColor.stripColor(message));
                    }
                    return null;
                case "hashCode": return first.hashCode();
                case "equals": return proxy == args[0];
                default:
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == int.class) return 0;
                    if (method.getReturnType() == long.class) return 0L;
                    return null;
            }
        });
    }

    private void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private void captureComponents(BaseComponent[] components) {
        componentMessages++;
        messages.add(ChatColor.stripColor(BaseComponent.toLegacyText(components)));
    }
    private static final class Counter implements GodAbility {
        private int resets;
        @Override public void clearCooldowns() { resets++; }
    }
}
