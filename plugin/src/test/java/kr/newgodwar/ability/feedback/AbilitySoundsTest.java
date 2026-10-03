package kr.newgodwar.ability.feedback;

import kr.newgodwar.ability.AbilityRegistry;
import kr.newgodwar.ability.builtin.DefaultAbilityRegistrar;
import org.junit.Test;
import static org.junit.Assert.*;

public class AbilitySoundsTest {
    @Test public void everyAbilityHasLegacyResolvableCastAndEndSoundsIncludingDedicatedVisuals() {
        AbilityRegistry registry=new AbilityRegistry();new DefaultAbilityRegistrar().registerAbilities(registry);
        int count=0;
        for(String id:registry.ids()) {
            AbilityStyle style=registry.get(id).create().style();count++;
            for(boolean advanced:new boolean[]{true,false}) {
                AbilitySounds.Voice voice=AbilitySounds.cast(style,advanced);
                assertNotNull(id,voice.sound);assertTrue(voice.volume>0&&voice.volume<=.6);
            }
            assertNotNull(id,AbilitySounds.finish(style).sound);
            assertNotEquals(AbilitySounds.cast(style,false).pitch,AbilitySounds.cast(style,true).pitch,0);
        }
        assertEquals(93,count);
    }

    @Test public void allRealReactionsHaveAnExplicitVoiceAndNoneIsSilent() {
        for(AbilityTheme theme:AbilityTheme.values())for(EffectCue cue:EffectCue.values()) {
            AbilitySounds.Voice voice=AbilitySounds.reaction(AbilityStyle.builder(theme).build(),cue);
            if(cue==EffectCue.NONE)assertNull(voice);
            else {assertNotNull(voice);assertNotNull(theme+" / "+cue,voice.sound);}
        }
    }
}
