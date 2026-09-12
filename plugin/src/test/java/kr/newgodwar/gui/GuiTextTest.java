package kr.newgodwar.gui;

import org.bukkit.ChatColor;
import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public final class GuiTextTest {
    @Test
    public void koreanTooltipWrapsWithoutLosingTextOrColors() {
        String text = "블레이즈 막대기를 들고 좌클릭하면 바라보는 위치에 번개가 떨어집니다. 필요한 조약돌을 준비하세요.";
        List<String> lines = GuiText.wrap(Arrays.asList(ChatColor.AQUA + text));
        assertTrue(lines.size() > 1);
        StringBuilder restored = new StringBuilder();
        for (String line : lines) {
            assertTrue(line.startsWith(ChatColor.AQUA.toString()));
            String plain = ChatColor.stripColor(line);
            assertTrue(plain.codePoints().map(cp -> cp >= 0x2E80 ? 2 : 1).sum() <= 48);
            restored.append(plain);
        }
        assertEquals(text, restored.toString());
    }

    @Test
    public void keepsExplicitParagraphsAndSupplementaryCharacters() {
        String text = "도감\n\n" + new String(Character.toChars(0x1F30D)) + " 월드";
        assertEquals(Arrays.asList("도감", "", "🌍 월드"), GuiText.wrap(Arrays.asList(text)));
    }
}
