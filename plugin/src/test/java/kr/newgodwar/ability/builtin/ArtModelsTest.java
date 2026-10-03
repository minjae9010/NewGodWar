package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.feedback.*;
import org.junit.Test;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.Assert.*;

public class ArtModelsTest {
    private static final String TEXTURE="art/(fx/[a-z]+/(flash|wave|sigil|ring|beam|spark|shard)[0-3]|fx/[a-z]+/(burst|slash|craft|chart|crash)[0-5]|[a-z_]+/[a-z_]+/(draw[01]|[a-z]+[0-3]))";

    @Test public void allDesignedModelsHaveBoundedStableArtAndStillVariants() throws Exception {
        for(Field field:AbilityDesigns.class.getFields()) {
            Object value=field.get(null);
            for(boolean animate:new boolean[]{true,false}) {
                ObjectModel source=value instanceof DesignedEffect?((DesignedEffect)value).model(animate):(ObjectModel)value;
                ObjectModel art=ArtModels.variant(source);assertNotNull(field.getName(),art);
                List<ObjectModel.Part> first=art.parts(0,1);assertTrue(first.size()<=ArtModels.MAX_PARTS);
                for(double t:new double[]{0,4,8,12,18,40,200,Double.NaN})for(double d:new double[]{0,1,3,6,Double.NaN}) {
                    List<ObjectModel.Part> parts=art.parts(t,d);assertEquals(first.size(),parts.size());
                    for(int i=0;i<parts.size();i++) {
                        ObjectModel.Part p=parts.get(i);
                        assertTrue(p.item);assertEquals("PAPER",p.material);assertTrue(p.art,p.art.matches(TEXTURE));
                        assertEquals("Billboarding is fixed at spawn",first.get(i).billboard,p.billboard);
                        for(double n:new double[]{p.x,p.y,p.z,p.roll,p.turn,p.pitch,p.sx,p.sy,p.sz})assertTrue(Double.isFinite(n));
                        assertTrue(p.sx>0&&p.sy>0&&p.sz>0);
                    }
                }
                if(!animate&&value instanceof DesignedEffect)assertEquals(art.parts(0,1).get(0).art,art.parts(17,1).get(0).art);
            }
        }
    }
    @Test public void formationsAndJudgmentKeepTheirGameplayCues() {
        ObjectModel scales=ArtModels.create("anubis/scales","relic","gold",false,false);
        assertTrue(scales.parts(8,4).get(0).roll < scales.parts(8,0).get(0).roll);
        List<ObjectModel.Part> parts=ArtModels.create("athena/phalanx","phalanx","holy",false,false).parts(8,1);
        for(int i=0;i<10;i+=2) {
            assertTrue(parts.get(i).art.contains("/glyph"));
            assertEquals(4.5,Math.hypot(parts.get(i).x,parts.get(i).z),.00001);
        }
    }
    @Test public void unknownAddonGeometryRemainsUntouched() {
        assertNull(ArtModels.variant((phase,detail)->Collections.emptyList()));
    }
    @Test public void particlesAndPackArtKeepSeparateRepresentations() {
        ObjectModel base=AbilityDesigns.MEDICINE.model(true),art=ArtModels.variant(base);
        assertNull(base.parts(8,1).get(0).art);
        assertNotNull(art.parts(8,1).get(0).art);
        assertNotEquals(art.parts(0,1).get(0).art,art.parts(8,1).get(0).art);
        assertNotEquals(art.parts(8,1).get(0).art,art.parts(18,1).get(0).art);
    }

