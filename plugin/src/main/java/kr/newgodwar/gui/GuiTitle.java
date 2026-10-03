package kr.newgodwar.gui;

import org.bukkit.ChatColor;

/** Four seamless 88px quadrants retain 2x detail within the font atlas glyph limit. */
final class GuiTitle {
    private GuiTitle() { }

    static String panel(String title, int size) {
        if (size != 45 && size != 54) return title;
        String glyph = size == 45 ? (ChatColor.stripColor(title).equals("능력 상세") ? "\uE109" : "\uE101")
            : SettingsView.MAIN.title.equals(title) ? "\uE104"
            : ChatColor.stripColor(title).equals("능력 목록") ? "\uE108"
            : ChatColor.stripColor(title).equals("보상과 확률") ? "\uE107" : "\uE102";
        return painted(title, glyph);
    }

    static String gambling(int balance, int cost) {
        return painted("보유 " + balance + " · 1회 " + cost, balance >= cost ? "\uE105" : "\uE106");
    }

    private static String painted(String title, String glyph) {
        int index = "\uE101\uE102\uE104\uE105\uE106\uE107\uE108\uE109".indexOf(glyph);
        char right = (char) (0xE110 + index * 3);
        return ChatColor.WHITE + "\uE100" + glyph + "\uE10B" + right + "\uE10B\uE10A"
            + (char) (right + 1) + "\uE10B" + (char) (right + 2) + "\uE10B\uE103"
            + ChatColor.WHITE + ChatColor.stripColor(title);
    }

    static boolean matches(String title, String actual, int size) {
        return title.equals(actual) || panel(title, size).equals(actual);
    }
}
