package kr.newgodwar.ability.feedback;

import org.bukkit.Bukkit;
import org.bukkit.inventory.meta.ItemMeta;
import java.io.InputStream;
import java.util.Properties;

/** The generated catalogue is shared with the ZIP compiler, including legacy model IDs. */
public final class PackModels {
    private static final Properties PACKS = load("/art-packs.properties");
    private static final Properties MODELS = load("/art-models.properties");
    private PackModels() { }

    private static Properties load(String resource) {
        Properties result = new Properties();
        try (InputStream input = PackModels.class.getResourceAsStream(resource)) {
            if (input != null) result.load(input);
        } catch (java.io.IOException error) { throw new IllegalStateException(resource, error); }
        return result;
    }

    public static String[] pack(String version) {
        if (version == null || !version.matches("[0-9]+(?:\\.[0-9]+){1,2}(?:-R.*)?")) return null;
        String value = PACKS.getProperty(version.split("-", 2)[0]);
        return value == null ? null : value.split("\\|");
    }
    public static boolean modern() {
        String[] pack = currentPack();
        return pack != null && "modern".equals(pack[2]);
    }
    public static String[] currentPack() { return Current.PACK; }
    private static final class Current {
        static final String[] PACK = detect();
        private static String[] detect() {
            try {
                String version = (String) Bukkit.getServer().getClass().getMethod("getMinecraftVersion").invoke(Bukkit.getServer());
                return pack(version);
            } catch (ReflectiveOperationException | LinkageError ignored) {
                return pack(Bukkit.getBukkitVersion());
            }
        }
    }
    public static int modelId(String key) {
        return Integer.parseInt(MODELS.getProperty(key, "0"));
    }
    public static void apply(ItemMeta meta, String path) throws ReflectiveOperationException {
        if (modern()) {
            Class<?> key = Class.forName("org.bukkit.NamespacedKey");
            ItemMeta.class.getMethod("setItemModel", key).invoke(meta, key.getConstructor(String.class, String.class).newInstance("newgodwar", path));
        } else {
            int id = modelId(path);
            if (id != 0) ItemMeta.class.getMethod("setCustomModelData", Integer.class).invoke(meta, Integer.valueOf(id));
        }
    }
    public static boolean gui(ItemMeta meta) throws ReflectiveOperationException {
        if (!(Boolean) ItemMeta.class.getMethod("hasCustomModelData").invoke(meta)) return false;
        String id = String.valueOf(ItemMeta.class.getMethod("getCustomModelData").invoke(meta));
        for (String key : MODELS.stringPropertyNames())
            if (key.startsWith("gui/") && id.equals(MODELS.getProperty(key))) return true;
        return false;
    }
}
