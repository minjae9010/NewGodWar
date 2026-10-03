package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.feedback.DesignedEffect;
import kr.newgodwar.ability.feedback.ObjectModel;
import org.junit.Test;
import java.lang.reflect.Field;
import java.util.List;
import static org.junit.Assert.*;

public final class AbilityDesignTest {
    @Test public void everyDesignKeepsStableReusableGeometryAcrossItsLifecycle() throws Exception {
        for (Field field : AbilityDesigns.class.getFields()) {
            Object value = field.get(null);
            ObjectModel model = value instanceof DesignedEffect ? ((DesignedEffect)value).model(true) : (ObjectModel)value;
            for (double detail : new double[] {0, 1, 3, 6, Double.NaN}) {
                List<ObjectModel.Part> first = model.parts(0, detail);
                assertTrue(field.getName(), first.size() > 0 && first.size() <= 24);
                for (double phase : new double[] {0, 4, 8, 12, 16, 18, 200, Double.NaN}) {
                    List<ObjectModel.Part> frame = model.parts(phase, detail);
                    assertEquals(field.getName(), first.size(), frame.size());
                    for (int i = 0; i < frame.size(); i++) {
                        ObjectModel.Part part = frame.get(i);
                        assertEquals(first.get(i).material, part.material);
                        assertEquals(first.get(i).item, part.item);
                        for (double v : new double[] {part.x,part.y,part.z,part.roll,part.turn}) assertTrue(Double.isFinite(v));
                        assertTrue(Math.abs(part.x) <= 6 && Math.abs(part.y) <= 3.5 && Math.abs(part.z) <= 6);
                        for (double scale : new double[] {part.sx,part.sy,part.sz}) assertTrue(scale > 0 && scale <= 6);
                    }
                }
            }
        }
    }

    @Test public void openingAndClosingPreserveAnchorsAndStaticModeKeepsTheFullPose() {
        ObjectModel animated = AbilityDesigns.MEDICINE.model(true), still = AbilityDesigns.MEDICINE.model(false);
        ObjectModel.Part opening=animated.parts(0,1).get(0), readable=animated.parts(8,1).get(0), closing=animated.parts(18,1).get(0);
        assertTrue(opening.sy < readable.sy && closing.sy < readable.sy);
        assertEquals(readable.y, opening.y, 0);
        assertEquals(readable.y, closing.y, 0);
        assertEquals(still.parts(0,1).get(0).sy, still.parts(18,1).get(0).sy, 0);
    }
}
