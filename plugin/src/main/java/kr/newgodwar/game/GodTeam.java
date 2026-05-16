package kr.newgodwar.game;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class GodTeam {

    public static final GodTeam RED = new GodTeam("red", "RED", "빨강", ChatColor.RED, 0);
    public static final GodTeam BLUE = new GodTeam("blue", "BLUE", "파랑", ChatColor.BLUE, 1);
    public static final GodTeam GREEN = new GodTeam("green", "GREEN", "초록", ChatColor.GREEN, 2);

    private static final GodTeam[] DEFAULTS = new GodTeam[] { RED, BLUE, GREEN };
    private static final Map<String, GodTeam> TEAMS = new LinkedHashMap<String, GodTeam>();
    private static final Map<String, GodTeam> ALIASES = new LinkedHashMap<String, GodTeam>();

    static {
        reload(null);
    }

    private final String id;
    private final String name;
    private final String defaultDisplayName;
    private final ChatColor color;
    private final int ordinal;

    private GodTeam(String id, String name, String defaultDisplayName, ChatColor color, int ordinal) {
        this.id = id;
        this.name = name;
        this.defaultDisplayName = defaultDisplayName;
        this.color = color;
        this.ordinal = ordinal;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public int ordinal() {
        return ordinal;
    }

    public String defaultDisplayName() {
        return defaultDisplayName;
    }

    public ChatColor color() {
        return color;
    }

    public boolean isDefaultTeam() {
        return this == RED || this == BLUE || this == GREEN || "red".equals(id) || "blue".equals(id) || "green".equals(id);
    }

    public String coloredName() {
        return color + defaultDisplayName + ChatColor.RESET;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof GodTeam)) {
            return false;
        }
        GodTeam team = (GodTeam) other;
        return id.equals(team.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public static GodTeam[] values() {
        Collection<GodTeam> teams = TEAMS.values();
        return teams.toArray(new GodTeam[teams.size()]);
    }

    public static List<String> ids() {
        List<String> ids = new ArrayList<String>();
        for (GodTeam team : values()) {
            ids.add(team.id());
        }
        return ids;
    }

    public static void reload(FileConfiguration config) {
        Map<String, GodTeam> loaded = new LinkedHashMap<String, GodTeam>();
        for (GodTeam team : DEFAULTS) {
            loaded.put(team.id(), team);
        }

        Map<String, String> displayNames = new LinkedHashMap<String, String>();
        if (config != null) {
            ConfigurationSection section = config.getConfigurationSection("teams");
            if (section != null) {
                int ordinal = loaded.size();
                for (String key : section.getKeys(false)) {
                    String id = normalizeId(key);
                    if (id == null) {
                        continue;
                    }
                    String displayName = config.getString("teams." + key + ".display-name", key);
                    displayNames.put(id, displayName);
                    if (loaded.containsKey(id)) {
                        continue;
                    }
                    ChatColor color = parseColor(config.getString("teams." + key + ".color"), ChatColor.WHITE);
                    loaded.put(id, existingOrNew(id, displayName, color, ordinal++));
                }
            }
        }

        TEAMS.clear();
        TEAMS.putAll(loaded);
        ALIASES.clear();
        for (GodTeam team : values()) {
            ALIASES.put(team.id().toLowerCase(Locale.ROOT), team);
            ALIASES.put(team.name().toLowerCase(Locale.ROOT), team);
            ALIASES.put(team.defaultDisplayName().toLowerCase(Locale.ROOT), team);
            String displayName = displayNames.get(team.id());
            if (displayName != null) {
                ALIASES.put(displayName.toLowerCase(Locale.ROOT), team);
            }
        }
    }

    public static GodTeam create(FileConfiguration config, String requestedId) {
        String id = normalizeId(requestedId);
        if (id == null || TEAMS.containsKey(id)) {
            id = nextId();
        }
        ChatColor color = nextDefaultColor();
        config.set("teams." + id + ".enabled", true);
        config.set("teams." + id + ".display-name", defaultNameFor(id));
        config.set("teams." + id + ".color", color.name());
        reload(config);
        return parse(id);
    }

    public static boolean remove(FileConfiguration config, GodTeam team) {
        if (config == null || team == null || team.isDefaultTeam() || values().length <= 2) {
            return false;
        }
        config.set("teams." + team.id(), null);
        reload(config);
        return true;
    }

    public static GodTeam parse(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.toLowerCase(Locale.ROOT).trim();
        return ALIASES.get(normalized);
    }

    private static GodTeam existingOrNew(String id, String displayName, ChatColor color, int ordinal) {
        GodTeam existing = TEAMS.get(id);
        if (existing != null) {
            return existing;
        }
        return new GodTeam(id, id.toUpperCase(Locale.ROOT).replace('-', '_'), displayName, color, ordinal);
    }

    private static String normalizeId(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace(' ', '-');
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-' || c == '_') {
                builder.append(c);
            }
        }
        return builder.length() == 0 ? null : builder.toString();
    }

    private static String nextId() {
        int index = values().length + 1;
        while (TEAMS.containsKey("team" + index)) {
            index++;
        }
        return "team" + index;
    }

    private static String defaultNameFor(String id) {
        if (id.startsWith("team")) {
            return "팀" + id.substring(4);
        }
        return id;
    }

    private static ChatColor nextDefaultColor() {
        ChatColor[] colors = new ChatColor[] {
            ChatColor.YELLOW,
            ChatColor.AQUA,
            ChatColor.GOLD,
            ChatColor.LIGHT_PURPLE,
            ChatColor.WHITE,
            ChatColor.DARK_GREEN,
            ChatColor.DARK_AQUA,
            ChatColor.DARK_PURPLE
        };
        return colors[values().length % colors.length];
    }

    private static ChatColor parseColor(String value, ChatColor fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            ChatColor color = ChatColor.valueOf(value.trim().toUpperCase(Locale.ROOT));
            return color.isColor() ? color : fallback;
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}
