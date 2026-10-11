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

    @Test public void solidProjectilesUseOneMeshWithoutChangingTheFallback() throws Exception {
        Field hammer=Class.forName("kr.newgodwar.ability.builtin.ThorAbility").getDeclaredField("HAMMER");
        hammer.setAccessible(true);
        for(ObjectModel source:new ObjectModel[]{SharedModels.SPEAR,(ObjectModel)hammer.get(null)}) {
            ObjectModel art=ArtModels.variant(source);
            for(double phase:new double[]{0,4,8,16,40}) {
                List<ObjectModel.Part> parts=art.parts(phase,1);
                assertEquals("One mesh, one flash and three trail sparks",5,parts.size());
                int meshes=0;
                for(ObjectModel.Part part:parts)if(part.art.contains("/glyph")) {
                    meshes++;
                    assertFalse("Projectile faces follow its flight direction",part.billboard);
                    assertEquals(Math.min(part.sx,part.sy),part.sz,.00001);
                }
                assertEquals(1,meshes);
            }
            for(ObjectModel.Part part:source.parts(8,1))assertNull("No-pack model is still native blocks/items",part.art);
        }
    }

    @Test public void solidWeaponDepthFollowsItsSizeAndDisappearsWithTheScene() {
        ObjectModel trident=ArtModels.create("design/trident","slash","water",true,false);
        ObjectModel.Part visible=trident.parts(8,1).get(0),hidden=trident.parts(18,1).get(0);
        assertEquals(.58,visible.sz,.00001);
        assertFalse(visible.billboard);
        assertEquals(.01,hidden.sx,.00001);
        assertEquals(.019,hidden.sy,.00001);
        assertEquals(.01,hidden.sz,.00001);
        ObjectModel dagger=ArtModels.create("design/dagger_step","slash","shadow",true,false);
        int blades=0;
        for(ObjectModel.Part part:dagger.parts(8,1))if(part.art.contains("/glyph"))blades++;
        assertEquals("The side profile comes from the mesh, not a duplicate display",1,blades);
    }

    @Test public void spearStaysPointFirstWithoutSpinningAndItsTipMatchesTheFlightAnchor() {
        ObjectModel spear=ArtModels.create("shared/spear","projectile","gold",false,false);
        ObjectModel.Part initial=spear.parts(0,1).get(0);
        for(double tick=0;tick<=120;tick+=.5) {
            ObjectModel.Part part=spear.parts(tick,1).get(0);
            double[] axis=displayedWeaponLongAxis(part);
            assertEquals("Spear must point along the anchor's flight direction",1,axis[2],.000001);
            assertEquals(initial.roll,part.roll,0);
            assertEquals(initial.turn,part.turn,0);
            assertEquals(initial.pitch,part.pitch,0);
            // Native spear's furthest tip is Y=15.3 in the 16px model; ItemDisplay centres models at 8.
            double tip=(15.3-8)/16*part.sy;
            assertEquals("Tip reaches the collision anchor exactly",0,part.z+axis[2]*tip,.000001);
            assertTrue("The shaft trails behind its contact point",part.z<0);
        }
    }

    @Test public void thrownHammerFliesHeadFirstWithAVerticalHeadAndTrailingGrip() {
        ObjectModel hammer=ArtModels.create("thor/hammer","projectile","storm",false,false);
        for(double tick=0;tick<=120;tick+=.5) {
            ObjectModel.Part part=hammer.parts(tick,1).get(0);
            double[] axis=displayedWeaponLongAxis(part);
            assertEquals("Grip-to-head axis must point along flight",1,axis[2],.000001);
            assertEquals(0,axis[0],.000001);assertEquals(0,axis[1],.000001);
            // Client Ry(PI) flips model X before Rz * Ry * Rx. The head's long model-X edge becomes vertical.
            assertEquals("Head is vertical in side view, not a standing T",1,
                Math.abs(Math.cos(part.turn)*Math.sin(part.roll)),.000001);
            assertEquals("Metal head centre follows the flight anchor",0,part.z+(12.4-8)/16*part.sy,.000001);
            assertTrue("Grip trails behind the head",part.z+(1.4-8)/16*part.sy<-.5);
            assertEquals(hammer.parts(0,1).get(0).roll,part.roll,0);
        }
    }

    @Test public void fallbackHammerHasTheSameSideOnFlightProfile() {
        for(double tick:new double[]{0,2,12,22,120}) {
            List<ObjectModel.Part> parts=ThorAbility.HAMMER.parts(tick,1);
            ObjectModel.Part head=parts.get(0),grip=parts.get(3);
            assertTrue("Head's long edge is vertical",head.sy>head.sx*2&&head.sy>head.sz*2);
            assertTrue("Grip is parallel to flight",grip.sz>grip.sx*5&&grip.sz>grip.sy*5);
            assertTrue("Entire grip is behind the head",grip.z+grip.sz/2<=head.z-head.sz/2+.000001);
            assertEquals(head.x,grip.x,0);assertEquals(head.y,grip.y,0);
        }
    }

    @Test public void tridentTipFacesItsForwardThrustThroughoutTheStroke() {
        ObjectModel trident=ArtModels.create("design/trident","slash","water",true,false);
        double previousZ=trident.parts(2,1).get(0).z;
        for(double tick=0;tick<=18;tick+=.5) {
            ObjectModel.Part part=trident.parts(tick,1).get(0);
            assertTrue("Prongs must lead the forward stroke",displayedWeaponLongAxis(part)[2]>.85);
            assertTrue("The weapon stays beside the hand",part.x<-.3);
            if(tick>2&&tick<=6) {
                assertTrue("The strike advances, it does not fly backward",part.z>previousZ);
                previousZ=part.z;
            }
        }
        assertTrue("The hand retracts after contact",trident.parts(16,1).get(0).z<trident.parts(8,1).get(0).z);
    }

    @Test public void fishHookTravelsOutAndBackWithoutInflatingBesideTheCamera() {
        ObjectModel hook=ArtModels.create("design/fish_hook","hook","water",true,false);
        assertEquals(2.6,hook.parts(0,1).get(0).z,.000001);
        assertEquals(3.4,hook.parts(4,1).get(0).z,.000001);
        assertEquals(1.1,hook.parts(10,1).get(0).z,.000001);
        double previousZ=3.4;
        for(double tick=0;tick<=18;tick+=.5) {
            ObjectModel.Part part=hook.parts(tick,1).get(0);
            assertTrue("Travel is expressed as position, not a giant icon",part.sx<=.7&&part.sy<=.81);
            assertTrue("The hook stays in front of the player",part.z>=1.1-.000001&&part.z<=3.4);
            if(tick>4&&tick<=10) {assertTrue(part.z<previousZ);previousZ=part.z;}
        }
    }

    private static double[] displayedWeaponLongAxis(ObjectModel.Part part) {
        // The client appends Ry(PI), which leaves the native weapons' +Y tip/handle axis unchanged.
        double y=Math.cos(part.pitch),z=Math.sin(part.pitch);
        double x=z*Math.sin(part.turn);z*=Math.cos(part.turn);
        return new double[]{x*Math.cos(part.roll)-y*Math.sin(part.roll),
            x*Math.sin(part.roll)+y*Math.cos(part.roll),z};
    }

    @Test public void solidCatalogueOnlyReusesExistingGlyphIdsAndMatchingSilhouettes() throws Exception {
        Properties solid=new Properties();
        try(InputStream input=ArtModels.class.getResourceAsStream("/solid-art.properties")) { solid.load(input); }
        Map<String,String> motifs=new HashMap<String,String>();
        for(String[] row:catalogue())motifs.put(row[0].toLowerCase(Locale.ROOT).replace('.','/'),row[1]);
        assertEquals(10,solid.size());
        for(String key:solid.stringPropertyNames()) {
            assertEquals(key,motifs.get(key),solid.getProperty(key));
            for(int fade=0;fade<4;fade++)assertTrue(key,PackModels.modelId("art/"+key+"/glyph"+fade)>=74000);
        }
    }

    @Test public void solidShieldFrontsFaceAwayFromTheProtectedCentre() {
        String[][] scenes={{"athena/phalanx","phalanx","5"},{"design/aegis_plates","guard","3"},
            {"design/black_armor","guard","3"},{"design/servant_wall","wall","5"},{"design/protego","dome","6"}};
        for(String[] scene:scenes)for(double phase:new double[]{0,4,8,18,40}) {
            ObjectModel model=ArtModels.create(scene[0],scene[1],"holy",!scene[1].equals("phalanx"),false);
            int shields=0;
            for(ObjectModel.Part p:model.parts(phase,3))if(!p.billboard&&p.art.contains("/glyph")) {
                double[] normal=displayedShieldFrontNormal(p);
                double outward=(p.x*normal[0]+p.z*normal[2])/Math.hypot(p.x,p.z);
                assertTrue(scene[0]+" front normal must have positive radial dot product",outward>0);
                assertEquals(scene[0]+" faces directly outward",1,outward,.000001);
                shields++;
            }
            assertEquals(scene[0],Integer.parseInt(scene[2]),shields);
        }
    }

    @Test public void solidShieldFrontSurvivesClientRotationForHeldAndBillboardModels() {
        ObjectModel held=ArtModels.create("shared/shield","guard","steel",false,false);
        assertEquals("Held shield faces ahead after the client's item rotation",1,
            displayedShieldFrontNormal(held.parts(8,1).get(0))[2],.000001);
        ObjectModel plates=ArtModels.create("design/aegis_plates","guard","holy",true,false);
        int emblems=0;
        for(ObjectModel.Part part:plates.parts(8,1))if(part.billboard&&part.art.contains("/glyph")) {
            assertEquals("Billboard's decorated face points toward the viewer",1,
                displayedShieldFrontNormal(part)[2],.000001);
            emblems++;
        }
        assertEquals(1,emblems);
    }

    private static double[] displayedShieldFrontNormal(ObjectModel.Part part) {
        // SolidArtwork puts the decorated face on model -Z (also asserted by verify_solid_art.py).
        // Verified in the 26.3 client's DisplayRenderer$ItemDisplayRenderer.submitInner:
        // item rendering appends Ry(PI), before DisplayBridge's Rz * Ry * Rx transforms the vertices.
        double x=-Math.sin(Math.PI),y=0,z=-Math.cos(Math.PI);
        double py=y*Math.cos(part.pitch)-z*Math.sin(part.pitch),pz=y*Math.sin(part.pitch)+z*Math.cos(part.pitch);
        double tx=x*Math.cos(part.turn)+pz*Math.sin(part.turn),tz=-x*Math.sin(part.turn)+pz*Math.cos(part.turn);
        return new double[]{tx*Math.cos(part.roll)-py*Math.sin(part.roll),
            tx*Math.sin(part.roll)+py*Math.cos(part.roll),tz};
    }

    @Test public void nonSolidShieldScenesKeepTheirExistingOrientation() {
        for(String motion:new String[]{"phalanx","guard","wall","dome"}) {
            ObjectModel model=ArtModels.create("design/test",motion,"holy",!motion.equals("phalanx"),false);
            for(ObjectModel.Part p:model.parts(8,3))if(!p.billboard&&p.art.contains("/glyph")) {
                double angle=Math.atan2(p.x,p.z);
                assertEquals(motion,Math.sin(-angle),Math.sin(p.turn),.000001);
                assertEquals(motion,Math.cos(-angle),Math.cos(p.turn),.000001);
            }
        }
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
        assertEquals("All bundled models are exercised",150,checked);
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
        for(int t:new int[]{0,2,4})for(ObjectModel.Part p:heal.parts(t,1))if(p.billboard&&p.art.startsWith("art/design/medicine/"))hero.add(p.art);
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
        // Hero emblems stay close to the anchor axis so their owner remains visually unambiguous.
        for(String[] row:catalogue())for(String motion:new String[]{row[3]}) {
            if(motion.matches("slash|punch|bow|shot|scope|hook|gust|step|exhaust|forge|shatter|mirror|wings|projectile|flock|relic|melody|strike|harvest|dome|weaken"))continue;
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
