package kr.newgodwar.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class Messages {

    private final JavaPlugin plugin;

    public Messages(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public String prefix() {
        return color(plugin.getConfig().getString("messages.prefix", "&6[신들의 전쟁]&r "));
    }

    public String get(String path) {
        FileConfiguration config = plugin.getConfig();
        return color(config.getString("messages." + path, path));
    }

    public String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message == null ? "" : message);
    }

    public void send(CommandSender sender, String message) {
        sender.sendMessage(prefix() + color(message));
    }

    public void sendKey(CommandSender sender, String key) {
        sender.sendMessage(prefix() + get(key));
    }
}
