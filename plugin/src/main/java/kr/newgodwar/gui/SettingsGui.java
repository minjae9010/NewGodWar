package kr.newgodwar.gui;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.ability.AbilityManager;
import kr.newgodwar.ability.api.AbilityDefinition;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class SettingsGui implements Listener {

    private static final String TITLE = ChatColor.DARK_PURPLE + "신들의 전쟁 설정";
    private static final int SIZE = 54;

    private final NewGodWarPlugin plugin;
    private final GameManager gameManager;
    private final AbilityManager abilityManager;
    private final Set<UUID> openViewers = new HashSet<UUID>();

    public SettingsGui(NewGodWarPlugin plugin, GameManager gameManager, AbilityManager abilityManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.abilityManager = abilityManager;
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

        handle(player, event.getRawSlot(), event.isRightClick(), event.isShiftClick());
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
            && event.getRawSlot() < SIZE;
    }

    private boolean isSettingsInventory(InventoryDragEvent event) {
        return openViewers.contains(event.getWhoClicked().getUniqueId())
            && event.getView() != null
            && TITLE.equals(event.getView().getTitle());
    }

    private void handle(Player player, int slot, boolean rightClick, boolean shiftClick) {
        if (slot < 0 || slot >= SIZE) {
            return;
        }

        if (slot == 10) {
            changeInt("game.min-players", -1, 1, 100, false);
            reopen(player);
            return;
        }
        if (slot == 12) {
            changeInt("game.min-players", 1, 1, 100, false);
            reopen(player);
            return;
        }
        if (slot == 14) {
            toggle("game.friendly-fire");
            gameManager.reloadSettings();
            reopen(player);
            return;
        }
        if (slot == 15) {
            toggle("game.auto-balance-teams");
            reopen(player);
            return;
        }
        if (slot == 16) {
            toggle("game.ability-roll-message");
            reopen(player);
            return;
        }
        if (slot == 19) {
            changeInt("game.announce-radius", -10, 0, 10000, false);
            reopen(player);
            return;
        }
        if (slot == 21) {
            changeInt("game.announce-radius", 10, 0, 10000, false);
            reopen(player);
            return;
        }
        if (slot == 22) {
            toggle("game.urf.enabled");
            reopen(player);
            return;
        }
        if (slot == 23) {
            changeDouble("game.urf.cooldown-multiplier", rightClick ? 0.05D : -0.05D, 0.0D, 1.0D, shiftClick);
            reopen(player);
            return;
        }
        if (slot == 24) {
            toggle("gamerules.enabled");
            reopen(player);
            return;
        }
        if (slot == 28) {
            try {
                gameManager.start();
            } catch (IllegalStateException ex) {
                plugin.messages().send(player, "&c" + ex.getMessage());
            }
            reopen(player);
            return;
        }
        if (slot == 29) {
            gameManager.stop(true);
            reopen(player);
            return;
        }
        if (slot == 30) {
            gameManager.autoBalance();
            plugin.messages().send(player, "&a온라인 플레이어를 자동으로 팀 배정했습니다.");
            reopen(player);
            return;
        }
        if (slot == 31) {
            plugin.reloadConfig();
            gameManager.reloadSettings();
            plugin.messages().send(player, "&a설정을 다시 불러왔습니다.");
            reopen(player);
            return;
        }
        if (slot == 33) {
            player.closeInventory();
            return;
        }
        if (slot == 45) {
            changeDouble("abilities.zeus.lightning-chance", rightClick ? -0.01D : 0.01D, 0.0D, 1.0D, shiftClick);
            reopen(player);
            return;
        }
        if (slot == 46) {
            changeInt("abilities.zeus.cooldown-seconds", rightClick ? -1 : 1, 0, 300, shiftClick);
            reopen(player);
            return;
        }
        if (slot == 47) {
            changeDouble("abilities.ares.damage-bonus", rightClick ? -0.05D : 0.05D, 0.0D, 10.0D, shiftClick);
            reopen(player);
            return;
        }
        if (slot == 48) {
            changeInt("abilities.hermes.speed-amplifier", rightClick ? -1 : 1, 0, 10, shiftClick);
            reopen(player);
            return;
        }
        if (slot == 49) {
            changeInt("abilities.poseidon.water-heal-interval-seconds", rightClick ? -1 : 1, 1, 300, shiftClick);
            reopen(player);
            return;
        }
        if (slot == 50) {
            changeDouble("abilities.poseidon.water-heal-amount", rightClick ? -0.5D : 0.5D, 0.0D, 20.0D, shiftClick);
            reopen(player);
            return;
        }

        AbilityDefinition ability = abilityAtSlot(slot);
        if (ability != null) {
            String path = "abilities." + ability.id() + ".enabled";
            plugin.getConfig().set(path, !abilityManager.isEnabled(ability));
            plugin.saveConfig();
            reopen(player);
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
        inventory.setItem(4, item("NETHER_STAR", "NETHER_STAR", 1, (short) 0,
            ChatColor.GOLD + "신들의 전쟁 관리",
            ChatColor.GRAY + "상태: " + ChatColor.YELLOW + gameManager.state(),
            ChatColor.GRAY + "참가자: " + ChatColor.YELLOW + participantCount(),
            ChatColor.GRAY + "마인크래프트: " + ChatColor.YELLOW + plugin.versionSupport().minecraftVersion()));

        inventory.setItem(10, item("REDSTONE_TORCH", "REDSTONE_TORCH_ON", 1, (short) 0,
            ChatColor.RED + "최소 인원 -1",
            ChatColor.GRAY + "현재: " + ChatColor.YELLOW + config.getInt("game.min-players", 2)));
        inventory.setItem(11, item("PLAYER_HEAD", "SKULL_ITEM", config.getInt("game.min-players", 2), (short) 3,
            ChatColor.YELLOW + "최소 시작 인원",
            ChatColor.GRAY + "게임 시작에 필요한 팀 배정 인원입니다."));
        inventory.setItem(12, item("TORCH", "TORCH", 1, (short) 0,
            ChatColor.GREEN + "최소 인원 +1",
            ChatColor.GRAY + "현재: " + ChatColor.YELLOW + config.getInt("game.min-players", 2)));

        inventory.setItem(14, toggleItem("game.friendly-fire", "팀킬 허용", "IRON_SWORD"));
        inventory.setItem(15, toggleItem("game.auto-balance-teams", "빈 팀이면 자동 배정", "COMPASS"));
        inventory.setItem(16, toggleItem("game.ability-roll-message", "능력 배정 안내", "PAPER"));

        inventory.setItem(19, item("REDSTONE", "REDSTONE", 1, (short) 0,
            ChatColor.RED + "알림 반경 -10",
            ChatColor.GRAY + "현재: " + ChatColor.YELLOW + config.getInt("game.announce-radius", 0)));
        inventory.setItem(20, item("MAP", "MAP", 1, (short) 0,
            ChatColor.YELLOW + "알림 반경",
            ChatColor.GRAY + "0이면 전체 방송으로 취급합니다.",
            ChatColor.GRAY + "현재: " + ChatColor.YELLOW + config.getInt("game.announce-radius", 0)));
        inventory.setItem(21, item("GLOWSTONE_DUST", "GLOWSTONE_DUST", 1, (short) 0,
            ChatColor.GREEN + "알림 반경 +10",
            ChatColor.GRAY + "현재: " + ChatColor.YELLOW + config.getInt("game.announce-radius", 0)));
        inventory.setItem(22, toggleItem("game.urf.enabled", "우르프 모드", "BLAZE_POWDER"));
        inventory.setItem(23, numberItem("우르프 쿨타임 배율", "game.urf.cooldown-multiplier", 0.2D, "왼쪽 -0.05 / 오른쪽 +0.05"));
        inventory.setItem(24, toggleItem("gamerules.enabled", "게임룰 자동 적용", "COMMAND"));

        inventory.setItem(28, item("EMERALD_BLOCK", "EMERALD_BLOCK", 1, (short) 0,
            ChatColor.GREEN + "게임 시작",
            ChatColor.GRAY + "/godwar start"));
        inventory.setItem(29, item("REDSTONE_BLOCK", "REDSTONE_BLOCK", 1, (short) 0,
            ChatColor.RED + "게임 종료",
            ChatColor.GRAY + "/godwar stop"));
        inventory.setItem(30, item("COMPASS", "COMPASS", 1, (short) 0,
            ChatColor.AQUA + "팀 자동 배정",
            ChatColor.GRAY + "/godwar autoteam"));
        inventory.setItem(31, item("BOOK", "BOOK", 1, (short) 0,
            ChatColor.YELLOW + "설정 다시 불러오기",
            ChatColor.GRAY + "config.yml을 다시 읽고 GUI를 갱신합니다."));
        inventory.setItem(33, item("BARRIER", "BARRIER", 1, (short) 0,
            ChatColor.RED + "닫기"));

        int[] slots = new int[] {36, 37, 38, 39, 40, 41, 42, 43};
        int index = 0;
        for (AbilityDefinition ability : abilityManager.registry().all()) {
            if (index >= slots.length) {
                break;
            }
            inventory.setItem(slots[index], abilityItem(ability));
            index++;
        }

        inventory.setItem(45, numberItem("제우스 번개 확률", "abilities.zeus.lightning-chance", 0.18D, "왼쪽 +0.01 / 오른쪽 -0.01"));
        inventory.setItem(46, numberItem("제우스 쿨타임", "abilities.zeus.cooldown-seconds", 8, "왼쪽 +1초 / 오른쪽 -1초"));
        inventory.setItem(47, numberItem("아레스 피해 배율", "abilities.ares.damage-bonus", 1.25D, "왼쪽 +0.05 / 오른쪽 -0.05"));
        inventory.setItem(48, numberItem("헤르메스 속도 증폭", "abilities.hermes.speed-amplifier", 1, "왼쪽 +1 / 오른쪽 -1"));
        inventory.setItem(49, numberItem("포세이돈 회복 간격", "abilities.poseidon.water-heal-interval-seconds", 5, "왼쪽 +1초 / 오른쪽 -1초"));
        inventory.setItem(50, numberItem("포세이돈 회복량", "abilities.poseidon.water-heal-amount", 1.0D, "왼쪽 +0.5 / 오른쪽 -0.5"));
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

    private ItemStack abilityItem(AbilityDefinition ability) {
        boolean enabled = abilityManager.isEnabled(ability);
        ChatColor color = enabled ? ChatColor.GREEN : ChatColor.RED;
        String state = enabled ? "활성" : "비활성";
        return item(enabled ? "ENCHANTED_BOOK" : "BOOK", enabled ? "ENCHANTED_BOOK" : "BOOK", 1, (short) 0,
            color + ability.name() + ChatColor.GRAY + " (" + ability.id() + ")",
            ChatColor.GRAY + "상태: " + color + state,
            ChatColor.GRAY + ability.description(),
            ChatColor.YELLOW + "일반: " + ChatColor.GRAY + ability.normalSkill(),
            ChatColor.YELLOW + "일반 돌 소모: " + ChatColor.GRAY + stoneCost(ability.normalStoneCost()),
            ChatColor.GOLD + "고급: " + ChatColor.GRAY + ability.advancedSkill(),
            ChatColor.GOLD + "고급 돌 소모: " + ChatColor.GRAY + stoneCost(ability.advancedStoneCost()),
            ChatColor.AQUA + "패시브: " + ChatColor.GRAY + ability.passiveSkill(),
            ChatColor.DARK_GRAY + "클릭하면 능력 사용 여부가 전환됩니다.");
    }

    private String stoneCost(int cost) {
        return cost <= 0 ? "없음" : cost + "개";
    }

    private ItemStack numberItem(String title, String path, int fallback, String clickHint) {
        int value = plugin.getConfig().getInt(path, fallback);
        return item("COMPARATOR", "REDSTONE_COMPARATOR", 1, (short) 0,
            ChatColor.AQUA + title,
            ChatColor.GRAY + "현재: " + ChatColor.YELLOW + value,
            ChatColor.GRAY + clickHint,
            ChatColor.DARK_GRAY + "Shift 클릭은 5배로 조정됩니다.");
    }

    private ItemStack numberItem(String title, String path, double fallback, String clickHint) {
        double value = plugin.getConfig().getDouble(path, fallback);
        return item("COMPARATOR", "REDSTONE_COMPARATOR", 1, (short) 0,
            ChatColor.AQUA + title,
            ChatColor.GRAY + "현재: " + ChatColor.YELLOW + trim(value),
            ChatColor.GRAY + clickHint,
            ChatColor.DARK_GRAY + "Shift 클릭은 5배로 조정됩니다.");
    }

    private AbilityDefinition abilityAtSlot(int slot) {
        int[] slots = new int[] {36, 37, 38, 39, 40, 41, 42, 43};
        int index = -1;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return null;
        }

        int current = 0;
        for (AbilityDefinition ability : abilityManager.registry().all()) {
            if (current == index) {
                return ability;
            }
            current++;
        }
        return null;
    }

    private void toggle(String path) {
        FileConfiguration config = plugin.getConfig();
        config.set(path, !config.getBoolean(path, false));
        plugin.saveConfig();
    }

    private void changeInt(String path, int delta, int min, int max, boolean multiply) {
        FileConfiguration config = plugin.getConfig();
        int value = config.getInt(path, min);
        if (multiply) {
            delta *= 5;
        }
        value = Math.max(min, Math.min(max, value + delta));
        config.set(path, value);
        plugin.saveConfig();
    }

    private void changeDouble(String path, double delta, double min, double max, boolean multiply) {
        FileConfiguration config = plugin.getConfig();
        double value = config.getDouble(path, min);
        if (multiply) {
            delta *= 5.0D;
        }
        value = Math.max(min, Math.min(max, value + delta));
        config.set(path, Math.round(value * 100.0D) / 100.0D);
        plugin.saveConfig();
    }

    private String trim(double value) {
        double rounded = Math.round(value * 100.0D) / 100.0D;
        if (rounded == Math.rint(rounded)) {
            return String.valueOf((int) rounded);
        }
        return String.valueOf(rounded);
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
