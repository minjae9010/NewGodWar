package kr.newgodwar.ability;

import kr.newgodwar.ability.builtin.DefaultAbilityRegistrar;
import kr.newgodwar.ability.feedback.AbilityStyle;
import kr.newgodwar.ability.feedback.EffectCue;
import org.junit.Test;
import kr.newgodwar.ability.api.GodAbility;
import kr.newgodwar.ability.feedback.AbilityTheme;
import kr.newgodwar.ability.feedback.SharedModels;
import static org.junit.Assert.*;

public final class AbilityStyleTest {
    @Test public void everyRegisteredBuiltinHasAnExplicitStyle() throws Exception {
        AbilityRegistry registry = new AbilityRegistry();
        new DefaultAbilityRegistrar().registerAbilities(registry);
        for (String id : registry.ids()) {
            GodAbility ability = registry.get(id).create();
            assertEquals(id, ability.getClass(), ability.getClass().getMethod("style").getDeclaringClass());
            assertNotSame(id, AbilityStyle.DEFAULT, ability.style());
        }
        assertEquals(EffectCue.NONE, new GodAbility() { }.style().cast(false));
        assertNotNull(new GodAbility() { }.style().theme());
    }

    @Test public void cuesStayCloseToTheBodyAndDoNotStackNativeSprites() {
        for (EffectCue cue : EffectCue.values()) {
            final int[] count = {0};
            cue.draw((ink, x, y, z, rgb) -> {
                assertTrue(cue + " invalid X", Double.isFinite(x) && Math.abs(x) <= 1.5);
                assertTrue(cue + " invalid Y", Double.isFinite(y) && y >= 0 && y <= 3.5);
                assertTrue(cue + " invalid Z", Double.isFinite(z) && Math.abs(z) <= 1.5);
                count[0]++;
            });
            assertTrue(count[0] <= 30);
            if (cue == EffectCue.NONE) assertEquals(0, count[0]);
            else assertTrue(count[0] > 0);
            if (cue == EffectCue.SLASH) assertEquals(1, count[0]);
            if (cue == EffectCue.HEAL) assertEquals(3, count[0]);
        }
    }

    @Test public void actionMeaningOverridesGenericDecoration() {
        for (String id : new String[] {"zeus", "thor", "echo", "hermione", "frost", "graviton", "pan"}) {
            AbilityStyle style = style(id);
            assertTrue(id, style.dedicated());
            assertEquals(id, EffectCue.NONE, style.cast(false));
            assertEquals(id, EffectCue.NONE, style.cast(true));
            assertEquals(id, EffectCue.NONE, style.hit());
            assertEquals(id, EffectCue.NONE, style.status("SLOWNESS"));
        }
        assertEquals(EffectCue.ROOT, style("gaia").status("SLOWNESS"));
        assertEquals(EffectCue.SLEEP, style("morpious").status("BLINDNESS"));
        assertEquals(EffectCue.BLIND, style("blinder").status("BLINDNESS"));
        assertEquals(EffectCue.STEALTH, style("clocking").status("INVISIBILITY"));
        assertEquals(EffectCue.SEAL, style("sejong").status("BLINDNESS"));
        assertEquals(EffectCue.POISON, style("acidarcher").status("POISON"));
        assertEquals(EffectCue.HEAL, style("asclepius").benefit());
        assertEquals(EffectCue.FORGE, style("blacksmith").cast(false));
        assertEquals(EffectCue.NONE, style("nasdaq").cast(false));
        assertEquals(EffectCue.NONE, style("snow").cast(true));
    }
    @Test public void abilityPolicyOwnsPrivacyTrailsAndFlight() {
        for (String id : new String[] {"clocking", "hecate", "loki", "sus", "honggildong", "selene", "assasin", "tajja", "bomber"})
            assertTrue(id, style(id).privateCast());
        assertFalse(style("thor").privateCast());
        assertEquals(AbilityTheme.SHADOW, style("acidarcher").trailTheme());
        assertEquals(AbilityTheme.LIGHTNING, style("artemis").trailTheme());
        assertSame(SharedModels.WINGS, style("hermes").flightModel());
        assertSame(SharedModels.FIRE_WINGS, style("jujak").flightModel());
        assertNull(style("nike").flightModel());
    }

    @Test public void addonCanDeclareAnIndependentStyleWithoutRegisteringAnId() {
        final AbilityStyle custom = AbilityStyle.builder(AbilityTheme.WATER)
            .normal(EffectCue.WATER).advanced(EffectCue.WINGS).hit(EffectCue.SLOW)
            .privateCast().trail(AbilityTheme.FROST).flight(SharedModels.WINGS).build();
        GodAbility addon = new GodAbility() {
            @Override public AbilityStyle style() { return custom; }
        };
        assertSame(custom, addon.style());
        assertEquals(EffectCue.WATER, addon.style().cast(false));
        assertEquals(EffectCue.WINGS, addon.style().cast(true));
        assertTrue(addon.style().privateCast());
        assertEquals(EffectCue.HIT, new GodAbility() { }.style().hit());
    }

    private AbilityStyle style(String id) {
        AbilityRegistry registry = new AbilityRegistry();
        new DefaultAbilityRegistrar().registerAbilities(registry);
        return registry.get(id).create().style();
    }
}
