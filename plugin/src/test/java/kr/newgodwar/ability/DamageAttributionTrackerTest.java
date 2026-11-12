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

    @Test
    public void cleanupReclaimsUnqueriedEntitiesAndPreservesTheTtlBoundary() {
        DamageAttributionTracker tracker = new DamageAttributionTracker(1000L);
        UUID source = UUID.randomUUID();
        for (int i = 0; i < 1000; i++) {
            tracker.remember(UUID.randomUUID(), source, 100L);
        }
        UUID fresh = UUID.randomUUID();
        tracker.remember(fresh, source, 101L);

        assertEquals(0, tracker.removeExpired(1100L));
        assertEquals(1000, tracker.removeExpired(1101L));
        assertEquals(source, tracker.resolve(fresh, 1101L));
        assertEquals(1, tracker.removeExpired(1102L));
        assertEquals(0, tracker.removeExpired(1102L));
    }

    @Test
    public void cleanupKeepsRenewedSourcesAndIgnoresConsumedEntries() {
        DamageAttributionTracker tracker = new DamageAttributionTracker(1000L);
        UUID target = UUID.randomUUID();
        UUID oldSource = UUID.randomUUID();
        UUID newSource = UUID.randomUUID();
        tracker.remember(target, oldSource, 100L);
        tracker.remember(target, newSource, 1000L);

        assertEquals(0, tracker.removeExpired(1101L));
        assertEquals(newSource, tracker.consume(target, 1101L));
        assertEquals(0, tracker.removeExpired(3000L));
        assertNull(tracker.resolve(target, 3000L));
    }
}
