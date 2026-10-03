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

    @Test
    public void wrapsAtWordsAndKeepsColorsAfterTheBreak() {
        String first = ChatColor.GRAY + "조약돌 10개를 모아서 주변의 아군에게 ";
        List<String> lines = GuiText.wrap(Arrays.asList(first + ChatColor.AQUA + "재생효과를줘요."));
        assertEquals(first, lines.get(0));
        assertTrue(lines.get(1).startsWith(ChatColor.GRAY.toString() + ChatColor.AQUA));
        assertEquals("재생효과를줘요.", ChatColor.stripColor(lines.get(1)));
    }

    @Test
    public void skillLayoutPreservesControlsNumbersAndConditions() {
        List<String> lines = GuiText.skill("블레이즈 막대기 좌클릭: 0.75초 뒤 피해 2를 줘요. 1.5블록 밖이면 빗나가요.");
        assertEquals(3, lines.size());
        assertEquals("블레이즈 막대기 좌클릭", ChatColor.stripColor(lines.get(0)));
        assertEquals("0.75초 뒤 피해 2를 줘요.", ChatColor.stripColor(lines.get(1)));
        assertEquals("1.5블록 밖이면 빗나가요.", ChatColor.stripColor(lines.get(2)));
        assertEquals(Arrays.asList(ChatColor.WHITE + "채팅으로 정답을 맞히세요."), GuiText.skill("채팅으로 정답을 맞히세요."));
    }
}
