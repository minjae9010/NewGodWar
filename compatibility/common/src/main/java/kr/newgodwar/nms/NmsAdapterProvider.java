package kr.newgodwar.nms;

import org.bukkit.plugin.Plugin;

public interface NmsAdapterProvider {

    boolean supports();

    int priority();

    NmsAdapter create(Plugin plugin);

    String name();
}
