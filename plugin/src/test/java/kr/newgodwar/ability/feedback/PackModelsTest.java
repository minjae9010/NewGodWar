package kr.newgodwar.ability.feedback;

import org.junit.Test;
import static org.junit.Assert.*;

public class PackModelsTest {
    @Test public void downloadAddressIsIndependentOfPluginReleases() {
        String[] pack = PackModels.pack("26.3");
        assertEquals("https://raw.githubusercontent.com/minjae9010/NewGodWar/master/resoucepack/"
            + pack[0], PackModels.downloadUrl(pack));
        assertTrue("The pack is named by its own hash", pack[0].equals("NewGodWar-Art-" + pack[1].substring(0, 8) + ".zip"));
        assertFalse(PackModels.downloadUrl(pack).contains("/releases/"));
    }
    @Test public void selectsExactReleasedVersionAndServerSuffix() {
        // One combined pack serves every release; only the runtime model system differs.
        assertEquals(PackModels.pack("26.3")[0], PackModels.pack("1.21.1-R0.1-SNAPSHOT")[0]);
        assertEquals(PackModels.pack("26.3")[0], PackModels.pack("1.14")[0]);
        assertEquals("legacy", PackModels.pack("1.21.3")[2]);
        assertEquals("modern", PackModels.pack("1.21.4")[2]);
        assertEquals("modern", PackModels.pack("26.3-R0.1-SNAPSHOT")[2]);
        assertTrue(PackModels.pack("26.1.2")[1].matches("[0-9a-f]{40}"));
    }
    @Test public void unknownAndPrereleaseClientsAreNotGivenAnIncompatiblePack() {
        for (String version : new String[] {null, "", "1.11.2", "1.21.12", "26.4", "26.3-pre1", "26.3-rc1"})
            assertNull(PackModels.pack(version));
    }
    @Test public void modelCatalogueIncludesMenuAndArt() {
        assertTrue(PackModels.modelId("gui/coin") >= 74000);
        assertNotEquals(PackModels.modelId("gui/coin"), PackModels.modelId("gui/home"));
        assertEquals(0, PackModels.modelId("otherplugin/missing"));
    }
}
