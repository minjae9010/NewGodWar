package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.feedback.*;
import org.junit.Test;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.Assert.*;

public class ArtModelsTest {
    @Test public void allDesignedModelsHaveBoundedStableArtAndStillVariants() throws Exception {
        for(Field field:AbilityDesigns.class.getFields()) {
            Object value=field.get(null);
            for(boolean animate:new boolean[]{true,false}) {
                ObjectModel source=value instanceof DesignedEffect?((DesignedEffect)value).model(animate):(ObjectModel)value;
                ObjectModel art=ArtModels.variant(source);assertNotNull(field.getName(),art);
                List<ObjectModel.Part> first=art.parts(0,1);assertTrue(first.size()<=12);
                for(double t:new double[]{0,4,8,12,18,40,200,Double.NaN})for(double d:new double[]{0,1,3,6,Double.NaN}) {
                    List<ObjectModel.Part> parts=art.parts(t,d);assertEquals(first.size(),parts.size());
                    for(ObjectModel.Part p:parts) {
                        assertTrue(p.item);assertEquals("PAPER",p.material);assertTrue(p.art.matches("art/[a-z_/]+/(glyph|ring|arc|mote)[0-3]"));
                        for(double n:new double[]{p.x,p.y,p.z,p.roll,p.turn,p.pitch,p.sx,p.sy,p.sz})assertTrue(Double.isFinite(n));
                        assertTrue(p.sx>0&&p.sy>0&&p.sz>0);
                    }
                }
                if(!animate&&value instanceof DesignedEffect)assertEquals(art.parts(0,1).get(0).art,art.parts(17,1).get(0).art);
            }
        }
    }
    @Test public void formationsAndJudgmentKeepTheirGameplayCues() {
        ObjectModel scales=ArtModels.create("anubis/scales","relic",false,false);
        assertTrue(scales.parts(8,4).get(0).roll < scales.parts(8,0).get(0).roll);
        ObjectModel formation=ArtModels.create("athena/phalanx","phalanx",false,false);
        List<ObjectModel.Part> parts=formation.parts(8,1);
        assertEquals(10,parts.size());
        for(int i=0;i<10;i+=2)assertEquals(4.5,Math.hypot(parts.get(i).x,parts.get(i).z),.00001);
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
            String[] source=row[0].split("\\.");
            Class<?> owner=source[0].equals("design")?AbilityDesigns.class:source[0].equals("shared")?SharedModels.class:
                Class.forName("kr.newgodwar.ability.builtin."+Character.toUpperCase(source[0].charAt(0))+source[0].substring(1)+"Ability");
            Field field=owner.getDeclaredField(source[1]);field.setAccessible(true);Object value=field.get(null);
            ObjectModel original=value instanceof DesignedEffect?((DesignedEffect)value).model(true):(ObjectModel)value;
            ObjectModel art=ArtModels.variant(original);assertNotNull(row[0],art);
            int count=art.parts(0,1).size();assertTrue(row[0]+" display budget",count>0&&count<=12);
            StringBuilder signature=new StringBuilder();
            for(double t:new double[]{0,8,16,40,200,Double.NaN,Double.POSITIVE_INFINITY}) {
                for(double detail:new double[]{0,1,3,6,Double.NaN}) {
                    List<ObjectModel.Part> parts=art.parts(t,detail);assertEquals(row[0]+" topology",count,parts.size());
                    for(ObjectModel.Part p:parts) {
                        for(double n:new double[]{p.x,p.y,p.z,p.roll,p.turn,p.pitch,p.sx,p.sy,p.sz})assertTrue(row[0],Double.isFinite(n));
                        assertTrue(row[0],p.sx>0&&p.sy>0&&p.sx<20&&p.sy<20);
                    }
                }
                if(t<18)for(ObjectModel.Part p:art.parts(t,1))signature.append(String.format(Locale.ROOT,"%.2f,%.2f,%.2f,%.2f,%.2f;",p.x,p.y,p.z,p.sx,p.sy));
            }
            silhouettes.add(signature.toString());checked++;
        }
        assertEquals("All bundled models are exercised",97,checked);
        assertTrue("Textures alone must not be the only difference between abilities",silhouettes.size()>=35);
    }

    @Test public void visibleBoundaryUsesThePaintedTextureCoreAndActualRadius() {
        for(String motion:new String[]{"clock","fire-rune","frost-rune","abyss","vortex"})for(double radius:new double[]{.5,1,3,6}) {
            ObjectModel model=ArtModels.create("design/test",motion,false,false);
            double expected=motion.endsWith("rune")?Math.min(3,radius):radius;
            for(double phase:new double[]{0,8,40}) {
                int boundaries=0;
                for(ObjectModel.Part part:model.parts(phase,radius))if(part.art.contains("/ring")) {
                    boundaries++;
                    // The generator's ring core is 94px from center on a 256px texture.
                    assertEquals(motion,expected,part.sx*94/256,.000001);
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
        for(String[] sample:samples)for(ObjectModel.Part part:ArtModels.create(sample[0],sample[1],true,false).parts(8,3))
            assertFalse(sample[0]+" must not suggest a ground radius",part.art.contains("/ring")&&part.y<.2);
    }

    @Test public void motionCommunicatesHealingBindingAndImpactInDifferentDirections() {
        ObjectModel heal=ArtModels.create("design/medicine","heal",true,false);
        assertTrue("Healing travels upward on its recipient",heal.parts(10,1).get(0).y>heal.parts(0,1).get(0).y);
        ObjectModel curse=ArtModels.create("design/akashic_curse","curse",true,false);
        ObjectModel.Part before=curse.parts(0,1).get(1),after=curse.parts(8,1).get(1);
        assertTrue("Bindings tighten inward",Math.hypot(after.x,after.z)<Math.hypot(before.x,before.z));
        ObjectModel forge=ArtModels.create("design/anvil","forge",true,false);
        assertTrue("Hammer strikes downward",forge.parts(8,1).get(0).y<forge.parts(0,1).get(0).y);
        ObjectModel laurel=ArtModels.create("design/laurel","crown",false,false);
        assertTrue("Earned laurels remain legible",laurel.parts(8,3).get(4).sx>laurel.parts(8,1).get(4).sx);
    }

    @Test public void illustratedPropsKeepAPerpendicularFaceForSideViewers() {
        for(String motion:new String[]{"pages","steam","bow","forge","bloom","mask","heal"}) {
            List<ObjectModel.Part> parts=ArtModels.create("design/test",motion,true,false).parts(8,1);
            ObjectModel.Part profile=parts.get(parts.size()-1);boolean paired=false;
            for(int i=0;i<parts.size()-1;i++) {
                ObjectModel.Part face=parts.get(i);
                if(face.art.contains("/glyph")&&face.x==profile.x&&face.y==profile.y&&face.z==profile.z
                        &&Math.abs(Math.cos(face.turn-profile.turn))<.000001)paired=true;
            }
            assertTrue(motion+" remains visible from a 90-degree view",paired);
        }
    }

    @Test public void handActionsRemainInFrontOfTheFirstPersonCamera() {
        for(String motion:new String[]{"pages","steam","forge"}) {
            ObjectModel.Part main=ArtModels.create("design/test",motion,true,false).parts(8,1).get(0);
            assertTrue(motion+" must be ahead of the camera",main.z>=1.0);
            assertTrue(motion+" must remain inside horizontal view",Math.abs(main.x)/main.z<.6);
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
