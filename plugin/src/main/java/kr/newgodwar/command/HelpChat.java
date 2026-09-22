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

    static void section(CommandSender sender, String title, boolean admin) {
        sender.sendMessage((admin ? "§6" : "§a") + " ▸ " + title);
    }

    static void topics(CommandSender sender, String... namesAndTopics) {
        TextComponent navigation = new TextComponent(" ");
        StringBuilder fallback = new StringBuilder("§7 ");
        for (int index = 0; index < namesAndTopics.length; index += 2) {
            String name = namesAndTopics[index];
            String command = "/gw help " + namesAndTopics[index + 1];
            append(navigation, link("§e[" + name + "] ", command, "§f" + command + "\n§7" + name + " 보기", true));
            if (index > 0) fallback.append(" §8| §7");
            fallback.append(name).append(": §f").append(command);
        }
        if (sender instanceof Player) {
            try {
                Player.Spigot spigot = ((Player) sender).spigot();
                if (spigot != null) { spigot.sendMessage(navigation); return; }
            } catch (LinkageError | UnsupportedOperationException ignored) { }
        }
        sender.sendMessage(fallback.toString());
    }

    static void footer(CommandSender sender, String prefix, int page, int pages) {
        String home = "§e[기본 명령]";
        String previous = "§b[이전]";
        String next = "§b[다음]";
        if (!(sender instanceof Player)) {
            sender.sendMessage("§8 ──────────────────────────────");
            sender.sendMessage("§7 기본 명령: /gw" + (page > 1 ? " | 이전: " + prefix + " " + (page - 1) : "")
                + (page < pages ? " | 다음: " + prefix + " " + (page + 1) : ""));
            return;
        }
        TextComponent navigation = new TextComponent("  ");
        if (page > 1) append(navigation, link(previous + "  ", prefix + " " + (page - 1), "이전 페이지", true));
        append(navigation, link(home + "  ", "/gw", "자주 쓰는 명령어로 돌아가기", true));
        if (page < pages) append(navigation, link(next, prefix + " " + (page + 1), "다음 페이지", true));
        try {
            Player.Spigot spigot = ((Player) sender).spigot();
            if (spigot != null) { spigot.sendMessage(navigation); return; }
        } catch (LinkageError | UnsupportedOperationException ignored) { }
        sender.sendMessage("§7 기본 명령: /gw | 페이지: " + prefix + " <1-" + pages + ">");
    }

    private static void append(TextComponent parent, BaseComponent[] children) {
        for (BaseComponent child : children) parent.addExtra(child);
    }
}
