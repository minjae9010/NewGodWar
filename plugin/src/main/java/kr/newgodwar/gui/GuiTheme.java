package kr.newgodwar.gui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.Arrays;

final class GuiTheme {
    private GuiTheme() { }

    static void frame(Inventory inventory) {
        inventory.clear();
        int lastRow = inventory.getSize() - 9;
        ItemStack border = item("BLACK_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", (short) 15, " ");
        ItemStack accent = item("CYAN_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", (short) 9, " ");
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
            meta.setLore(Arrays.asList(lore));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    static ItemStack heading(String title, String subtitle) {
        return item("BOOK", "BOOK", (short) 0, ChatColor.AQUA + "" + ChatColor.BOLD + title,
            "", ChatColor.GRAY + subtitle);
    }

    static ItemStack close() {
        return item("BARRIER", "BARRIER", (short) 0, ChatColor.RED + "닫기", ChatColor.GRAY + "클릭하여 창을 닫습니다.");
    }
}
