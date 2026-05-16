package kr.newgodwar.nms;

import org.bukkit.entity.Player;

public interface NmsAdapter {

    String getServerVersion();

    void sendActionBar(Player player, String message);

    void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut);
}
