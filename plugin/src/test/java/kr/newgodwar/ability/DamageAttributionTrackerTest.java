package kr.newgodwar.ability;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class DamageAttributionTrackerTest {

    @Test
    public void resolvesAndConsumesRecentSource() {
        DamageAttributionTracker tracker = new DamageAttributionTracker(15000L);
        UUID target = UUID.randomUUID();
        UUID source = UUID.randomUUID();

        tracker.remember(target, source, 1000L);

        assertEquals(source, tracker.resolve(target, 15999L));
        assertEquals(source, tracker.consume(target, 16000L));
        assertNull(tracker.resolve(target, 16000L));
    }

    @Test
    public void expiresAndRejectsSelfAttribution() {
        DamageAttributionTracker tracker = new DamageAttributionTracker(1000L);
        UUID target = UUID.randomUUID();
        UUID source = UUID.randomUUID();

        tracker.remember(target, source, 100L);
        assertNull(tracker.resolve(target, 1101L));

        tracker.remember(target, target, 2000L);
        assertNull(tracker.resolve(target, 2000L));
    }
}
