package kr.newgodwar.ability.feedback;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class WingModelsTest {
    @Test public void feathersStayBehindTheBodyAndFlapSymmetrically() {
        for(boolean fire:new boolean[]{true,false}) {
            ObjectModel model=WingModels.create(fire,true,false);
            for(int tick=0;tick<240;tick++) {
                List<ObjectModel.Part> parts=model.parts(tick,1);
                assertEquals(16,parts.size());
                for(int i=0;i<8;i++) {
                    ObjectModel.Part left=parts.get(i),right=parts.get(i+8);
                    assertFalse(left.billboard);
                    assertEquals(-left.x,right.x,1e-9);
                    assertEquals(left.y,right.y,1e-9);
                    assertEquals(left.z,right.z,1e-9);
                    assertTrue(left.z<-.35);
                    assertTrue("Feathers are slim rather than overlapping square icons",left.sx<.8);
                    assertEquals(left.art,model.parts(0,1).get(i).art);
                }
            }
            assertTrue(Math.abs(model.parts(10,1).get(5).x)>Math.abs(model.parts(0,1).get(5).x)*1.7);
            assertNotEquals(model.parts(10,1).get(5).y,model.parts(30,1).get(5).y,.05);
        }
    }

    @Test public void phoenixHasLongStreamersAndStaticModeFreezesTheWholePose() {
        ObjectModel fire=WingModels.create(true,true,true),holy=WingModels.create(false,true,true);
        assertTrue(fire.parts(0,1).get(7).sy>holy.parts(0,1).get(7).sy*2);
        for(int i=0;i<16;i++) {
            ObjectModel.Part a=fire.parts(0,1).get(i),b=fire.parts(200,1).get(i);
            assertEquals(a.x,b.x,0);assertEquals(a.y,b.y,0);assertEquals(a.roll,b.roll,0);
        }
        assertTrue(WingModels.create(true,false,false).parts(0,1).size()<=48);
    }
}
