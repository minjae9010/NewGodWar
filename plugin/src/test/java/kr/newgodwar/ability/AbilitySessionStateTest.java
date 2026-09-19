package kr.newgodwar.ability;

import kr.newgodwar.ability.builtin.BaseAbility;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

import static org.junit.Assert.*;

public class AbilitySessionStateTest {
    @Test public void restoresRemainingCooldownAndTargetWithoutUsingWallClockDeadline() {
        YamlConfiguration saved = new YamlConfiguration();
        saved.set("target", "ReturningPlayer");
        saved.set("cooldowns.1", 120000L);
        saved.set("cooldowns.2", -1L);
        BaseAbility restored = new BaseAbility() { };
        restored.loadSession(saved);
        assertTrue(restored.cooldownRemainingMillis(1) > 110000L);
        assertTrue(restored.cooldownRemainingMillis(1) <= 120000L);
        assertEquals(0L, restored.cooldownRemainingMillis(2));
        YamlConfiguration checkpoint = new YamlConfiguration();
        restored.saveSession(checkpoint);
        assertEquals("ReturningPlayer", checkpoint.getString("target"));
        assertTrue(checkpoint.getLong("cooldowns.1") > 110000L);
        assertEquals(0L, checkpoint.getLong("cooldowns.2"));
    }
}