    @Test public void everyCatalogueMotionStaysFiniteWithinTheDisplayBudget() throws Exception {
        Set<String> silhouettes=new HashSet<String>();int checked=0;
        for(String[] row:catalogue()) {
            assertTrue(row[0]+" has a palette",row.length==5||row.length==6);
            String[] source=row[0].split("\\.");
            Class<?> owner=source[0].equals("design")?AbilityDesigns.class:source[0].equals("shared")?SharedModels.class:
                Class.forName("kr.newgodwar.ability.builtin."+Character.toUpperCase(source[0].charAt(0))+source[0].substring(1)+"Ability");
            Field field=owner.getDeclaredField(source[1]);field.setAccessible(true);Object value=field.get(null);
            ObjectModel original=value instanceof DesignedEffect?((DesignedEffect)value).model(true):(ObjectModel)value;
            ObjectModel art=ArtModels.variant(original);assertNotNull(row[0],art);
            int count=art.parts(0,1).size();assertTrue(row[0]+" display budget",count>0&&count<=ArtModels.MAX_PARTS);
            StringBuilder signature=new StringBuilder();
            for(double t:new double[]{0,8,16,40,200,Double.NaN,Double.POSITIVE_INFINITY}) {
                for(double detail:new double[]{0,1,3,6,Double.NaN}) {
                    List<ObjectModel.Part> parts=art.parts(t,detail);assertEquals(row[0]+" topology",count,parts.size());
                    for(ObjectModel.Part p:parts) {
                        for(double n:new double[]{p.x,p.y,p.z,p.roll,p.turn,p.pitch,p.sx,p.sy})assertTrue(row[0],Double.isFinite(n));
                        assertTrue(row[0],p.sx>0&&p.sy>0&&p.sx<40&&p.sy<40);
                        assertTrue(row[0]+" uses its palette",!p.art.startsWith("art/fx/")||p.art.startsWith("art/fx/"+row[4]+"/"));
                    }
                }
                if(t<18)for(ObjectModel.Part p:art.parts(t,1))signature.append(String.format(Locale.ROOT,"%.2f,%.2f,%.2f,%.2f,%.2f;",p.x,p.y,p.z,p.sx,p.sy));
            }
            silhouettes.add(signature.toString());checked++;
        }
        assertEquals("All bundled models are exercised",145,checked);
        assertTrue("Textures alone must not be the only difference between abilities",silhouettes.size()>=35);
    }

    @Test public void visibleBoundaryUsesThePaintedTextureCoreAndActualRadius() {
        for(String motion:new String[]{"clock","fire-rune","frost-rune","abyss","vortex","cage","melody","echo"})for(double radius:new double[]{.5,1,3,6}) {
            ObjectModel model=ArtModels.create("design/test",motion,"gold",false,false);
            double expected=motion.endsWith("rune")?Math.min(3,radius):motion.matches("cage|melody|echo")?Math.max(1,radius):radius;
            for(double phase:new double[]{0,8,40}) {
                int boundaries=0;
                for(ObjectModel.Part part:model.parts(phase,radius))if(part.art.contains("/ring")) {
                    boundaries++;
                    if(part.sx*ArtModels.BOUNDARY_TEXTURE_RADIUS<.1)continue; // a boundary may fade out with its scene
                    assertEquals(motion,expected,part.sx*ArtModels.BOUNDARY_TEXTURE_RADIUS,.000001);
                    assertEquals(part.sx,part.sy,0);assertEquals(Math.PI/2,part.pitch,0);
                }
                assertEquals(motion+" has one unambiguous boundary",1,boundaries);
            }
        }
    }

    @Test public void personalBuffsAndChargeDoNotInventAnAreaOfEffect() {
        String[][] samples={{"design/medicine","heal"},{"design/akashic_book","pages"},{"design/feast","steam"},
            {"design/rose_gate","bloom"},{"design/black_armor","guard"},{"design/heart_bind","bind"},
            {"design/cloak","veil"},{"design/explosion_charge","charge"}};
        for(String[] sample:samples)for(double t:new double[]{2,8,14})
            for(ObjectModel.Part part:ArtModels.create(sample[0],sample[1],"gold",!sample[1].equals("charge"),false).parts(t,3)) {
                // A casting circle stays within the body's footprint; shockwaves are brief flourishes, not boundaries.
                if(part.art.contains("/sigil")&&part.y<.2)assertTrue(sample[0]+" must not suggest a ground radius",part.sx<=2.3);
            }
        for(ObjectModel.Part part:ArtModels.create("design/explosion_charge","charge","fire",false,false).parts(8,4))
            assertFalse("The charge circle hangs in the sky",part.art.contains("/sigil")&&part.y<.2);
    }

    @Test public void motionCommunicatesHealingBindingAndImpactInDifferentDirections() {
        ObjectModel heal=ArtModels.create("design/medicine","heal","nature",true,false);
        assertTrue("Healing travels upward on its recipient",heal.parts(8,1).get(4).y>heal.parts(2,1).get(4).y);
        ObjectModel bind=ArtModels.create("design/counter_lock","bind","steel",true,false);
        assertTrue("Bindings tighten inward",Math.abs(bind.parts(8,1).get(3).x)<Math.abs(bind.parts(0,1).get(3).x));
        ObjectModel forge=ArtModels.create("design/anvil","forge","fire",true,false);
        assertTrue("Hammer strikes downward",forge.parts(4,1).get(2).y<forge.parts(2.5,1).get(2).y);
        assertTrue("The finished piece rises from the anvil",forge.parts(14,1).get(13).y>forge.parts(9,1).get(13).y);
        ObjectModel laurel=ArtModels.create("design/laurel","crown","gold",false,false);
        assertTrue("Earned laurels remain legible",laurel.parts(8,3).get(4).sx>laurel.parts(8,1).get(4).sx);
    }

