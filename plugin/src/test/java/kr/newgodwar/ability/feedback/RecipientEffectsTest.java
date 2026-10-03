package kr.newgodwar.ability.feedback;

import kr.newgodwar.ability.AbilityRegistry;
import kr.newgodwar.ability.builtin.AbilityDesigns;
import kr.newgodwar.ability.builtin.DefaultAbilityRegistrar;
import org.junit.Test;
import static org.junit.Assert.*;

public class RecipientEffectsTest {
    @Test public void victimsDoNotEquipTheirAttackersWeaponsOrCastTheirSpells() {
        AbilityRegistry registry=new AbilityRegistry();new DefaultAbilityRegistrar().registerAbilities(registry);
        for(String id:new String[]{"sniper","archer","gigachad","midoriya","onepunch","miner"}) {
            AbilityStyle style=registry.get(id).create().style();
            assertSame(id,AbilityDesigns.STATUS_HIT,style.receivedEffect(EffectCue.HIT));
        }
        assertSame(AbilityDesigns.STATUS_WATER,registry.get("poseidon").create().style().receivedEffect(EffectCue.WATER));
        assertSame(AbilityDesigns.STATUS_FIRE,registry.get("apollon").create().style().receivedEffect(EffectCue.FIRE));
        assertSame(AbilityDesigns.STATUS_POISON,registry.get("harry").create().style().receivedEffect(EffectCue.POISON));
        assertSame(AbilityDesigns.EARTH_ROOTS,registry.get("gaia").create().style().receivedEffect(EffectCue.ROOT));
        assertSame(AbilityDesigns.VOODOO_DOLL,registry.get("voodoo").create().style().receivedEffect(EffectCue.POISON));
    }

    @Test public void statusRecipientsDoNotOpenCastingCirclesOrDistantBeams() {
        for(DesignedEffect design:new DesignedEffect[]{AbilityDesigns.STATUS_POISON,AbilityDesigns.STATUS_BLIND,
            AbilityDesigns.STATUS_SEAL,AbilityDesigns.STATUS_SLOW,AbilityDesigns.STATUS_CLEANSE,AbilityDesigns.STATUS_HEAL,
            AbilityDesigns.STATUS_MUSIC,AbilityDesigns.STATUS_SLEEP,AbilityDesigns.STATUS_HUNGER,AbilityDesigns.STATUS_FROST}) {
            for(int tick=0;tick<18;tick++)for(ObjectModel.Part part:ArtModels.variant(design.model(true)).parts(tick,1)) {
                assertFalse(part.art.contains("/circle"));assertFalse(part.art.contains("/beam"));
                assertTrue(Math.abs(part.x)<.8);assertTrue(Math.abs(part.z)<.8);assertTrue(part.y<2.7);
            }
        }
    }

    @Test public void readyReticleDoesNotFireAndHitDoesNotInventAnotherProjectile() {
        ObjectModel scope=ArtModels.variant(AbilityDesigns.SCOPE.model(true));
        for(int tick=0;tick<=18;tick++)for(ObjectModel.Part part:scope.parts(tick,1)) {
            assertFalse(part.art.contains("/beam"));assertFalse(part.art.contains("/burst"));
            assertTrue(part.z<1.1);
        }
        for(int tick=0;tick<=18;tick++)for(ObjectModel.Part part:ArtModels.variant(AbilityDesigns.BULLET.model(true)).parts(tick,1)) {
            assertTrue("Shot must not invent a distant hit",part.z<1.5);
            assertFalse(part.art.contains("/beam"));
        }
        for(ObjectModel.Part part:ArtModels.variant(AbilityDesigns.STATUS_HIT.model(true)).parts(4,1)) {
            assertFalse(part.art.contains("/glyph"));assertFalse(part.art.contains("/beam"));
            assertTrue(Math.abs(part.z)<.6);assertTrue(Math.abs(part.x)<.7);
        }
    }
}
