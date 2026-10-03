package kr.newgodwar.ability.builtin;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import static org.junit.Assert.*;

public final class SiksinAbilityTest {
    private final UUID owner = UUID.fromString("ee58b32c-6a1a-4b10-893e-0e3deaa47766");

    @Test public void sixMealsHaveDistinctNamesMaterialsAndPackModelIds() {
        Set<String> names = new HashSet<String>(), materials = new HashSet<String>();
        Set<Integer> models = new HashSet<Integer>();
        for (SiksinAbility.BuffKind meal : SiksinAbility.BuffKind.values()) {
            assertTrue(names.add(meal.foodName));
            assertTrue(materials.add(meal.material));
            assertTrue(models.add(meal.modelId));
            assertTrue(meal.modelId >= 73101 && meal.modelId <= 73106);
            assertTrue(meal.seconds == 12 || meal.seconds == 18);
        }
        assertEquals(6, names.size());
    }

    @Test public void foodProtocolPreservesOwnerRecipientModeBuffAndSession() {
        SiksinAbility ability = new SiksinAbility();
        for (boolean sharing : new boolean[] {false, true}) {
            for (SiksinAbility.BuffKind buff : SiksinAbility.BuffKind.values()) {
                String marker = ability.marker(owner, sharing, buff);
                SiksinAbility.FoodData data = SiksinAbility.parseMarker(marker);
                assertNotNull(data);
                assertEquals(owner, data.ownerId);
                assertEquals(sharing, data.teamFood);
                assertSame(buff, data.buff);
                assertNotNull(data.sessionId);
                assertNull(SiksinAbility.parseMarker(marker.replace(sharing ? ":TEAM:" : ":SOLO:", ":INVALID:")));
                assertNull(SiksinAbility.parseMarker(marker + ":extra"));
                assertNull(SiksinAbility.parseMarker(marker.substring(0, marker.lastIndexOf(':'))));
            }
        }
    }

    @Test public void checkpointKeepsFoodValidButReassignmentUsesAnotherSession() {
        SiksinAbility original = new SiksinAbility();
        String food = original.marker(owner, true, SiksinAbility.BuffKind.REGENERATION);
        YamlConfiguration saved = new YamlConfiguration();
        original.saveSession(saved);
        SiksinAbility restored = new SiksinAbility();
        restored.loadSession(saved);
        assertEquals(food, restored.marker(owner, true, SiksinAbility.BuffKind.REGENERATION));
        assertNotEquals(food, new SiksinAbility().marker(owner, true, SiksinAbility.BuffKind.REGENERATION));
        saved.set("food-session", "invalid");
        restored.loadSession(saved);
        assertNotEquals(food, restored.marker(owner, true, SiksinAbility.BuffKind.REGENERATION));
    }

    @Test public void malformedAndLegacyMarkersCannotBecomeBuffFood() {
        String session = UUID.randomUUID().toString();
        assertNull(SiksinAbility.parseMarker(null));
        assertNull(SiksinAbility.parseMarker("NGW_SIKSIN_FOOD:" + owner + ":TEAM:SPEED"));
        assertNull(SiksinAbility.parseMarker("NGW_SIKSIN_FOOD:" + owner + ":TEAM:UNKNOWN:" + session));
        assertNull(SiksinAbility.parseMarker("NGW_SIKSIN_FOOD:1-1-1-1-1:TEAM:SPEED:" + session));
        assertNull(SiksinAbility.parseMarker("NGW_SIKSIN_FOOD:" + owner + ":TEAM:SPEED:1-1-1-1-1"));
    }
}
