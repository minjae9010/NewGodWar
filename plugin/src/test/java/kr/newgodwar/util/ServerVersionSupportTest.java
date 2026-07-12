package kr.newgodwar.util;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ServerVersionSupportTest {

    @Test
    public void detectsPaperCompatibleServerNames() {
        assertTrue(ServerVersionSupport.detectPaperServer("Paper"));
        assertTrue(ServerVersionSupport.detectPaperServer("Purpur"));
        assertTrue(ServerVersionSupport.detectPaperServer("Pufferfish"));
        assertTrue(ServerVersionSupport.detectPaperServer("Folia"));
    }

    @Test
    public void rejectsSpigotAndCraftBukkitServerNames() {
        assertFalse(ServerVersionSupport.detectPaperServer("Spigot"));
        assertFalse(ServerVersionSupport.detectPaperServer("CraftBukkit"));
    }

    @Test
    public void includesMinecraft26_2AsPaperDownloadTarget() {
        assertTrue(ServerVersionSupport.paperDownloadVersions().contains("26.2"));
    }
}
