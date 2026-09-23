package kr.newgodwar.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/** Inventory operations run on the server thread; resource costs use storage slots only. */
public final class InventoryItems {
    private InventoryItems() { }

    public static int count(PlayerInventory inventory, Material material) {
        long total = 0;
        for (ItemStack item : inventory.getStorageContents()) {
            if (item != null && item.getType() == material && item.getAmount() > 0) total += item.getAmount();
        }
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    /** Metadata is preserved on remaining stacks. Insufficient funds leave every slot unchanged. */
    public static boolean take(PlayerInventory inventory, Material material, int amount) {
        if (amount <= 0) return true;
        if (count(inventory, material) < amount) return false;
        ItemStack[] contents = inventory.getStorageContents();
        int remaining = amount;
        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            ItemStack item = contents[slot];
            if (item == null || item.getType() != material || item.getAmount() <= 0) continue;
            int taken = Math.min(remaining, item.getAmount());
            ItemStack leftover = item.clone();
            leftover.setAmount(item.getAmount() - taken);
            inventory.setItem(slot, leftover.getAmount() == 0 ? null : leftover);
            remaining -= taken;
        }
        return true;
    }

    /** addItem may mutate its arguments; never derive another grant from those mutated amounts. */
    public static void give(Player player, ItemStack... items) {
        List<ItemStack> copies = new ArrayList<ItemStack>();
        for (ItemStack item : items) {
            int remaining = item.getAmount();
            int limit = Math.max(1, item.getMaxStackSize());
            while (remaining > 0) {
                int amount = Math.min(limit, remaining);
                ItemStack stack = item.clone();
                stack.setAmount(amount);
                copies.add(stack);
                remaining -= amount;
            }
        }
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(copies.toArray(new ItemStack[copies.size()]));
        if (leftovers.isEmpty()) return;
        player.sendMessage(ChatColor.YELLOW + "인벤토리가 꽉 찼습니다. 들어가지 못한 아이템을 발밑에 떨어뜨립니다.");
        for (ItemStack leftover : leftovers.values()) {
            if (leftover != null && leftover.getType() != Material.AIR && leftover.getAmount() > 0) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover.clone());
            }
        }
    }
}
