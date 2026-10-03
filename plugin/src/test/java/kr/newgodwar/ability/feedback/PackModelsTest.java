package kr.newgodwar.ability.feedback;

import org.junit.Test;
import static org.junit.Assert.*;

public class PackModelsTest {
    @Test public void selectsExactReleasedVersionAndServerSuffix() {
        assertEquals("NewGodWar-Art-1.21-1.21.1.zip", PackModels.pack("1.21.1-R0.1-SNAPSHOT")[0]);
        assertEquals("legacy", PackModels.pack("1.21.3")[2]);
        assertEquals("modern", PackModels.pack("1.21.4")[2]);
        assertEquals("NewGodWar-Art-26.3.zip", PackModels.pack("26.3-R0.1-SNAPSHOT")[0]);
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
