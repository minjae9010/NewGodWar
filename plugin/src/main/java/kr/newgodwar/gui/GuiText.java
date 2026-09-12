package kr.newgodwar.gui;

import org.bukkit.ChatColor;
import java.util.ArrayList;
import java.util.List;

/** Bounds tooltip width, counting Korean/full-width characters as two cells. */
final class GuiText {
    private GuiText() { }

    static List<String> wrap(List<String> lore) {
        List<String> result = new ArrayList<String>();
        for (String text : lore) {
            for (String paragraph : text.split("\n", -1)) {
                StringBuilder line = new StringBuilder();
                int width = 0;
                for (int i = 0; i < paragraph.length();) {
                    char c = paragraph.charAt(i);
                    if (c == ChatColor.COLOR_CHAR && i + 1 < paragraph.length()) {
                        line.append(c).append(paragraph.charAt(i + 1));
                        i += 2;
                        continue;
                    }
                    int cp = paragraph.codePointAt(i);
                    int cells = cp >= 0x2E80 ? 2 : 1;
                    if (width + cells > 48) {
                        String previous = line.toString();
                        result.add(previous);
                        line = new StringBuilder(ChatColor.getLastColors(previous));
                        width = 0;
                    }
                    line.appendCodePoint(cp);
                    width += cells;
                    i += Character.charCount(cp);
                }
                result.add(line.toString());
            }
        }
        return result;
    }
}
