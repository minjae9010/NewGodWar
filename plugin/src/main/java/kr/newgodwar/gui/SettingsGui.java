package kr.newgodwar.gui;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.game.GameManager;
import kr.newgodwar.game.GodTeam;
import kr.newgodwar.game.KilltimeMode;
import kr.newgodwar.util.BukkitCompat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SettingsGui implements Listener {

    private static final int SIZE = 54;
    private static final int BACK_SLOT = 49;
    private static final int HOME_SLOT = 45;
    private static final int HELP_SLOT = 47;
    private static final int TEAM_PREV_SLOT = 21;
    private static final int TEAM_NEXT_SLOT = 23;
    private static final int TEAM_ADD_SLOT = 24;
    private static final int CLOSE_SLOT = 53;
    private static final int[] TEAM_LIST_SLOTS = new int[] {10, 11, 12, 13, 14, 15, 16};
    private static final int TEAMS_PER_PAGE = TEAM_LIST_SLOTS.length;
    private static final int REWARDS_PER_PAGE = 21;
    private static final ChatColor[] TEAM_COLORS = new ChatColor[] {
        ChatColor.RED,
        ChatColor.BLUE,
        ChatColor.GREEN,
        ChatColor.YELLOW,
        ChatColor.AQUA,
        ChatColor.GOLD,
        ChatColor.LIGHT_PURPLE,
        ChatColor.WHITE,
        ChatColor.GRAY,
        ChatColor.DARK_RED,
        ChatColor.DARK_BLUE,
        ChatColor.DARK_GREEN,
        ChatColor.DARK_AQUA,
        ChatColor.DARK_PURPLE,
        ChatColor.DARK_GRAY,
        ChatColor.BLACK
    };
    private static final Set<String> TRUE_DEFAULT_TOGGLES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
        "core.protect-diamond-from-explosion",
        "core.forbid-diamond-pickaxe",
        "core.require-empty-hand",
        "game.ability-roll-message",
        "game.allow-mid-join",
        "game.auto-balance-teams",
        "game.clear-inventory",
        "game.fast-start",
        "game.give-skyblock-items",
        "game.ignore-bed",
        "game.remove-entities",
        "game.reveal-abilities-on-end",
        "game.select-right",
        "gambling.enabled",
        "gamerules.enabled",
        "gamerules.restore-on-stop",
        "scoreboard.enabled",
        "scoreboard.team-prefixes",
        "ui.resource-pack.enabled",
        "gamerules.rules.doImmediateRespawn",
        "gamerules.rules.naturalRegeneration",
        "world.autosave",
        "world.reset-game-world-on-stop"
    )));

    private final NewGodWarPlugin plugin;
    private final GameManager gameManager;
    private final StarterItemsGui starterItemsGui;
    private final Set<UUID> openViewers = new HashSet<UUID>();
    private final Set<UUID> refreshingViewers = new HashSet<UUID>();
    private final Map<UUID, SettingsView> openViews = new HashMap<UUID, SettingsView>();
    private final Map<UUID, Integer> teamPages = new HashMap<UUID, Integer>();
    private final Map<UUID, Integer> rewardPages = new HashMap<UUID, Integer>();
    private final Map<UUID, String> selectedTeamIds = new HashMap<UUID, String>();
    private final Map<UUID, String> pendingTeamRenameIds = new ConcurrentHashMap<UUID, String>();

    public SettingsGui(NewGodWarPlugin plugin, GameManager gameManager, StarterItemsGui starterItemsGui) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.starterItemsGui = starterItemsGui;
    }

    public void open(Player player) {
        openPage(player, SettingsPage.MAIN, null);
    }

    public void openWorld(Player player) {
        openPage(player, SettingsPage.WORLD, null);
    }

    public void openPage(Player player, SettingsPage page, GodTeam team) {
        if (!player.hasPermission("newgodwar.admin")) {
            plugin.messages().send(player, "&c권한이 없습니다.");
            return;
        }
        UUID uuid = player.getUniqueId();
        // A direct navigation cancels a pending chat rename and any previous team selection.
        pendingTeamRenameIds.remove(uuid);
        selectedTeamIds.remove(uuid);
        teamPages.remove(uuid);
        rewardPages.remove(uuid);
        if (page == SettingsPage.ITEMS) {
            starterItemsGui.open(player);
            return;
        }
        if (page == SettingsPage.TEAM && team != null) selectedTeamIds.put(uuid, team.id());
        open(player, page == SettingsPage.TEAM && team != null ? SettingsView.TEAM_DETAIL : page.view);
    }

    private void open(Player player, SettingsView view) {
        Inventory inventory = Bukkit.createInventory(player, SIZE, GuiTheme.title(player, plugin, view.title, SIZE));
        fill(inventory, view, player);
        // Closing the previous inventory fires synchronously; preserve destination state during it.
        boolean alreadyRefreshing = !refreshingViewers.add(player.getUniqueId());
        try {
            player.openInventory(inventory);
        } finally {
            if (!alreadyRefreshing) refreshingViewers.remove(player.getUniqueId());
        }
        openViewers.add(player.getUniqueId());
        openViews.put(player.getUniqueId(), view);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player) || !isSettingsInventory(event)) {
            return;
        }
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        if (!player.hasPermission("newgodwar.admin")) {
            player.closeInventory();
            plugin.messages().send(player, "&c권한이 없습니다.");
            return;
        }

        SettingsView view = viewOf(event.getView().getTitle());
        if (view != currentView(player)) return;
        handle(player, view, event.getRawSlot(), event.getClick());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (isSettingsInventory(event)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        final String teamId = pendingTeamRenameIds.remove(uuid);
        if (teamId == null) {
            return;
        }
        event.setCancelled(true);
        final Player player = event.getPlayer();
        final String name = event.getMessage().trim();
        Bukkit.getScheduler().runTask(plugin, () -> finishTeamRename(player, teamId, name));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // A rename prompt must not capture the first chat message after reconnecting.
        UUID uuid = event.getPlayer().getUniqueId();
        pendingTeamRenameIds.remove(uuid);
        refreshingViewers.remove(uuid);
        openViewers.remove(uuid);
        openViews.remove(uuid);
        teamPages.remove(uuid);
        rewardPages.remove(uuid);
        selectedTeamIds.remove(uuid);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (refreshingViewers.contains(uuid)) {
            return;
        }
        openViewers.remove(uuid);
        openViews.remove(uuid);
        teamPages.remove(uuid);
        rewardPages.remove(uuid);
        selectedTeamIds.remove(uuid);
    }

    private boolean isSettingsInventory(InventoryClickEvent event) {
        return event.getView() != null
            && openViewers.contains(event.getWhoClicked().getUniqueId())
            && viewOf(event.getView().getTitle()) != null;
    }

    private boolean isSettingsInventory(InventoryDragEvent event) {
        return event.getView() != null
            && openViewers.contains(event.getWhoClicked().getUniqueId())
            && viewOf(event.getView().getTitle()) != null;
    }

    private SettingsView viewOf(String title) {
        for (SettingsView view : SettingsView.values()) {
            if (GuiTitle.matches(view.title, title, SIZE)) {
                return view;
            }
        }
        return null;
    }

    private void handle(Player player, SettingsView view, int slot, ClickType click) {
        if (slot == CLOSE_SLOT) {
            player.closeInventory();
            return;
        }
        if (slot == BACK_SLOT && view != SettingsView.MAIN) {
            switchView(player, backView(view));
            reopen(player, currentView(player));
            return;
        }
        if (slot == HOME_SLOT) {
            switchView(player, SettingsView.MAIN);
            reopen(player, SettingsView.MAIN);
            return;
        }
        if (slot == (view == SettingsView.MAIN ? 46 : HELP_SLOT)) {
            plugin.messages().send(player, "&b설정 위치: &f" + breadcrumb(view));
            plugin.messages().send(player, "&7" + pageGuide(view));
            plugin.messages().send(player, "&e/gw gui &7뒤에 game, combat, team, world, core, rules, display, gambling, items를 입력하면 바로 열려요.");
            return;
        }

        slot = ChestLayout.logicalSlot(view, slot);
        if (slot < 0) return;

        if (view == SettingsView.STOP_CONFIRM) {
            if (slot == 10 && click == ClickType.LEFT) {
                // Consume this confirmation before executing; duplicate clicks cannot stop twice.
                switchView(player, SettingsView.GAME);
                gameManager.stop(true);
                plugin.messages().send(player, "&a게임을 종료했습니다.");
            } else if (slot == 16) {
                switchView(player, SettingsView.GAME);
            }
        } else if (view == SettingsView.MAIN) {
            handleMain(player, slot);
        } else if (view == SettingsView.GAME) {
            handleGame(player, slot, click);
        } else if (view == SettingsView.TEAM) {
            handleTeam(player, slot, click);
        } else if (view == SettingsView.TEAM_DETAIL) {
            handleTeamDetail(player, slot, click);
        } else if (view == SettingsView.WORLD) {
            handleWorld(player, slot, click);
        } else if (view == SettingsView.WORLD_CORE) {
            handleWorldCore(player, slot, click);
        } else if (view == SettingsView.WORLD_RULES) {
            handleWorldRules(player, slot);
        } else if (view == SettingsView.PICKAXE_UNLOCK) {
            handlePickaxeUnlock(slot, click);
        } else if (view == SettingsView.DISPLAY) {
            handleDisplay(player, slot, click);
        } else if (view == SettingsView.COMBAT) {
            handleCombat(player, slot, click);
        } else if (view == SettingsView.GAMBLING) {
            handleGambling(player, slot, click);
        } else if (view == SettingsView.GAMBLING_NORMAL) {
            handleRewardChance(player, "gambling.rewards.normal", slot, click);
        }

        reopen(player, currentView(player));
    }

    private void handleMain(Player player, int slot) {
        if (slot == 4) {
            switchView(player, SettingsView.TEAM);
        } else if (slot == 10) {
            switchView(player, SettingsView.GAME);
        } else if (slot == 11) {
            switchView(player, SettingsView.TEAM);
        } else if (slot == 12) {
            switchView(player, SettingsView.WORLD);
        } else if (slot == 13) {
            switchView(player, SettingsView.WORLD_CORE);
        } else if (slot == 14) {
            switchView(player, SettingsView.COMBAT);
        } else if (slot == 15) {
            switchView(player, SettingsView.DISPLAY);
        } else if (slot == 16) {
            switchView(player, SettingsView.GAMBLING);
        } else if (slot == 17) {
            switchView(player, SettingsView.WORLD_RULES);
        } else if (slot == 23) {
            player.closeInventory();
            starterItemsGui.open(player);
        } else if (slot == 24) {
            plugin.reloadConfig();
            plugin.removeLegacyTajjaGamblingRewards();
            gameManager.reloadSettings();
            plugin.messages().send(player, "&a설정을 다시 불러왔습니다.");
        }
    }

    private void handleTeam(Player player, int slot, ClickType click) {
        if (slot == TEAM_PREV_SLOT) {
            changeTeamPage(player, -1);
        } else if (slot == TEAM_NEXT_SLOT) {
            changeTeamPage(player, 1);
        } else if (slot == TEAM_ADD_SLOT) {
            GodTeam created = GodTeam.create(plugin.getConfig(), null);
            plugin.saveConfig();
            gameManager.reloadSettings();
            setTeamPage(player, lastTeamPage());
            plugin.messages().send(player, "&a" + gameManager.teamColoredName(created) + " 팀을 추가했습니다.");
        } else if (isTeamListSlot(slot)) {
            GodTeam team = teamAt(player, slot);
            if (team == null) {
                return;
            }
            selectedTeamIds.put(player.getUniqueId(), team.id());
            switchView(player, SettingsView.TEAM_DETAIL);
        }
    }

    private void handleTeamDetail(Player player, int slot, ClickType click) {
        GodTeam team = selectedTeam(player);
        if (team == null) {
            switchView(player, SettingsView.TEAM);
            return;
        }
        if (slot == 10) {
            if (click == ClickType.SHIFT_RIGHT) {
                removeTeam(player, team);
                switchView(player, SettingsView.TEAM);
                return;
            }
            cycleTeamColor(team, click == ClickType.RIGHT ? -1 : 1);
            gameManager.refreshAllPlayerDisplays();
            plugin.messages().send(player, "&a" + gameManager.teamColoredName(team) + " 팀 색상을 변경했습니다.");
        } else if (slot == 12) {
            gameManager.setSpawn(team, player.getLocation());
            plugin.messages().send(player, "&a현재 위치를 " + gameManager.teamColoredName(team) + " 팀 스폰으로 등록했습니다.");
        } else if (slot == 13) {
            startTeamRename(player, team);
        } else if (slot == 14) {
            setTempleFromTarget(player, team);
        } else if (slot == 16) {
            toggleTeamEnabled(player, team);
        }
    }

    private void handleGame(Player player, int slot, ClickType click) {
        if (slot == 4) {
            switchView(player, SettingsView.TEAM);
        } else if (slot == 0) {
            changeInt("game.min-players", -1, 1, 100);
        } else if (slot == 2) {
            changeInt("game.min-players", 1, 1, 100);
        } else if (slot == 5) {
            changeInt("game.ability-reroll-count", -1, 0, 100);
        } else if (slot == 7) {
            changeInt("game.ability-reroll-count", 1, 0, 100);
        } else if (slot == 9) {
            toggle("game.clear-inventory");
        } else if (slot == 10) {
            if (click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT) {
                player.closeInventory();
                starterItemsGui.open(player);
            } else {
                toggle("game.give-skyblock-items");
            }
        } else if (slot == 11) {
            toggle("game.fast-start");
        } else if (slot == 12) {
            toggle("game.select-right");
        } else if (slot == 13) {
            toggle("game.auto-balance-teams");
        } else if (slot == 14) {
            toggle("game.allow-mid-join");
            gameManager.refreshAllPlayerDisplays();
        } else if (slot == 15) {
            changeInt("game.skip-ready-countdown-seconds", -1, 0, 600);
        } else if (slot == 17) {
            changeInt("game.skip-ready-countdown-seconds", 1, 0, 600);
        } else if (slot == 18) {
            try {
                gameManager.start();
            } catch (IllegalStateException ex) {
                plugin.messages().send(player, "&c" + ex.getMessage());
            }
        } else if (slot == 19) {
            toggle("game.reveal-abilities-on-end");
        } else if (slot == 20) {
            switchView(player, SettingsView.STOP_CONFIRM);
        } else if (slot == 21) {
            gameManager.autoBalance();
            plugin.messages().send(player, "&a온라인 플레이어를 자동으로 팀 배정했습니다.");
        } else if (slot == 22) {
            toggle("game.remove-entities");
        }
    }

    private void handleWorld(Player player, int slot, ClickType click) {
        if (slot == 1) {
            toggle("world.autosave");
        } else if (slot == 2) {
            toggle("world.spawn-animals");
        } else if (slot == 3) {
            toggle("world.spawn-monsters");
        } else if (slot == 5) {
            changeWorldStartTime(click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT ? -6000 : -1000);
        } else if (slot == 6) {
            plugin.getConfig().set("world.start-time", (int) (player.getWorld().getTime() % 24000L));
            plugin.saveConfig();
            plugin.messages().send(player, "&a게임 시작 시간을 현재 월드 시간으로 저장했습니다.");
        } else if (slot == 7) {
            changeWorldStartTime(click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT ? 6000 : 1000);
        } else if (slot == 9) {
            cycleWorldDifficulty(-1);
        } else if (slot == 10) {
            cycleWorldDifficulty(click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT ? -1 : 1);
        } else if (slot == 11) {
            cycleWorldDifficulty(1);
        } else if (slot == 13) {
            toggle("world.reset-game-world-on-stop");
        } else if (slot == 15) {
            setGameWorldToCurrent(player);
        } else if (slot == 16) {
            plugin.getConfig().set("world.game-world", "");
            plugin.saveConfig();
            plugin.messages().send(player, "&a게임 월드 지정을 해제했습니다.");
        } else if (slot == 18) {
            player.closeInventory();
            player.performCommand("godwar world list");
        } else if (slot == 19) {
            player.closeInventory();
            player.performCommand("godwar world help");
        }
    }

    private void handleWorldCore(Player player, int slot, ClickType click) {
        if (slot == 10) {
            toggle("core.protect-diamond-from-explosion");
        } else if (slot == 11) {
            toggle("core.forbid-diamond-pickaxe");
        } else if (slot == 12) {
            toggle("core.require-empty-hand");
        } else if (slot == 18) {
            switchView(player, SettingsView.PICKAXE_UNLOCK);
        }
    }

    private void handleWorldRules(Player player, int slot) {
        String path = null;
        if (slot == 0) path = "gamerules.enabled";
        else if (slot == 1) path = "gamerules.restore-on-stop";
        else if (slot == 3) path = "gamerules.rules.locatorBar";
        else if (slot == 10) path = "gamerules.rules.keepInventory";
        else if (slot == 11) path = "gamerules.rules.doImmediateRespawn";
        else if (slot == 12) path = "gamerules.rules.naturalRegeneration";
        else if (slot == 13) path = "gamerules.rules.doDaylightCycle";
        else if (slot == 14) path = "gamerules.rules.doWeatherCycle";
        else if (slot == 15) {
            boolean enabled = !plugin.getConfig().getBoolean("gamerules.rules.doFireTick", false);
            plugin.getConfig().set("gamerules.rules.doFireTick", enabled);
            plugin.getConfig().set("gamerules.rules.fire_spread_radius_around_player", enabled ? 128 : 0);
            plugin.saveConfig();
        } else if (slot == 2) {
            if (plugin.getConfig().getBoolean("gamerules.enabled", true)) player.performCommand("godwar gamerule apply");
            else plugin.messages().send(player, "&e게임룰 적용 사용을 먼저 켜 주세요.");
        }
        if (path != null) toggle(path);
    }

    private void handlePickaxeUnlock(int slot, ClickType click) {
        if (slot == 10) {
            changePickaxeUnlockSeconds("core.pickaxe-unlock.wooden-seconds", click);
        } else if (slot == 11) {
            changePickaxeUnlockSeconds("core.pickaxe-unlock.stone-seconds", click);
        } else if (slot == 12) {
            changePickaxeUnlockSeconds("core.pickaxe-unlock.iron-seconds", click);
        } else if (slot == 13) {
            changePickaxeUnlockSeconds("core.pickaxe-unlock.diamond-seconds", click);
        } else if (slot == 16) {
            changePickaxeUnlockSeconds("core.explosion-unlock-seconds", click);
        }
    }

    private void handleDisplay(Player player, int slot, ClickType click) {
        if (slot == 0) {
            toggle("ui.resource-pack.enabled");
        } else if (slot == 2) {
            toggle("abilities.effects.enabled");
        } else if (slot == 3) {
            toggle("abilities.effects.particles");
        } else if (slot == 10) {
            toggle("scoreboard.enabled");
            gameManager.refreshAllPlayerDisplays();
        } else if (slot == 11) {
            toggle("scoreboard.team-prefixes");
            gameManager.refreshAllPlayerDisplays();
        } else if (slot == 12) {
            toggle("game.ability-roll-message");
        } else if (slot == 13) {
            toggle("abilities.messages.enabled");
        } else if (slot == 14) {
            toggle("abilities.effects.sounds");
        } else if (slot == 15) {
            toggle("abilities.messages.success");
        } else if (slot == 16) {
            toggle("abilities.messages.failure");
        } else if (slot == 17) {
            toggle("abilities.messages.timer");
        } else if (slot == 18) {
            toggle("game.killtime-bossbar");
            gameManager.refreshGameTimerBar();
        } else if (slot == 19) {
            toggle("abilities.effects.objects");
        } else if (slot == 20) {
            toggle("abilities.effects.animations");
        } else if (slot == 21) {
            toggle("abilities.effects.action-bar");
        } else if (slot == 22) {
            toggle("abilities.messages.compact");
        }
    }

    private void handleCombat(Player player, int slot, ClickType click) {
        if (slot == 10) {
            toggle("game.friendly-fire");
            gameManager.reloadSettings();
        } else if (slot == 11) {
            toggle("game.ignore-bed");
        } else if (slot == 12) {
            cycleEliminatedPlayerAction(click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT ? -1 : 1);
        } else if (slot == 14) {
            if (click == ClickType.RIGHT) changeUrfCooldownPercent(5);
            else if (click == ClickType.SHIFT_RIGHT) changeUrfCooldownPercent(-5);
            else toggle("game.urf.enabled");
            gameManager.refreshAllPlayerDisplays();
        } else if (slot == 19) {
            changeKilltimeSeconds(click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT ? -60 : -10);
        } else if (slot == 21) {
            changeKilltimeSeconds(click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT ? 60 : 10);
        } else if (slot == 22) {
            cycleKilltimeMode();
        } else if (slot == 23) {
            switchView(player, SettingsView.WORLD_RULES);
        }
    }

    private void handleGambling(Player player, int slot, ClickType click) {
        if (slot == 10) {
            toggle("gambling.enabled");
            gameManager.refreshAllPlayerDisplays();
        } else if (slot == 12) {
            changeInt("gambling.cost.cobblestone", -1, 1, 2304);
        } else if (slot == 14) {
            changeInt("gambling.cost.cobblestone", 1, 1, 2304);
        } else if (slot == 15) {
            switchView(player, SettingsView.GAMBLING_NORMAL);
        } else if (slot == 17) {
            plugin.reloadConfig();
            plugin.removeLegacyTajjaGamblingRewards();
            plugin.messages().send(player, "&a도박 상품 설정을 다시 불러왔습니다.");
        }
    }

    private void handleRewardChance(Player player, String path, int slot, ClickType click) {
        int size = plugin.getConfig().getMapList(path).size();
        int page = rewardPage(player, size);
        if (slot == 22 || slot == 23) {
            int pages = rewardPageCount(size);
            rewardPages.put(player.getUniqueId(), (page + (slot == 22 ? pages - 1 : 1)) % pages);
            return;
        }
        if (slot < 0 || slot >= REWARDS_PER_PAGE) {
            return;
        }
        int index = page * REWARDS_PER_PAGE + slot;
        if (index >= size) {
            if (isRewardItemClick(click)) {
                addRewardItemFromHand(player, path);
            } else {
                plugin.messages().send(player, "&e빈 상품 슬롯입니다. 손에 아이템을 들고 Q 또는 가운데 클릭으로 추가하세요.");
            }
            return;
        }
        if (isRewardItemClick(click)) {
            changeRewardItemFromHand(player, path, index);
            return;
        }
        int delta = 0;
        if (click == ClickType.LEFT) {
            delta = 1;
        } else if (click == ClickType.RIGHT) {
            delta = -1;
        } else if (click == ClickType.SHIFT_LEFT) {
            delta = 5;
        } else if (click == ClickType.SHIFT_RIGHT) {
            delta = -5;
        }
        if (delta != 0) {
            changeRewardChance(path, index, delta);
        }
    }

    private SettingsView currentView(Player player) {
        SettingsView view = openViews.get(player.getUniqueId());
        return view == null ? SettingsView.MAIN : view;
    }

    private void switchView(Player player, SettingsView view) {
        openViews.put(player.getUniqueId(), view);
    }

    private SettingsView backView(SettingsView view) {
        if (view == SettingsView.STOP_CONFIRM) return SettingsView.GAME;
        if (view == SettingsView.GAMBLING_NORMAL) {
            return SettingsView.GAMBLING;
        }
        if (view == SettingsView.PICKAXE_UNLOCK) {
            return SettingsView.WORLD_CORE;
        }
        if (view == SettingsView.TEAM_DETAIL) {
            return SettingsView.TEAM;
        }
        return SettingsView.MAIN;
    }

    private void reopen(final Player player, final SettingsView view) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            final UUID uuid = player.getUniqueId();
            if (player.isOnline() && openViewers.contains(uuid) && currentView(player) == view) {
                refreshingViewers.add(uuid);
                open(player, view);
                Bukkit.getScheduler().runTask(plugin, () -> refreshingViewers.remove(uuid));
            }
        });
    }

    private void fill(Inventory inventory, SettingsView view, Player player) {
        GuiTheme.frame(inventory);
        Inventory content = Bukkit.createInventory(null, 27);
        if (view == SettingsView.STOP_CONFIRM) {
            content.setItem(10, item("REDSTONE_BLOCK", "REDSTONE_BLOCK", 1, (short) 0,
                ChatColor.RED + "게임 끝내기", ChatColor.GRAY + "좌클릭하면 게임을 끝내요.",
                ChatColor.GRAY + "능력과 진행 기록이 정리돼요."));
            content.setItem(16, item("EMERALD_BLOCK", "EMERALD_BLOCK", 1, (short) 0,
                ChatColor.GREEN + "게임 계속하기", ChatColor.GRAY + "창을 닫아도 게임은 계속돼요."));
        } else if (view == SettingsView.MAIN) {
            fillMain(content);
        } else if (view == SettingsView.GAME) {
            fillGame(content);
        } else if (view == SettingsView.TEAM) {
            fillTeam(content, player);
        } else if (view == SettingsView.TEAM_DETAIL) {
            fillTeamDetail(content, player);
        } else if (view == SettingsView.WORLD) {
            fillWorld(content, player);
        } else if (view == SettingsView.WORLD_CORE) {
            fillWorldCore(content);
        } else if (view == SettingsView.WORLD_RULES) {
            fillWorldRules(content);
        } else if (view == SettingsView.PICKAXE_UNLOCK) {
            fillPickaxeUnlock(content);
        } else if (view == SettingsView.DISPLAY) {
            fillDisplay(content);
        } else if (view == SettingsView.COMBAT) {
            fillCombat(content);
        } else if (view == SettingsView.GAMBLING) {
            fillGambling(content);
        } else if (view == SettingsView.GAMBLING_NORMAL) {
            fillRewardChance(content, "gambling.rewards.normal", "도박", player);
        }
        applyControlIcons(content, view);
        int[] slots = ChestLayout.settings(view);
        for (int logical = 0; logical < content.getSize(); logical++) {
            ItemStack entry = content.getItem(logical);
            if (entry == null) continue;
            if (slots[logical] < 0) throw new IllegalStateException("Unmapped GUI control: " + view + "/" + logical);
            inventory.setItem(slots[logical], entry);
        }
        String shortcut = null;
        for (SettingsPage page : SettingsPage.values()) {
            if (page.view == view) shortcut = "/gw gui " + page.id();
        }
        if (view == SettingsView.TEAM_DETAIL && selectedTeam(player) != null) shortcut = "/gw gui team " + selectedTeam(player).id();
        inventory.setItem(4, GuiTheme.icon(GuiTheme.item("BOOK", "BOOK", (short) 0,
            ChatColor.AQUA + "" + ChatColor.BOLD + breadcrumb(view),
            ChatColor.GRAY + pageGuide(view),
            ChatColor.GRAY + "변경한 설정은 자동 저장돼요. 적용 시점은 항목 설명을 확인하세요.",
            ChatColor.YELLOW + (shortcut == null ? "바꿀 항목에 마우스를 올려 보세요." : "바로 열기: " + shortcut)), GuiIcon.SETTINGS));
        inventory.setItem(HOME_SLOT, GuiTheme.icon(GuiTheme.item("COMPASS", "COMPASS", (short) 0,
            ChatColor.AQUA + "설정 홈", ChatColor.GRAY + "8개 설정 분류와 시작 아이템으로 이동"), GuiIcon.HOME));
        int helpSlot = view == SettingsView.MAIN ? 46 : HELP_SLOT;
        inventory.setItem(helpSlot, GuiTheme.icon(GuiTheme.item("BOOK", "BOOK", (short) 0,
            ChatColor.YELLOW + "이 설정은 어디에 있나요?", ChatColor.GRAY + pageGuide(view),
            ChatColor.GRAY + "클릭: 채팅에서 설정 경로 안내"), GuiIcon.HELP));
        if (view != SettingsView.MAIN) {
            inventory.setItem(BACK_SLOT, backItem());
        }
        inventory.setItem(CLOSE_SLOT, closeItem());
        GuiTheme.painted(player, inventory, plugin, 4, HOME_SLOT, helpSlot, CLOSE_SLOT);
        if (view == SettingsView.MAIN) {
            GuiTheme.painted(player, inventory, plugin, 48, 50);
            GuiTheme.captioned(player, inventory, plugin, 10, 12, 14, 16, 28, 30, 32, 34);
        } else {
            GuiTheme.painted(player, inventory, plugin, BACK_SLOT);
        }
        GuiTheme.present(player, inventory, plugin);
    }

    private String breadcrumb(SettingsView view) {
        if (view == SettingsView.MAIN) return "설정 홈";
        String title = ChatColor.stripColor(view.title).replace(" 설정", "");
        SettingsView parent = backView(view);
        return "설정 > " + (parent == SettingsView.MAIN ? "" : ChatColor.stripColor(parent.title).replace(" 설정", "") + " > ") + title;
    }

    private void applyControlIcons(Inventory content, SettingsView view) {
        switch (view) {
            case STOP_CONFIRM:
                iconSlots(content, GuiIcon.STOP, 10);
                iconSlots(content, GuiIcon.CANCEL, 16);
                break;
            case GAME:
                iconSlots(content, GuiIcon.MINUS, 0, 5, 15);
                iconSlots(content, GuiIcon.PLUS, 2, 7, 17);
                iconSlots(content, GuiIcon.INFO, 4);
                iconSlots(content, GuiIcon.PLAY, 18);
                iconSlots(content, GuiIcon.STOP, 20);
                iconSlots(content, GuiIcon.TEAM, 21);
                break;
            case TEAM:
                iconSlots(content, GuiIcon.PREVIOUS, TEAM_PREV_SLOT);
                iconSlots(content, GuiIcon.NEXT, TEAM_NEXT_SLOT);
                iconSlots(content, GuiIcon.PLUS, TEAM_ADD_SLOT);
                break;
            case WORLD:
                iconSlots(content, GuiIcon.MINUS, 5);
                iconSlots(content, GuiIcon.PLUS, 7);
                iconSlots(content, GuiIcon.PREVIOUS, 9);
                iconSlots(content, GuiIcon.NEXT, 11);
                iconSlots(content, GuiIcon.MAP, 18);
                iconSlots(content, GuiIcon.HELP, 19);
                break;
            case WORLD_CORE:
                iconSlots(content, GuiIcon.CORE, 18);
                break;
            case COMBAT:
                iconSlots(content, GuiIcon.MINUS, 19);
                iconSlots(content, GuiIcon.PLUS, 21);
                break;
            case GAMBLING:
                iconSlots(content, plugin.getConfig().getBoolean("gambling.enabled", true) ? GuiIcon.TOGGLE_ON : GuiIcon.TOGGLE_OFF, 10);
                iconSlots(content, GuiIcon.MINUS, 12);
                iconSlots(content, GuiIcon.COIN, 13);
                iconSlots(content, GuiIcon.PLUS, 14);
                iconSlots(content, GuiIcon.REWARDS, 15);
                iconSlots(content, GuiIcon.REFRESH, 17);
                break;
            default: break;
        }
    }

    private void iconSlots(Inventory inventory, GuiIcon icon, int... slots) {
        for (int slot : slots) inventory.setItem(slot, GuiTheme.icon(inventory.getItem(slot), icon));
    }

    private String pageGuide(SettingsView view) {
        switch (view) {
            case MAIN: return "시작은 게임 진행 · 전투 규칙은 전투/부활 · 텍스처는 화면/연출";
            case GAME: return "게임 시작 준비와 지급 설정 · 우르프·킬타임은 전투/부활에 있어요.";
            case COMBAT: return "우르프·킬타임·팀킬·부활·탈락을 여기서 설정해요.";
            case TEAM: case TEAM_DETAIL: return "팀을 고른 뒤 이름·색상·스폰·심장을 등록하세요.";
            case WORLD: return "월드 선택·초기화·시간·난이도·자연 스폰 · 세부 규칙은 월드 게임룰";
            case WORLD_CORE: case PICKAXE_UNLOCK: return "심장 파괴 조건과 보호 시간 · 심장 위치 등록은 팀 설정";
            case WORLD_RULES: return "사망 시 아이템·자연 회복·시간·날씨 · 변경 후 지금 적용을 누르세요.";
            case DISPLAY: return "메뉴 텍스처·능력 효과·채팅 안내 · 우르프·킬타임은 전투/부활";
            case GAMBLING: case GAMBLING_NORMAL: return "도박 사용 여부·가격·상품·확률을 여기서 바꿔요.";
            case STOP_CONFIRM: return "게임을 끝내기 전 마지막 확인이에요.";
            default: return "바꿀 항목에 마우스를 올려 설명과 조작 방법을 확인하세요.";
        }
    }

    private ItemStack readinessItem() {
        ArrayList<String> lines = new ArrayList<String>();
        for (String line : gameManager.startChecklist()) lines.add(ChatColor.GRAY + line);
        lines.add("");
        lines.add(ChatColor.YELLOW + "클릭: 팀 스폰과 심장 등록");
        return GuiTheme.item("BOOK", "BOOK", (short) 0, ChatColor.AQUA + "시작 준비",
            lines.toArray(new String[0]));
    }

    private void fillMain(Inventory inventory) {
        inventory.setItem(4, readinessItem());
        inventory.setItem(10, categoryItem(GuiIcon.GAME, "EMERALD_BLOCK", "게임 진행",
            ChatColor.GRAY + "시작 조건, 지급, 시작/종료",
            ChatColor.DARK_GRAY + "클릭해서 열기"));
        inventory.setItem(11, categoryItem(GuiIcon.TEAM, "WHITE_WOOL", "팀 / 등록",
            ChatColor.GRAY + "팀 배정, 색상, 스폰, 심장",
            ChatColor.DARK_GRAY + "클릭해서 열기"));
        inventory.setItem(12, categoryItem(GuiIcon.WORLD, "GRASS_BLOCK", "월드",
            ChatColor.GRAY + "시작 시간, 난이도, 게임 월드",
            ChatColor.DARK_GRAY + "클릭해서 열기"));
        inventory.setItem(13, categoryItem(GuiIcon.CORE, "DIAMOND_BLOCK", "심장 보호",
            ChatColor.GRAY + "파괴 조건, 곡괭이·폭발 보호 시간",
            ChatColor.DARK_GRAY + "클릭해서 열기"));
        inventory.setItem(14, categoryItem(GuiIcon.COMBAT, "IRON_SWORD", "전투 / 부활",
            ChatColor.GRAY + "우르프, 킬타임, 팀킬, 부활·탈락", ChatColor.DARK_GRAY + "클릭해서 열기"));
        inventory.setItem(15, categoryItem(GuiIcon.DISPLAY, "ITEM_FRAME", "화면 / 연출",
            ChatColor.GRAY + "메뉴 텍스처, 메시지, 능력 효과",
            ChatColor.DARK_GRAY + "클릭해서 열기"));
        inventory.setItem(16, categoryItem(GuiIcon.GAMBLING, "GOLD_INGOT", "도박",
            ChatColor.GRAY + "도박 사용, 가격, 상품 설정",
            ChatColor.DARK_GRAY + "클릭해서 열기"));
        inventory.setItem(17, categoryItem(GuiIcon.WORLD, "COMMAND_BLOCK", "월드 게임룰",
            ChatColor.GRAY + "사망 시 아이템, 시간·날씨, 위치 표시", ChatColor.DARK_GRAY + "클릭해서 열기"));
        inventory.setItem(23, categoryItem(GuiIcon.ITEMS, "CHEST", "시작 아이템 편집",
            ChatColor.GRAY + "게임 시작 때 나눠 줄 아이템을 넣으세요.", ChatColor.YELLOW + "바로 열기: /gw gui items"));
        inventory.setItem(24, GuiTheme.icon(item("BOOK", "BOOK", 1, (short) 0,
            ChatColor.YELLOW + "설정 다시 불러오기",
            ChatColor.GRAY + "config.yml을 직접 수정했을 때 눌러 주세요."), GuiIcon.REFRESH));
    }

    private void fillGame(Inventory inventory) {
        inventory.setItem(4, readinessItem());
        FileConfiguration config = plugin.getConfig();
        inventory.setItem(0, item("REDSTONE_TORCH", "REDSTONE_TORCH_ON", 1, (short) 0,
            ChatColor.RED + "최소 인원 -1",
            ChatColor.GRAY + "현재: " + ChatColor.YELLOW + config.getInt("game.min-players", 2)));
        inventory.setItem(1, item("PLAYER_HEAD", "SKULL_ITEM", config.getInt("game.min-players", 2), (short) 3,
            ChatColor.YELLOW + "최소 시작 인원",
            ChatColor.GRAY + "참가자: " + ChatColor.YELLOW + participantCount()));
        inventory.setItem(2, item("TORCH", "TORCH", 1, (short) 0,
            ChatColor.GREEN + "최소 인원 +1",
            ChatColor.GRAY + "현재: " + ChatColor.YELLOW + config.getInt("game.min-players", 2)));
        inventory.setItem(5, item("REDSTONE_TORCH", "REDSTONE_TORCH_ON", 1, (short) 0,
            ChatColor.RED + "재추첨 횟수 -1",
            ChatColor.GRAY + "현재: " + ChatColor.YELLOW + config.getInt("game.ability-reroll-count", 1) + "회"));
        inventory.setItem(6, item("NETHER_STAR", "NETHER_STAR", Math.max(1, config.getInt("game.ability-reroll-count", 1)), (short) 0,
            ChatColor.YELLOW + "능력 재추첨 가능 횟수",
            ChatColor.GRAY + "/gw no 사용 가능 횟수",
            ChatColor.GRAY + "현재: " + ChatColor.YELLOW + config.getInt("game.ability-reroll-count", 1) + "회"));
        inventory.setItem(7, item("TORCH", "TORCH", 1, (short) 0,
            ChatColor.GREEN + "재추첨 횟수 +1",
            ChatColor.GRAY + "현재: " + ChatColor.YELLOW + config.getInt("game.ability-reroll-count", 1) + "회"));

        inventory.setItem(9, toggleItem("game.clear-inventory", "시작할 때 인벤토리 비우기", "CHEST"));
        inventory.setItem(10, toggleItem("game.give-skyblock-items", "시작 아이템 지급", "ICE",
            ChatColor.GRAY + "우클릭: 시작 아이템 열기"));
        inventory.setItem(11, toggleItem("game.fast-start", "빠른 시작", "SUGAR"));
        inventory.setItem(12, toggleItem("game.select-right", "능력 재추첨 기회", "NETHER_STAR"));
        inventory.setItem(13, toggleItem("game.auto-balance-teams", "시작 시 팀 자동 배정", "COMPASS"));
        inventory.setItem(14, toggleItem("game.allow-mid-join", "중간 참여 허용", "ENDER_PEARL"));
        inventory.setItem(15, item("REDSTONE_TORCH", "REDSTONE_TORCH_ON", 1, (short) 0,
            ChatColor.RED + "시작 대기 -1초",
            ChatColor.GRAY + "현재: " + ChatColor.YELLOW + config.getInt("game.skip-ready-countdown-seconds", 5) + "초"));
        inventory.setItem(16, item("WATCH", "WATCH", Math.max(1, config.getInt("game.skip-ready-countdown-seconds", 5)), (short) 0,
            ChatColor.YELLOW + "능력 확정 후 대기 시간",
            ChatColor.GRAY + "모두 확정하면 이 시간 뒤에 시작해요.",
            ChatColor.GRAY + "/gw skip에도 같은 시간이 적용돼요.",
            ChatColor.GRAY + "현재: " + ChatColor.YELLOW + config.getInt("game.skip-ready-countdown-seconds", 5) + "초"));
        inventory.setItem(17, item("TORCH", "TORCH", 1, (short) 0,
            ChatColor.GREEN + "시작 대기 +1초",
            ChatColor.GRAY + "현재: " + ChatColor.YELLOW + config.getInt("game.skip-ready-countdown-seconds", 5) + "초"));

        inventory.setItem(18, item("EMERALD_BLOCK", "EMERALD_BLOCK", 1, (short) 0,
            ChatColor.GREEN + "게임 시작",
            ChatColor.GRAY + "/gw start"));
        inventory.setItem(19, toggleItem("game.reveal-abilities-on-end", "종료 후 능력 공개", "ENCHANTED_BOOK"));
        inventory.setItem(20, item("REDSTONE_BLOCK", "REDSTONE_BLOCK", 1, (short) 0,
            ChatColor.RED + "게임 종료",
            ChatColor.GRAY + "클릭하면 종료 확인 화면이 열려요."));
        inventory.setItem(21, item("COMPASS", "COMPASS", 1, (short) 0,
            ChatColor.AQUA + "팀 자동 배정",
            ChatColor.GRAY + "/gw autoteam"));
        inventory.setItem(22, toggleItem("game.remove-entities", "시작할 때 엔티티 정리", "ROTTEN_FLESH",
            ChatColor.GRAY + "게임 시작 준비 단계에서 적용해요."));
    }

    private void fillTeam(Inventory inventory, Player player) {
        GodTeam[] teams = GodTeam.values();
        int page = clampedTeamPage(player, teams.length);
        int start = page * TEAMS_PER_PAGE;
        int end = Math.min(teams.length, start + TEAMS_PER_PAGE);
        String pageText = (page + 1) + "/" + (lastTeamPage() + 1);
        String rangeText = teams.length == 0 ? "팀 없음" : (start + 1) + "-" + end + "/" + teams.length;
        for (int i = start; i < end; i++) {
            GodTeam team = teams[i];
            inventory.setItem(TEAM_LIST_SLOTS[i - start], teamListItem(team));
        }
        inventory.setItem(TEAM_PREV_SLOT, item("ARROW", "ARROW", 1, (short) 0,
            ChatColor.AQUA + "이전 팀 페이지",
            ChatColor.GRAY + "현재 페이지: " + ChatColor.YELLOW + pageText,
            ChatColor.GRAY + "표시 팀: " + ChatColor.WHITE + rangeText,
            ChatColor.DARK_GRAY + "첫 페이지에서는 마지막 페이지로 이동"));
        inventory.setItem(TEAM_NEXT_SLOT, item("ARROW", "ARROW", 1, (short) 0,
            ChatColor.AQUA + "다음 팀 페이지",
            ChatColor.GRAY + "현재 페이지: " + ChatColor.YELLOW + pageText,
            ChatColor.GRAY + "표시 팀: " + ChatColor.WHITE + rangeText,
            ChatColor.DARK_GRAY + "마지막 페이지에서는 첫 페이지로 이동"));
        inventory.setItem(TEAM_ADD_SLOT, item("EMERALD", "EMERALD", 1, (short) 0,
            ChatColor.GREEN + "팀 추가",
            ChatColor.GRAY + "새 팀을 만들어요. 이름과 색상도 바꿀 수 있어요."));
    }

    private void fillTeamDetail(Inventory inventory, Player player) {
        GodTeam team = selectedTeam(player);
        if (team == null) {
            inventory.setItem(13, item("BARRIER", "BARRIER", 1, (short) 0,
                ChatColor.RED + "선택된 팀이 없습니다",
                ChatColor.GRAY + "뒤로 돌아가 팀을 다시 선택하세요."));
            return;
        }
        inventory.setItem(4, teamSummaryItem(team));
        inventory.setItem(10, teamColorItem(team));
        inventory.setItem(12, teamSpawnItem(team));
        inventory.setItem(13, teamNameItem(team));
        inventory.setItem(14, teamTempleItem(team));
        inventory.setItem(16, teamEnabledItem(team));
    }

    private void fillWorld(Inventory inventory, Player player) {
        FileConfiguration config = plugin.getConfig();
        inventory.setItem(0, sectionItem("월드 시작 설정", (short) 3,
            ChatColor.GRAY + "게임 시작 시 모든 월드에 적용"));
        inventory.setItem(1, toggleItem("world.autosave", "서버 자동 저장", "BOOK"));
        inventory.setItem(2, toggleItem("world.spawn-animals", "동물 스폰", "WHEAT"));
        inventory.setItem(3, toggleItem("world.spawn-monsters", "몬스터 스폰", "BONE"));

        inventory.setItem(5, item("REDSTONE_TORCH", "REDSTONE_TORCH_ON", 1, (short) 0,
            ChatColor.RED + "시작 시간 -1000",
            ChatColor.GRAY + "Shift 클릭: -6000",
            ChatColor.GRAY + "현재: " + ChatColor.YELLOW + worldTimeText(config.getInt("world.start-time", 6000))));
        inventory.setItem(6, worldStartTimeItem(player));
        inventory.setItem(7, item("TORCH", "TORCH", 1, (short) 0,
            ChatColor.GREEN + "시작 시간 +1000",
            ChatColor.GRAY + "Shift 클릭: +6000",
            ChatColor.GRAY + "현재: " + ChatColor.YELLOW + worldTimeText(config.getInt("world.start-time", 6000))));

        inventory.setItem(9, item("ARROW", "ARROW", 1, (short) 0,
            ChatColor.AQUA + "난이도 이전",
            ChatColor.GRAY + "현재: " + ChatColor.YELLOW + configuredWorldDifficulty().name()));
        inventory.setItem(10, worldDifficultyItem());
        inventory.setItem(11, item("ARROW", "ARROW", 1, (short) 0,
            ChatColor.AQUA + "난이도 다음",
            ChatColor.GRAY + "현재: " + ChatColor.YELLOW + configuredWorldDifficulty().name()));

        inventory.setItem(13, toggleItem("world.reset-game-world-on-stop", "게임 월드 자동 초기화", "ENDER_CHEST",
            ChatColor.GRAY + "게임이 끝나면 시작 전 월드로 되돌려요."));
        inventory.setItem(15, gameWorldSetItem(player));
        inventory.setItem(16, gameWorldClearItem());
        inventory.setItem(18, item("MAP", "MAP", 1, (short) 0,
            ChatColor.YELLOW + "월드 목록 보기",
            ChatColor.GRAY + "사용 가능한 월드와 로비를 확인해요."));
        inventory.setItem(19, item("PAPER", "PAPER", 1, (short) 0,
            ChatColor.YELLOW + "월드 도움말",
            ChatColor.GRAY + "월드 생성·백업·삭제 명령을 확인해요."));
    }

    private void fillWorldCore(Inventory inventory) {
        inventory.setItem(9, GuiTheme.icon(sectionItem("심장 파괴 조건", (short) 11,
            ChatColor.GRAY + "우리 팀의 다이아 심장이 깨지면 탈락해요.",
            ChatColor.GRAY + "팀 스폰과 심장 위치 등록은 설정 > 팀"), GuiIcon.CORE));
        inventory.setItem(10, toggleItem("core.protect-diamond-from-explosion", "코어 폭파 보호", "DIAMOND_BLOCK"));
        inventory.setItem(11, toggleItem("core.forbid-diamond-pickaxe", "다이아 곡괭이 금지", "DIAMOND_PICKAXE"));
        inventory.setItem(12, toggleItem("core.require-empty-hand", "코어 맨손 파괴", "BARRIER"));

        inventory.setItem(18, coreProtectionMenuItem());
    }

    private void fillWorldRules(Inventory inventory) {
        inventory.setItem(0, toggleItem("gamerules.enabled", "게임룰 적용 사용", "COMMAND_BLOCK",
            ChatColor.GRAY + "게임 시작 시 자동 적용과 지금 적용에 필요해요."));
        inventory.setItem(1, toggleItem("gamerules.restore-on-stop", "종료할 때 이전 게임룰 복구", "REDSTONE_COMPARATOR"));
        inventory.setItem(2, GuiTheme.icon(item("EMERALD_BLOCK", "EMERALD_BLOCK", 1, (short) 0,
            ChatColor.GREEN + "지금 모든 월드에 적용", ChatColor.GRAY + "저장한 게임룰을 모든 월드에 적용해요.",
            ChatColor.YELLOW + "/gw gamerule apply"), GuiIcon.CONFIRM));
        inventory.setItem(3, toggleItem("gamerules.rules.locatorBar", "플레이어 위치 표시 막대", "COMPASS",
            ChatColor.GRAY + "지원 서버의 경험치바 부근에 위치를 표시해요."));
        inventory.setItem(10, toggleItem("gamerules.rules.keepInventory", "죽어도 아이템 유지", "CHEST"));
        inventory.setItem(11, toggleItem("gamerules.rules.doImmediateRespawn", "사망 화면 없이 즉시 부활", "TOTEM_OF_UNDYING"));
        inventory.setItem(12, toggleItem("gamerules.rules.naturalRegeneration", "배고픔으로 체력 회복", "GOLDEN_APPLE"));
        inventory.setItem(13, toggleItem("gamerules.rules.doDaylightCycle", "낮과 밤 흐름", "CLOCK"));
        inventory.setItem(14, toggleItem("gamerules.rules.doWeatherCycle", "날씨 자동 변화", "WATER_BUCKET"));
        inventory.setItem(15, toggleItem("gamerules.rules.doFireTick", "불 번짐", "FLINT_AND_STEEL",
            ChatColor.GRAY + "최신 서버의 불 번짐 범위도 함께 바꿔요."));
        inventory.setItem(18, GuiTheme.heading("언제 적용되나요?",
            "변경은 자동 저장돼요. 지금 적용하거나 다음 게임 시작에 적용하세요."));
    }

    private void fillPickaxeUnlock(Inventory inventory) {
        inventory.setItem(4, sectionItem("코어 보호 설정", (short) 6,
            ChatColor.GRAY + "게임 시작 후 코어 보호 해제 시점"));
        inventory.setItem(10, pickaxeUnlockItem("WOODEN_PICKAXE", "WOOD_PICKAXE", "나무 곡괭이", "core.pickaxe-unlock.wooden-seconds"));
        inventory.setItem(11, pickaxeUnlockItem("STONE_PICKAXE", "STONE_PICKAXE", "돌 곡괭이", "core.pickaxe-unlock.stone-seconds"));
        inventory.setItem(12, pickaxeUnlockItem("IRON_PICKAXE", "IRON_PICKAXE", "철 곡괭이", "core.pickaxe-unlock.iron-seconds"));
        inventory.setItem(13, pickaxeUnlockItem("DIAMOND_PICKAXE", "DIAMOND_PICKAXE", "다이아 곡괭이", "core.pickaxe-unlock.diamond-seconds"));
        inventory.setItem(16, explosionUnlockItem());
    }

    private void fillDisplay(Inventory inventory) {
        boolean packConfigured = plugin.getConfig().getBoolean("abilities.effects.resource-pack.enabled", true);
        inventory.setItem(0, toggleItem("ui.resource-pack.enabled", "메뉴 배경 / 아이콘", "PAINTING",
            ChatColor.GRAY + "팩을 받은 플레이어의 메뉴에 적용해요."));
        inventory.setItem(1, GuiTheme.icon(item("BOOK", "BOOK", 1, (short) 0,
            ChatColor.AQUA + "텍스처팩 연결 안내",
            ChatColor.GRAY + "팩 주소: " + (packConfigured ? ChatColor.GREEN + "설정됨" : ChatColor.YELLOW + "미설정"),
            ChatColor.GRAY + "config.yml > abilities.effects.resource-pack",
            ChatColor.GRAY + "url: auto로 GitHub 버전별 팩을 연결해요.",
            ChatColor.GRAY + "팩을 거절하면 기본 아이콘으로 보여요."), GuiIcon.HELP));
        inventory.setItem(2, toggleItem("abilities.effects.enabled", "능력 시각·소리 효과", "FIREWORK_ROCKET"));
        inventory.setItem(3, toggleItem("abilities.effects.particles", "능력 파티클", "BLAZE_POWDER"));
        inventory.setItem(10, toggleItem("scoreboard.enabled", "스코어보드 안내 사용", "ITEM_FRAME"));
        inventory.setItem(11, toggleItem("scoreboard.team-prefixes", "이름 앞에 팀 표시", "NAME_TAG"));
        inventory.setItem(12, toggleItem("game.ability-roll-message", "능력 배정 안내", "PAPER"));
        inventory.setItem(13, toggleItem("abilities.messages.enabled", "능력 안내 메시지", "BOOK"));
        inventory.setItem(14, toggleItem("abilities.effects.sounds", "능력 효과음", "NOTE_BLOCK"));
        inventory.setItem(15, toggleItem("abilities.messages.success", "능력 사용 완료 문구", "INK_SACK"));
        inventory.setItem(16, toggleItem("abilities.messages.failure", "능력 실패/제한 문구", "REDSTONE"));
        inventory.setItem(17, toggleItem("abilities.messages.timer", "능력 타이머 채팅", "WATCH"));
        inventory.setItem(18, toggleItem("game.killtime-bossbar", "킬타임 보스바", "WATCH"));
        inventory.setItem(19, toggleItem("abilities.effects.objects", "능력 입체 오브젝트", "ARMOR_STAND"));
        inventory.setItem(20, toggleItem("abilities.effects.animations", "능력 오브젝트 움직임", "FEATHER"));
        inventory.setItem(21, toggleItem("abilities.effects.action-bar", "능력 액션바 안내", "PAPER"));
        inventory.setItem(22, toggleItem("abilities.messages.compact", "간결한 전투 안내", "FEATHER",
            ChatColor.GRAY + "충전·모드·중요 버프·효과 시작과 종료는 유지해요.",
            ChatColor.GRAY + "반복 발동·패시브·초 단위 카운트다운만 생략해요."));
    }

    private void fillCombat(Inventory inventory) {
        FileConfiguration config = plugin.getConfig();
        inventory.setItem(10, toggleItem("game.friendly-fire", "팀킬 허용", "IRON_SWORD"));
        inventory.setItem(11, toggleItem("game.ignore-bed", "침대 대신 팀 스폰에서 부활", "BED"));
        inventory.setItem(12, eliminatedPlayerActionItem(config));
        inventory.setItem(14, GuiTheme.icon(urfItem(), GuiIcon.COMBAT));
        inventory.setItem(19, item("REDSTONE_TORCH", "REDSTONE_TORCH_ON", 1, (short) 0,
            ChatColor.RED + "킬타임 -10초",
            ChatColor.GRAY + "Shift 클릭: -60초",
            ChatColor.GRAY + "현재: " + ChatColor.YELLOW + durationText(config.getInt("game.killtime-seconds", 300))));
        inventory.setItem(20, killtimeSecondsItem(config));
        inventory.setItem(21, item("TORCH", "TORCH", 1, (short) 0,
            ChatColor.GREEN + "킬타임 +10초",
            ChatColor.GRAY + "Shift 클릭: +60초",
            ChatColor.GRAY + "현재: " + ChatColor.YELLOW + durationText(config.getInt("game.killtime-seconds", 300))));
        inventory.setItem(22, killtimeModeItem());
        inventory.setItem(23, categoryItem(GuiIcon.WORLD, "COMMAND_BLOCK", "사망 시 아이템·즉시 부활",
            ChatColor.GRAY + "월드 게임룰에서 바꿀 수 있어요.", ChatColor.YELLOW + "클릭: 월드 게임룰 열기"));
    }

    private void fillGambling(Inventory inventory) {
        FileConfiguration config = plugin.getConfig();
        int cost = Math.max(1, config.getInt("gambling.cost.cobblestone", 32));
        inventory.setItem(10, gamblingItem(config));
        inventory.setItem(12, item("REDSTONE_TORCH", "REDSTONE_TORCH_ON", 1, (short) 0,
            ChatColor.RED + "도박 가격 -1",
            ChatColor.GRAY + "현재: " + ChatColor.YELLOW + "조약돌 " + cost + "개"));
        inventory.setItem(13, item("COBBLESTONE", "COBBLESTONE", 1, (short) 0,
            ChatColor.YELLOW + "한 번 뽑기 가격",
            ChatColor.GRAY + "조약돌 " + ChatColor.WHITE + cost + "개",
            ChatColor.GRAY + "양옆의 − / + 버튼으로 바꿔요."));
        inventory.setItem(14, item("TORCH", "TORCH", 1, (short) 0,
            ChatColor.GREEN + "도박 가격 +1",
            ChatColor.GRAY + "현재: " + ChatColor.YELLOW + "조약돌 " + cost + "개"));
        inventory.setItem(15, item("DIAMOND", "DIAMOND", 1, (short) 0,
            ChatColor.AQUA + "상품 / 확률 편집",
            ChatColor.GRAY + "상품: " + ChatColor.WHITE + config.getMapList("gambling.rewards.normal").size() + "개",
            ChatColor.GRAY + "클릭: 상품별 확률과 아이템 바꾸기"));
        inventory.setItem(17, item("BOOK", "BOOK", 1, (short) 0,
            ChatColor.YELLOW + "상품 설정 다시 불러오기",
            ChatColor.GRAY + "config.yml을 직접 수정했을 때 눌러 주세요."));
    }

    private void fillRewardChance(Inventory inventory, String path, String title, Player player) {
        List<Map<?, ?>> rewards = plugin.getConfig().getMapList(path);
        int total = totalChance(path);
        int page = rewardPage(player, rewards.size());
        int start = page * REWARDS_PER_PAGE;
        int limit = Math.min(REWARDS_PER_PAGE, rewards.size() - start);
        for (int i = 0; i < limit; i++) {
            inventory.setItem(i, rewardItem(path, rewards.get(start + i), start + i, total));
        }
        if (limit < REWARDS_PER_PAGE) {
            inventory.setItem(limit, GuiTheme.icon(addRewardItem(rewards.size()), GuiIcon.PLUS));
        }
        inventory.setItem(21, GuiTheme.icon(item("PAPER", "PAPER", 1, (short) 0,
            ChatColor.YELLOW + title + " 상품 편집 · " + (page + 1) + "/" + rewardPageCount(rewards.size()),
            ChatColor.GRAY + "가중치가 높을수록 더 자주 나와요.",
            ChatColor.GRAY + "확률 = 상품 가중치 / 전체 가중치",
            ChatColor.GRAY + "전체 가중치: " + ChatColor.WHITE + total,
            ChatColor.GRAY + "당첨 문구 바꾸기:",
            ChatColor.YELLOW + "/gw gamblereward " + rewardType(path) + " <번호> message <문구>"), GuiIcon.INFO));
        inventory.setItem(22, GuiTheme.icon(item("ARROW", "ARROW", 1, (short) 0,
            ChatColor.AQUA + "이전 상품 페이지", ChatColor.GRAY + "페이지 " + (page + 1) + "/" + rewardPageCount(rewards.size())), GuiIcon.PREVIOUS));
        inventory.setItem(23, GuiTheme.icon(item("ARROW", "ARROW", 1, (short) 0,
            ChatColor.AQUA + "다음 상품 페이지", ChatColor.GRAY + "마지막 페이지에서 상품을 추가할 수 있어요."), GuiIcon.NEXT));
    }

    private int rewardPageCount(int size) {
        // Keep one extra slot for adding a reward, including when a page is full.
        return Math.max(1, size / REWARDS_PER_PAGE + 1);
    }

    private int rewardPage(Player player, int size) {
        Integer value = rewardPages.get(player.getUniqueId());
        return value == null ? 0 : Math.max(0, Math.min(rewardPageCount(size) - 1, value));
    }

    private ItemStack urfItem() {
        boolean enabled = plugin.getConfig().getBoolean("game.urf.enabled", false);
        ChatColor color = enabled ? ChatColor.GREEN : ChatColor.RED;
        int percent = plugin.abilities().urfCooldownPercent();
        return item("BLAZE_POWDER", "BLAZE_POWDER", 1, (short) 0,
            color + "우르프 모드: " + (enabled ? "켜짐" : "꺼짐"),
            ChatColor.GRAY + "능력 쿨타임 감소율: " + ChatColor.YELLOW + percent + "%",
            ChatColor.GRAY + "좌클릭: 우르프 켜기/끄기",
            ChatColor.GRAY + "우클릭: 감소율 +5%",
            ChatColor.GRAY + "Shift+우클릭: 감소율 -5%");
    }

    private ItemStack killtimeSecondsItem(FileConfiguration config) {
        int seconds = Math.max(0, config.getInt("game.killtime-seconds", 300));
        return item("WATCH", "WATCH", Math.max(1, Math.min(64, seconds <= 0 ? 1 : seconds / 10)), (short) 0,
            ChatColor.YELLOW + "킬타임 보호 시간",
            ChatColor.GRAY + "시작 후 " + gameManager.killtimeMode().displayName() + " 유지 시간",
            ChatColor.GRAY + "현재: " + ChatColor.YELLOW + durationText(seconds),
            ChatColor.GRAY + "좌우 버튼: 10초 단위",
            ChatColor.GRAY + "Shift 클릭: 60초 단위");
    }

    private ItemStack killtimeModeItem() {
        KilltimeMode mode = gameManager.killtimeMode();
        return item(mode == KilltimeMode.CORE_ONLY ? "DIAMOND_BLOCK" : "IRON_SWORD", "IRON_SWORD", 1, (short) 0,
            ChatColor.YELLOW + "킬타임 방식: " + ChatColor.WHITE + mode.displayName(),
            ChatColor.GRAY + (mode == KilltimeMode.CORE_ONLY
                ? "전투는 가능하고, 심장만 보호해요." : "플레이어끼리 피해를 줄 수 없어요."),
            ChatColor.AQUA + "클릭: 다른 보호 방식으로 바꾸기");
    }

    private ItemStack gamblingItem(FileConfiguration config) {
        boolean enabled = config.getBoolean("gambling.enabled", true);
        ChatColor color = enabled ? ChatColor.GREEN : ChatColor.RED;
        int cost = Math.max(1, config.getInt("gambling.cost.cobblestone", 32));
        int rewards = config.getMapList("gambling.rewards.normal").size();
        return item("GOLD_INGOT", "GOLD_INGOT", 1, (short) 0,
            color + "도박 허용: " + (enabled ? "켜짐" : "꺼짐"),
            ChatColor.GRAY + "가격: " + ChatColor.YELLOW + "조약돌 " + cost + "개",
            ChatColor.GRAY + "상품: " + ChatColor.WHITE + rewards + "개",
            ChatColor.YELLOW + "클릭: " + (enabled ? "끄기" : "켜기"));
    }

    private ItemStack eliminatedPlayerActionItem(FileConfiguration config) {
        String action = normalizedEliminatedPlayerAction(config);
        return item(eliminatedPlayerActionIcon(action), "ENDER_PEARL", 1, (short) 0,
            ChatColor.YELLOW + "탈락 후: " + ChatColor.WHITE + eliminatedPlayerActionLabel(action),
            ChatColor.GRAY + ("kick".equals(action) ? "탈락한 플레이어를 서버에서 내보내요."
                : "midjoin".equals(action) ? "남은 팀이 2개 이상이면 다른 팀으로 옮겨요."
                : "none".equals(action) ? "팀·위치·게임 모드를 그대로 둬요." : "관전 모드로 바꿔요."),
            ChatColor.YELLOW + "좌클릭: 다음 · 우클릭: 이전");
    }

    private String eliminatedPlayerActionIcon(String action) {
        if ("kick".equals(action)) {
            return "BARRIER";
        }
        if ("midjoin".equals(action)) {
            return "ENDER_PEARL";
        }
        if ("none".equals(action)) {
            return "PAPER";
        }
        return "SPYGLASS";
    }

    private ItemStack teamListItem(GodTeam team) {
        ChatColor color = gameManager.teamColor(team);
        boolean enabled = gameManager.isTeamEnabled(team);
        boolean spawnConfigured = (gameManager.spawns().get(team) != null && gameManager.spawns().get(team).toLocation() != null);
        boolean templeConfigured = (gameManager.temples().get(team) != null && gameManager.temples().get(team).toLocation() != null);
        ChatColor enabledColor = enabled ? ChatColor.GREEN : ChatColor.RED;
        ChatColor spawnColor = spawnConfigured ? ChatColor.GREEN : ChatColor.RED;
        ChatColor templeColor = templeConfigured ? ChatColor.GREEN : ChatColor.RED;
        ChatColor titleColor = enabled ? color : ChatColor.DARK_GRAY;
        return item(teamWoolMaterial(team), "WOOL", 1, teamWoolData(team),
            titleColor + gameManager.teamDisplayName(team) + ChatColor.YELLOW + " 팀",
            ChatColor.GRAY + "참가 상태: " + enabledColor + (enabled ? "활성" : "비활성"),
            ChatColor.GRAY + "팀원: " + ChatColor.WHITE + teamMemberCount(team) + "명",
            ChatColor.GRAY + "스폰: " + spawnColor + (spawnConfigured ? "설정됨" : "등록 또는 월드 불러오기 필요")
                + ChatColor.GRAY + " / 심장: " + templeColor + (templeConfigured ? "설정됨" : "등록 또는 월드 불러오기 필요"),
            ChatColor.DARK_GRAY + "클릭해서 팀 설정 열기");
    }

    private ItemStack teamSummaryItem(GodTeam team) {
        ChatColor color = gameManager.teamColor(team);
        boolean enabled = gameManager.isTeamEnabled(team);
        boolean spawnConfigured = (gameManager.spawns().get(team) != null && gameManager.spawns().get(team).toLocation() != null);
        boolean templeConfigured = (gameManager.temples().get(team) != null && gameManager.temples().get(team).toLocation() != null);
        ChatColor enabledColor = enabled ? ChatColor.GREEN : ChatColor.RED;
        ChatColor spawnColor = spawnConfigured ? ChatColor.GREEN : ChatColor.RED;
        ChatColor templeColor = templeConfigured ? ChatColor.GREEN : ChatColor.RED;
        ChatColor titleColor = enabled ? color : ChatColor.DARK_GRAY;
        return item(teamWoolMaterial(team), "WOOL", 1, teamWoolData(team),
            titleColor + gameManager.teamDisplayName(team) + ChatColor.YELLOW + " 팀 기본 정보",
            ChatColor.GRAY + "참가 상태: " + enabledColor + (enabled ? "활성" : "비활성"),
            ChatColor.GRAY + "팀원: " + ChatColor.WHITE + teamMemberCount(team) + "명",
            ChatColor.GRAY + "스폰: " + spawnColor + (spawnConfigured ? "설정됨" : "등록 또는 월드 불러오기 필요")
                + ChatColor.GRAY + " / 심장: " + templeColor + (templeConfigured ? "설정됨" : "등록 또는 월드 불러오기 필요"),
            ChatColor.GRAY + "현재 색상: " + color + colorLabel(color));
    }

    private ItemStack teamColorItem(GodTeam team) {
        ChatColor color = gameManager.teamColor(team);
        boolean enabled = gameManager.isTeamEnabled(team);
        ChatColor titleColor = enabled ? color : ChatColor.DARK_GRAY;
        return item(teamWoolMaterial(team), "WOOL", 1, teamWoolData(team),
            titleColor + gameManager.teamDisplayName(team) + ChatColor.YELLOW + " 팀 색상",
            ChatColor.GRAY + "현재 색상: " + color + colorLabel(color),
            ChatColor.GRAY + "좌클릭: 다음 색상",
            ChatColor.GRAY + "우클릭: 이전 색상",
            ChatColor.RED + "Shift+우클릭: 팀원 없는 커스텀 팀 삭제");
    }

    private ItemStack teamNameItem(GodTeam team) {
        return item("NAME_TAG", "NAME_TAG", 1, (short) 0,
            ChatColor.YELLOW + gameManager.teamDisplayName(team) + ChatColor.GOLD + " 팀 이름",
            ChatColor.GRAY + "클릭한 뒤 채팅에 새 이름을 입력하세요.",
            ChatColor.GRAY + "돌아가려면 " + ChatColor.WHITE + "취소" + ChatColor.GRAY + "를 입력하세요.");
    }

    private ItemStack teamSpawnItem(GodTeam team) {
        boolean configured = (gameManager.spawns().get(team) != null && gameManager.spawns().get(team).toLocation() != null);
        boolean enabled = gameManager.isTeamEnabled(team);
        ChatColor stateColor = configured ? ChatColor.GREEN : ChatColor.RED;
        ChatColor titleColor = enabled ? stateColor : ChatColor.DARK_GRAY;
        ChatColor enabledColor = enabled ? ChatColor.GREEN : ChatColor.RED;
        return item("BED", "BED", 1, (short) 0,
            titleColor + gameManager.teamDisplayName(team) + " 팀 스폰",
            ChatColor.GRAY + "상태: " + stateColor + (configured ? "설정됨" : "등록 또는 월드 불러오기 필요"),
            ChatColor.YELLOW + "클릭: 내 위치를 스폰으로 등록");
    }

    private ItemStack teamTempleItem(GodTeam team) {
        boolean configured = (gameManager.temples().get(team) != null && gameManager.temples().get(team).toLocation() != null);
        boolean enabled = gameManager.isTeamEnabled(team);
        ChatColor stateColor = configured ? ChatColor.GREEN : ChatColor.RED;
        ChatColor titleColor = enabled ? stateColor : ChatColor.DARK_GRAY;
        ChatColor enabledColor = enabled ? ChatColor.GREEN : ChatColor.RED;
        return item("DIAMOND_BLOCK", "DIAMOND_BLOCK", 1, (short) 0,
            titleColor + gameManager.teamDisplayName(team) + " 팀 다이아 심장",
            ChatColor.GRAY + "상태: " + stateColor + (configured ? "설정됨" : "등록 또는 월드 불러오기 필요"),
            ChatColor.YELLOW + "클릭: 바라보는 다이아 블록을 심장으로 등록");
    }

    private ItemStack teamEnabledItem(GodTeam team) {
        boolean enabled = gameManager.isTeamEnabled(team);
        ChatColor stateColor = enabled ? ChatColor.GREEN : ChatColor.RED;
        return item(enabled ? "LIME_DYE" : "GRAY_DYE", enabled ? "INK_SACK" : "INK_SACK", 1, enabled ? (short) 10 : (short) 8,
            stateColor + gameManager.teamDisplayName(team) + " 팀 " + (enabled ? "활성" : "비활성"),
            ChatColor.GRAY + "현재 팀원: " + ChatColor.WHITE + teamMemberCount(team) + "명",
            ChatColor.GRAY + "끄면 자동 배정과 게임 참여에서 빠져요.",
            ChatColor.RED + "이미 배정된 팀원도 팀에서 빠져요.",
            ChatColor.YELLOW + "클릭: " + (enabled ? "참가 끄기" : "참가 켜기"));
    }

    private ItemStack toggleItem(String path, String title, String icon) {
        return toggleItem(path, title, icon, new String[0]);
    }

    private ItemStack toggleItem(String path, String title, String icon, String... extraLore) {
        boolean enabled = plugin.getConfig().getBoolean(path, defaultToggleValue(path));
        ChatColor color = enabled ? ChatColor.GREEN : ChatColor.RED;
        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.GRAY + "현재: " + color + (enabled ? "켜짐" : "꺼짐"));
        if (extraLore != null) {
            lore.addAll(Arrays.asList(extraLore));
        }
        lore.add(ChatColor.YELLOW + "클릭 → " + (enabled ? "끄기" : "켜기"));
        return GuiTheme.icon(item(icon, icon, 1, (short) 0,
            ChatColor.WHITE + title,
            lore.toArray(new String[lore.size()])), enabled ? GuiIcon.TOGGLE_ON : GuiIcon.TOGGLE_OFF);
    }

    private ItemStack categoryItem(GuiIcon model, String icon, String title, String... lore) {
        return GuiTheme.icon(item(icon, icon, 1, (short) 0, ChatColor.AQUA + "" + ChatColor.BOLD + title, lore), model);
    }

    private ItemStack sectionItem(String title, short damage, String... lore) {
        return item("LIGHT_BLUE_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", 1, damage,
            ChatColor.AQUA + "■ " + title, lore);
    }

    private ItemStack backItem() {
        return GuiTheme.icon(item("ARROW", "ARROW", 1, (short) 0,
            ChatColor.AQUA + "뒤로",
            ChatColor.GRAY + "상위 설정 화면으로 돌아갑니다."), GuiIcon.BACK);
    }

    private ItemStack closeItem() {
        return GuiTheme.close();
    }

    private void toggle(String path) {
        FileConfiguration config = plugin.getConfig();
        config.set(path, !config.getBoolean(path, defaultToggleValue(path)));
        plugin.saveConfig();
    }

    private void cycleEliminatedPlayerAction(int delta) {
        String[] actions = new String[] {"spectator", "kick", "midjoin", "none"};
        String current = normalizedEliminatedPlayerAction(plugin.getConfig());
        int index = 0;
        for (int i = 0; i < actions.length; i++) {
            if (actions[i].equals(current)) {
                index = i;
                break;
            }
        }
        int next = (index + delta) % actions.length;
        if (next < 0) {
            next += actions.length;
        }
        plugin.getConfig().set("game.eliminated-player-action", actions[next]);
        plugin.saveConfig();
    }

    private String normalizedEliminatedPlayerAction(FileConfiguration config) {
        String action = config.getString("game.eliminated-player-action", "spectator");
        if (action == null) {
            return "spectator";
        }
        action = action.toLowerCase().trim();
        if ("kick".equals(action) || "midjoin".equals(action) || "none".equals(action) || "spectator".equals(action)) {
            return action;
        }
        return "spectator";
    }

    private String eliminatedPlayerActionLabel(String action) {
        if ("kick".equals(action)) {
            return "킥";
        }
        if ("midjoin".equals(action)) {
            return "자동 중간참여";
        }
        if ("none".equals(action)) {
            return "그대로 둠";
        }
        return "관전";
    }

    private boolean defaultToggleValue(String path) {
        return path != null && (path.startsWith("abilities.messages.") || path.startsWith("abilities.effects.")
            || TRUE_DEFAULT_TOGGLES.contains(path));
    }

    private void changeInt(String path, int delta, int min, int max) {
        FileConfiguration config = plugin.getConfig();
        int value = config.getInt(path, min);
        value = Math.max(min, Math.min(max, value + delta));
        config.set(path, value);
        plugin.saveConfig();
    }

    private void changeKilltimeSeconds(int delta) {
        changeInt("game.killtime-seconds", delta, 0, 7200);
        gameManager.refreshGameTimerBar();
        gameManager.refreshAllPlayerDisplays();
    }

    private void cycleKilltimeMode() {
        KilltimeMode current = gameManager.killtimeMode();
        KilltimeMode next = current == KilltimeMode.PLAYER_COMBAT ? KilltimeMode.CORE_ONLY : KilltimeMode.PLAYER_COMBAT;
        plugin.getConfig().set("game.killtime-mode", next.configValue());
        plugin.saveConfig();
        gameManager.refreshGameTimerBar();
        gameManager.refreshAllPlayerDisplays();
    }

    private void changeWorldStartTime(int delta) {
        FileConfiguration config = plugin.getConfig();
        int current = config.getInt("world.start-time", 6000);
        int next = (current + delta) % 24000;
        if (next < 0) {
            next += 24000;
        }
        config.set("world.start-time", next);
        plugin.saveConfig();
    }

    private void cycleWorldDifficulty(int delta) {
        Difficulty[] difficulties = new Difficulty[] {
            Difficulty.PEACEFUL,
            Difficulty.EASY,
            Difficulty.NORMAL,
            Difficulty.HARD
        };
        Difficulty current = configuredWorldDifficulty();
        int index = 1;
        for (int i = 0; i < difficulties.length; i++) {
            if (difficulties[i] == current) {
                index = i;
                break;
            }
        }
        int next = (index + delta) % difficulties.length;
        if (next < 0) {
            next += difficulties.length;
        }
        plugin.getConfig().set("world.difficulty", difficulties[next].name());
        plugin.saveConfig();
    }

    private Difficulty configuredWorldDifficulty() {
        String value = plugin.getConfig().getString("world.difficulty", "EASY");
        if (value != null) {
            try {
                return Difficulty.valueOf(value.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                return Difficulty.EASY;
            }
        }
        return Difficulty.EASY;
    }

    private void setGameWorldToCurrent(Player player) {
        World world = player.getWorld();
        org.bukkit.Location lobby = gameManager.lobbyLocation();
        if (lobby != null && lobby.getWorld() != null && lobby.getWorld().equals(world)) {
            plugin.messages().send(player, "&c로비 월드는 게임 월드로 지정할 수 없습니다.");
            return;
        }
        plugin.getConfig().set("world.game-world", world.getName());
        plugin.saveConfig();
        gameManager.reloadSettings();
        plugin.messages().send(player, "&a현재 월드를 게임 월드로 지정했습니다: &f" + world.getName());
    }

    private void changePickaxeUnlockSeconds(String path, ClickType click) {
        int delta = 0;
        if (click == ClickType.LEFT) {
            delta = 60;
        } else if (click == ClickType.RIGHT) {
            delta = -60;
        } else if (click == ClickType.SHIFT_LEFT) {
            delta = 300;
        } else if (click == ClickType.SHIFT_RIGHT) {
            delta = -300;
        } else if (click == ClickType.MIDDLE) {
            plugin.getConfig().set(path, -1);
            plugin.saveConfig();
            return;
        }
        if (delta != 0) {
            FileConfiguration config = plugin.getConfig();
            int current = config.getInt(path, -1);
            int value = current < 0 && delta > 0 ? delta : current + delta;
            config.set(path, Math.max(-1, Math.min(7200, value)));
            plugin.saveConfig();
        }
    }

    private ItemStack pickaxeUnlockItem(String modernMaterial, String legacyMaterial, String title, String path) {
        int seconds = plugin.getConfig().getInt(path, -1);
        ChatColor color = seconds < 0 ? ChatColor.RED : ChatColor.GREEN;
        return item(modernMaterial, legacyMaterial, 1, (short) 0,
            color + title + " 허용 시간: " + pickaxeUnlockText(seconds),
            ChatColor.GRAY + "게임 시작부터 기다려야 하는 시간이에요.",
            ChatColor.GRAY + "좌클릭 +1분 / 우클릭 -1분",
            ChatColor.GRAY + "Shift를 누르면 5분씩 바꿔요.",
            ChatColor.GRAY + "가운데 클릭: 계속 보호");
    }

    private ItemStack explosionUnlockItem() {
        int seconds = plugin.getConfig().getInt("core.explosion-unlock-seconds", -1);
        ChatColor color = seconds < 0 ? ChatColor.RED : ChatColor.GREEN;
        return item("TNT", "TNT", 1, (short) 0,
            color + "코어 폭파 허용 시간: " + pickaxeUnlockText(seconds),
            ChatColor.GRAY + "시작 후 폭파 보호를 해제할 시간이에요.",
            ChatColor.GRAY + "폭파 보호가 켜져 있을 때 적용돼요.",
            ChatColor.GRAY + "좌클릭 +1분 / 우클릭 -1분",
            ChatColor.GRAY + "Shift를 누르면 5분씩 바꿔요.",
            ChatColor.GRAY + "가운데 클릭: 계속 보호");
    }

    private ItemStack coreProtectionMenuItem() {
        return item("DIAMOND_BLOCK", "DIAMOND_BLOCK", 1, (short) 0,
            ChatColor.YELLOW + "코어 보호 설정",
            ChatColor.GRAY + "곡괭이와 폭발로 심장을 깰 수 있는 시간을 정해요.",
            ChatColor.YELLOW + "클릭: 보호 시간 설정");
    }

    private String pickaxeUnlockText(int seconds) {
        if (seconds < 0) {
            return "해제 안 함";
        }
        if (seconds == 0) {
            return "즉시";
        }
        return durationText(seconds);
    }

    private String durationText(int seconds) {
        if (seconds < 0) {
            return "해제 안 함";
        }
        if (seconds == 0) {
            return "0초";
        }
        int minutes = seconds / 60;
        int remain = seconds % 60;
        if (minutes <= 0) {
            return remain + "초";
        }
        if (remain == 0) {
            return minutes + "분";
        }
        return minutes + "분 " + remain + "초";
    }

    private ItemStack worldStartTimeItem(Player player) {
        int time = plugin.getConfig().getInt("world.start-time", 6000);
        return item("WATCH", "WATCH", Math.max(1, Math.min(64, time / 1000 + 1)), (short) 0,
            ChatColor.YELLOW + "게임 시작 월드 시간",
            ChatColor.GRAY + "현재 설정: " + ChatColor.WHITE + worldTimeText(time),
            ChatColor.GRAY + "클릭: 현재 월드 시간으로 저장",
            ChatColor.GRAY + "현재 월드 시간: " + ChatColor.AQUA + worldTimeText((int) (player.getWorld().getTime() % 24000L)));
    }

    private ItemStack worldDifficultyItem() {
        Difficulty difficulty = configuredWorldDifficulty();
        return item(difficultyMaterial(difficulty), "IRON_SWORD", 1, (short) 0,
            ChatColor.YELLOW + "게임 시작 난이도: " + ChatColor.WHITE + difficulty.name(),
            ChatColor.GRAY + "좌클릭: 다음 난이도",
            ChatColor.GRAY + "우클릭: 이전 난이도",
            ChatColor.GRAY + "게임을 시작하면 모든 월드에 적용돼요.");
    }

    private ItemStack gameWorldSetItem(Player player) {
        String configured = plugin.getConfig().getString("world.game-world", "");
        String current = configured == null || configured.trim().length() == 0 ? "미지정" : configured;
        boolean currentWorldSelected = player.getWorld().getName().equalsIgnoreCase(current);
        ChatColor color = currentWorldSelected ? ChatColor.GREEN : ChatColor.YELLOW;
        return item("COMPASS", "COMPASS", 1, (short) 0,
            color + "현재 월드를 게임 월드로 지정",
            ChatColor.GRAY + "현재 위치: " + ChatColor.WHITE + player.getWorld().getName(),
            ChatColor.GRAY + "지정된 게임 월드: " + ChatColor.AQUA + current,
            ChatColor.GRAY + "자동 초기화를 켜면 종료 후 이 월드를 복원해요.");
    }

    private ItemStack gameWorldClearItem() {
        String configured = plugin.getConfig().getString("world.game-world", "");
        boolean empty = configured == null || configured.trim().length() == 0;
        return item("BARRIER", "BARRIER", 1, (short) 0,
            (empty ? ChatColor.RED : ChatColor.YELLOW) + "게임 월드 지정 해제",
            ChatColor.GRAY + "현재: " + ChatColor.WHITE + (empty ? "미지정" : configured),
            ChatColor.GRAY + "해제하면 월드 자동 초기화도 멈춰요.");
    }

    private String difficultyMaterial(Difficulty difficulty) {
        if (difficulty == Difficulty.PEACEFUL) {
            return "POPPY";
        }
        if (difficulty == Difficulty.NORMAL) {
            return "IRON_SWORD";
        }
        if (difficulty == Difficulty.HARD) {
            return "NETHERITE_SWORD";
        }
        return "STONE_SWORD";
    }

    private String worldTimeText(int time) {
        int normalized = time % 24000;
        if (normalized < 0) {
            normalized += 24000;
        }
        String phase;
        if (normalized < 6000) {
            phase = "아침";
        } else if (normalized < 12000) {
            phase = "낮";
        } else if (normalized < 18000) {
            phase = "저녁";
        } else {
            phase = "밤";
        }
        return normalized + "틱 (" + phase + ")";
    }

    private void changeRewardChance(String path, int index, int delta) {
        FileConfiguration config = plugin.getConfig();
        List<Map<?, ?>> source = config.getMapList(path);
        if (index < 0 || index >= source.size()) {
            return;
        }
        List<Map<String, Object>> updated = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < source.size(); i++) {
            Map<String, Object> copied = copyReward(source.get(i));
            if (i == index) {
                int current = intValue(copied.get("chance"), 0);
                copied.put("chance", Math.max(0, current + delta));
            }
            updated.add(copied);
        }
        config.set(path, updated);
        plugin.saveConfig();
    }

    private void changeRewardItemFromHand(Player player, String path, int index) {
        ItemStack item = player.getItemInHand();
        if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) {
            plugin.messages().send(player, "&c손에 든 아이템이 없습니다.");
            return;
        }
        changeRewardItem(path, index, item);
        plugin.messages().send(player, "&a도박 상품 #" + (index + 1) + " 아이템을 손에 든 아이템으로 변경했습니다.");
    }

    private void addRewardItemFromHand(Player player, String path) {
        ItemStack item = player.getItemInHand();
        if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) {
            plugin.messages().send(player, "&c손에 든 아이템이 없습니다.");
            return;
        }
        int index = addRewardItem(path, item);
        plugin.messages().send(player, "&a도박 상품 #" + (index + 1) + "을 손에 든 아이템으로 추가했습니다.");
    }

    private void changeRewardItem(String path, int index, ItemStack item) {
        FileConfiguration config = plugin.getConfig();
        List<Map<?, ?>> source = config.getMapList(path);
        if (index < 0 || index >= source.size()) {
            return;
        }
        List<Map<String, Object>> updated = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < source.size(); i++) {
            Map<String, Object> copied = copyReward(source.get(i));
            if (i == index) {
                ItemStack saved = item.clone();
                copied.put("item", saved);
                copied.put("material", saved.getType().name());
                copied.put("amount", saved.getAmount());
                copied.remove("legacy-material");
            }
            updated.add(copied);
        }
        config.set(path, updated);
        plugin.saveConfig();
    }

    private int addRewardItem(String path, ItemStack item) {
        FileConfiguration config = plugin.getConfig();
        List<Map<?, ?>> source = config.getMapList(path);
        List<Map<String, Object>> updated = new ArrayList<Map<String, Object>>();
        for (Map<?, ?> reward : source) {
            updated.add(copyReward(reward));
        }
        ItemStack saved = item.clone();
        Map<String, Object> reward = new LinkedHashMap<String, Object>();
        reward.put("chance", 1);
        reward.put("item", saved);
        reward.put("material", saved.getType().name());
        reward.put("amount", saved.getAmount());
        reward.put("message", "&a도박 상품에 당첨되었습니다!");
        updated.add(reward);
        config.set(path, updated);
        plugin.saveConfig();
        return updated.size() - 1;
    }

    private boolean isRewardItemClick(ClickType click) {
        return click == ClickType.MIDDLE || click == ClickType.DROP || click == ClickType.CONTROL_DROP;
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

    private void changeUrfCooldownPercent(int delta) {
        plugin.abilities().setUrfCooldownPercent(plugin.abilities().urfCooldownPercent() + delta);
    }

    private GodTeam teamAt(Player player, int slot) {
        int slotIndex = teamListSlotIndex(slot);
        if (slotIndex < 0) {
            return null;
        }
        int index = currentTeamPage(player) * TEAMS_PER_PAGE + slotIndex;
        GodTeam[] teams = GodTeam.values();
        return index >= 0 && index < teams.length ? teams[index] : null;
    }

    private GodTeam selectedTeam(Player player) {
        return GodTeam.parse(selectedTeamIds.get(player.getUniqueId()));
    }

    private boolean isTeamListSlot(int slot) {
        return teamListSlotIndex(slot) >= 0;
    }

    private int teamListSlotIndex(int slot) {
        for (int i = 0; i < TEAM_LIST_SLOTS.length; i++) {
            if (TEAM_LIST_SLOTS[i] == slot) {
                return i;
            }
        }
        return -1;
    }

    private void changeTeamPage(Player player, int delta) {
        setTeamPage(player, currentTeamPage(player) + delta);
    }

    private void setTeamPage(Player player, int page) {
        int max = lastTeamPage();
        int next = page;
        if (next < 0) {
            next = max;
        } else if (next > max) {
            next = 0;
        }
        teamPages.put(player.getUniqueId(), next);
    }

    private int currentTeamPage(Player player) {
        Integer page = teamPages.get(player.getUniqueId());
        return page == null ? 0 : Math.max(0, Math.min(lastTeamPage(), page.intValue()));
    }

    private int clampedTeamPage(Player player, int teamCount) {
        int max = Math.max(0, (Math.max(1, teamCount) - 1) / TEAMS_PER_PAGE);
        int page = currentTeamPage(player);
        if (page > max) {
            page = max;
            teamPages.put(player.getUniqueId(), page);
        }
        return page;
    }

    private int lastTeamPage() {
        return Math.max(0, (Math.max(1, GodTeam.values().length) - 1) / TEAMS_PER_PAGE);
    }

    private void removeTeam(Player player, GodTeam team) {
        if (teamMemberCount(team) > 0) {
            plugin.messages().send(player, "&c팀원이 있는 팀은 삭제할 수 없습니다. 먼저 팀 배정을 해제해주세요.");
            return;
        }
        if (!GodTeam.remove(plugin.getConfig(), team)) {
            plugin.messages().send(player, "&c기본 팀은 삭제할 수 없고, 팀은 최소 2개 이상 남아야 합니다.");
            return;
        }
        plugin.getConfig().set("spawns." + team.id(), null);
        plugin.getConfig().set("temples." + team.id(), null);
        plugin.saveConfig();
        gameManager.reloadSettings();
        setTeamPage(player, currentTeamPage(player));
        plugin.messages().send(player, "&a" + team.defaultDisplayName() + " 팀을 삭제했습니다.");
    }

    private void startTeamRename(Player player, GodTeam team) {
        UUID uuid = player.getUniqueId();
        pendingTeamRenameIds.put(uuid, team.id());
        openViewers.remove(uuid);
        openViews.remove(uuid);
        teamPages.remove(uuid);
        player.closeInventory();
        plugin.messages().send(player, "&e채팅에 새 팀 이름을 입력하세요. 취소하려면 &f취소&e 를 입력하세요.");
    }

    private void finishTeamRename(Player player, String teamId, String name) {
        GodTeam team = GodTeam.parse(teamId);
        if (team == null) {
            plugin.messages().send(player, "&c팀을 찾을 수 없어 이름 변경을 취소했습니다.");
            return;
        }
        if ("취소".equalsIgnoreCase(name) || "cancel".equalsIgnoreCase(name)) {
            plugin.messages().send(player, "&e팀 이름 변경을 취소했습니다.");
        } else if (name.length() == 0 || name.length() > 16) {
            plugin.messages().send(player, "&c팀 이름은 1~16글자로 입력해주세요.");
        } else {
            plugin.getConfig().set("teams." + team.id() + ".display-name", name);
            plugin.saveConfig();
            gameManager.reloadSettings();
            gameManager.refreshAllPlayerDisplays();
            plugin.messages().send(player, "&a팀 이름을 &f" + name + "&a 으로 변경했습니다.");
        }
        if (player.isOnline()) {
            selectedTeamIds.put(player.getUniqueId(), team.id());
            open(player, SettingsView.TEAM_DETAIL);
        }
    }

    private void toggleTeamEnabled(Player player, GodTeam team) {
        boolean enabled = gameManager.isTeamEnabled(team);
        if (enabled && gameManager.activeTeams().size() <= 2) {
            plugin.messages().send(player, "&c활성 팀은 최소 2개 이상이어야 합니다.");
            return;
        }
        int members = teamMemberCount(team);
        plugin.getConfig().set("teams." + team.id() + ".enabled", !enabled);
        plugin.saveConfig();
        gameManager.reloadSettings();
        if (enabled && members > 0) {
            plugin.messages().send(player, "&a" + gameManager.teamColoredName(team) + " 팀을 비활성화했습니다. "
                + "&e배정되어 있던 " + members + "명은 팀에서 해제되었습니다.");
        } else {
            plugin.messages().send(player, "&a" + gameManager.teamColoredName(team) + " 팀을 "
                + (!enabled ? "활성화" : "비활성화") + "했습니다.");
        }
    }

    private int teamMemberCount(GodTeam team) {
        int count = 0;
        for (GodTeam assigned : gameManager.teamAssignments().values()) {
            if (team.equals(assigned)) {
                count++;
            }
        }
        return count;
    }

    private void cycleTeamColor(GodTeam team, int direction) {
        ChatColor current = gameManager.teamColor(team);
        int currentIndex = 0;
        for (int i = 0; i < TEAM_COLORS.length; i++) {
            if (TEAM_COLORS[i] == current) {
                currentIndex = i;
                break;
            }
        }
        int nextIndex = (currentIndex + direction) % TEAM_COLORS.length;
        if (nextIndex < 0) {
            nextIndex += TEAM_COLORS.length;
        }
        plugin.getConfig().set("teams." + team.id() + ".color", TEAM_COLORS[nextIndex].name());
        plugin.saveConfig();
    }

    private void setTempleFromTarget(Player player, GodTeam team) {
        Block block = BukkitCompat.getTargetBlock(player, 8);
        if (block == null || block.getType() != Material.DIAMOND_BLOCK) {
            plugin.messages().send(player, "&c바라보는 블록이 다이아몬드 블록이어야 합니다.");
            return;
        }
        if (gameManager.setTemple(team, block)) {
            plugin.messages().send(player, "&a" + gameManager.teamColoredName(team) + " 팀의 다이아 심장을 등록했습니다.");
        } else {
            plugin.messages().send(player, "&c이미 다른 팀의 심장으로 등록된 블록입니다. 다른 다이아몬드 블록을 선택하세요.");
        }
    }

    private String colorLabel(ChatColor color) {
        if (color == ChatColor.RED) return "빨강";
        if (color == ChatColor.BLUE) return "파랑";
        if (color == ChatColor.GREEN) return "초록";
        if (color == ChatColor.YELLOW) return "노랑";
        if (color == ChatColor.AQUA) return "하늘";
        if (color == ChatColor.GOLD) return "금색";
        if (color == ChatColor.LIGHT_PURPLE) return "분홍";
        if (color == ChatColor.WHITE) return "흰색";
        if (color == ChatColor.GRAY) return "회색";
        if (color == ChatColor.DARK_RED) return "진한 빨강";
        if (color == ChatColor.DARK_BLUE) return "진한 파랑";
        if (color == ChatColor.DARK_GREEN) return "진한 초록";
        if (color == ChatColor.DARK_AQUA) return "진한 하늘";
        if (color == ChatColor.DARK_PURPLE) return "보라";
        if (color == ChatColor.DARK_GRAY) return "진한 회색";
        if (color == ChatColor.BLACK) return "검정";
        return color.name();
    }

    private String teamWoolMaterial(GodTeam team) {
        return woolMaterial(gameManager.teamColor(team));
    }

    private short teamWoolData(GodTeam team) {
        return woolData(gameManager.teamColor(team));
    }

    private String woolMaterial(ChatColor color) {
        if (color == ChatColor.RED) return "RED_WOOL";
        if (color == ChatColor.BLUE) return "BLUE_WOOL";
        if (color == ChatColor.GREEN) return "GREEN_WOOL";
        if (color == ChatColor.YELLOW) return "YELLOW_WOOL";
        if (color == ChatColor.AQUA) return "LIGHT_BLUE_WOOL";
        if (color == ChatColor.GOLD) return "ORANGE_WOOL";
        if (color == ChatColor.LIGHT_PURPLE) return "PINK_WOOL";
        if (color == ChatColor.GRAY) return "LIGHT_GRAY_WOOL";
        if (color == ChatColor.DARK_RED) return "RED_WOOL";
        if (color == ChatColor.DARK_BLUE) return "BLUE_WOOL";
        if (color == ChatColor.DARK_GREEN) return "GREEN_WOOL";
        if (color == ChatColor.DARK_AQUA) return "CYAN_WOOL";
        if (color == ChatColor.DARK_PURPLE) return "PURPLE_WOOL";
        if (color == ChatColor.DARK_GRAY) return "GRAY_WOOL";
        if (color == ChatColor.BLACK) return "BLACK_WOOL";
        return "WHITE_WOOL";
    }

    private short woolData(ChatColor color) {
        if (color == ChatColor.RED) return 14;
        if (color == ChatColor.BLUE) return 11;
        if (color == ChatColor.GREEN) return 13;
        if (color == ChatColor.YELLOW) return 4;
        if (color == ChatColor.AQUA) return 3;
        if (color == ChatColor.GOLD) return 1;
        if (color == ChatColor.LIGHT_PURPLE) return 6;
        if (color == ChatColor.GRAY) return 8;
        if (color == ChatColor.DARK_RED) return 14;
        if (color == ChatColor.DARK_BLUE) return 11;
        if (color == ChatColor.DARK_GREEN) return 13;
        if (color == ChatColor.DARK_AQUA) return 9;
        if (color == ChatColor.DARK_PURPLE) return 10;
        if (color == ChatColor.DARK_GRAY) return 7;
        if (color == ChatColor.BLACK) return 15;
        return 0;
    }

    private int participantCount() {
        int count = 0;
        for (Player player : BukkitCompat.onlinePlayers()) {
            if (gameManager.teamOf(player) != null && !gameManager.isObserver(player)) {
                count++;
            }
        }
        return count;
    }

    private ItemStack item(String modernMaterial, String legacyMaterial, int amount, short damage, String name, String... lore) {
        Material material = material(modernMaterial, legacyMaterial);
        ItemStack stack = new ItemStack(material, Math.max(1, Math.min(64, amount)), damage);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES, org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            if (lore != null && lore.length > 0) {
                meta.setLore(GuiText.wrap(Arrays.asList(lore)));
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack rewardItem(String path, Map<?, ?> reward, int index, int totalChance) {
        ItemStack preview = rewardStack(reward);
        int amount = reward.get("item") instanceof ItemStack ? Math.max(1, preview.getAmount())
            : Math.max(1, intValue(reward.get("amount"), 1));
        int chance = Math.max(0, intValue(reward.get("chance"), 0));
        int percent = totalChance <= 0 ? 0 : (int) Math.round((chance * 100.0D) / totalChance);
        return displayItem(preview, amount,
            ChatColor.YELLOW + rewardName(reward, index),
            ChatColor.GRAY + "당첨 확률: 약 " + ChatColor.AQUA + percent + "%" + ChatColor.GRAY + " · 가중치 " + chance,
            ChatColor.GRAY + "수량: " + ChatColor.WHITE + amount + "개",
            ChatColor.GRAY + "좌클릭 +1 / 우클릭 -1",
            ChatColor.GRAY + "Shift를 누르면 5씩 바꿔요.",
            ChatColor.GRAY + "Q/가운데 클릭: 손에 든 아이템으로 바꾸기");
    }

    private ItemStack addRewardItem(int index) {
        return item("CHEST", "CHEST", 1, (short) 0,
            ChatColor.GREEN + "상품 추가 #" + (index + 1),
            ChatColor.GRAY + "손에 아이템을 들고 이 슬롯에서 Q",
            ChatColor.GRAY + "또는 가운데 클릭으로 새 상품 추가",
            ChatColor.GRAY + "기본 확률 가중치: " + ChatColor.WHITE + "1");
    }

    private String rewardType(String path) {
        return "normal";
    }

    private ItemStack rewardStack(Map<?, ?> reward) {
        Object configuredItem = reward.get("item");
        if (configuredItem instanceof ItemStack) {
            ItemStack stack = ((ItemStack) configuredItem).clone();
            return stack.getType() == Material.AIR ? new ItemStack(Material.CHEST) : stack;
        }
        String modern = stringValue(reward.get("material"), "CHEST");
        String legacy = stringValue(reward.get("legacy-material"), modern);
        return new ItemStack(material(modern, legacy));
    }

    private ItemStack displayItem(ItemStack base, int amount, String name, String... lore) {
        ItemStack stack = base == null ? new ItemStack(Material.CHEST) : base.clone();
        stack.setAmount(Math.max(1, Math.min(64, amount)));
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES, org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            if (lore != null && lore.length > 0) {
                meta.setLore(GuiText.wrap(Arrays.asList(lore)));
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private String rewardName(Map<?, ?> reward, int index) {
        Object message = reward.get("message");
        if (message != null && message.toString().trim().length() > 0) {
            return trim(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', message.toString())), 22);
        }
        Object messages = reward.get("messages");
        if (messages instanceof Iterable<?>) {
            for (Object line : (Iterable<?>) messages) {
                if (line != null && line.toString().trim().length() > 0) {
                    return trim(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', line.toString())), 22);
                }
            }
        }
        return "상품 " + (index + 1);
    }

    private int totalChance(String path) {
        int total = 0;
        for (Map<?, ?> reward : plugin.getConfig().getMapList(path)) {
            total += Math.max(0, intValue(reward.get("chance"), 0));
        }
        return total;
    }

    private int intValue(Object value, int fallback) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private String stringValue(Object value, String fallback) {
        return value == null ? fallback : value.toString();
    }

    private String trim(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private Material material(String modernName, String legacyName) {
        // Old configuration/control names must remain recognizable without the optional pack.
        if ("WATCH".equals(modernName)) modernName = "CLOCK";
        if ("BED".equals(modernName)) modernName = "RED_BED";
        if ("REDSTONE_COMPARATOR".equals(modernName)) modernName = "COMPARATOR";
        if ("INK_SACK".equals(modernName)) modernName = "INK_SAC";
        Material modern = Material.matchMaterial(modernName);
        if (modern != null) {
            return modern;
        }
        if ("GRASS_BLOCK".equals(legacyName)) legacyName = "GRASS";
        if ("COMMAND_BLOCK".equals(legacyName)) legacyName = "COMMAND";
        if ("WHITE_WOOL".equals(legacyName)) legacyName = "WOOL";
        if ("FIREWORK_ROCKET".equals(legacyName)) legacyName = "FIREWORK";
        if ("TOTEM_OF_UNDYING".equals(legacyName)) legacyName = "TOTEM";
        if ("CLOCK".equals(legacyName)) legacyName = "WATCH";
        Material legacy = Material.matchMaterial(legacyName);
        if (legacy != null) {
            return legacy;
        }
        return Material.STONE;
    }

}
