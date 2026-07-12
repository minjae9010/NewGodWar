package kr.newgodwar.util;

import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ServerVersionSupport {

    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");
    private static final Set<String> PAPER_DOWNLOAD_VERSIONS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
        "1.7.10",
        "1.8.8",
        "1.9.4",
        "1.10.2",
        "1.11.2",
        "1.12",
        "1.12.1",
        "1.12.2",
        "1.13",
        "1.13.1",
        "1.13.2",
        "1.14",
        "1.14.1",
        "1.14.2",
        "1.14.3",
        "1.14.4",
        "1.15",
        "1.15.1",
        "1.15.2",
        "1.16.1",
        "1.16.2",
        "1.16.3",
        "1.16.4",
        "1.16.5",
        "1.17",
        "1.17.1",
        "1.18",
        "1.18.1",
        "1.18.2",
        "1.19",
        "1.19.1",
        "1.19.2",
        "1.19.3",
        "1.19.4",
        "1.20",
        "1.20.1",
        "1.20.2",
        "1.20.4",
        "1.20.5",
        "1.20.6",
        "1.21",
        "1.21.1",
        "1.21.3",
        "1.21.4",
        "1.21.5",
        "1.21.6",
        "1.21.7",
        "1.21.8",
        "1.21.9",
        "1.21.10",
        "1.21.11",
        "26.1.1",
        "26.1.2",
        "26.2"
    )));

    private final String minecraftVersion;
    private final String bukkitVersion;
    private final String serverName;
    private final boolean paperServer;
    private final boolean paperDownloadVersion;

    private ServerVersionSupport(String minecraftVersion, String bukkitVersion, String serverName,
                                 boolean paperServer, boolean paperDownloadVersion) {
        this.minecraftVersion = minecraftVersion;
        this.bukkitVersion = bukkitVersion;
        this.serverName = serverName;
        this.paperServer = paperServer;
        this.paperDownloadVersion = paperDownloadVersion;
    }

    public static ServerVersionSupport detect() {
        String minecraft = detectMinecraftVersion();
        String bukkit = Bukkit.getBukkitVersion();
        String serverName = Bukkit.getName();
        return new ServerVersionSupport(
            minecraft,
            bukkit,
            serverName,
            detectPaperServer(serverName),
            isPaperDownloadVersion(minecraft, bukkit)
        );
    }

    public String minecraftVersion() {
        return minecraftVersion;
    }

    public String bukkitVersion() {
        return bukkitVersion;
    }

    public boolean supported() {
        return paperServer && paperDownloadVersion;
    }

    public boolean paperServer() {
        return paperServer;
    }

    public boolean paperDownloadVersion() {
        return paperDownloadVersion;
    }

    public String summary() {
        return "Minecraft " + minecraftVersion + " on " + serverName + " (Bukkit API " + bukkitVersion + ")";
    }

    private static String detectMinecraftVersion() {
        try {
            Method method = Bukkit.getServer().getClass().getMethod("getMinecraftVersion");
            Object value = method.invoke(Bukkit.getServer());
            if (value instanceof String && !((String) value).trim().isEmpty()) {
                return (String) value;
            }
        } catch (Throwable ignored) {
        }
        return Bukkit.getBukkitVersion();
    }

    private static boolean isPaperDownloadVersion(String minecraftVersion, String bukkitVersion) {
        String minecraft = normalize(minecraftVersion);
        if (PAPER_DOWNLOAD_VERSIONS.contains(minecraft)) {
            return true;
        }
        return PAPER_DOWNLOAD_VERSIONS.contains(normalize(bukkitVersion));
    }

    static boolean detectPaperServer(String serverName) {
        String normalizedName = serverName == null ? "" : serverName.toLowerCase(Locale.ROOT);
        if (normalizedName.contains("paper")
            || normalizedName.contains("purpur")
            || normalizedName.contains("pufferfish")
            || normalizedName.contains("folia")) {
            return true;
        }
        return classExists("io.papermc.paper.ServerBuildInfo")
            || classExists("io.papermc.paper.configuration.Configuration")
            || classExists("com.destroystokyo.paper.PaperConfig");
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name, false, ServerVersionSupport.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError ex) {
            return false;
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        Matcher matcher = VERSION_PATTERN.matcher(value);
        if (!matcher.find()) {
            return value.trim();
        }
        String version = matcher.group(1) + "." + matcher.group(2);
        if (matcher.group(3) != null) {
            version += "." + matcher.group(3);
        }
        return version;
    }

    public static Set<String> paperDownloadVersions() {
        return PAPER_DOWNLOAD_VERSIONS;
    }
}
