package kr.newgodwar.ability;

import kr.newgodwar.ability.api.AbilityDefinition;
import kr.newgodwar.ability.api.AbilityInfo;
import kr.newgodwar.ability.api.GodAbility;
import org.junit.Test;

import static org.junit.Assert.*;

public final class AbilityRegistryTest {
    @AbilityInfo(id = "addon_test", name = "Addon Test", description = "test")
    public static final class Sample implements GodAbility { }

    @AbilityInfo(id = "addon_test", name = "Other", description = "test")
    public static final class Duplicate implements GodAbility { }

    @Test
    public void unregisterClearsEveryIndexAndAllowsReinstallation() {
        AbilityRegistry registry = new AbilityRegistry();
        registry.register(Sample.class);
        AbilityDefinition definition = registry.get("ADDON_TEST");
        assertSame(definition, registry.getByName(" addon test "));
        assertNotSame(definition.create(), definition.create());
        assertSame(definition, registry.unregister(" ADDON_TEST "));
        assertNull(registry.getByName("Addon Test"));
        assertFalse(registry.isRegistered(Sample.class));
        assertTrue(registry.ids().isEmpty());
        assertNull(registry.unregister("addon_test"));
        registry.register(Sample.class);
        assertTrue(registry.isRegistered(Sample.class));
    }

    @Test
    public void duplicateRegistrationDoesNotDamageOriginalIndexes() {
        AbilityRegistry registry = new AbilityRegistry();
        registry.register(Sample.class);
        try {
            registry.register(Duplicate.class);
            fail("Duplicate must fail");
        } catch (IllegalArgumentException expected) {
            assertEquals(1, registry.all().size());
            assertTrue(registry.isRegistered(Sample.class));
            assertFalse(registry.isRegistered(Duplicate.class));
            assertNull(registry.getByName("Other"));
        }
    }

    @Test
    public void removingOneAddonPreservesOtherDefinitions() {
        AbilityRegistry registry = new AbilityRegistry();
        registry.register(Sample.class);
        AbilityDefinition other = new AbilityDefinition("other", "Other", "test", "test", true, Sample::new);
        registry.register(other);
        registry.unregister("addon_test");
        assertSame(other, registry.get("other"));
        assertSame(other, registry.getByName("Other"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyDefinitionIdIsRejected() {
        new AbilityRegistry().register(new AbilityDefinition(" ", "Other", "test", "test", true, Sample::new));
    }

    @Test(expected = NullPointerException.class)
    public void nullFactoryIsRejectedBeforeRegistration() {
        new AbilityDefinition("other", "Other", "test", "test", true, null);
    }
}
