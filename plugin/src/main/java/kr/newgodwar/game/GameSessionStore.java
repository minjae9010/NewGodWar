package kr.newgodwar.game;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/** A committed session is never replaced by a partially written YAML file. */
public final class GameSessionStore {
    private final File file;

    public GameSessionStore(File dataFolder) {
        file = new File(dataFolder, "game-session.yml");
    }

    public YamlConfiguration load() throws IOException {
        if (!file.exists()) return null;
        YamlConfiguration data = new YamlConfiguration();
        try {
            data.load(file);
            if (data.getInt("version") != 1 || !data.isString("state")) {
                throw new IllegalArgumentException("Missing or unsupported session header");
            }
            GameState.valueOf(data.getString("state"));
            if (!data.getBoolean("complete")) throw new IllegalArgumentException("Incomplete session");
        } catch (InvalidConfigurationException | IllegalArgumentException ex) {
            throw new IOException("Cannot read " + file.getName() + ": " + ex.getMessage(), ex);
        }
        return data;
    }

    public void save(YamlConfiguration data) throws IOException {
        data.set("version", 1);
        data.set("complete", true);
        Path target = file.toPath();
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), "game-session-", ".tmp");
        try {
            byte[] bytes = data.saveToString().getBytes(StandardCharsets.UTF_8);
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            // Fail safely on filesystems without atomic replacement; keep the last checkpoint.
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
