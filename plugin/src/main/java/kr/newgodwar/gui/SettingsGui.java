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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SettingsGui implements Listener {

    private static final int SIZE = 27;
    private static final int BACK_SLOT = 22;
    private static final int CLOSE_SLOT = 26;

    private final NewGodWarPlugin plugin;
    private final GameManager gameManager;
    private final Set<UUID> openViewers = new HashSet<UUID>();
    private final Map<UUID, SettingsView> openViews = new HashMap<UUID, SettingsView>();

    public SettingsGui(NewGodWarPlugin plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    public void open(Player player) {
        open(player, SettingsView.MAIN);
    }

    private void open(Player player, SettingsView view) {
        Inventory inventory = Bukkit.createInventory(player, SIZE, view.title);
        fill(inventory, view);
        player.openInventory(inventory);
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
        handle(player, view, event.getRawSlot(), event.getClick());
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
        openViews.remove(event.getPlayer().getUniqueId());
    }

    private boolean isSettingsInventory(InventoryClickEvent event) {
        return event.getView() != null
            && openViewers.contains(event.getWhoClicked().getUniqueId())
            && viewOf(event.getView().getTitle()) != null
            && event.getRawSlot() >= 0
            && event.getRawSlot() < SIZE;
    }

    private boolean isSettingsInventory(InventoryDragEvent event) {
        return event.getView() != null
            && openViewers.contains(event.getWhoClicked().getUniqueId())
            && viewOf(event.getView().getTitle()) != null;
    }

    private SettingsView viewOf(String title) {
        for (SettingsView view : SettingsView.values()) {
            if (view.title.equals(title)) {
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

        if (view == SettingsView.MAIN) {
            handleMain(player, slot);
        } else if (view == SettingsView.GAME) {
            handleGame(player, slot);
        } else if (view == SettingsView.WORLD_CORE) {
            handleWorldCore(player, slot);
        } else if (view == SettingsView.DISPLAY) {
            handleDisplay(player, slot, click);
        } else if (view == SettingsView.GAMBLING) {
            handleGambling(player, slot);
        } else if (view == SettingsView.GAMBLING_NORMAL) {
            handleRewardChance(player, "gambling.rewards.normal", slot, click);
        } else if (view == SettingsView.GAMBLING_TAJJA) {
            handleRewardChance(player, "gambling.rewards.tajja", slot, click);
        }

        reopen(player, currentView(player));
    }

    private void handleMain(Player player, int slot) {
        if (slot == 10) {
            switchView(player, SettingsView.GAME);
        } else if (slot == 12) {
            switchView(player, SettingsView.WORLD_CORE);
        } else if (slot == 14) {
            switchView(player, SettingsView.DISPLAY);
        } else if (slot == 16) {
            switchView(player, SettingsView.GAMBLING);
        } else if (slot == 24) {
            plugin.reloadConfig();
            gameManager.reloadSettings();
            plugin.messages().send(player, "&a설정을 다시 불러왔습니다.");
        }
    }

    private void handleGame(Player player, int slot) {
        if (slot == 0) {
            changeInt("game.min-players", -1, 1, 100);
        } else if (slot == 2) {
            changeInt("game.min-players", 1, 1, 100);
        } else if (slot == 9) {
            toggle("game.clear-inventory");
        } else if (slot == 10) {
            toggle("game.give-skyblock-items");
        } else if (slot == 11) {
            toggle("game.fast-start");
        } else if (slot == 12) {
            toggle("game.select-right");
        } else if (slot == 13) {
            toggle("game.auto-balance-teams");
        } else if (slot == 14) {
            toggle("game.allow-mid-join");
            gameManager.refreshAllPlayerDisplays();
        } else if (slot == 18) {
            try {
                gameManager.start();
            } catch (IllegalStateException ex) {
                plugin.messages().send(player, "&c" + ex.getMessage());
            }
        } else if (slot == 20) {
            gameManager.stop(true);
        } else if (slot == 21) {
            gameManager.autoBalance();
            plugin.messages().send(player, "&a온라인 플레이어를 자동으로 팀 배정했습니다.");
        }
    }

    private void handleWorldCore(Player player, int slot) {
        if (slot == 3) {
            toggle("game.remove-entities");
        } else if (slot == 4) {
            toggle("game.ignore-bed");
        } else if (slot == 5) {
            toggle("game.friendly-fire");
            gameManager.reloadSettings();
        } else if (slot == 9) {
            toggle("world.autosave");
        } else if (slot == 10) {
            toggle("world.spawn-animals");
        } else if (slot == 11) {
            toggle("world.spawn-monsters");
        } else if (slot == 13) {
            toggle("core.protect-diamond-from-explosion");
        } else if (slot == 14) {
            toggle("core.forbid-diamond-pickaxe");
        } else if (slot == 16) {
            toggle("gamerules.enabled");
        } else if (slot == 17) {
            toggle("gamerules.restore-on-stop");
        }
    }

    private void handleDisplay(Player player, int slot, ClickType click) {
        if (slot == 10) {
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
            if (click == ClickType.RIGHT) {
                changeUrfCooldownPercent(5);
            } else if (click == ClickType.SHIFT_RIGHT) {
                changeUrfCooldownPercent(-5);
            } else {
                toggle("game.urf.enabled");
            }
            gameManager.refreshAllPlayerDisplays();
        } else if (slot == 15) {
            toggle("abilities.messages.success");
        } else if (slot == 16) {
            toggle("abilities.messages.failure");
        } else if (slot == 17) {
            toggle("abilities.messages.timer");
        }
    }

    private void handleGambling(Player player, int slot) {
        if (slot == 10) {
            toggle("gambling.enabled");
            gameManager.refreshAllPlayerDisplays();
        } else if (slot == 12) {
            changeInt("gambling.cost.cobblestone", -1, 1, 2304);
        } else if (slot == 14) {
            changeInt("gambling.cost.cobblestone", 1, 1, 2304);
        } else if (slot == 15) {
            switchView(player, SettingsView.GAMBLING_NORMAL);
        } else if (slot == 16) {
            switchView(player, SettingsView.GAMBLING_TAJJA);
        } else if (slot == 17) {
            plugin.reloadConfig();
            plugin.messages().send(player, "&a도박 상품 설정을 다시 불러왔습니다.");
        }
    }

    private void handleRewardChance(Player player, String path, int slot, ClickType click) {
        if (slot < 0 || slot >= 18) {
            return;
        }
        int index = slot;
        int size = plugin.getConfig().getMapList(path).size();
        if (index >= size) {
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
        if (view == SettingsView.GAMBLING_NORMAL || view == SettingsView.GAMBLING_TAJJA) {
            return SettingsView.GAMBLING;
        }
        return SettingsView.MAIN;
    }

    private void reopen(final Player player, final SettingsView view) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline() && openViewers.contains(player.getUniqueId())) {
                open(player, view);
            }
        });
    }

    private void fill(Inventory inventory, SettingsView view) {
        fillBackground(inventory);
        if (view == SettingsView.MAIN) {
            fillMain(inventory);
        } else if (view == SettingsView.GAME) {
            fillGame(inventory);
        } else if (view == SettingsView.WORLD_CORE) {
            fillWorldCore(inventory);
        } else if (view == SettingsView.DISPLAY) {
            fillDisplay(inventory);
        } else if (view == SettingsView.GAMBLING) {
            fillGambling(inventory);
        } else if (view == SettingsView.GAMBLING_NORMAL) {
            fillRewardChance(inventory, "gambling.rewards.normal", "일반");
        } else if (view == SettingsView.GAMBLING_TAJJA) {
            fillRewardChance(inventory, "gambling.rewards.tajja", "타짜");
        }
        if (view != SettingsView.MAIN) {
            inventory.setItem(BACK_SLOT, backItem());
        }
        inventory.setItem(CLOSE_SLOT, closeItem());
    }

    private void fillMain(Inventory inventory) {
        inventory.setItem(10, categoryItem("EMERALD_BLOCK", "게임 진행",
            ChatColor.GRAY + "시작 조건, 지급, 시작/종료",
            ChatColor.DARK_GRAY + "클릭해서 열기"));
        inventory.setItem(12, categoryItem("DIAMOND_BLOCK", "월드 / 코어",
            ChatColor.GRAY + "월드 규칙, 신전 보호, 팀킬",
            ChatColor.DARK_GRAY + "클릭해서 열기"));
        inventory.setItem(14, categoryItem("ITEM_FRAME", "표시 / 우르프",
            ChatColor.GRAY + "스코어보드, Prefix, 우르프",
            ChatColor.DARK_GRAY + "클릭해서 열기"));
        inventory.setItem(16, categoryItem("GOLD_INGOT", "도박",
            ChatColor.GRAY + "도박 사용, 가격, 상품 설정",
            ChatColor.DARK_GRAY + "클릭해서 열기"));
        inventory.setItem(24, item("BOOK", "BOOK", 1, (short) 0,
            ChatColor.YELLOW + "설정 다시 불러오기",
            ChatColor.GRAY + "config.yml을 다시 읽습니다."));
    }

    private void fillGame(Inventory inventory) {
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

        inventory.setItem(9, toggleItem("game.clear-inventory", "인벤토리 클리어", "CHEST"));
        inventory.setItem(10, toggleItem("game.give-skyblock-items", "스카이블럭 아이템 지급", "ICE"));
        inventory.setItem(11, toggleItem("game.fast-start", "빠른 시작", "SUGAR"));
        inventory.setItem(12, toggleItem("game.select-right", "능력 재추첨 기회", "NETHER_STAR"));
        inventory.setItem(13, toggleItem("game.auto-balance-teams", "시작 시 팀 자동 배정", "COMPASS"));
        inventory.setItem(14, toggleItem("game.allow-mid-join", "중간 참여 허용", "ENDER_PEARL"));

        inventory.setItem(18, item("EMERALD_BLOCK", "EMERALD_BLOCK", 1, (short) 0,
            ChatColor.GREEN + "게임 시작",
            ChatColor.GRAY + "/t start"));
        inventory.setItem(20, item("REDSTONE_BLOCK", "REDSTONE_BLOCK", 1, (short) 0,
            ChatColor.RED + "게임 종료",
            ChatColor.GRAY + "/t stop"));
        inventory.setItem(21, item("COMPASS", "COMPASS", 1, (short) 0,
            ChatColor.AQUA + "팀 자동 배정",
            ChatColor.GRAY + "/gw autoteam"));
    }

    private void fillWorldCore(Inventory inventory) {
        inventory.setItem(3, toggleItem("game.remove-entities", "엔티티 제거", "ROTTEN_FLESH"));
        inventory.setItem(4, toggleItem("game.ignore-bed", "침대 무시", "BED"));
        inventory.setItem(5, toggleItem("game.friendly-fire", "팀킬 허용", "IRON_SWORD"));

        inventory.setItem(9, toggleItem("world.autosave", "서버 자동 저장", "BOOK"));
        inventory.setItem(10, toggleItem("world.spawn-animals", "동물 스폰", "WHEAT"));
        inventory.setItem(11, toggleItem("world.spawn-monsters", "몬스터 스폰", "BONE"));

        inventory.setItem(13, toggleItem("core.protect-diamond-from-explosion", "코어 폭파 보호", "DIAMOND_BLOCK"));
        inventory.setItem(14, toggleItem("core.forbid-diamond-pickaxe", "다이아 곡괭이 금지", "DIAMOND_PICKAXE"));
        inventory.setItem(16, toggleItem("gamerules.enabled", "게임룰 자동 적용", "COMMAND_BLOCK"));
        inventory.setItem(17, toggleItem("gamerules.restore-on-stop", "종료 시 게임룰 복구", "REDSTONE_COMPARATOR"));
    }

    private void fillDisplay(Inventory inventory) {
        inventory.setItem(10, toggleItem("scoreboard.enabled", "스코어보드 안내 사용", "ITEM_FRAME"));
        inventory.setItem(11, toggleItem("scoreboard.team-prefixes", "팀 Prefix 표시", "NAME_TAG"));
        inventory.setItem(12, toggleItem("game.ability-roll-message", "능력 배정 타이틀", "PAPER"));
        inventory.setItem(13, toggleItem("abilities.messages.enabled", "능력 안내 메시지", "BOOK"));
        inventory.setItem(14, urfItem());
        inventory.setItem(15, toggleItem("abilities.messages.success", "능력 사용 완료 문구", "INK_SACK"));
        inventory.setItem(16, toggleItem("abilities.messages.failure", "능력 실패/제한 문구", "REDSTONE"));
        inventory.setItem(17, toggleItem("abilities.messages.timer", "능력 타이머 채팅", "WATCH"));
    }

    private void fillGambling(Inventory inventory) {
        FileConfiguration config = plugin.getConfig();
        int cost = Math.max(1, config.getInt("gambling.cost.cobblestone", 32));
        inventory.setItem(10, gamblingItem(config));
        inventory.setItem(12, item("REDSTONE_TORCH", "REDSTONE_TORCH_ON", 1, (short) 0,
            ChatColor.RED + "도박 가격 -1",
            ChatColor.GRAY + "현재: " + ChatColor.YELLOW + "조약돌 " + cost + "개"));
        inventory.setItem(13, item("CHEST", "CHEST", 1, (short) 0,
            ChatColor.YELLOW + "상품 설정",
            ChatColor.GRAY + "일반 상품: " + ChatColor.WHITE + config.getMapList("gambling.rewards.normal").size() + "개",
            ChatColor.GRAY + "타짜 상품: " + ChatColor.WHITE + config.getMapList("gambling.rewards.tajja").size() + "개",
            ChatColor.DARK_GRAY + "config.yml의 gambling.rewards에서 편집"));
        inventory.setItem(14, item("TORCH", "TORCH", 1, (short) 0,
            ChatColor.GREEN + "도박 가격 +1",
            ChatColor.GRAY + "현재: " + ChatColor.YELLOW + "조약돌 " + cost + "개"));
        inventory.setItem(15, item("DIAMOND", "DIAMOND", 1, (short) 0,
            ChatColor.AQUA + "일반 확률 편집",
            ChatColor.GRAY + "상품별 chance 값을 GUI에서 조정합니다."));
        inventory.setItem(16, item("GOLD_INGOT", "GOLD_INGOT", 1, (short) 0,
            ChatColor.GOLD + "타짜 확률 편집",
            ChatColor.GRAY + "타짜 능력 보유자용 chance 값을 조정합니다."));
        inventory.setItem(17, item("BOOK", "BOOK", 1, (short) 0,
            ChatColor.YELLOW + "상품 설정 다시 불러오기",
            ChatColor.GRAY + "config.yml을 다시 읽습니다."));
    }

    private void fillRewardChance(Inventory inventory, String path, String title) {
        List<Map<?, ?>> rewards = plugin.getConfig().getMapList(path);
        int total = totalChance(path);
        int limit = Math.min(18, rewards.size());
        for (int i = 0; i < limit; i++) {
            inventory.setItem(i, rewardItem(rewards.get(i), i, total));
        }
        inventory.setItem(18, item("PAPER", "PAPER", 1, (short) 0,
            ChatColor.YELLOW + title + " 상품 확률",
            ChatColor.GRAY + "총 가중치: " + ChatColor.WHITE + total,
            ChatColor.GRAY + "좌클릭: +1 / 우클릭: -1",
            ChatColor.GRAY + "쉬프트 좌클릭: +5 / 쉬프트 우클릭: -5",
            ChatColor.DARK_GRAY + "확률은 chance 가중치 기준입니다."));
    }

    private void fillBackground(Inventory inventory) {
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, item("BLACK_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", 1, (short) 15, " "));
        }
    }

    private ItemStack urfItem() {
        boolean enabled = plugin.getConfig().getBoolean("game.urf.enabled", false);
        ChatColor color = enabled ? ChatColor.GREEN : ChatColor.RED;
        String state = enabled ? "켜짐" : "꺼짐";
        int percent = plugin.abilities().urfCooldownPercent();
        return item("BLAZE_POWDER", "BLAZE_POWDER", 1, (short) 0,
            color + "우르프 모드: " + state,
            ChatColor.GRAY + "능력 쿨타임 감소율: " + ChatColor.YELLOW + percent + "%",
            ChatColor.GRAY + "좌클릭: 우르프 켜기/끄기",
            ChatColor.GRAY + "우클릭: 감소율 +5%",
            ChatColor.GRAY + "쉬프트+우클릭: 감소율 -5%",
            ChatColor.DARK_GRAY + "game.urf.enabled");
    }

    private ItemStack gamblingItem(FileConfiguration config) {
        boolean enabled = config.getBoolean("gambling.enabled", true);
        ChatColor color = enabled ? ChatColor.GREEN : ChatColor.RED;
        String state = enabled ? "켜짐" : "꺼짐";
        int cost = Math.max(1, config.getInt("gambling.cost.cobblestone", 32));
        int normalRewards = config.getMapList("gambling.rewards.normal").size();
        int tajjaRewards = config.getMapList("gambling.rewards.tajja").size();
        return item("GOLD_INGOT", "GOLD_INGOT", 1, (short) 0,
            color + "도박 허용: " + state,
            ChatColor.GRAY + "가격: " + ChatColor.YELLOW + "조약돌 " + cost + "개",
            ChatColor.GRAY + "일반 상품: " + ChatColor.WHITE + normalRewards + "개",
            ChatColor.GRAY + "타짜 상품: " + ChatColor.WHITE + tajjaRewards + "개",
            ChatColor.GRAY + "클릭: 도박 켜기/끄기",
            ChatColor.DARK_GRAY + "상품은 config.yml에서 편집");
    }

    private ItemStack toggleItem(String path, String title, String icon) {
        boolean enabled = plugin.getConfig().getBoolean(path, defaultToggleValue(path));
        ChatColor color = enabled ? ChatColor.GREEN : ChatColor.RED;
        String state = enabled ? "켜짐" : "꺼짐";
        return item(icon, icon, 1, (short) 0,
            color + title + ": " + state,
            ChatColor.GRAY + "클릭하면 설정이 전환됩니다.",
            ChatColor.DARK_GRAY + path);
    }

    private ItemStack categoryItem(String icon, String title, String... lore) {
        return item(icon, icon, 1, (short) 0, ChatColor.YELLOW + title, lore);
    }

    private ItemStack backItem() {
        return item("ARROW", "ARROW", 1, (short) 0,
            ChatColor.AQUA + "뒤로",
            ChatColor.GRAY + "설정 메인으로 돌아갑니다.");
    }

    private ItemStack closeItem() {
        return item("BARRIER", "BARRIER", 1, (short) 0, ChatColor.RED + "닫기");
    }

    private void toggle(String path) {
        FileConfiguration config = plugin.getConfig();
        config.set(path, !config.getBoolean(path, defaultToggleValue(path)));
        plugin.saveConfig();
    }

    private boolean defaultToggleValue(String path) {
        return path != null && path.startsWith("abilities.messages.");
    }

    private void changeInt(String path, int delta, int min, int max) {
        FileConfiguration config = plugin.getConfig();
        int value = config.getInt(path, min);
        value = Math.max(min, Math.min(max, value + delta));
        config.set(path, value);
        plugin.saveConfig();
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
            if (lore != null && lore.length > 0) {
                meta.setLore(new ArrayList<String>(Arrays.asList(lore)));
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack rewardItem(Map<?, ?> reward, int index, int totalChance) {
        String modern = stringValue(reward.get("material"), "CHEST");
        String legacy = stringValue(reward.get("legacy-material"), modern);
        int amount = Math.max(1, intValue(reward.get("amount"), 1));
        int chance = Math.max(0, intValue(reward.get("chance"), 0));
        int percent = totalChance <= 0 ? 0 : (int) Math.round((chance * 100.0D) / totalChance);
        return item(modern, legacy, amount, (short) 0,
            ChatColor.YELLOW + rewardName(reward, index),
            ChatColor.GRAY + "확률 가중치: " + ChatColor.WHITE + chance,
            ChatColor.GRAY + "현재 비율: " + ChatColor.AQUA + percent + "%",
            ChatColor.GRAY + "수량: " + ChatColor.WHITE + amount,
            ChatColor.GRAY + "좌클릭 +1 / 우클릭 -1",
            ChatColor.GRAY + "쉬프트 좌클릭 +5 / 쉬프트 우클릭 -5");
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
