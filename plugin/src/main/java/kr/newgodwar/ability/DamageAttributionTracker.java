package kr.newgodwar.ability;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class DamageAttributionTracker {

    private final long ttlMillis;
    private final Map<UUID, Entry> entries = new ConcurrentHashMap<UUID, Entry>();

    DamageAttributionTracker(long ttlMillis) {
        this.ttlMillis = Math.max(0L, ttlMillis);
    }

    void remember(UUID targetId, UUID sourceId, long nowMillis) {
        if (targetId != null && sourceId != null && !targetId.equals(sourceId)) {
            entries.put(targetId, new Entry(sourceId, nowMillis));
        }
    }

    UUID resolve(UUID targetId, long nowMillis) {
        Entry entry = entries.get(targetId);
        if (entry == null) {
            return null;
        }
        if (nowMillis - entry.createdAtMillis > ttlMillis) {
            entries.remove(targetId, entry);
            return null;
        }
        return entry.sourceId;
    }

    UUID consume(UUID targetId, long nowMillis) {
        Entry entry = entries.remove(targetId);
        if (entry == null || nowMillis - entry.createdAtMillis > ttlMillis) {
            return null;
        }
        return entry.sourceId;
    }

    void clear() {
        entries.clear();
    }

    private static final class Entry {
        private final UUID sourceId;
        private final long createdAtMillis;

        private Entry(UUID sourceId, long createdAtMillis) {
            this.sourceId = sourceId;
            this.createdAtMillis = createdAtMillis;
        }
    }
}
