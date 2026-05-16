package kr.newgodwar.gui;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.ability.api.AbilityDefinition;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public final class GamblingGui implements Listener, CommandExecutor {

    private static final String TITLE = ChatColor.BLACK + ":::::::: 카지노 ::::::::";
    private final NewGodWarPlugin plugin;
    private final Set<UUID> openViewers = new HashSet<UUID>();
    private final Random random = new Random();

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
        inventory.setItem(4, item("GOLD_INGOT", "GOLD_INGOT",
            ChatColor.YELLOW + "가챠" + ChatColor.AQUA + " ★ " + ChatColor.GREEN + "가챠",
            ChatColor.WHITE + "조약돌 32개를 소모해 다양한 아이템을",
            ChatColor.WHITE + "뽑을 수 있습니다."));
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
        if (!player.getInventory().contains(Material.COBBLESTONE, 32)) {
            player.sendMessage(ChatColor.RED + "조약돌이 부족합니다! 정신차려임마.");
            return;
        }
        player.getInventory().removeItem(new ItemStack(Material.COBBLESTONE, 32));

        AbilityDefinition ability = plugin.abilities().get(player);
        boolean tajja = ability != null && "tajja".equalsIgnoreCase(ability.id());
        int roll = random.nextInt(100);
        if (tajja) {
            if (roll < 10) {
                reward(player, Material.DIAMOND, 3, ChatColor.AQUA + "와우! 축하합니다! 다이아몬드 3개입니다!");
            } else if (roll < 20) {
                reward(player, logMaterial(), 3, ChatColor.GOLD + "대박! 짜잔! 원목 3개 당첨 축하드립니다!");
            } else if (roll < 85) {
                reward(player, Material.IRON_INGOT, 3, "평범하군요! 철괴 3개를 드립니다.");
            } else if (roll < 95) {
                reward(player, Material.IRON_INGOT, 4, "평범하군요! 철괴 4개를 드립니다.");
            } else {
                reward(player, Material.DIAMOND, 22, ChatColor.YELLOW + "헐... 대박, 당신의 운은 미쳤군요!");
                player.sendMessage(ChatColor.AQUA + "다이아몬드 22개에 당첨되셨습니다.");
            }
            return;
        }

        if (roll < 5) {
            reward(player, Material.DIAMOND, 3, ChatColor.AQUA + "와우! 축하합니다! 다이아몬드 3개입니다!");
        } else if (roll < 20) {
            reward(player, logMaterial(), 3, ChatColor.GOLD + "대박! 짜잔! 원목 3개 당첨 축하드립니다!");
        } else if (roll < 35) {
            player.sendMessage(ChatColor.RED + "꽝!");
            player.sendMessage(ChatColor.BLUE + "서버의 신의 자비로 능력의 막대를 드립니다.");
            player.getInventory().addItem(new ItemStack(Material.BLAZE_ROD, 1));
        } else if (roll < 80) {
            reward(player, Material.IRON_INGOT, 3, "평범하군요! 철괴 3개를 드립니다.");
        } else if (roll < 99) {
            reward(player, Material.IRON_INGOT, 4, "평범하군요! 철괴 4개를 드립니다.");
        } else {
            reward(player, Material.DIAMOND, 22, ChatColor.YELLOW + "헐... 대박, 당신의 운은 미쳤군요!");
            player.sendMessage(ChatColor.AQUA + "다이아몬드 22개에 당첨되셨습니다.");
        }
    }

    private void reward(Player player, Material material, int amount, String message) {
        player.sendMessage(message);
        player.getInventory().addItem(new ItemStack(material, amount));
    }

    private Material logMaterial() {
        Material modern = Material.matchMaterial("OAK_LOG");
        if (modern != null) {
            return modern;
        }
        Material legacy = Material.matchMaterial("LOG");
        return legacy == null ? Material.WOOD : legacy;
    }

    private ItemStack item(String modernMaterial, String legacyMaterial, String name, String... lore) {
        Material material = Material.matchMaterial(modernMaterial);
        if (material == null) {
            material = Material.matchMaterial(legacyMaterial);
        }
        if (material == null) {
            material = Material.STONE;
        }
        ItemStack stack = new ItemStack(material, 1);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
