package kr.newgodwar.nms;

import org.bukkit.plugin.Plugin;

public final class ReflectionNmsAdapterProvider implements NmsAdapterProvider {

    @Override
    public boolean supports() {
        return true;
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public NmsAdapter create(Plugin plugin) {
        return new ReflectionNmsAdapter(plugin);
    }

    @Override
    public String name() {
        return "reflection";
    }
}
