package kr.newgodwar.game;

import org.bukkit.ChatColor;

import java.util.Locale;

public enum GodTeam {
    RED("red", "빨강", ChatColor.RED),
    BLUE("blue", "파랑", ChatColor.BLUE),
    GREEN("green", "초록", ChatColor.GREEN);

    private final String id;
    private final String defaultDisplayName;
    private final ChatColor color;

    GodTeam(String id, String defaultDisplayName, ChatColor color) {
        this.id = id;
        this.defaultDisplayName = defaultDisplayName;
        this.color = color;
    }

    public String id() {
        return id;
    }

    public String defaultDisplayName() {
        return defaultDisplayName;
    }

    public ChatColor color() {
        return color;
    }

    public String coloredName() {
        return color + defaultDisplayName + ChatColor.RESET;
    }

    public static GodTeam parse(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        for (GodTeam team : values()) {
            if (team.id.equals(normalized) || team.name().equalsIgnoreCase(value) || team.defaultDisplayName.equals(value)) {
                return team;
            }
        }
        return null;
    }
}
