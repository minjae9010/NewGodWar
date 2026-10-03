package kr.newgodwar.gui;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.ability.feedback.PackModels;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

final class GuiTheme {
    private GuiTheme() { }
    private static final java.util.Map<Inventory, java.util.Map<Integer, ItemStack[]>> LEGACY_ICONS =
        new java.util.WeakHashMap<Inventory, java.util.Map<Integer, ItemStack[]>>();

    // Compile against 1.12: newer item components are optional and never change vanilla assets.
    private static final Constructor<?> KEY;
    private static final Method SET_MODEL, GET_MODEL;
    static {
        Constructor<?> key = null;
        Method set = null, get = null;
        try {
            Class<?> type = Class.forName("org.bukkit.NamespacedKey");
            key = type.getConstructor(String.class, String.class);
            set = ItemMeta.class.getMethod("setItemModel", type);
            get = ItemMeta.class.getMethod("getItemModel");
        } catch (ReflectiveOperationException | LinkageError ignored) { }
        KEY = key;
        SET_MODEL = set;
        GET_MODEL = get;
    }

    static ItemStack icon(ItemStack item, GuiIcon icon) {
        if (item == null || icon == null) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        try {
            PackModels.apply(meta, icon.key());
            item.setItemMeta(meta);
        } catch (ReflectiveOperationException | LinkageError ignored) { }
        return item;
    }

    /** Call after rendering every view, including refreshes, before sending it to the viewer. */
    static void present(Player player, Inventory inventory, NewGodWarPlugin plugin) {
        if (!PackModels.modern()) {
            presentLegacy(player, inventory, plugin);
            return;
        }
        if (GET_MODEL == null || SET_MODEL == null) return;
        if (plugin.getConfig().getBoolean("ui.resource-pack.enabled", true)
                && plugin.effectArtPack() != null && plugin.effectArtPack().ready(player)) return;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || !item.hasItemMeta()) continue;
            ItemMeta meta = item.getItemMeta();
            try {
                Object model = GET_MODEL.invoke(meta);
                if (model != null && model.toString().startsWith("newgodwar:gui/")) {
                    SET_MODEL.invoke(meta, new Object[] {null});
                    item.setItemMeta(meta);
                    inventory.setItem(slot, item);
                }
            } catch (ReflectiveOperationException | LinkageError ignored) { }
        }
    }

    private static void presentLegacy(Player player, Inventory inventory, NewGodWarPlugin plugin) {
        boolean ready = plugin.getConfig().getBoolean("ui.resource-pack.enabled", true)
            && plugin.effectArtPack() != null && plugin.effectArtPack().ready(player);
        java.util.Map<Integer, ItemStack[]> originals = LEGACY_ICONS.get(inventory);
        if (originals == null) {
            originals = new java.util.HashMap<Integer, ItemStack[]>();
            LEGACY_ICONS.put(inventory, originals);
        }
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || !item.hasItemMeta()) continue;
            ItemMeta meta = item.getItemMeta();
            try {
                if (!PackModels.gui(meta)) continue;
                if (ready) {
                    // Legacy models dispatch on PAPER; keep the original material for fallback.
                    ItemStack[] prior = originals.get(slot);
                    if (prior != null && item.equals(prior[1])) continue;
                    ItemStack original = item.clone();
                    item.setType(Material.PAPER);
                    item.setDurability((short) 0);
                    item.setItemMeta(meta);
                    originals.put(slot, new ItemStack[] {original, item.clone()});
                } else {
                    ItemStack[] prior = originals.remove(slot);
                    if (prior != null && item.equals(prior[1])) {
                        item = prior[0].clone();
                        meta = item.getItemMeta();
                    }
                    ItemMeta.class.getMethod("setCustomModelData", Integer.class).invoke(meta, new Object[] {null});
                }
                item.setItemMeta(meta);
                inventory.setItem(slot, item);
            } catch (ReflectiveOperationException | LinkageError ignored) { }
        }
        if (!ready) LEGACY_ICONS.remove(inventory);
    }

    static void frame(Inventory inventory) {
        inventory.clear();
        int lastRow = inventory.getSize() - 9;
        ItemStack border = icon(item("BLACK_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", (short) 15, " "), GuiIcon.FRAME);
        ItemStack accent = icon(item("CYAN_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", (short) 9, " "), GuiIcon.ACCENT);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (slot < 9 || slot >= lastRow || slot % 9 == 0 || slot % 9 == 8) inventory.setItem(slot, border);
        }
        for (int slot : new int[] {0,8,lastRow,lastRow + 8}) inventory.setItem(slot, accent);
    }

    static ItemStack item(String modern, String legacy, short data, String title, String... lore) {
        Material material = Material.matchMaterial(modern);
        if (material == null) material = Material.matchMaterial(legacy);
        if (material == null) material = Material.PAPER;
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(title);
            meta.setLore(GuiText.wrap(Arrays.asList(lore)));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    static ItemStack heading(String title, String subtitle) {
        return icon(item("BOOK", "BOOK", (short) 0, ChatColor.AQUA + "" + ChatColor.BOLD + title,
            ChatColor.GRAY + subtitle), GuiIcon.INFO);
    }

    static ItemStack close() {
        return icon(item("BARRIER", "BARRIER", (short) 0, ChatColor.RED + "닫기",
            ChatColor.GRAY + "클릭하여 창 닫기 · Esc로도 닫을 수 있어요."), GuiIcon.CLOSE);
    }
}
