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
        assertFalse(restored.isSkillConsumed(1));
        assertFalse(restored.isSkillConsumed(2));
        YamlConfiguration checkpoint = new YamlConfiguration();
        restored.saveSession(checkpoint);
        assertEquals("ReturningPlayer", checkpoint.getString("target"));
        assertTrue(checkpoint.getLong("cooldowns.1") > 110000L);
        assertEquals(0L, checkpoint.getLong("cooldowns.2"));
    }

    @Test public void consumedSkillsSurviveSaveLoadAndCooldownResets() throws Exception {
        YamlConfiguration saved = new YamlConfiguration();
        saved.set("consumed-skills", java.util.Arrays.asList(1));
        BaseAbility restored = new BaseAbility() { };
        restored.loadSession(saved);
        restored.clearCooldowns();
        assertTrue(restored.isSkillConsumed(1));
        assertFalse(restored.isSkillConsumed(2));
        YamlConfiguration checkpoint = new YamlConfiguration();
        restored.saveSession(checkpoint);
        YamlConfiguration decoded = new YamlConfiguration();
        decoded.loadFromString(checkpoint.saveToString());
        BaseAbility reloaded = new BaseAbility() { };
        reloaded.loadSession(decoded);
        assertTrue(reloaded.isSkillConsumed(1));
        assertFalse(reloaded.isSkillConsumed(2));
        assertFalse(new BaseAbility() { }.isSkillConsumed(1));
    }
}
