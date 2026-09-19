package kr.newgodwar.game;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.*;

public class GameSessionStoreTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test public void commitsReplacementAndIgnoresUnfinishedWrite() throws Exception {
        GameSessionStore store = new GameSessionStore(folder.getRoot());
        assertNull(store.load());
        YamlConfiguration data = new YamlConfiguration();
        data.set("state", "RUNNING");
        data.set("world.snapshot", "game-unique-snapshot");
        data.set("elapsed-millis", 123456L);
        store.save(data);
        Files.write(new File(folder.getRoot(), "game-session-aborted.tmp").toPath(), "state: ENDED".getBytes(StandardCharsets.UTF_8));
        assertEquals("RUNNING", store.load().getString("state"));
        assertEquals(123456L, store.load().getLong("elapsed-millis"));
        data.set("state", "ENDED");
        store.save(data);
        assertEquals("ENDED", store.load().getString("state"));
    }

    @Test public void rejectsCorruptTruncatedAndUnsupportedSessionsWithoutChangingThem() throws Exception {
        GameSessionStore store = new GameSessionStore(folder.getRoot());
        File file = new File(folder.getRoot(), "game-session.yml");
        for (String text : new String[] {"state: [broken", "", "version: 1\nstate: RUNNING\n",
            "version: 2\nstate: RUNNING\ncomplete: true\n", "version: 1\nstate: INVALID\ncomplete: true\n"}) {
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            Files.write(file.toPath(), bytes);
            try { store.load(); fail("Corrupt session accepted"); } catch (IOException expected) { }
            assertArrayEquals(bytes, Files.readAllBytes(file.toPath()));
        }
    }
}