    @Test public void impactIgnitesOnTheBeatThenDies() {
        for(String motion:new String[]{"slash","punch","heal","bloom","lightning","banner"}) {
            ObjectModel model=ArtModels.create("design/test",motion,"gold",true,false);
            boolean before=false,during=false,after=false;
            for(ObjectModel.Part p:model.parts(2,1))if(p.art.contains("/burst"))before|=p.sx>.02;
            for(ObjectModel.Part p:model.parts(4,1))if(p.art.contains("/burst"))during|=p.sx>.5&&p.art.endsWith("burst0");
            for(ObjectModel.Part p:model.parts(18,1))if(p.art.contains("/burst"))after|=p.sx>.02;
            assertFalse(motion+" flash waits for the impact",before);
            assertTrue(motion+" flashes at full strength on impact",during);
            assertFalse(motion+" flash has gone by the end",after);
        }
    }

    @Test public void sequencesReplayFrameByFrameFromEachCast() {
        ObjectModel heal=ArtModels.create("design/medicine","heal","nature",true,false);
        List<String> bursts=new ArrayList<String>();
        for(int t=4;t<=14;t+=2)for(ObjectModel.Part p:heal.parts(t,1))if(p.art.contains("/burst"))bursts.add(p.art);
        assertEquals(Arrays.asList("art/fx/nature/burst0","art/fx/nature/burst1","art/fx/nature/burst2",
            "art/fx/nature/burst3","art/fx/nature/burst4","art/fx/nature/burst5"),bursts);
        // The emblem is traced in during the charge, then the finished emblem appears on the impact beat.
        List<String> hero=new ArrayList<String>();
        for(int t:new int[]{0,2,4})for(ObjectModel.Part p:heal.parts(t,1))if(p.art.startsWith("art/design/medicine/"))hero.add(p.art);
        assertEquals(Arrays.asList("art/design/medicine/draw0","art/design/medicine/draw1","art/design/medicine/glyph3"),hero);
        ObjectModel still=ArtModels.create("design/medicine","heal","nature",true,true);
        for(ObjectModel.Part p:still.parts(0,1))assertFalse("Still mode never shows a half-drawn emblem",p.art.contains("/draw"));
    }

    @Test public void emblemsAreReadableAboveTheHeadAndWeaponsSitAtTheHand() {
        ObjectModel.Part hero=null;
        for(ObjectModel.Part p:ArtModels.create("design/medicine","heal","nature",true,false).parts(8,1))
            if(p.art.contains("/glyph"))hero=p;
        assertNotNull(hero);assertTrue(hero.billboard);assertEquals(0,hero.x,0);assertTrue(Math.abs(hero.z)<.1);assertTrue(hero.y>2.2);
        for(String motion:new String[]{"slash","punch","bow","forge"}) {
            ObjectModel.Part weapon=ArtModels.create("design/test",motion,"steel",true,false).parts(8,1).get(0);
            assertFalse(weapon.billboard);
            assertTrue(motion+" must be ahead of the camera",weapon.z>=.9);
            assertTrue(motion+" must stay at the right hand, beside the view centre",weapon.x<0&&-weapon.x/weapon.z<.6);
        }
    }

    @Test public void billboardsStayOnRingsAroundTheAnchorAxis() throws Exception {
        // A billboard rotates its offset with the viewer, so only axis-symmetric placements are allowed.
        for(String[] row:catalogue())for(String motion:new String[]{row[3]}) {
            if(motion.matches("slash|punch|bow|shot|scope|hook|gust|step|exhaust|forge|shatter|mirror|wings|projectile|flock|relic|melody|strike|harvest|dome"))continue;
            for(ObjectModel.Part p:ArtModels.create("design/test",motion,"gold",true,false).parts(8,2))
                if(p.billboard&&p.art.contains("/glyph")&&!motion.matches("coins|bind|curse|knot|stock|crash|wrong"))
                    assertEquals(row[0],0,Math.hypot(p.x,p.z),.1);
        }
    }

    private static List<String[]> catalogue() throws IOException {
        List<String[]> rows=new ArrayList<String[]>();
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(ArtModels.class.getResourceAsStream("/effect-art.tsv")),StandardCharsets.UTF_8))) {
            String line;while((line=reader.readLine())!=null)if(!line.startsWith("#")&&!line.trim().isEmpty())rows.add(line.split("\\|"));
        }
        return rows;
    }
}
