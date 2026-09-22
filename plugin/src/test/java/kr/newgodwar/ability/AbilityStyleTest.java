package kr.newgodwar.ability;

import kr.newgodwar.ability.builtin.DefaultAbilityRegistrar;
import kr.newgodwar.ability.feedback.AbilityStyle;
import kr.newgodwar.ability.feedback.EffectCue;
import org.junit.Test;
import java.util.HashSet;
import static org.junit.Assert.*;

public final class AbilityStyleTest {
    @Test public void everyRegisteredBuiltinHasAnExplicitStyle() {
        AbilityRegistry registry = new AbilityRegistry();
        new DefaultAbilityRegistrar().registerAbilities(registry);
        assertEquals(new HashSet<String>(registry.ids()), AbilityStyle.ids());
        assertEquals(EffectCue.NONE, AbilityStyle.of("external_addon").cast(false));
        assertNotNull(AbilityStyle.of(null).theme());
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
            AbilityStyle style = AbilityStyle.of(id);
            assertTrue(id, style.dedicated());
            assertEquals(id, EffectCue.NONE, style.cast(false));
            assertEquals(id, EffectCue.NONE, style.cast(true));
            assertEquals(id, EffectCue.NONE, style.hit());
            assertEquals(id, EffectCue.NONE, style.status("SLOWNESS"));
        }
        assertEquals(EffectCue.ROOT, AbilityStyle.of("gaia").status("SLOWNESS"));
        assertEquals(EffectCue.SLEEP, AbilityStyle.of("morpious").status("BLINDNESS"));
        assertEquals(EffectCue.BLIND, AbilityStyle.of("blinder").status("BLINDNESS"));
        assertEquals(EffectCue.STEALTH, AbilityStyle.of("clocking").status("INVISIBILITY"));
        assertEquals(EffectCue.SEAL, AbilityStyle.of("sejong").status("BLINDNESS"));
        assertEquals(EffectCue.POISON, AbilityStyle.of("acidarcher").status("POISON"));
        assertEquals(EffectCue.HEAL, AbilityStyle.of("asclepius").benefit());
        assertEquals(EffectCue.FORGE, AbilityStyle.of("blacksmith").cast(false));
        assertEquals(EffectCue.NONE, AbilityStyle.of("nasdaq").cast(false));
        assertEquals(EffectCue.NONE, AbilityStyle.of("snow").cast(true));
    }
}
