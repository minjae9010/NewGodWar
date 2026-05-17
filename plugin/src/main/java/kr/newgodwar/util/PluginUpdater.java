package kr.newgodwar.util;

import kr.newgodwar.NewGodWarPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PluginUpdater {

    private static final Pattern JSON_STRING_PATTERN = Pattern.compile("\"%s\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");
    private static final Pattern DOWNLOAD_URL_PATTERN = Pattern.compile("\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.jar)\"");
    private static final int CONNECT_TIMEOUT_MILLIS = 10000;
    private static final int READ_TIMEOUT_MILLIS = 20000;

    private final NewGodWarPlugin plugin;
    private final AtomicBoolean checking = new AtomicBoolean(false);

    private volatile UpdateInfo lastInfo;
    private volatile BukkitTask scheduledTask;
    private volatile String lastNotifiedVersion;

    public PluginUpdater(NewGodWarPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("updates.enabled", true)) {
            return;
        }

        long initialDelayTicks = Math.max(1L, plugin.getConfig().getLong("updates.initial-delay-seconds", 10L)) * 20L;
        long intervalMinutes = plugin.getConfig().getLong("updates.check-interval-minutes", 60L);
        boolean autoDownload = plugin.getConfig().getBoolean("updates.auto-download", true);

        if (intervalMinutes <= 0L) {
            scheduledTask = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> checkAndNotify(autoDownload), initialDelayTicks);
            return;
        }

        long intervalTicks = Math.max(1L, intervalMinutes) * 60L * 20L;
        scheduledTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> checkAndNotify(autoDownload), initialDelayTicks, intervalTicks);
    }

    public void shutdown() {
        BukkitTask task = scheduledTask;
        if (task != null) {
            task.cancel();
            scheduledTask = null;
        }
    }

    public UpdateInfo lastInfo() {
        return lastInfo;
    }

    public boolean checkNow(final boolean download, final Consumer<UpdateInfo> callback) {
        if (!plugin.getConfig().getBoolean("updates.enabled", true)) {
            UpdateInfo disabled = UpdateInfo.disabled(pluginVersion());
            lastInfo = disabled;
            runCallback(callback, disabled);
            return true;
        }
        if (!checking.compareAndSet(false, true)) {
            return false;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            UpdateInfo info;
            try {
                info = fetchLatest();
                if (download && info.updateAvailable()) {
                    info = downloadUpdate(info);
                }
            } catch (Exception ex) {
                info = UpdateInfo.error(pluginVersion(), ex.getMessage());
                plugin.getLogger().warning("Update check failed: " + ex.getMessage());
            } finally {
                checking.set(false);
            }

            lastInfo = info;
            runCallback(callback, info);
        });
        return true;
    }

    public void notifyAdminIfOutdated(final Player player) {
        if (!plugin.getConfig().getBoolean("updates.notify-admins", true)) {
            return;
        }
        if (!player.hasPermission("newgodwar.admin")) {
            return;
        }

        UpdateInfo info = lastInfo;
        if (info != null) {
            if (info.updateAvailable()) {
                sendNotice(player, info);
            }
            return;
        }

        boolean autoDownload = plugin.getConfig().getBoolean("updates.auto-download", true);
        checkNow(autoDownload, checked -> {
            if (checked.updateAvailable() && player.isOnline() && player.hasPermission("newgodwar.admin")) {
                sendNotice(player, checked);
            }
        });
    }

    public void sendStatus(CommandSender sender, UpdateInfo info) {
        if (!info.enabled()) {
            plugin.messages().send(sender, "&7자동 업데이트가 꺼져 있습니다.");
            return;
        }
        if (info.errorMessage() != null) {
            plugin.messages().send(sender, "&c업데이트 확인 실패: &f" + info.errorMessage());
            return;
        }
        if (!info.updateAvailable()) {
            plugin.messages().send(sender, "&a현재 최신 버전입니다. &7(" + info.currentVersion() + ")");
            return;
        }
        sendNotice(sender, info);
    }

    public void sendNotice(CommandSender sender, UpdateInfo info) {
        plugin.messages().send(sender, "&c새 NewGodWar 버전이 있습니다: &f"
            + info.currentVersion() + " &7-> &a" + info.latestVersion());
        if (info.downloadedFile() != null) {
            plugin.messages().send(sender, "&a업데이트 jar 준비 완료: &f" + info.downloadedFile().getPath());
            plugin.messages().send(sender, "&7서버를 재시작하면 새 버전이 적용됩니다.");
        } else {
            plugin.messages().send(sender, "&e/godwar update download &7명령으로 서버 실행 중에 업데이트 jar를 받을 수 있습니다.");
        }
        if (info.releaseUrl() != null) {
            sender.sendMessage(ChatColor.DARK_GRAY + "  " + ChatColor.GRAY + info.releaseUrl());
        }
    }

    private void checkAndNotify(boolean autoDownload) {
        checkNow(autoDownload, info -> {
            if (!info.updateAvailable()) {
                return;
            }
            plugin.getLogger().warning("NewGodWar " + info.latestVersion() + " is available. Current version: " + info.currentVersion());
            if (info.downloadedFile() != null) {
                plugin.getLogger().warning("Downloaded update jar to " + info.downloadedFile().getPath() + ". Restart the server to apply it.");
            }
            notifyOnlineAdminsOnce(info);
        });
    }

    private void notifyOnlineAdminsOnce(UpdateInfo info) {
        if (!plugin.getConfig().getBoolean("updates.notify-admins", true)) {
            return;
        }
        if (info.latestVersion().equals(lastNotifiedVersion)) {
            return;
        }
        lastNotifiedVersion = info.latestVersion();
        for (Player player : BukkitCompat.onlinePlayers()) {
            if (player.hasPermission("newgodwar.admin")) {
                sendNotice(player, info);
            }
        }
    }

    private void runCallback(final Consumer<UpdateInfo> callback, final UpdateInfo info) {
        if (callback == null) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> callback.accept(info));
    }

    private UpdateInfo fetchLatest() throws IOException {
        String owner = plugin.getConfig().getString("updates.github.owner", "minjae9010");
        String repo = plugin.getConfig().getString("updates.github.repo", "NewGodWar");
        URL url = new URL("https://api.github.com/repos/" + owner + "/" + repo + "/releases/latest");
        String body = httpGet(url);

        String tagName = extractJsonString(body, "tag_name");
        String releaseUrl = extractJsonString(body, "html_url");
        String downloadUrl = findJarDownloadUrl(body);
        String latestVersion = stripVersionPrefix(tagName);
        String currentVersion = pluginVersion();

        if (latestVersion == null || latestVersion.length() == 0) {
            throw new IOException("latest release tag_name을 찾을 수 없습니다.");
        }

        boolean updateAvailable = compareVersions(latestVersion, currentVersion) > 0;
        return UpdateInfo.checked(currentVersion, latestVersion, releaseUrl, downloadUrl, updateAvailable);
    }

    private UpdateInfo downloadUpdate(UpdateInfo info) throws IOException {
        if (info.downloadUrl() == null) {
            return info.withError("릴리즈 jar asset을 찾을 수 없습니다.");
        }

        URL url = new URL(info.downloadUrl());
        HttpURLConnection connection = openConnection(url);
        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) {
            throw new IOException("jar 다운로드 HTTP " + status);
        }

        File updateFolder = resolveUpdateFolder();
        if (!updateFolder.exists() && !updateFolder.mkdirs()) {
            throw new IOException("업데이트 폴더를 만들 수 없습니다: " + updateFolder.getPath());
        }

        File target = new File(updateFolder, safeFileName(fileNameFromUrl(info.downloadUrl(), info.latestVersion())));
        try (InputStream input = connection.getInputStream(); FileOutputStream output = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
        } finally {
            connection.disconnect();
        }

        return info.withDownloadedFile(target);
    }

    private HttpURLConnection openConnection(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(READ_TIMEOUT_MILLIS);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("User-Agent", "NewGodWar/" + pluginVersion());
        return connection;
    }

    private String httpGet(URL url) throws IOException {
        HttpURLConnection connection = openConnection(url);
        int status = connection.getResponseCode();
        InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
        String body = readText(stream);
        connection.disconnect();
        if (status < 200 || status >= 300) {
            throw new IOException("GitHub API HTTP " + status + ": " + body);
        }
        return body;
    }

    private String readText(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = stream.read(buffer)) >= 0) {
            output.write(buffer, 0, read);
        }
        return new String(output.toByteArray(), "UTF-8");
    }

    private String extractJsonString(String json, String key) {
        Pattern pattern = Pattern.compile(String.format(JSON_STRING_PATTERN.pattern(), Pattern.quote(key)));
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return null;
        }
        return unescapeJson(matcher.group(1));
    }

    private String findJarDownloadUrl(String json) {
        Matcher matcher = DOWNLOAD_URL_PATTERN.matcher(json);
        String fallback = null;
        while (matcher.find()) {
            String url = unescapeJson(matcher.group(1));
            String lower = url.toLowerCase(Locale.ROOT);
            if (lower.contains("newgodwar") && lower.endsWith(".jar")) {
                return url;
            }
            if (fallback == null && lower.endsWith(".jar")) {
                fallback = url;
            }
        }
        return fallback;
    }

    private String unescapeJson(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != '\\' || i + 1 >= value.length()) {
                builder.append(c);
                continue;
            }
            char escaped = value.charAt(++i);
            if (escaped == '"' || escaped == '\\' || escaped == '/') {
                builder.append(escaped);
            } else if (escaped == 'n') {
                builder.append('\n');
            } else if (escaped == 'r') {
                builder.append('\r');
            } else if (escaped == 't') {
                builder.append('\t');
            } else {
                builder.append(escaped);
            }
        }
        return builder.toString();
    }

    private File resolveUpdateFolder() {
        File reflected = invokeUpdateFolder(Bukkit.class, null);
        if (reflected != null) {
            return reflected;
        }
        reflected = invokeUpdateFolder(plugin.getServer().getClass(), plugin.getServer());
        if (reflected != null) {
            return reflected;
        }
        File pluginsFolder = plugin.getDataFolder().getParentFile();
        return new File(pluginsFolder == null ? new File(".") : pluginsFolder, "update");
    }

    private File invokeUpdateFolder(Class<?> type, Object target) {
        try {
            Method method = type.getMethod("getUpdateFolderFile");
            Object result = method.invoke(target);
            return result instanceof File ? (File) result : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String fileNameFromUrl(String url, String latestVersion) {
        String path = url;
        int query = path.indexOf('?');
        if (query >= 0) {
            path = path.substring(0, query);
        }
        int slash = path.lastIndexOf('/');
        String fileName = slash >= 0 ? path.substring(slash + 1) : path;
        try {
            fileName = URLDecoder.decode(fileName, "UTF-8");
        } catch (Exception ignored) {
        }
        if (fileName.length() == 0 || !fileName.toLowerCase(Locale.ROOT).endsWith(".jar")) {
            return "NewGodWar-" + latestVersion + ".jar";
        }
        return fileName;
    }

    private String safeFileName(String fileName) {
        return fileName.replace('\\', '_').replace('/', '_').replace(':', '_');
    }

    private String pluginVersion() {
        return plugin.getDescription().getVersion();
    }

    private String stripVersionPrefix(String version) {
        if (version == null) {
            return null;
        }
        String trimmed = version.trim();
        return trimmed.startsWith("v") || trimmed.startsWith("V") ? trimmed.substring(1) : trimmed;
    }

    private int compareVersions(String left, String right) {
        String[] leftParts = splitVersion(left);
        String[] rightParts = splitVersion(right);
        int max = Math.max(leftParts.length, rightParts.length);
        for (int i = 0; i < max; i++) {
            String l = i < leftParts.length ? leftParts[i] : "";
            String r = i < rightParts.length ? rightParts[i] : "";
            if (l.equals(r)) {
                continue;
            }
            if (l.length() == 0) {
                return isNumeric(r) ? compareInts(0, parseInt(r)) : 1;
            }
            if (r.length() == 0) {
                return isNumeric(l) ? compareInts(parseInt(l), 0) : -1;
            }
            boolean lNumeric = isNumeric(l);
            boolean rNumeric = isNumeric(r);
            if (lNumeric && rNumeric) {
                int compared = compareInts(parseInt(l), parseInt(r));
                if (compared != 0) {
                    return compared;
                }
                continue;
            }
            if (lNumeric != rNumeric) {
                return lNumeric ? 1 : -1;
            }
            int compared = l.compareToIgnoreCase(r);
            if (compared != 0) {
                return compared;
            }
        }
        return 0;
    }

    private String[] splitVersion(String version) {
        return stripVersionPrefix(version).split("[^0-9A-Za-z]+");
    }

    private boolean isNumeric(String text) {
        if (text == null || text.length() == 0) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isDigit(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private int parseInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            return Integer.MAX_VALUE;
        }
    }

    private int compareInts(int left, int right) {
        return left < right ? -1 : (left == right ? 0 : 1);
    }

    public static final class UpdateInfo {
        private final boolean enabled;
        private final String currentVersion;
        private final String latestVersion;
        private final String releaseUrl;
        private final String downloadUrl;
        private final boolean updateAvailable;
        private final File downloadedFile;
        private final String errorMessage;

        private UpdateInfo(boolean enabled, String currentVersion, String latestVersion, String releaseUrl,
                           String downloadUrl, boolean updateAvailable, File downloadedFile, String errorMessage) {
            this.enabled = enabled;
            this.currentVersion = currentVersion;
            this.latestVersion = latestVersion;
            this.releaseUrl = releaseUrl;
            this.downloadUrl = downloadUrl;
            this.updateAvailable = updateAvailable;
            this.downloadedFile = downloadedFile;
            this.errorMessage = errorMessage;
        }

        private static UpdateInfo checked(String currentVersion, String latestVersion, String releaseUrl,
                                          String downloadUrl, boolean updateAvailable) {
            return new UpdateInfo(true, currentVersion, latestVersion, releaseUrl, downloadUrl, updateAvailable, null, null);
        }

        private static UpdateInfo disabled(String currentVersion) {
            return new UpdateInfo(false, currentVersion, currentVersion, null, null, false, null, null);
        }

        private static UpdateInfo error(String currentVersion, String errorMessage) {
            return new UpdateInfo(true, currentVersion, null, null, null, false, null, errorMessage);
        }

        private UpdateInfo withDownloadedFile(File file) {
            return new UpdateInfo(enabled, currentVersion, latestVersion, releaseUrl, downloadUrl, updateAvailable, file, errorMessage);
        }

        private UpdateInfo withError(String errorMessage) {
            return new UpdateInfo(enabled, currentVersion, latestVersion, releaseUrl, downloadUrl, updateAvailable, downloadedFile, errorMessage);
        }

        public boolean enabled() {
            return enabled;
        }

        public String currentVersion() {
            return currentVersion;
        }

        public String latestVersion() {
            return latestVersion;
        }

        public String releaseUrl() {
            return releaseUrl;
        }

        public String downloadUrl() {
            return downloadUrl;
        }

        public boolean updateAvailable() {
            return updateAvailable;
        }

        public File downloadedFile() {
            return downloadedFile;
        }

        public String errorMessage() {
            return errorMessage;
        }
    }
}
