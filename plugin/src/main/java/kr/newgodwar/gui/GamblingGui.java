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

    private static final String TITLE = ChatColor.DARK_GRAY + "신들의 전쟁 · 보상 뽑기";
    private static final int DRAW_SLOT = 13;
    private static final int CLOSE_SLOT = 49;
    private static final int PREVIOUS_SLOT = 45;
    private static final int NEXT_SLOT = 53;
    private static final int[] REWARD_SLOTS = {
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
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
        Inventory inventory = Bukkit.createInventory(player, 54, TITLE);
        ViewState state = new ViewState();
        render(player, inventory, state);
        player.openInventory(inventory);
        openViewers.put(player.getUniqueId(), state);
    }

    private void render(Player player, Inventory inventory, ViewState state) {
        GuiTheme.frame(inventory);
        int cost = gambleCost();
        int balance = InventoryItems.count(player.getInventory(), Material.COBBLESTONE);
        boolean affordable = balance >= cost;
        List<Reward> rewards = rewards("gambling.rewards.normal");
        int pages = (rewards.size() - 1) / REWARD_SLOTS.length + 1;
        state.page = Math.max(0, Math.min(state.page, pages - 1));

        inventory.setItem(4, GuiTheme.icon(GuiTheme.heading("조약돌 보상 뽑기",
            "아래 보상과 확률을 확인한 뒤 가운데 버튼을 눌러요."), GuiIcon.GAMBLING));
        inventory.setItem(7, GuiTheme.icon(item("BOOK", "BOOK", ChatColor.AQUA + "이용 안내",
            ChatColor.WHITE + "1. 아래 아이템에 마우스를 올려 보상과 확률 확인",
            ChatColor.WHITE + "2. 조약돌 비용과 보유량 확인",
            ChatColor.WHITE + "3. 가운데 [1회 뽑기]를 좌클릭",
            "",
            ChatColor.GRAY + "클릭할 때마다 비용을 지불하고 결과를 1개 뽑아요.",
            ChatColor.GRAY + "각 뽑기는 독립적이며 같은 결과도 연속으로 나와요.",
            ChatColor.GRAY + "가방이 가득 차면 보상은 발밑에 떨어져요."), GuiIcon.HELP));
        inventory.setItem(10, GuiTheme.icon(item("COBBLESTONE", "COBBLESTONE",
            ChatColor.WHITE + "내 조약돌 · " + balance + "개",
            ChatColor.GRAY + "1회 비용  " + ChatColor.WHITE + cost + "개",
            ChatColor.GRAY + "뽑을 수 있는 횟수  " + ChatColor.AQUA + (balance / cost) + "회",
            "",
            ChatColor.GRAY + "가방 안의 조약돌을 사용해요.",
            ChatColor.GRAY + "보조 손에 든 조약돌은 세지 않아요."), GuiIcon.COIN));
        inventory.setItem(DRAW_SLOT, GuiTheme.icon(item(affordable ? "GOLD_INGOT" : "GRAY_DYE",
            affordable ? "GOLD_INGOT" : "INK_SACK",
            (affordable ? ChatColor.GOLD : ChatColor.GRAY) + "" + ChatColor.BOLD
                + (affordable ? "1회 뽑기 · 조약돌 " + cost + "개" : "조약돌이 부족해요"),
            ChatColor.WHITE + "보유 " + balance + "개 / 비용 " + cost + "개",
            affordable ? ChatColor.GRAY + "비용을 낸 뒤 남는 조약돌  " + (balance - cost) + "개"
                : ChatColor.RED + "조약돌 " + (cost - balance) + "개가 더 필요해요.",
            "",
            affordable ? ChatColor.YELLOW + "좌클릭 · 비용을 내고 1회 뽑기"
                : ChatColor.GRAY + "조약돌을 모은 뒤 다시 열어 주세요."), affordable ? GuiIcon.GAMBLING : GuiIcon.CANCEL));
        inventory.setItem(16, resultItem(state));

        long total = totalWeight(rewards);
        for (int index = 0; index < REWARD_SLOTS.length; index++) {
            int rewardIndex = state.page * REWARD_SLOTS.length + index;
            if (rewardIndex >= rewards.size()) break;
            inventory.setItem(REWARD_SLOTS[index], rewardPreview(rewards.get(rewardIndex), total));
        }
        inventory.setItem(48, GuiTheme.icon(item("CHEST", "CHEST", ChatColor.AQUA + "보상 목록",
            ChatColor.WHITE + "총 " + rewards.size() + "가지 결과 · " + (state.page + 1) + " / " + pages + "페이지",
            ChatColor.GRAY + "아이템에 마우스를 올리면 수량과 확률을 볼 수 있어요.",
            ChatColor.GRAY + "목록의 아이템은 미리보기예요."), GuiIcon.REWARDS));
        if (state.page > 0) {
            inventory.setItem(PREVIOUS_SLOT, GuiTheme.icon(item("ARROW", "ARROW",
                ChatColor.WHITE + "이전 보상", ChatColor.YELLOW + "클릭 · 이전 페이지"), GuiIcon.PREVIOUS));
        }
        if (state.page + 1 < pages) {
            inventory.setItem(NEXT_SLOT, GuiTheme.icon(item("ARROW", "ARROW",
                ChatColor.WHITE + "다음 보상", ChatColor.YELLOW + "클릭 · 다음 페이지"), GuiIcon.NEXT));
        }
        inventory.setItem(CLOSE_SLOT, GuiTheme.close());
        GuiTheme.present(player, inventory, plugin);
    }

    private ItemStack resultItem(ViewState state) {
        if (state.lastReward == null) {
            return GuiTheme.icon(item("CHEST", "CHEST", ChatColor.WHITE + "최근 뽑기 결과",
                ChatColor.GRAY + "뽑기를 하면 이곳에 결과가 표시돼요.",
                ChatColor.GRAY + "아래 목록에서 받을 수 있는 보상을 확인하세요."), GuiIcon.REWARDS);
        }
        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.GRAY + "방금 받은 결과");
        lore.addAll(state.lastReward.messages);
        if (state.lastReward.item != null) {
            lore.add(ChatColor.GRAY + "지급 수량  " + ChatColor.WHITE + state.lastReward.item.getAmount() + "개");
        }
        lore.add("");
        lore.add(ChatColor.GRAY + (state.lastReward.item == null
            ? "아이템 지급이 없는 결과예요." : "보상은 가방 또는 발밑을 확인하세요."));
        return GuiTheme.icon(item("CHEST", "CHEST", ChatColor.GREEN + "뽑기 완료!",
            lore.toArray(new String[lore.size()])), GuiIcon.REWARDS);
    }

    private ItemStack rewardPreview(Reward reward, long total) {
        ItemStack preview = reward.item == null ? new ItemStack(Material.PAPER) : reward.item.clone();
        preview.setAmount(Math.max(1, Math.min(preview.getMaxStackSize(), preview.getAmount())));
        ItemMeta meta = preview.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<String>();
            lore.add(ChatColor.AQUA + "뽑기 확률  " + ChatColor.WHITE + chanceText(reward.chance, total));
            lore.add(reward.item == null ? ChatColor.GRAY + "아이템 지급이 없는 결과예요."
                : ChatColor.GRAY + "당첨 수량  " + ChatColor.WHITE + reward.item.getAmount() + "개");
            lore.add("");
            lore.addAll(reward.messages);
            if (meta.hasLore()) {
                lore.add("");
                lore.addAll(meta.getLore());
            }
            lore.add("");
            lore.add(ChatColor.DARK_GRAY + "미리보기 · 뽑기는 위쪽 가운데 버튼");
            if (reward.item == null) meta.setDisplayName(ChatColor.WHITE + "메시지 결과");
            meta.setLore(GuiText.wrap(lore));
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
        if (!(event.getWhoClicked() instanceof Player) || !isGamblingInventory(event)) {
            return;
        }
        event.setCancelled(true);
        if (event.getRawSlot() == CLOSE_SLOT) {
            event.getWhoClicked().closeInventory();
            return;
        }
        Player player = (Player) event.getWhoClicked();
        if (event.getRawSlot() == DRAW_SLOT && event.getClick() == ClickType.LEFT) {
            gamble(player);
            return;
        }
        ViewState state = openViewers.get(player.getUniqueId());
        int pages = (rewards("gambling.rewards.normal").size() - 1) / REWARD_SLOTS.length + 1;
        if (event.getRawSlot() == PREVIOUS_SLOT && state.page > 0) {
            state.page--;
            render(player, event.getInventory(), state);
        } else if (event.getRawSlot() == NEXT_SLOT && state.page + 1 < pages) {
            state.page++;
            render(player, event.getInventory(), state);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (openViewers.containsKey(event.getWhoClicked().getUniqueId())
            && event.getView() != null
            && TITLE.equals(event.getView().getTitle())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        openViewers.remove(event.getPlayer().getUniqueId());
    }

    private boolean isGamblingInventory(InventoryClickEvent event) {
        return openViewers.containsKey(event.getWhoClicked().getUniqueId())
            && event.getView() != null
            && TITLE.equals(event.getView().getTitle());
    }

    private void gamble(Player player) {
        if (!plugin.getConfig().getBoolean("gambling.enabled", true)) {
            player.sendMessage(ChatColor.RED + "지금은 보상 뽑기가 꺼져 있어요.");
            return;
        }
        int cost = gambleCost();
        if (!InventoryItems.take(player.getInventory(), Material.COBBLESTONE, cost)) {
            player.sendMessage(ChatColor.RED + "조약돌이 부족해요. 한 번 뽑으려면 " + cost + "개가 필요해요.");
            refresh(player);
            return;
        }
        Reward reward = chooseReward();
        reward.give(player);
        ViewState state = openViewers.get(player.getUniqueId());
        if (state != null) state.lastReward = reward;
        refresh(player);
    }

    private void refresh(Player player) {
        ViewState state = openViewers.get(player.getUniqueId());
        if (state != null && TITLE.equals(player.getOpenInventory().getTitle())) {
            render(player, player.getOpenInventory().getTopInventory(), state);
        }
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

    private static final class ViewState {
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
