package kr.newgodwar.listener;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.ability.AbilityManager;
import kr.newgodwar.ability.api.AbilityDefinition;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.EnchantingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;

public final class AthenaEnchantListener implements Listener {

    private static final int LAPIS_SLOT = 1;
    private static final String FAKE_LAPIS_NAME = ChatColor.BLUE + "아테나의 청금석";

    private final NewGodWarPlugin plugin;
    private final AbilityManager abilityManager;

    public AthenaEnchantListener(NewGodWarPlugin plugin, AbilityManager abilityManager) {
        this.plugin = plugin;
        this.abilityManager = abilityManager;
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player) || !(event.getInventory() instanceof EnchantingInventory)) {
            return;
        }
        Player player = (Player) event.getPlayer();
        if (isAthena(player)) {
            ensureFreeLapis((EnchantingInventory) event.getInventory());
        }
    }

    @EventHandler
    public void onPrepare(PrepareItemEnchantEvent event) {
        if (isAthena(event.getEnchanter()) && event.getInventory() instanceof EnchantingInventory) {
            ensureFreeLapis((EnchantingInventory) event.getInventory());
        }
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        if (isAthena(event.getEnchanter()) && event.getInventory() instanceof EnchantingInventory) {
            refillNextTick((EnchantingInventory) event.getInventory());
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory instanceof EnchantingInventory)) {
            return;
        }
        EnchantingInventory enchantingInventory = (EnchantingInventory) inventory;
        if (event.getRawSlot() == LAPIS_SLOT && isFakeLapis(enchantingInventory.getSecondary())) {
            event.setCancelled(true);
            return;
        }
        if (event.getWhoClicked() instanceof Player && isAthena((Player) event.getWhoClicked())) {
            refillNextTick(enchantingInventory);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory instanceof EnchantingInventory)) {
            return;
        }
        EnchantingInventory enchantingInventory = (EnchantingInventory) inventory;
        if (event.getRawSlots().contains(LAPIS_SLOT) && isFakeLapis(enchantingInventory.getSecondary())) {
            event.setCancelled(true);
            return;
        }
        if (event.getWhoClicked() instanceof Player && isAthena((Player) event.getWhoClicked())) {
            refillNextTick(enchantingInventory);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory() instanceof EnchantingInventory)) {
            return;
        }
        EnchantingInventory inventory = (EnchantingInventory) event.getInventory();
        if (isFakeLapis(inventory.getSecondary())) {
            inventory.setSecondary(null);
        }
    }

    private void refillNextTick(final EnchantingInventory inventory) {
        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                ensureFreeLapis(inventory);
            }
        });
    }

    private void ensureFreeLapis(EnchantingInventory inventory) {
        ItemStack secondary = inventory.getSecondary();
        if (isEmpty(secondary)) {
            inventory.setSecondary(fakeLapis());
        }
    }

    private ItemStack fakeLapis() {
        Material material = Material.matchMaterial("LAPIS_LAZULI");
        short durability = 0;
        if (material == null) {
            material = Material.matchMaterial("INK_SACK");
            durability = 4;
        }
        if (material == null) {
            material = Material.matchMaterial("LEGACY_INK_SACK");
            durability = 4;
        }
        ItemStack item = new ItemStack(material, 64, durability);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(FAKE_LAPIS_NAME);
            meta.setLore(Collections.singletonList(ChatColor.GRAY + "아테나 전용 인챈트 보조 아이템"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isFakeLapis(ItemStack item) {
        if (isEmpty(item) || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() && FAKE_LAPIS_NAME.equals(meta.getDisplayName());
    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType() == Material.AIR || item.getAmount() <= 0;
    }

    private boolean isAthena(Player player) {
        AbilityDefinition definition = abilityManager.get(player);
        return definition != null && "athena".equalsIgnoreCase(definition.id());
    }
}
