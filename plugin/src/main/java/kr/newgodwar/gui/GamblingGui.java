package kr.newgodwar.gui;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.ability.api.AbilityDefinition;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class GamblingGui implements Listener, CommandExecutor {

    private static final String TITLE = ChatColor.BLACK + ":::::::: 카지노 ::::::::";
    private final NewGodWarPlugin plugin;
    private final Set<UUID> openViewers = new HashSet<UUID>();

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
            player.sendMessage(ChatColor.RED + "이 기능은 잠겨있습니다!");
            return;
        }
        Inventory inventory = Bukkit.createInventory(player, 9, TITLE);
        int cost = gambleCost();
        inventory.setItem(4, item("GOLD_INGOT", "GOLD_INGOT",
            ChatColor.YELLOW + "가챠" + ChatColor.AQUA + " ★ " + ChatColor.GREEN + "가챠",
            ChatColor.WHITE + "조약돌 " + cost + "개를 소모해 다양한 아이템을",
            ChatColor.WHITE + "뽑을 수 있습니다.",
            ChatColor.DARK_GRAY + "상품은 config.yml의 gambling.rewards에서 설정합니다."));
        player.openInventory(inventory);
        openViewers.add(player.getUniqueId());
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player) || !isGamblingInventory(event)) {
            return;
        }
        event.setCancelled(true);
        if (event.getRawSlot() == 4) {
            gamble((Player) event.getWhoClicked());
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (openViewers.contains(event.getWhoClicked().getUniqueId())
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
        return openViewers.contains(event.getWhoClicked().getUniqueId())
            && event.getView() != null
            && TITLE.equals(event.getView().getTitle())
            && event.getRawSlot() >= 0
            && event.getRawSlot() < event.getView().getTopInventory().getSize();
    }

    private void gamble(Player player) {
        int cost = gambleCost();
        if (!player.getInventory().contains(Material.COBBLESTONE, cost)) {
            player.sendMessage(ChatColor.RED + "조약돌이 부족합니다! 정신차려임마.");
            return;
        }
        player.getInventory().removeItem(new ItemStack(Material.COBBLESTONE, cost));

        AbilityDefinition ability = plugin.abilities().get(player);
        boolean tajja = ability != null && "tajja".equalsIgnoreCase(ability.id());
        Reward reward = chooseReward(tajja);
        reward.give(player);
    }

    private int gambleCost() {
        return Math.max(1, plugin.getConfig().getInt("gambling.cost.cobblestone", 32));
    }

    private Reward chooseReward(boolean tajja) {
        List<Reward> rewards = rewards(tajja ? "gambling.rewards.tajja" : "gambling.rewards.normal", tajja);
        long total = 0L;
        for (Reward reward : rewards) {
            total += reward.chance;
        }
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

    private List<Reward> rewards(String path, boolean tajja) {
        List<Reward> rewards = configuredRewards(path);
        return rewards.isEmpty() ? defaultRewards(tajja) : rewards;
    }

    private List<Reward> configuredRewards(String path) {
        List<Reward> rewards = new ArrayList<Reward>();
        FileConfiguration config = plugin.getConfig();
        for (Map<?, ?> map : config.getMapList(path)) {
            int chance = intValue(map.get("chance"), 0);
            Material material = material(stringValue(map.get("material"), "AIR"), stringValue(map.get("legacy-material"), "AIR"));
            int amount = intValue(map.get("amount"), 0);
            List<String> messages = messages(map);
            if (chance > 0 && !messages.isEmpty()) {
                rewards.add(new Reward(chance, material, amount, messages));
            }
        }
        return rewards;
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

    private List<Reward> defaultRewards(boolean tajja) {
        List<Reward> rewards = new ArrayList<Reward>();
        if (tajja) {
            rewards.add(new Reward(10, Material.DIAMOND, 3, one(ChatColor.AQUA + "와우! 축하합니다! 다이아몬드 3개입니다!")));
            rewards.add(new Reward(10, material("OAK_LOG", "LOG"), 3, one(ChatColor.GOLD + "대박! 짜잔! 원목 3개 당첨 축하드립니다!")));
            rewards.add(new Reward(65, Material.IRON_INGOT, 3, one("평범하군요! 철괴 3개를 드립니다.")));
            rewards.add(new Reward(10, Material.IRON_INGOT, 4, one("평범하군요! 철괴 4개를 드립니다.")));
            rewards.add(new Reward(5, Material.DIAMOND, 22, list(ChatColor.YELLOW + "헐... 대박, 당신의 운은 미쳤군요!", ChatColor.AQUA + "다이아몬드 22개에 당첨되셨습니다.")));
            return rewards;
        }
        rewards.add(new Reward(5, Material.DIAMOND, 3, one(ChatColor.AQUA + "와우! 축하합니다! 다이아몬드 3개입니다!")));
        rewards.add(new Reward(15, material("OAK_LOG", "LOG"), 3, one(ChatColor.GOLD + "대박! 짜잔! 원목 3개 당첨 축하드립니다!")));
        rewards.add(new Reward(15, Material.BLAZE_ROD, 1, list(ChatColor.RED + "꽝!", ChatColor.BLUE + "서버의 신의 자비로 능력의 막대를 드립니다.")));
        rewards.add(new Reward(45, Material.IRON_INGOT, 3, one("평범하군요! 철괴 3개를 드립니다.")));
        rewards.add(new Reward(19, Material.IRON_INGOT, 4, one("평범하군요! 철괴 4개를 드립니다.")));
        rewards.add(new Reward(1, Material.DIAMOND, 22, list(ChatColor.YELLOW + "헐... 대박, 당신의 운은 미쳤군요!", ChatColor.AQUA + "다이아몬드 22개에 당첨되셨습니다.")));
        return rewards;
    }

    private ItemStack item(String modernMaterial, String legacyMaterial, String name, String... lore) {
        Material material = material(modernMaterial, legacyMaterial);
        ItemStack stack = new ItemStack(material, 1);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
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

    private List<String> list(String first, String second) {
        List<String> messages = new ArrayList<String>();
        messages.add(first);
        messages.add(second);
        return messages;
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private static final class Reward {
        private final int chance;
        private final Material material;
        private final int amount;
        private final List<String> messages;

        private Reward(int chance, Material material, int amount, List<String> messages) {
            this.chance = chance;
            this.material = material;
            this.amount = amount;
            this.messages = messages;
        }

        private void give(Player player) {
            for (String message : messages) {
                player.sendMessage(message);
            }
            if (amount > 0 && material != null && material != Material.AIR) {
                player.getInventory().addItem(new ItemStack(material, amount));
            }
        }
    }
}
