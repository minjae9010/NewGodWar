package kr.newgodwar.nms;

import org.bukkit.plugin.Plugin;

import java.util.ServiceLoader;

public final class NmsAdapters {

    private NmsAdapters() {
    }

    public static NmsAdapter create(Plugin plugin) {
        NmsAdapterProvider selected = null;
        for (NmsAdapterProvider provider : ServiceLoader.load(NmsAdapterProvider.class)) {
            if (!provider.supports()) {
                continue;
            }
            if (selected == null || provider.priority() > selected.priority()) {
                selected = provider;
            }
        }

        if (selected == null) {
            throw new IllegalStateException("No NMS compatibility adapter provider is available.");
        }
        plugin.getLogger().info("Using compatibility adapter: " + selected.name());
        return selected.create(plugin);
    }
}
