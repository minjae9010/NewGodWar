package kr.newgodwar.util;

import org.junit.Test;
import static org.junit.Assert.*;

public final class GameTipsTest {
    @Test public void upgradesOnlyExactBundledTipsAndIsIdempotent() {
        String legacy = "&f팀 설정: &b/gw settings&7의 팀 메뉴에서 팀 추가, 이름, 색상, 스폰, 심장을 관리할 수 있습니다.";
        String updated = GameTips.conciseDefaultTip(legacy);
        assertNotEquals(legacy, updated);
        assertFalse(updated.contains("/gw settings"));
        assertEquals(updated, GameTips.conciseDefaultTip(updated));
        String custom = legacy + " 우리 서버는 초록 팀부터 설정하세요.";
        assertEquals(custom, GameTips.conciseDefaultTip(custom));
        assertEquals("", GameTips.conciseDefaultTip(""));
        assertNull(GameTips.conciseDefaultTip(null));
    }
}
