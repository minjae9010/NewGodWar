package kr.newgodwar.ability.feedback;

import org.junit.Test;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class ObjectFrameTest {
    @Test public void duplicateRefreshesAndDisabledAnimationSampleOnlyOnce() {
        AtomicInteger samples = new AtomicInteger();
        ObjectFrame frame = new ObjectFrame((phase, detail) -> {
            samples.incrementAndGet();
            return Collections.singletonList(part(phase, detail, 0, "fx/spark"));
        });
        List<ObjectModel.Part> pose = frame.sample(8, 1);
        for (int tick = 0; tick < 40; tick += 2) assertSame(pose, frame.sample(8, 1));
        assertEquals(1, samples.get());
        assertNotSame(pose, frame.sample(8, 2));
        assertEquals(2, frame.sample(8, 2).get(0).sx, 0);
        assertEquals(2, samples.get());
        assertEquals(10, frame.sample(10, 2).get(0).x, 0);
        assertEquals(3, samples.get());
    }

    @Test public void movingPartsDoNotResendTheirUnchangedItemAndTextureChangesStillUpdate() {
        ObjectModel.Part first = part(0, 1, 0, "fx/spark");
        ObjectModel.Part moved = part(1, 1, 0, "fx/spark");
        assertFalse(ObjectFrame.sameTransform(first, moved));
        assertTrue(ObjectFrame.sameItem(first, moved));
        ObjectModel.Part textured = part(1, 1, 0, "fx/shard");
        assertTrue(ObjectFrame.sameTransform(moved, textured));
        assertFalse(ObjectFrame.sameItem(moved, textured));
        assertFalse(ObjectFrame.sameItem(textured, part(1, 1, 0, null)));
        assertFalse(ObjectFrame.sameTransform(moved, part(1, 1, .2, "fx/spark")));
        assertFalse(ObjectFrame.sameTransform(moved, part(1, 2, 0, "fx/spark")));
    }

    @Test public void aReusedModelListCannotOverwriteTheLastRenderedPose() {
        List<ObjectModel.Part> reused = new ArrayList<ObjectModel.Part>();
        ObjectFrame frame = new ObjectFrame((phase, detail) -> {
            reused.clear(); reused.add(part(phase, detail, 0, "fx/spark"));
            return reused;
        });
        List<ObjectModel.Part> before = frame.sample(0, 1);
        List<ObjectModel.Part> after = frame.sample(2, 1);
        assertEquals(0, before.get(0).x, 0);
        assertEquals(2, after.get(0).x, 0);
        assertFalse(ObjectFrame.sameTransform(before.get(0), after.get(0)));
        assertTrue(ObjectFrame.sameItem(before.get(0), after.get(0)));
    }

    @Test public void changingAnimationModeAndRecreatingScenesCannotReuseAnOldPose() {
        AtomicInteger samples = new AtomicInteger();
        ObjectModel model = (phase, detail) -> {
            samples.incrementAndGet();
            return Collections.singletonList(part(phase, detail, 0, "fx/spark"));
        };
        ObjectFrame frame = new ObjectFrame(model);
        assertEquals(4, frame.sample(4, 1).get(0).x, 0);
        assertEquals(8, frame.sample(8, 1).get(0).x, 0); // Animation disabled.
        assertEquals(12, frame.sample(12, 1).get(0).x, 0); // Animation enabled again.
        ObjectFrame recreated = new ObjectFrame(model);
        assertEquals(0, recreated.sample(0, 1).get(0).x, 0);
        assertEquals(12, frame.sample(12, 1).get(0).x, 0);
        assertEquals(4, samples.get());
    }

    @Test public void independentlyAllocatedStaticPosesDoNotNeedAnotherMatrix() {
        ObjectFrame frame = new ObjectFrame((phase, detail) -> Collections.singletonList(part(0, 1, 0, "fx/spark")));
        ObjectModel.Part first = frame.sample(0, 1).get(0);
        ObjectModel.Part later = frame.sample(2, 1).get(0);
        assertNotSame(first, later);
        assertTrue(ObjectFrame.sameTransform(first, later));
        assertTrue(ObjectFrame.sameItem(first, later));
        assertFalse(ObjectFrame.sameTransform(null, first));
    }

    @Test public void billboardTrailStaysBehindTheFlightDirectionIncludingPitch() {
        ObjectModel.Part trail = billboard(0, 0, -2);
        Location anchor = new Location(null, 10, 64, 20, 90, 45);
        Location placed = ObjectFrame.displayLocation(anchor, trail);
        double offset = Math.sqrt(2);
        assertEquals(10 + offset, placed.getX(), .000001);
        assertEquals(64 + offset, placed.getY(), .000001);
        assertEquals(20, placed.getZ(), .000001);
        assertEquals(90, placed.getYaw(), 0);
        assertEquals(45, placed.getPitch(), 0);
        assertEquals("Placement cannot move the scene anchor", 10, anchor.getX(), 0);
        assertEquals(64, anchor.getY(), 0);
        assertEquals(20, anchor.getZ(), 0);
    }

    @Test public void billboardOffsetMovesItsEntityWithoutRetriggeringMeshInterpolation() {
        ObjectModel.Part before = billboard(.5, 1, -1), after = billboard(.5, 2, -2);
        assertFalse(ObjectFrame.sameOffset(before, after));
        assertTrue(ObjectFrame.sameTransform(before, after));
        assertTrue(ObjectFrame.sameItem(before, after));
        Location anchor = new Location(null, 0, 0, 0);
        Location first = ObjectFrame.displayLocation(anchor, before), second = ObjectFrame.displayLocation(anchor, after);
        assertEquals(.5, first.getX(), 0);
        assertEquals(1, first.getY(), 0);
        assertEquals(-1, first.getZ(), 0);
        assertEquals(2, second.getY(), 0);
        assertEquals(-2, second.getZ(), 0);
    }

    @Test public void fixedMeshesKeepTheirOffsetInTheTransformation() {
        Location anchor = new Location(null, 10, 64, 20, 90, 45);
        ObjectModel.Part fixed = part(1, 1, 0, "fx/spark");
        Location placed = ObjectFrame.displayLocation(anchor, fixed);
        assertEquals(anchor, placed);
        assertFalse(ObjectFrame.sameTransform(fixed, part(2, 1, 0, "fx/spark")));
        assertFalse(ObjectFrame.sameTransform(fixed, billboard(1, 0, 0)));
    }

    private static ObjectModel.Part billboard(double x, double y, double z) {
        return new ObjectModel.Part("PAPER", true, x, y, z, 1, 1, 1, 0, 0, 0, "fx/spark", true);
    }

    private static ObjectModel.Part part(double x, double scale, double pitch, String art) {
        return new ObjectModel.Part("PAPER", true, x, 0, 0, scale, scale, scale, 0, 0, pitch, art);
    }
}
