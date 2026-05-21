package kr.newgodwar.game;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class StarterItems {

    public static final String PATH = "game.skyblock-items";

    private StarterItems() {
    }

    public static List<Map<?, ?>> configuredEntries(FileConfiguration config) {
        List<Map<?, ?>> entries = config.getMapList(PATH);
        if (entries.isEmpty() && !config.isSet(PATH)) {
            return new ArrayList<Map<?, ?>>(defaultEntries());
        }
        return entries;
    }

    public static List<Map<String, Object>> copiedConfiguredEntries(FileConfiguration config) {
        List<Map<String, Object>> copied = new ArrayList<Map<String, Object>>();
        for (Map<?, ?> entry : configuredEntries(config)) {
            copied.add(copyEntry(entry));
        }
        return copied;
    }

    public static List<Map<String, Object>> defaultEntries() {
        List<Map<String, Object>> entries = new ArrayList<Map<String, Object>>();
        Map<String, Object> meat = new LinkedHashMap<String, Object>();
        meat.put("material", "COOKED_BEEF");
        meat.put("amount", 64);
        entries.add(meat);
        return entries;
    }

    public static Map<String, Object> fromItem(ItemStack item) {
        ItemStack saved = item.clone();
        Map<String, Object> entry = fromMaterial(saved.getType(), saved.getAmount());
        if (saved.getDurability() != 0) {
            entry.put("damage", saved.getDurability());
        }
        entry.put("item", saved);
        return entry;
    }

    public static Map<String, Object> fromMaterial(Material material, int amount) {
        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put("material", material.name());
        entry.put("amount", amount);
        return entry;
    }

    public static ItemStack toItemStack(Map<?, ?> entry) {
        if (entry == null) {
            return null;
        }
        Object configuredItem = entry.get("item");
        if (configuredItem instanceof ItemStack) {
            ItemStack stack = ((ItemStack) configuredItem).clone();
            if (stack.getAmount() <= 0) {
                stack.setAmount(amount(entry));
            }
            return stack.getType() == Material.AIR ? null : stack;
        }
        Material material = material(entry);
        if (material == null || material == Material.AIR) {
            return null;
        }
        return new ItemStack(material, amount(entry), damage(entry));
    }

    public static String displayName(Map<?, ?> entry) {
        ItemStack stack = toItemStack(entry);
        if (stack == null) {
            return "UNKNOWN x0";
        }
        return stack.getType().name() + " x" + stack.getAmount();
    }

    public static Material matchMaterial(String token) {
        if (token == null) {
            return null;
        }
        return Material.matchMaterial(token.toUpperCase(Locale.ROOT));
    }

    public static Map<String, Object> copyEntry(Map<?, ?> source) {
        Map<String, Object> copied = new LinkedHashMap<String, Object>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() != null) {
                copied.put(entry.getKey().toString(), entry.getValue());
            }
        }
        return copied;
    }

    private static Material material(Map<?, ?> entry) {
        Object configured = entry.get("material");
        Material material = configured == null ? null : matchMaterial(configured.toString());
        if (material != null) {
            return material;
        }
        Object legacy = entry.get("legacy-material");
        return legacy == null ? null : matchMaterial(legacy.toString());
    }

    private static int amount(Map<?, ?> entry) {
        return Math.max(1, parseInt(entry.get("amount"), 1));
    }

    private static short damage(Map<?, ?> entry) {
        return (short) Math.max(0, parseInt(entry.get("damage"), 0));
    }

    private static int parseInt(Object value, int fallback) {
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
}
