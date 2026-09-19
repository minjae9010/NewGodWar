package kr.newgodwar.command;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Clickable chat with the same readable text for console and older implementations. */
final class HelpChat {
    private HelpChat() { }

    static BaseComponent[] link(String text, String command, String hover, boolean navigate) {
        BaseComponent[] components = TextComponent.fromLegacyText(text);
        for (BaseComponent component : components) {
            component.setClickEvent(new ClickEvent(navigate ? ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND, command));
            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(hover)));
        }
        return components;
    }

    static void send(CommandSender sender, String text, String command, String hover, boolean navigate) {
        if (sender instanceof Player) {
            try {
                Player.Spigot spigot = ((Player) sender).spigot();
                if (spigot != null) {
                    spigot.sendMessage(link(text, command, hover, navigate));
                    return;
                }
            } catch (LinkageError | UnsupportedOperationException ignored) {
                // Plain text remains usable on implementations without component chat.
            }
        }
        sender.sendMessage(text);
    }

    static void header(CommandSender sender, String title, int page, int pages, int count) {
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GOLD + "§l NewGodWar §r§8› §f" + title
            + (count < 0 ? "" : " §8| §e" + page + "/" + pages + " §7· " + count + "개"));
        sender.sendMessage(ChatColor.DARK_GRAY + " ──────────────────────────────");
    }

    static void footer(CommandSender sender, String prefix, int page, int pages) {
        String home = "§e[목차]";
        String previous = "§b[이전]";
        String next = "§b[다음]";
        if (!(sender instanceof Player)) {
            sender.sendMessage("§8 ──────────────────────────────");
            sender.sendMessage("§7 목차: /gw help" + (page > 1 ? " | 이전: " + prefix + " " + (page - 1) : "")
                + (page < pages ? " | 다음: " + prefix + " " + (page + 1) : ""));
            return;
        }
        TextComponent navigation = new TextComponent("  ");
        if (page > 1) append(navigation, link(previous + "  ", prefix + " " + (page - 1), "이전 페이지", true));
        append(navigation, link(home + "  ", "/gw help", "기능별 목차", true));
        if (page < pages) append(navigation, link(next, prefix + " " + (page + 1), "다음 페이지", true));
        try {
            Player.Spigot spigot = ((Player) sender).spigot();
            if (spigot != null) { spigot.sendMessage(navigation); return; }
        } catch (LinkageError | UnsupportedOperationException ignored) { }
        sender.sendMessage("§7 목차: /gw help | 페이지: " + prefix + " <1-" + pages + ">");
    }

    private static void append(TextComponent parent, BaseComponent[] children) {
        for (BaseComponent child : children) parent.addExtra(child);
    }
}
