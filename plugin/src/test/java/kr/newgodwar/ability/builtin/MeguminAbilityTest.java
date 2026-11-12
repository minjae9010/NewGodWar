package kr.newgodwar.ability.builtin;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public final class MeguminAbilityTest {
    @Test public void legacySpentExplosionBecomesRechargeableWithoutMutatingItsCheckpoint() {
        YamlConfiguration saved = new YamlConfiguration();
        saved.set("consumed-skills", Arrays.asList(1));
        saved.set("target", "ExistingTarget");
        MeguminAbility restored = new MeguminAbility(); restored.loadSession(saved);
        assertFalse(restored.isSkillConsumed(1));
        assertTrue(restored.cooldownRemainingMillis(1) > 88000L);
        assertEquals(Arrays.asList(1), saved.getIntegerList("consumed-skills"));
        YamlConfiguration migrated = new YamlConfiguration(); restored.saveSession(migrated);
        assertTrue(migrated.getIntegerList("consumed-skills").isEmpty());
        assertEquals("ExistingTarget", migrated.getString("target"));
    }

    @Test public void newCheckpointPreservesBothCooldownsWithoutRestoringACast() {
        YamlConfiguration saved = new YamlConfiguration();
        saved.set("cooldowns.1", 42000L); saved.set("cooldowns.2", 17000L);
        MeguminAbility restored = new MeguminAbility(); restored.loadSession(saved);
        assertTrue(restored.cooldownRemainingMillis(1) > 40000L);
        assertTrue(restored.cooldownRemainingMillis(1) <= 42000L);
        assertTrue(restored.cooldownRemainingMillis(2) > 15000L);
        assertTrue(restored.activeTimerLines().stream().noneMatch(line -> line.contains("폭렬 발동")));
        assertFalse(restored.isSkillConsumed(1));
    }
}
