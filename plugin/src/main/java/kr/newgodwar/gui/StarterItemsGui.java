package kr.newgodwar.gui;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.game.StarterItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class StarterItemsGui implements Listener {

    private static final int SIZE = 54;
    private static final String TITLE = ChatColor.DARK_GREEN + "기본 지급 아이템 창고";

    private final NewGodWarPlugin plugin;
    private final Set<UUID> openViewers = new HashSet<UUID>();

    public StarterItemsGui(NewGodWarPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(player, SIZE, TITLE);
        List<Map<?, ?>> entries = StarterItems.configuredEntries(plugin.getConfig());
        for (int i = 0; i < entries.size() && i < inventory.getSize(); i++) {
            ItemStack item = StarterItems.toItemStack(entries.get(i));
            if (item != null) {
                inventory.setItem(i, item);
            }
        }
        player.openInventory(inventory);
        openViewers.add(player.getUniqueId());
        plugin.messages().send(player, "&7창고에 넣은 아이템이 게임 시작 기본 지급 목록으로 저장됩니다.");
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player) || !isStarterItemsInventory(event)) {
            return;
        }
        Player player = (Player) event.getPlayer();
        openViewers.remove(player.getUniqueId());
        List<Map<String, Object>> entries = new ArrayList<Map<String, Object>>();
        for (ItemStack item : event.getInventory().getContents()) {
            if (item != null && item.getAmount() > 0 && item.getType() != Material.AIR) {
                entries.add(StarterItems.fromItem(item));
            }
        }
        plugin.getConfig().set(StarterItems.PATH, entries);
        plugin.saveConfig();
        plugin.messages().send(player, "&a기본 지급 아이템 창고를 저장했습니다. &f" + entries.size() + "종&a이 지급됩니다.");
    }

    private boolean isStarterItemsInventory(InventoryCloseEvent event) {
        return event.getView() != null
            && TITLE.equals(event.getView().getTitle())
            && openViewers.contains(event.getPlayer().getUniqueId());
    }
}
