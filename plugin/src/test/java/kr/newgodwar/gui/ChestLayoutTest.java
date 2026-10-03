package kr.newgodwar.gui;

import org.junit.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

public final class ChestLayoutTest {
    @Test
    public void settingsControlsNeverOverlapNavigationOrEachOther() {
        for (SettingsView view : SettingsView.values()) {
            Set<Integer> occupied = new HashSet<Integer>();
            int[] slots = ChestLayout.settings(view);
            for (int logical = 0; logical < slots.length; logical++) {
                int slot = slots[logical];
                if (slot < 0) continue;
                assertTrue(view + " slot " + slot, slot >= 9 && slot < 54);
                assertNotEquals(49, slot);
                assertNotEquals(53, slot);
                assertNotEquals(45, slot);
                assertNotEquals(47, slot);
                assertTrue(view + " overlap " + slot, occupied.add(slot));
                assertEquals(logical, ChestLayout.logicalSlot(view, slot));
            }
            assertEquals(-1, ChestLayout.logicalSlot(view, 54));
            assertEquals(-1, ChestLayout.logicalSlot(view, -999));
        }
    }

    @Test
    public void killtimeModeRemainsAccessibleBesideBackButton() {
        int slot = ChestLayout.settings(SettingsView.COMBAT)[22];
        assertEquals(22, ChestLayout.logicalSlot(SettingsView.COMBAT, slot));
        assertNotEquals(49, slot);
    }

    @Test
    public void catalogMarginsAndFooterCannotSelectAbilities() {
        assertEquals(28, ChestLayout.CATALOG.length);
        for (int slot = 0; slot < 54; slot++) {
            int index = ChestLayout.catalogIndex(slot);
            if (slot < 9 || slot >= 45 || slot % 9 == 0 || slot % 9 == 8) {
                assertEquals(-1, index);
            } else {
                assertTrue(index >= 0);
                assertEquals(slot, ChestLayout.CATALOG[index]);
            }
        }
    }

    @Test
    public void mainContainsAllEightCategoriesAndTheItemShortcut() {
        for (int logical = 10; logical <= 17; logical++) {
            int slot = ChestLayout.settings(SettingsView.MAIN)[logical];
            assertTrue(slot >= 0);
            assertEquals(logical, ChestLayout.logicalSlot(SettingsView.MAIN, slot));
        }
        assertTrue(ChestLayout.settings(SettingsView.MAIN)[23] >= 0);
    }
}
