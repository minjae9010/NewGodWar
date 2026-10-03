package kr.newgodwar.gui;

import org.bukkit.ChatColor;
import org.junit.Test;
import static org.junit.Assert.*;

public final class GuiTitleTest {
    @Test
    public void packedAndFallbackTitlesRouteToTheSamePage() {
        for (SettingsView view : SettingsView.values()) {
            assertTrue(GuiTitle.matches(view.title, view.title, 54));
            assertTrue(GuiTitle.matches(view.title, GuiTitle.panel(view.title, 54), 54));
            assertFalse(GuiTitle.matches(view.title, GuiTitle.panel(view.title, 45), 54));
            assertFalse(GuiTitle.matches(view.title, GuiTitle.panel("다른 화면", 54), 54));
        }
    }

    @Test
    public void backgroundIsUntintedAndUsesTheCorrectChestHeight() {
        String plain = ChatColor.BLACK + "능력 정보";
        assertEquals(ChatColor.WHITE + "\uE100\uE101\uE10B\uE110\uE10B\uE10A\uE111\uE10B\uE112\uE10B\uE103" + ChatColor.WHITE + "능력 정보",
            GuiTitle.panel(plain, 45));
        assertTrue(GuiTitle.panel(plain, 54).contains("\uE102"));
        assertEquals(plain, GuiTitle.panel(plain, 27));
    }

    @Test
    public void drawTitleShowsBalanceCostAndInsufficientFundsState() {
        assertTrue(GuiTitle.gambling(64, 32).contains("\uE105"));
        assertTrue(GuiTitle.gambling(32, 32).contains("\uE105"));
        assertTrue(GuiTitle.gambling(31, 32).contains("\uE106"));
        assertTrue(GuiTitle.gambling(31, 32).endsWith("보유 31 · 1회 32"));
        assertTrue(GuiTitle.panel("보상과 확률", 54).contains("\uE107"));
        assertTrue(GuiTitle.panel("능력 목록", 54).contains("\uE108"));
        assertTrue(GuiTitle.panel("능력 상세", 45).contains("\uE109"));
    }
}
