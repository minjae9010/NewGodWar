package kr.newgodwar.gui;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.util.InventoryItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class GamblingGui implements Listener, CommandExecutor {

    private static final String REWARDS_TITLE = ChatColor.DARK_GRAY + "보상과 확률";
    private static final int PREVIOUS_SLOT = 45, NEXT_SLOT = 53, GALLERY_CLOSE_SLOT = 51;
    private static final int[] REWARD_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    };
    private final NewGodWarPlugin plugin;
    private final Map<UUID, ViewState> openViewers = new HashMap<UUID, ViewState>();

    public GamblingGui(NewGodWarPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.messages().send(sender, "&c플레이어만 사용할 수 있습니다.");
            return true;
        }
        open((Player) sender);
        return true;
    }

    public void open(Player player) {
        if (!plugin.getConfig().getBoolean("gambling.enabled", true)) {
            player.sendMessage(ChatColor.RED + "지금은 보상 뽑기가 꺼져 있어요.");
            return;
        }
        show(player, new ViewState());
    }

    private void show(Player player, ViewState state) {
        int balance = InventoryItems.count(player.getInventory(), Material.COBBLESTONE);
        String title = state.catalog ? GuiTheme.title(player, plugin, REWARDS_TITLE, 54)
            : GuiTheme.gamblingTitle(player, plugin, balance, gambleCost());
        Inventory inventory = Bukkit.createInventory(state, state.catalog ? 54 : GamblingLayout.SIZE, title);
        GuiTheme.frame(inventory);
        if (state.catalog) renderRewards(player, inventory, state);
        else renderDraw(player, inventory, state, balance);
        GuiTheme.present(player, inventory, plugin);
        // Closing the previous screen is synchronous. Its close must not discard the new screen's state.
        state.inventory = inventory;
        openViewers.put(player.getUniqueId(), state);
        player.openInventory(inventory);
    }

    private void renderDraw(Player player, Inventory inventory, ViewState state, int balance) {
        int cost = gambleCost();
        boolean affordable = balance >= cost;
        inventory.setItem(10, GuiTheme.icon(item("COBBLESTONE", "COBBLESTONE",
            ChatColor.WHITE + "조약돌 " + balance + "개",
            ChatColor.GRAY + "1회 " + cost + "개 · " + (balance / cost) + "회 가능"), GuiIcon.COIN));
        ItemStack draw = GuiTheme.icon(item(affordable ? "GOLD_INGOT" : "BARRIER",
            affordable ? "GOLD_INGOT" : "BARRIER",
            (affordable ? ChatColor.GOLD : ChatColor.RED) + "1회 뽑기 · " + cost + " 조약돌",
            affordable ? ChatColor.GRAY + "클릭하면 한 번 뽑아요."
                : ChatColor.GRAY + "조약돌 " + (cost - balance) + "개가 더 필요해요."),
            affordable ? GuiIcon.GAMBLING : GuiIcon.CANCEL);
        for (int slot = 0; slot < GamblingLayout.SIZE; slot++) {
            if (GamblingLayout.draw(slot)) inventory.setItem(slot, draw.clone());
        }
        inventory.setItem(16, resultItem(state));
        inventory.setItem(GamblingLayout.REWARDS, GuiTheme.icon(item("CHEST", "CHEST",
            ChatColor.AQUA + "보상과 확률 보기", ChatColor.GRAY + "비용 없이 목록을 확인해요."), GuiIcon.REWARDS));
        inventory.setItem(GamblingLayout.CLOSE, GuiTheme.close());
        GuiTheme.painted(player, inventory, plugin, 12, 13, 14, 21, 22, 23,
            GamblingLayout.REWARDS, GamblingLayout.CLOSE);
        GuiTheme.captioned(player, inventory, plugin, 10);
    }

    private void renderRewards(Player player, Inventory inventory, ViewState state) {
        List<Reward> rewards = rewards("gambling.rewards.normal");
        int pages = GamblingLayout.pages(rewards.size(), REWARD_SLOTS.length);
        state.page = Math.max(0, Math.min(state.page, pages - 1));
        long total = totalWeight(rewards);
        for (int i = 0; i < REWARD_SLOTS.length; i++) {
            int index = state.page * REWARD_SLOTS.length + i;
            if (index >= rewards.size()) break;
            inventory.setItem(REWARD_SLOTS[i], rewardPreview(rewards.get(index), total));
        }
        inventory.setItem(40, item("PAPER", "PAPER", ChatColor.WHITE + "보상 " + (state.page + 1) + " / " + pages,
            ChatColor.GRAY + "총 " + rewards.size() + "종 · 수량과 확률을 확인하세요."));
        inventory.setItem(GamblingLayout.GALLERY_BACK, GuiTheme.icon(item("ARROW", "ARROW",
            ChatColor.AQUA + "뽑기로 돌아가기"), GuiIcon.BACK));
        inventory.setItem(GALLERY_CLOSE_SLOT, GuiTheme.close());
        if (state.page > 0) inventory.setItem(PREVIOUS_SLOT, GuiTheme.icon(item("ARROW", "ARROW",
            ChatColor.WHITE + "이전 보상"), GuiIcon.PREVIOUS));
        if (state.page + 1 < pages) inventory.setItem(NEXT_SLOT, GuiTheme.icon(item("ARROW", "ARROW",
            ChatColor.WHITE + "다음 보상"), GuiIcon.NEXT));
        GuiTheme.painted(player, inventory, plugin, GamblingLayout.GALLERY_BACK, GALLERY_CLOSE_SLOT);
    }

    private ItemStack resultItem(ViewState state) {
        if (state.lastReward == null) return GuiTheme.icon(item("CHEST", "CHEST",
            ChatColor.GRAY + "아직 뽑지 않았어요"), GuiIcon.REWARDS);
        Reward reward = state.lastReward;
        ItemStack result = reward.item == null ? new ItemStack(Material.PAPER) : reward.item.clone();
        result.setAmount(Math.max(1, Math.min(result.getMaxStackSize(), result.getAmount())));
        ItemMeta meta = result.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "받은 보상");
            meta.setLore(GuiText.wrap(reward.messages));
            result.setItemMeta(meta);
        }
        return result;
    }

    private ItemStack rewardPreview(Reward reward, long total) {
        ItemStack preview = reward.item == null ? new ItemStack(Material.PAPER) : reward.item.clone();
        preview.setAmount(Math.max(1, Math.min(preview.getMaxStackSize(), preview.getAmount())));
        ItemMeta meta = preview.getItemMeta();
        if (meta != null) {
            if (reward.item == null) meta.setDisplayName(ChatColor.WHITE + "아이템 없음");
            meta.setLore(GuiText.wrap(Arrays.asList(
                ChatColor.GRAY + "수량  " + ChatColor.WHITE + (reward.item == null ? "없음" : reward.item.getAmount() + "개"),
                ChatColor.GRAY + "확률  " + ChatColor.AQUA + chanceText(reward.chance, total))));
            preview.setItemMeta(meta);
        }
        return preview;
    }

    private long totalWeight(List<Reward> rewards) {
        long total = 0L;
        for (Reward reward : rewards) total += reward.chance;
        return total;
    }

    private String chanceText(int weight, long total) {
        double percentage = weight * 100.0 / total;
        if (percentage < 0.01) return "0.01% 미만";
        if (weight < total && percentage > 99.99) return "99.99% 초과";
        return String.format(Locale.ROOT, "%.2f%%", percentage);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        final Player player = (Player) event.getWhoClicked();
        final ViewState state = current(player, event.getView().getTopInventory());
        if (state == null) return;
        event.setCancelled(true);
        final int slot = event.getRawSlot();
        if (slot < 0 || slot >= state.inventory.getSize() || event.getClick() != ClickType.LEFT) return;
        final Inventory clicked = state.inventory;
        // Reopening with a new balance/title is deferred until after the click transaction.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline() || current(player, clicked) != state
                || player.getOpenInventory().getTopInventory() != clicked) return;
            if (slot == (state.catalog ? GALLERY_CLOSE_SLOT : GamblingLayout.CLOSE)) {
                player.closeInventory(); return;
            }
            if (!state.catalog) {
                if (GamblingLayout.draw(slot)) gamble(player, state);
                else if (GamblingLayout.rewards(slot)) { state.catalog = true; show(player, state); }
            } else if (slot == GamblingLayout.GALLERY_BACK) {
                state.catalog = false; show(player, state);
            } else {
                int pages = GamblingLayout.pages(rewards("gambling.rewards.normal").size(), REWARD_SLOTS.length);
                if (slot == PREVIOUS_SLOT && state.page > 0) { state.page--; show(player, state); }
                else if (slot == NEXT_SLOT && state.page + 1 < pages) { state.page++; show(player, state); }
            }
        });
    }

    private ViewState current(Player player, Inventory inventory) {
        ViewState state = openViewers.get(player.getUniqueId());
        return state != null && state.inventory == inventory && inventory.getHolder() == state ? state : null;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player
            && current((Player) event.getWhoClicked(), event.getView().getTopInventory()) != null) event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        ViewState state = openViewers.get(event.getPlayer().getUniqueId());
        if (state != null && state.inventory == event.getInventory()) openViewers.remove(event.getPlayer().getUniqueId());
    }

    private void gamble(Player player, ViewState state) {
        if (!plugin.getConfig().getBoolean("gambling.enabled", true)) {
            player.sendMessage(ChatColor.RED + "지금은 보상 뽑기가 꺼져 있어요."); return;
        }
        int cost = gambleCost();
        if (!InventoryItems.take(player.getInventory(), Material.COBBLESTONE, cost)) {
            player.sendMessage(ChatColor.RED + "조약돌이 부족해요. 1회 비용은 " + cost + "개예요.");
            show(player, state); return;
        }
        Reward reward = chooseReward();
        reward.give(player);
        state.lastReward = reward;
        show(player, state);
    }

    private int gambleCost() {
        return Math.max(1, plugin.getConfig().getInt("gambling.cost.cobblestone", 32));
    }

    private Reward chooseReward() {
        List<Reward> rewards = rewards("gambling.rewards.normal");
        long total = totalWeight(rewards);
        long roll = ThreadLocalRandom.current().nextLong(total);
        long cursor = 0L;
        for (Reward reward : rewards) {
            cursor += reward.chance;
            if (roll < cursor) {
                return reward;
            }
        }
        return rewards.get(rewards.size() - 1);
    }

    private List<Reward> rewards(String path) {
        List<Reward> rewards = configuredRewards(path);
        return rewards.isEmpty() ? defaultRewards() : rewards;
    }

    private List<Reward> configuredRewards(String path) {
        List<Reward> rewards = new ArrayList<Reward>();
        FileConfiguration config = plugin.getConfig();
        for (Map<?, ?> map : config.getMapList(path)) {
            int chance = intValue(map.get("chance"), 0);
            ItemStack item = rewardItem(map);
            List<String> messages = messages(map);
            if (chance > 0 && !messages.isEmpty()) {
                rewards.add(new Reward(chance, item, messages));
            }
        }
        return rewards;
    }

    private ItemStack rewardItem(Map<?, ?> map) {
        Object configuredItem = map.get("item");
        if (configuredItem instanceof ItemStack) {
            ItemStack stack = ((ItemStack) configuredItem).clone();
            return stack.getType() == Material.AIR || stack.getAmount() <= 0 ? null : stack;
        }
        Material material = material(stringValue(map.get("material"), "AIR"), stringValue(map.get("legacy-material"), "AIR"));
        int amount = intValue(map.get("amount"), 0);
        return material == Material.AIR || amount <= 0 ? null : new ItemStack(material, amount);
    }

    private List<String> messages(Map<?, ?> map) {
        List<String> messages = new ArrayList<String>();
        Object many = map.get("messages");
        if (many instanceof Iterable<?>) {
            for (Object message : (Iterable<?>) many) {
                if (message != null && message.toString().trim().length() > 0) {
                    messages.add(color(message.toString()));
                }
            }
        }
        Object one = map.get("message");
        if (messages.isEmpty() && one != null && one.toString().trim().length() > 0) {
            messages.add(color(one.toString()));
        }
        return messages;
    }

    private List<Reward> defaultRewards() {
        List<Reward> rewards = new ArrayList<Reward>();
        rewards.add(new Reward(5, new ItemStack(Material.DIAMOND, 3), one(ChatColor.AQUA + "다이아몬드 3개 당첨!")));
        rewards.add(new Reward(15, new ItemStack(material("OAK_LOG", "LOG"), 3), one(ChatColor.GOLD + "원목 3개 당첨!")));
        rewards.add(new Reward(15, new ItemStack(Material.BLAZE_ROD, 1), one(ChatColor.GOLD + "블레이즈 막대기 1개를 받았어요.")));
        rewards.add(new Reward(45, new ItemStack(Material.IRON_INGOT, 3), one("철괴 3개를 받았어요.")));
        rewards.add(new Reward(19, new ItemStack(Material.IRON_INGOT, 4), one("철괴 4개를 받았어요.")));
        rewards.add(new Reward(1, new ItemStack(Material.DIAMOND, 22), one(ChatColor.AQUA + "대박! 다이아몬드 22개 당첨!")));
        return rewards;
    }

    private ItemStack item(String modernMaterial, String legacyMaterial, String name, String... lore) {
        Material material = material(modernMaterial, legacyMaterial);
        ItemStack stack = new ItemStack(material, 1);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(GuiText.wrap(Arrays.asList(lore)));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private Material material(String modernMaterial, String legacyMaterial) {
        Material material = Material.matchMaterial(modernMaterial);
        if (material == null) {
            material = Material.matchMaterial(legacyMaterial);
        }
        if (material == null) {
            material = Material.STONE;
        }
        return material;
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

    private List<String> one(String message) {
        List<String> messages = new ArrayList<String>();
        messages.add(message);
        return messages;
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private static final class ViewState implements InventoryHolder {
        private Inventory inventory;
        private boolean catalog;
        @Override public Inventory getInventory() { return inventory; }
        private int page;
        private Reward lastReward;
    }

    private static final class Reward {
        private final int chance;
        private final ItemStack item;
        private final List<String> messages;

        private Reward(int chance, ItemStack item, List<String> messages) {
            this.chance = chance;
            this.item = item == null ? null : item.clone();
            this.messages = messages;
        }

        private void give(Player player) {
            for (String message : messages) {
                player.sendMessage(message);
            }
            if (item != null && item.getType() != Material.AIR && item.getAmount() > 0) {
                InventoryItems.give(player, item);
            }
        }
    }
}
