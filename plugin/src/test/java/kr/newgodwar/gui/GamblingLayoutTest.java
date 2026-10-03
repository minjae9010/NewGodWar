package kr.newgodwar.gui;

import org.junit.Test;
import static org.junit.Assert.*;

public final class GamblingLayoutTest {
    @Test
    public void onlyTheFullPrimaryButtonCanSpendCurrency() {
        int count = 0;
        for (int slot = -1; slot <= 72; slot++) {
            boolean primary = (slot >= 12 && slot <= 14) || (slot >= 21 && slot <= 23);
            assertEquals(primary, GamblingLayout.draw(slot));
            if (GamblingLayout.draw(slot)) count++;
        }
        assertEquals(6, count);
        assertFalse(GamblingLayout.draw(GamblingLayout.REWARDS));
        assertFalse(GamblingLayout.draw(GamblingLayout.CLOSE));
        assertFalse(GamblingLayout.draw(GamblingLayout.GALLERY_BACK));
    }

    @Test
    public void galleryIncludesPartialAndEmptyPagesWithoutDroppingRewards() {
        assertEquals(1, GamblingLayout.pages(0, 21));
        assertEquals(1, GamblingLayout.pages(21, 21));
        assertEquals(2, GamblingLayout.pages(22, 21));
        assertEquals(2, GamblingLayout.pages(42, 21));
        assertEquals(3, GamblingLayout.pages(43, 21));
    }
}
