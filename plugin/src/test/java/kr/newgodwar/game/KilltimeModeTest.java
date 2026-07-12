package kr.newgodwar.game;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class KilltimeModeTest {

    @Test
    public void defaultsToPlayerCombat() {
        assertEquals(KilltimeMode.PLAYER_COMBAT, KilltimeMode.parse(null));
        assertEquals(KilltimeMode.PLAYER_COMBAT, KilltimeMode.parse("unknown"));
    }

    @Test
    public void parsesCoreOnlyAliases() {
        assertEquals(KilltimeMode.CORE_ONLY, KilltimeMode.parse("core-only"));
        assertEquals(KilltimeMode.CORE_ONLY, KilltimeMode.parse("CORE_ONLY"));
        assertEquals(KilltimeMode.CORE_ONLY, KilltimeMode.parse("core"));
    }

    @Test
    public void exposesStableConfigValues() {
        assertEquals("player-combat", KilltimeMode.PLAYER_COMBAT.configValue());
        assertEquals("core-only", KilltimeMode.CORE_ONLY.configValue());
    }
}
