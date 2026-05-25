package kr.newgodwar.game;

import kr.newgodwar.NewGodWarPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class WorldBackupManager {

    private static final String BACKUP_DIRECTORY = "world-backups";
    private static final String SNAPSHOT_DIRECTORY = "world-snapshots";

    private final NewGodWarPlugin plugin;

    public WorldBackupManager(NewGodWarPlugin plugin) {
        this.plugin = plugin;
    }

    public File backupRoot() {
        return new File(plugin.getDataFolder(), BACKUP_DIRECTORY);
    }

    public File snapshotRoot() {
        return new File(plugin.getDataFolder(), SNAPSHOT_DIRECTORY);
    }

    public BackupResult createBackup(String requestedName) throws IOException {
        String name = sanitizeBackupName(requestedName);
        if (requestedName != null && requestedName.trim().length() > 0 && name == null) {
            throw new IOException("백업 이름은 영문, 숫자, 점, 밑줄, 하이픈만 사용할 수 있습니다.");
        }
        if (name == null) {
            name = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date());
        }
        File destination = new File(backupRoot(), name);
        if (destination.exists()) {
            throw new IOException("이미 같은 이름의 백업이 있습니다: " + name);
        }

        List<World> worlds = new ArrayList<World>(Bukkit.getWorlds());
        if (worlds.isEmpty()) {
            throw new IOException("백업할 월드가 없습니다.");
        }

        Bukkit.savePlayers();
        for (World world : worlds) {
            world.save();
        }

        File worldsRoot = new File(destination, "worlds");
        if (!worldsRoot.mkdirs() && !worldsRoot.isDirectory()) {
            throw new IOException("백업 폴더를 만들 수 없습니다: " + worldsRoot.getPath());
        }

        List<String> copiedWorlds = new ArrayList<String>();
        for (World world : worlds) {
            File source = world.getWorldFolder();
            if (source == null || !source.isDirectory()) {
                continue;
            }
            copyDirectory(source.toPath(), new File(worldsRoot, world.getName()).toPath(), true);
            copiedWorlds.add(world.getName());
        }

        YamlConfiguration metadata = new YamlConfiguration();
        metadata.set("name", name);
        metadata.set("created-at", System.currentTimeMillis());
        metadata.set("worlds", copiedWorlds);
        metadata.save(new File(destination, "metadata.yml"));

        return new BackupResult(name, copiedWorlds);
    }

    public List<BackupInfo> listBackups() {
        File root = backupRoot();
        File[] directories = root.listFiles(File::isDirectory);
        if (directories == null || directories.length == 0) {
            return Collections.emptyList();
        }

        List<BackupInfo> backups = new ArrayList<BackupInfo>();
        for (File directory : directories) {
            backups.add(readBackupInfo(directory));
        }
        Collections.sort(backups, new Comparator<BackupInfo>() {
            @Override
            public int compare(BackupInfo left, BackupInfo right) {
                return Long.compare(right.createdAtMillis, left.createdAtMillis);
            }
        });
        return backups;
    }

    public LoadResult loadBackup(String backupName, String requestedPrefix) throws IOException {
        String name = sanitizeBackupName(backupName);
        if (name == null) {
            throw new IOException("백업 이름은 영문, 숫자, 점, 밑줄, 하이픈만 사용할 수 있습니다.");
        }
        File backup = new File(backupRoot(), name);
        File worldsRoot = new File(backup, "worlds");
        File[] sourceWorlds = worldsRoot.listFiles(File::isDirectory);
        if (sourceWorlds == null || sourceWorlds.length == 0) {
            throw new IOException("백업을 찾을 수 없거나 백업 안에 월드가 없습니다: " + name);
        }

        String prefix = sanitizeWorldName(requestedPrefix);
        if (requestedPrefix != null && requestedPrefix.trim().length() > 0 && prefix == null) {
            throw new IOException("로드 월드 이름은 영문, 숫자, 점, 밑줄, 하이픈만 사용할 수 있습니다.");
        }
        if (prefix == null) {
            prefix = "ngw-" + name;
        }

        File container = Bukkit.getWorldContainer();
        List<String> loadedWorlds = new ArrayList<String>();
        for (File sourceWorld : sourceWorlds) {
            String targetName = sourceWorlds.length == 1 ? prefix : prefix + "-" + sourceWorld.getName();
            File target = targetWorldFolder(targetName);
            if (Bukkit.getWorld(targetName) != null || hasWorldFolder(targetName)) {
                throw new IOException("로드 대상 월드가 이미 있습니다: " + targetName);
            }
            copyDirectory(sourceWorld.toPath(), target.toPath(), false);
            prepareCopiedWorldFolder(target);

            World world = Bukkit.createWorld(new WorldCreator(targetName));
            if (world == null) {
                deleteDirectory(target);
                throw new IOException("월드를 로드하지 못했습니다: " + targetName);
            }
            loadedWorlds.add(world.getName());
        }
        return new LoadResult(name, loadedWorlds);
    }

    public World copyWorld(String sourceWorldName, String targetWorldName, String worldType) throws IOException {
        String sourceName = sanitizeWorldName(sourceWorldName);
        String targetName = sanitizeWorldName(targetWorldName);
        if (sourceName == null || targetName == null) {
            throw new IOException("월드 이름은 영문, 숫자, 점, 밑줄, 하이픈만 사용할 수 있습니다.");
        }
        if (Bukkit.getWorld(targetName) != null) {
            throw new IOException("복사 대상 월드가 이미 로드되어 있습니다: " + targetName);
        }
        File source = worldFolder(sourceName);
        if (source == null || !source.isDirectory()) {
            throw new IOException("복사할 월드 폴더를 찾을 수 없습니다: " + sourceName);
        }
        File target = targetWorldFolder(targetName);
        if (hasWorldFolder(targetName)) {
            throw new IOException("복사 대상 월드 폴더가 이미 있습니다: " + targetName);
        }

        World loadedSource = Bukkit.getWorld(sourceName);
        if (loadedSource != null) {
            loadedSource.save();
        }
        copyDirectory(source.toPath(), target.toPath(), true);
        prepareCopiedWorldFolder(target);

        World copied = Bukkit.createWorld(creator(targetName, worldType));
        if (copied == null) {
            deleteDirectory(target);
            throw new IOException("복사한 월드를 로드하지 못했습니다: " + targetName);
        }
        copied.save();
        return copied;
    }

    public void saveWorldSnapshot(World world, String snapshotName) throws IOException {
        if (world == null) {
            throw new IOException("스냅샷을 만들 월드를 찾을 수 없습니다.");
        }
        String name = sanitizeBackupName(snapshotName);
        if (name == null) {
            throw new IOException("스냅샷 이름은 영문, 숫자, 점, 밑줄, 하이픈만 사용할 수 있습니다.");
        }
        world.save();
        File destination = new File(snapshotRoot(), name);
        if (destination.exists()) {
            deleteDirectory(destination);
        }
        copyDirectory(world.getWorldFolder().toPath(), destination.toPath(), true);
    }

    public void restoreWorldSnapshot(String worldName, String snapshotName) throws IOException {
        String safeWorldName = sanitizeWorldName(worldName);
        String safeSnapshotName = sanitizeBackupName(snapshotName);
        if (safeWorldName == null || safeSnapshotName == null) {
            throw new IOException("월드 이름 또는 스냅샷 이름이 올바르지 않습니다.");
        }
        File source = new File(snapshotRoot(), safeSnapshotName);
        if (!source.isDirectory()) {
            throw new IOException("월드 스냅샷을 찾을 수 없습니다: " + safeSnapshotName);
        }
        for (File existing : existingWorldFolders(safeWorldName)) {
            deleteDirectory(existing);
        }
        File target = targetWorldFolder(safeWorldName);
        copyDirectory(source.toPath(), target.toPath(), false);
        prepareCopiedWorldFolder(target);
    }

    public void sendBackupList(CommandSender sender) {
        List<BackupInfo> backups = listBackups();
        if (backups.isEmpty()) {
            plugin.messages().send(sender, "&e저장된 월드 백업이 없습니다.");
            return;
        }
        sender.sendMessage(plugin.messages().prefix() + ChatColor.YELLOW + "월드 백업 목록");
        for (BackupInfo backup : backups) {
            sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.WHITE + backup.name
                + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + backup.worlds.size() + "개 월드"
                + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + formatDate(backup.createdAtMillis));
        }
    }

    private BackupInfo readBackupInfo(File directory) {
        File metadataFile = new File(directory, "metadata.yml");
        YamlConfiguration metadata = YamlConfiguration.loadConfiguration(metadataFile);
        String name = metadata.getString("name", directory.getName());
        long createdAt = metadata.getLong("created-at", directory.lastModified());
        List<String> worlds = metadata.getStringList("worlds");
        if (worlds.isEmpty()) {
            File[] sourceWorlds = new File(directory, "worlds").listFiles(File::isDirectory);
            if (sourceWorlds != null) {
                for (File sourceWorld : sourceWorlds) {
                    worlds.add(sourceWorld.getName());
                }
            }
        }
        return new BackupInfo(name, createdAt, worlds);
    }

    public boolean hasWorldFolder(String worldName) {
        return !existingWorldFolders(worldName).isEmpty();
    }

    public List<File> existingWorldFolders(String worldName) {
        List<File> folders = new ArrayList<File>();
        addExistingWorldFolder(folders, loadedWorldFolder(worldName));
        addExistingWorldFolder(folders, dimensionWorldFolder(worldName));
        addExistingWorldFolder(folders, new File(Bukkit.getWorldContainer(), worldName));
        return folders;
    }

    private File worldFolder(String worldName) {
        World loaded = Bukkit.getWorld(worldName);
        if (loaded != null) {
            return loaded.getWorldFolder();
        }
        File dimensionFolder = dimensionWorldFolder(worldName);
        if (dimensionFolder != null && dimensionFolder.isDirectory()) {
            return dimensionFolder;
        }
        return new File(Bukkit.getWorldContainer(), worldName);
    }

    private File targetWorldFolder(String worldName) {
        File loaded = loadedWorldFolder(worldName);
        if (loaded != null) {
            return loaded;
        }
        File dimensionFolder = dimensionWorldFolder(worldName);
        if (dimensionFolder != null && usesDimensionWorldFolders()) {
            return dimensionFolder;
        }
        return new File(Bukkit.getWorldContainer(), worldName);
    }

    private File loadedWorldFolder(String worldName) {
        World loaded = Bukkit.getWorld(worldName);
        return loaded == null ? null : loaded.getWorldFolder();
    }

    private File dimensionWorldFolder(String worldName) {
        File root = dimensionNamespaceRoot();
        return root == null ? null : new File(root, worldName);
    }

    private boolean usesDimensionWorldFolders() {
        return dimensionNamespaceRoot() != null;
    }

    private File dimensionNamespaceRoot() {
        for (World world : Bukkit.getWorlds()) {
            File folder = world.getWorldFolder();
            File parent = folder == null ? null : folder.getParentFile();
            File grandParent = parent == null ? null : parent.getParentFile();
            if (parent != null && grandParent != null
                && parent.getName().equals("minecraft")
                && grandParent.getName().equals("dimensions")) {
                return parent;
            }
        }
        if (!Bukkit.getWorlds().isEmpty()) {
            File legacyDefaultFolder = new File(Bukkit.getWorldContainer(), Bukkit.getWorlds().get(0).getName());
            File root = new File(legacyDefaultFolder, "dimensions" + File.separator + "minecraft");
            if (root.isDirectory()) {
                return root;
            }
        }
        return null;
    }

    private void addExistingWorldFolder(List<File> folders, File folder) {
        if (folder == null || !folder.isDirectory()) {
            return;
        }
        for (File existing : folders) {
            if (sameFile(existing, folder)) {
                return;
            }
        }
        folders.add(folder);
    }

    private boolean sameFile(File left, File right) {
        try {
            return left != null && right != null && left.getCanonicalFile().equals(right.getCanonicalFile());
        } catch (IOException ex) {
            return false;
        }
    }

    public static WorldCreator creator(String worldName, String worldType) {
        WorldCreator creator = new WorldCreator(worldName);
        String type = worldType == null ? "normal" : worldType.toLowerCase(Locale.ROOT);
        if ("void".equals(type)) {
            creator.generator(new VoidWorldGenerator());
            creator.generateStructures(false);
        } else if ("flat".equals(type)) {
            creator.type(WorldType.FLAT);
        }
        return creator;
    }

    private String sanitizeBackupName(String name) {
        if (name == null || name.trim().length() == 0) {
            return null;
        }
        String trimmed = name.trim();
        if (!trimmed.matches("[A-Za-z0-9._-]{1,64}")) {
            return null;
        }
        if (".".equals(trimmed) || "..".equals(trimmed)) {
            return null;
        }
        return trimmed;
    }

    private String sanitizeWorldName(String name) {
        if (name == null || name.trim().length() == 0) {
            return null;
        }
        String trimmed = name.trim();
        if (!trimmed.matches("[A-Za-z0-9._-]{1,64}")) {
            return null;
        }
        if (".".equals(trimmed) || "..".equals(trimmed)) {
            return null;
        }
        return trimmed;
    }

    private String formatDate(long millis) {
        if (millis <= 0L) {
            return "날짜 없음";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(new Date(millis));
    }

    private void copyDirectory(final Path source, final Path target, final boolean backupMode) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attrs) throws IOException {
                Path relative = source.relativize(directory);
                Files.createDirectories(target.resolve(relative));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (backupMode && shouldSkipVolatileWorldFile(file.getFileName().toString())) {
                    return FileVisitResult.CONTINUE;
                }
                Path relative = source.relativize(file);
                Files.copy(file, target.resolve(relative), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private boolean shouldSkipVolatileWorldFile(String fileName) {
        return "session.lock".equalsIgnoreCase(fileName);
    }

    private void prepareCopiedWorldFolder(File folder) throws IOException {
        deleteIfExists(new File(folder, "uid.dat"));
        deleteIfExists(new File(folder, "session.lock"));
        deleteIfExists(new File(folder, "data" + File.separator + "paper" + File.separator + "metadata.dat"));
    }

    private void deleteIfExists(File file) throws IOException {
        if (file.exists() && !file.delete()) {
            throw new IOException("월드 로드 준비 중 파일을 삭제하지 못했습니다: " + file.getPath());
        }
    }

    private void deleteDirectory(File directory) throws IOException {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else if (!file.delete()) {
                    throw new IOException("파일을 삭제하지 못했습니다: " + file.getPath());
                }
            }
        }
        if (!directory.delete()) {
            throw new IOException("폴더를 삭제하지 못했습니다: " + directory.getPath());
        }
    }

    public static final class BackupResult {
        private final String name;
        private final List<String> worlds;

        private BackupResult(String name, List<String> worlds) {
            this.name = name;
            this.worlds = worlds;
        }

        public String name() {
            return name;
        }

        public List<String> worlds() {
            return Collections.unmodifiableList(worlds);
        }
    }

    public static final class LoadResult {
        private final String backupName;
        private final List<String> worlds;

        private LoadResult(String backupName, List<String> worlds) {
            this.backupName = backupName;
            this.worlds = worlds;
        }

        public String backupName() {
            return backupName;
        }

        public List<String> worlds() {
            return Collections.unmodifiableList(worlds);
        }
    }

    public static final class BackupInfo {
        private final String name;
        private final long createdAtMillis;
        private final List<String> worlds;

        private BackupInfo(String name, long createdAtMillis, List<String> worlds) {
            this.name = name;
            this.createdAtMillis = createdAtMillis;
            this.worlds = worlds;
        }

        public String name() {
            return name;
        }

        public List<String> worlds() {
            return Collections.unmodifiableList(worlds);
        }
    }
}
