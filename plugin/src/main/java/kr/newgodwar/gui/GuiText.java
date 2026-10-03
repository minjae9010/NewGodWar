package kr.newgodwar.gui;

import org.bukkit.ChatColor;
import java.util.ArrayList;
import java.util.List;

/** Bounds tooltip width, counting Korean/full-width characters as two cells. */
final class GuiText {
    private GuiText() { }

    /** Put controls first, then give each effect or condition its own line. */
    static List<String> skill(String text) {
        List<String> lines = new ArrayList<String>();
        int colon = text.indexOf(": ");
        if (colon >= 0) {
            lines.add(ChatColor.AQUA + text.substring(0, colon));
            text = text.substring(colon + 2);
        }
        for (String sentence : text.split("(?<=[.!?])\\s+")) {
            lines.add(ChatColor.WHITE + sentence);
        }
        return lines;
    }

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
                        int boundary = previous.lastIndexOf(' ') + 1;
                        // Prefer a word boundary, but hard-wrap unusually long words.
                        if (boundary > 0 && cells(previous.substring(0, boundary)) >= 24) {
                            String completed = previous.substring(0, boundary);
                            result.add(completed);
                            line = new StringBuilder(ChatColor.getLastColors(completed))
                                .append(previous.substring(boundary));
                            width = cells(line.toString());
                        } else {
                            result.add(previous);
                            line = new StringBuilder(ChatColor.getLastColors(previous));
                            width = 0;
                        }
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

    private static int cells(String text) {
        String plain = ChatColor.stripColor(text);
        return plain.codePoints().map(cp -> cp >= 0x2E80 ? 2 : 1).sum();
    }
}
