package kr.newgodwar.gui;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.ability.AbilityManager;
import kr.newgodwar.ability.api.AbilityDefinition;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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

public final class AbilityGui implements Listener {

    private static final String TITLE = ChatColor.DARK_AQUA + "신의 능력 보기";
    private static final int SIZE = 54;

    private final NewGodWarPlugin plugin;
    private final AbilityManager abilityManager;
    private final Set<UUID> openViewers = new HashSet<UUID>();

    public AbilityGui(NewGodWarPlugin plugin, AbilityManager abilityManager) {
        this.plugin = plugin;
        this.abilityManager = abilityManager;
    }

    public void open(Player viewer, Player target) {
        Inventory inventory = Bukkit.createInventory(viewer, SIZE, TITLE);
        fill(inventory, viewer, target);
        viewer.openInventory(inventory);
        openViewers.add(viewer.getUniqueId());
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player) || !isAbilityInventory(event)) {
            return;
        }
        event.setCancelled(true);
        if (event.getRawSlot() == 49) {
            event.getWhoClicked().closeInventory();
            return;
        }

        Player player = (Player) event.getWhoClicked();
        AbilityDefinition ability = abilityAtSlot(event.getRawSlot());
        if (ability != null && event.isRightClick() && player.hasPermission("newgodwar.admin")) {
            abilityManager.toggleBlacklisted(ability.id());
            plugin.messages().send(player, "&a" + ability.name() + " 블랙리스트 상태를 전환했습니다.");
            open(player, player);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (isAbilityInventory(event)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        openViewers.remove(event.getPlayer().getUniqueId());
    }

    private boolean isAbilityInventory(InventoryClickEvent event) {
        return openViewers.contains(event.getWhoClicked().getUniqueId())
            && event.getView() != null
            && TITLE.equals(event.getView().getTitle())
            && event.getRawSlot() < SIZE;
    }

    private boolean isAbilityInventory(InventoryDragEvent event) {
        return openViewers.contains(event.getWhoClicked().getUniqueId())
            && event.getView() != null
            && TITLE.equals(event.getView().getTitle());
    }

    private void fill(Inventory inventory, Player viewer, Player target) {
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, item("GRAY_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", 1, (short) 7, " "));
        }

        Player shown = target == null ? viewer : target;
        AbilityDefinition current = abilityManager.get(shown);
        inventory.setItem(4, currentAbilityItem(shown, current));

        int[] slots = new int[] {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
        };

        int index = 0;
        for (AbilityDefinition ability : abilityManager.registry().all()) {
            if (index >= slots.length) {
                break;
            }
            inventory.setItem(slots[index], abilityItem(ability, viewer.hasPermission("newgodwar.admin")));
            index++;
        }

        inventory.setItem(49, item("BARRIER", "BARRIER", 1, (short) 0, ChatColor.RED + "닫기"));
    }

    private ItemStack currentAbilityItem(Player target, AbilityDefinition ability) {
        if (ability == null) {
            return item("BOOK", "BOOK", 1, (short) 0,
                ChatColor.YELLOW + target.getName() + " 님의 현재 능력",
                ChatColor.GRAY + "아직 능력이 배정되지 않았습니다.",
                ChatColor.DARK_GRAY + "게임 시작 후 자동으로 배정됩니다.");
        }
        return item("NETHER_STAR", "NETHER_STAR", 1, (short) 0,
            ChatColor.GOLD + target.getName() + " 님의 현재 능력",
            ChatColor.WHITE + ability.name() + ChatColor.GRAY + " (" + ability.id() + ")",
            ChatColor.GRAY + ability.description(),
            ChatColor.DARK_GRAY + "제작자: " + ability.author());
    }

    private ItemStack abilityItem(AbilityDefinition ability, boolean showAdminState) {
        boolean enabled = abilityManager.isEnabled(ability);
        boolean blacklisted = abilityManager.isBlacklisted(ability);
        ChatColor color = enabled ? ChatColor.GREEN : ChatColor.RED;
        String state = blacklisted ? "블랙리스트" : (enabled ? "사용 가능" : "비활성");
        ArrayList<String> lore = new ArrayList<String>();
        lore.add(ChatColor.GRAY + ability.description());
        lore.add(ChatColor.DARK_GRAY + "ID: " + ability.id());
        lore.add(ChatColor.DARK_GRAY + "제작자: " + ability.author());
        if (showAdminState) {
            lore.add(color + state);
            lore.add(ChatColor.DARK_GRAY + "우클릭: 블랙리스트 전환");
        }
        return item(enabled ? "ENCHANTED_BOOK" : "BOOK", enabled ? "ENCHANTED_BOOK" : "BOOK", 1, (short) 0,
            color + ability.name(), lore);
    }

    private AbilityDefinition abilityAtSlot(int slot) {
        int[] slots = new int[] {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
        };
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

    private ItemStack item(String modernMaterial, String legacyMaterial, int amount, short damage, String name, String... lore) {
        return item(modernMaterial, legacyMaterial, amount, damage, name, new ArrayList<String>(Arrays.asList(lore)));
    }

    private ItemStack item(String modernMaterial, String legacyMaterial, int amount, short damage, String name, ArrayList<String> lore) {
        Material material = material(modernMaterial, legacyMaterial);
        ItemStack stack = new ItemStack(material, Math.max(1, Math.min(64, amount)), damage);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore);
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
