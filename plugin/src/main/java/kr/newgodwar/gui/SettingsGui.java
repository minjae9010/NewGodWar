package kr.newgodwar.gui;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.game.GameManager;
import kr.newgodwar.util.BukkitCompat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class SettingsGui implements Listener {

    private static final String TITLE = ChatColor.BLACK + ":::::: 설정 ::::::";
    private static final int SIZE = 27;

    private final NewGodWarPlugin plugin;
    private final GameManager gameManager;
    private final Set<UUID> openViewers = new HashSet<UUID>();

    public SettingsGui(NewGodWarPlugin plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(player, SIZE, TITLE);
        fill(inventory);
        player.openInventory(inventory);
        openViewers.add(player.getUniqueId());
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

        handle(player, event.getRawSlot());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (isSettingsInventory(event)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        openViewers.remove(event.getPlayer().getUniqueId());
    }

    private boolean isSettingsInventory(InventoryClickEvent event) {
        return openViewers.contains(event.getWhoClicked().getUniqueId())
            && event.getView() != null
            && TITLE.equals(event.getView().getTitle())
            && event.getRawSlot() >= 0
            && event.getRawSlot() < SIZE;
    }

    private boolean isSettingsInventory(InventoryDragEvent event) {
        return openViewers.contains(event.getWhoClicked().getUniqueId())
            && event.getView() != null
            && TITLE.equals(event.getView().getTitle());
    }

    private void handle(Player player, int slot) {
        if (slot == 0) {
            changeInt("game.min-players", -1, 1, 100);
            reopen(player);
            return;
        }
        if (slot == 2) {
            changeInt("game.min-players", 1, 1, 100);
            reopen(player);
            return;
        }
        if (slot == 4) {
            toggle("game.friendly-fire");
            gameManager.reloadSettings();
            reopen(player);
            return;
        }
        if (slot == 6) {
            toggle("game.auto-balance-teams");
            reopen(player);
            return;
        }
        if (slot == 8) {
            toggle("game.ability-roll-message");
            reopen(player);
            return;
        }
        if (slot == 10) {
            toggle("game.allow-mid-join");
            gameManager.refreshAllPlayerDisplays();
            reopen(player);
            return;
        }
        if (slot == 11) {
            toggle("game.urf.enabled");
            gameManager.refreshAllPlayerDisplays();
            reopen(player);
            return;
        }
        if (slot == 12) {
            toggle("gamerules.enabled");
            reopen(player);
            return;
        }
        if (slot == 14) {
            toggle("scoreboard.enabled");
            gameManager.refreshAllPlayerDisplays();
            reopen(player);
            return;
        }
        if (slot == 16) {
            toggle("scoreboard.team-prefixes");
            gameManager.refreshAllPlayerDisplays();
            reopen(player);
            return;
        }
        if (slot == 18) {
            try {
                gameManager.start();
            } catch (IllegalStateException ex) {
                plugin.messages().send(player, "&c" + ex.getMessage());
            }
            reopen(player);
            return;
        }
        if (slot == 20) {
            gameManager.stop(true);
            reopen(player);
            return;
        }
        if (slot == 22) {
            gameManager.autoBalance();
            plugin.messages().send(player, "&a온라인 플레이어를 자동으로 팀 배정했습니다.");
            reopen(player);
            return;
        }
        if (slot == 24) {
            plugin.reloadConfig();
            gameManager.reloadSettings();
            plugin.messages().send(player, "&a설정을 다시 불러왔습니다.");
            reopen(player);
            return;
        }
        if (slot == 26) {
            player.closeInventory();
        }
    }

    private void reopen(final Player player) {
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (player.isOnline() && openViewers.contains(player.getUniqueId())) {
                    open(player);
                }
            }
        });
    }

    private void fill(Inventory inventory) {
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, item("BLACK_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", 1, (short) 15, " "));
        }

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

        inventory.setItem(4, toggleItem("game.friendly-fire", "팀킬 허용", "IRON_SWORD"));
        inventory.setItem(6, toggleItem("game.auto-balance-teams", "팀 자동 배정", "COMPASS"));
        inventory.setItem(8, toggleItem("game.ability-roll-message", "능력 배정 안내", "PAPER"));
        inventory.setItem(10, toggleItem("game.allow-mid-join", "중간 참여 허용", "ENDER_PEARL"));
        inventory.setItem(11, urfItem());
        inventory.setItem(12, toggleItem("gamerules.enabled", "게임룰 자동 적용", "COMMAND"));
        inventory.setItem(14, toggleItem("scoreboard.enabled", "스코어보드 안내 사용", "ITEM_FRAME"));
        inventory.setItem(16, toggleItem("scoreboard.team-prefixes", "팀 Prefix 표시", "NAME_TAG"));

        inventory.setItem(18, item("EMERALD_BLOCK", "EMERALD_BLOCK", 1, (short) 0,
            ChatColor.GREEN + "게임 시작",
            ChatColor.GRAY + "/t start"));
        inventory.setItem(20, item("REDSTONE_BLOCK", "REDSTONE_BLOCK", 1, (short) 0,
            ChatColor.RED + "게임 종료",
            ChatColor.GRAY + "/t stop"));
        inventory.setItem(22, item("COMPASS", "COMPASS", 1, (short) 0,
            ChatColor.AQUA + "팀 자동 배정",
            ChatColor.GRAY + "/gw autoteam"));
        inventory.setItem(24, item("BOOK", "BOOK", 1, (short) 0,
            ChatColor.YELLOW + "설정 다시 불러오기",
            ChatColor.GRAY + "config.yml을 다시 읽습니다."));
        inventory.setItem(26, item("BARRIER", "BARRIER", 1, (short) 0,
            ChatColor.RED + "닫기"));
    }

    private ItemStack urfItem() {
        boolean enabled = plugin.getConfig().getBoolean("game.urf.enabled", false);
        ChatColor color = enabled ? ChatColor.GREEN : ChatColor.RED;
        String state = enabled ? "켜짐" : "꺼짐";
        double multiplier = plugin.getConfig().getDouble("game.urf.cooldown-multiplier", 0.2D);
        return item("BLAZE_POWDER", "BLAZE_POWDER", 1, (short) 0,
            color + "우르프 모드: " + state,
            ChatColor.GRAY + "능력 쿨타임 배율: " + ChatColor.YELLOW + multiplier,
            ChatColor.GRAY + "클릭하면 설정이 전환됩니다.",
            ChatColor.DARK_GRAY + "game.urf.enabled");
    }

    private ItemStack toggleItem(String path, String title, String icon) {
        boolean enabled = plugin.getConfig().getBoolean(path, false);
        ChatColor color = enabled ? ChatColor.GREEN : ChatColor.RED;
        String state = enabled ? "켜짐" : "꺼짐";
        return item(icon, icon, 1, (short) 0,
            color + title + ": " + state,
            ChatColor.GRAY + "클릭하면 설정이 전환됩니다.",
            ChatColor.DARK_GRAY + path);
    }

    private void toggle(String path) {
        FileConfiguration config = plugin.getConfig();
        config.set(path, !config.getBoolean(path, false));
        plugin.saveConfig();
    }

    private void changeInt(String path, int delta, int min, int max) {
        FileConfiguration config = plugin.getConfig();
        int value = config.getInt(path, min);
        value = Math.max(min, Math.min(max, value + delta));
        config.set(path, value);
        plugin.saveConfig();
    }

    private int participantCount() {
        int count = 0;
        for (Player player : BukkitCompat.onlinePlayers()) {
            if (gameManager.teamOf(player) != null) {
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
            if (lore != null && lore.length > 0) {
                meta.setLore(new ArrayList<String>(Arrays.asList(lore)));
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private Material material(String modernName, String legacyName) {
        Material modern = Material.matchMaterial(modernName);
        if (modern != null) {
            return modern;
        }
        Material legacy = Material.matchMaterial(legacyName);
        if (legacy != null) {
            return legacy;
        }
        return Material.STONE;
    }
}
