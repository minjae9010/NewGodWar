package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.feedback.ObjectModel;
import kr.newgodwar.ability.feedback.SharedModels;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public final class ObjectModelTest {
    @Test public void everyAnimatedModelFitsTheEntityAndSpatialBudget() {
        for (ObjectModel model : models()) for (double detail : new double[] {-1, 0, 1, 3, 100, Double.NaN}) {
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
        assertEquals(5 * SharedModels.SHIELD.parts(0, 1).size(), AthenaAbility.PHALANX.parts(0, 1).size());
        assertTrue(RunesmithAbility.FROST_RUNE.parts(0, 1).size() > RunesmithAbility.FIRE_RUNE.parts(0, 1).size());
        assertNotEquals(AnubisAbility.SCALES.parts(0, 0).get(1).roll, AnubisAbility.SCALES.parts(0, 4).get(1).roll, 0.001);
        assertNotEquals(SharedModels.WINGS.parts(0, 1).get(0).y, SharedModels.WINGS.parts(5, 1).get(0).y, 0.001);
        assertNotEquals(GravitonAbility.GRAVITY.parts(0, 1).get(1).x, GravitonAbility.GRAVITY.parts(5, 1).get(1).x, 0.001);
    }
    @Test public void customModelsUseTheSameAnimationContract() {
        ObjectModel custom = ObjectModel.animated((phase, detail) -> java.util.Collections.singletonList(
            new ObjectModel.Part("GOLD_BLOCK", false, 0, phase, detail, 0.1, 0.1, 0.1, 0, 0)));
        ObjectModel.Part part = custom.parts(Double.NaN, Double.POSITIVE_INFINITY).get(0);
        assertEquals(0, part.y, 0);
        assertEquals(1, part.z, 0);
        assertEquals(6, custom.parts(0, 100).get(0).z, 0);
    }

    private ObjectModel[] models() {
        return new ObjectModel[] {ThorAbility.HAMMER, SharedModels.SPEAR, SharedModels.SHIELD,
            AthenaAbility.PHALANX, SharedModels.WINGS, SharedModels.FIRE_WINGS,
            RunesmithAbility.FIRE_RUNE, RunesmithAbility.FROST_RUNE, AnubisAbility.SCALES, GravitonAbility.GRAVITY};
    }
}
