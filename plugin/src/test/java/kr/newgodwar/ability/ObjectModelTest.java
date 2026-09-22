package kr.newgodwar.ability;

import kr.newgodwar.ability.feedback.ObjectModel;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public final class ObjectModelTest {
    @Test public void everyAnimatedModelFitsTheEntityAndSpatialBudget() {
        for (ObjectModel model : ObjectModel.values()) for (double detail : new double[] {-1, 0, 1, 3, 100, Double.NaN}) {
            int size = model.parts(0, detail).size();
            assertTrue(model.toString(), size > 0 && size <= 48);
            for (double phase : new double[] {0, 2, 20, 200, Double.NaN}) {
                List<ObjectModel.Part> parts = model.parts(phase, detail);
                assertEquals("Animation must reuse its entity assembly", size, parts.size());
                for (ObjectModel.Part part : parts) {
                    assertTrue(Double.isFinite(part.x) && Math.abs(part.x) <= 6);
                    assertTrue(Double.isFinite(part.y) && Math.abs(part.y) <= 3);
                    assertTrue(Double.isFinite(part.z) && Math.abs(part.z) <= 6);
                    for (double scale : new double[] {part.sx, part.sy, part.sz}) assertTrue(Double.isFinite(scale) && scale > 0 && scale <= 6);
                    assertTrue(Double.isFinite(part.roll) && Double.isFinite(part.turn));
                    assertFalse(part.material.isEmpty());
                }
            }
        }
    }

    @Test public void objectsRepresentTheirActualActions() {
        assertEquals(5 * ObjectModel.SHIELD.parts(0, 1).size(), ObjectModel.PHALANX.parts(0, 1).size());
        assertTrue(ObjectModel.FROST_RUNE.parts(0, 1).size() > ObjectModel.FIRE_RUNE.parts(0, 1).size());
        assertNotEquals(ObjectModel.SCALES.parts(0, 0).get(1).roll, ObjectModel.SCALES.parts(0, 4).get(1).roll, 0.001);
        assertNotEquals(ObjectModel.WINGS.parts(0, 1).get(0).y, ObjectModel.WINGS.parts(5, 1).get(0).y, 0.001);
        assertNotEquals(ObjectModel.GRAVITY.parts(0, 1).get(1).x, ObjectModel.GRAVITY.parts(5, 1).get(1).x, 0.001);
    }
}
