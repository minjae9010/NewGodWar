package kr.newgodwar.game;

import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.*;

import static org.junit.Assert.*;

public final class SidebarUpdaterTest {
    @Test
    public void unchangedRefreshDoesNotWriteScores() {
        Board fixture = new Board();
        Map<String, Integer> lines = Collections.singletonMap("ability", 12);
        SidebarUpdater.update(fixture.sidebar, lines);
        assertEquals(1, fixture.writes);
        SidebarUpdater.update(fixture.sidebar, lines);
        assertEquals(1, fixture.writes);
        assertEquals(0, fixture.resets);
    }

    @Test
    public void changedAndRemovedLinesPreserveOtherObjectives() {
        Board fixture = new Board();
        fixture.sidebarValues.put("old ability", 12);
        fixture.otherValues.put("old ability", 7);
        fixture.otherValues.put("external", 8);
        SidebarUpdater.update(fixture.sidebar, Collections.singletonMap("new ability", 11));
        assertEquals(Collections.singletonMap("new ability", 11), fixture.sidebarValues);
        assertEquals(Integer.valueOf(7), fixture.otherValues.get("old ability"));
        assertEquals(Integer.valueOf(8), fixture.otherValues.get("external"));
        assertEquals(1, fixture.resets);
        SidebarUpdater.update(fixture.sidebar, Collections.emptyMap());
        assertTrue(fixture.sidebarValues.isEmpty());
        assertEquals(2, fixture.otherValues.size());
    }

    private static final class Board {
        final Map<String, Integer> sidebarValues = new HashMap<>();
        final Map<String, Integer> otherValues = new HashMap<>();
        final Scoreboard board;
        final Objective sidebar;
        final Objective other;
        int writes;
        int resets;

        Board() {
            board = proxy(Scoreboard.class, (p, m, a) -> {
                switch (m.getName()) {
                    case "getEntries":
                        Set<String> entries = new HashSet<>(sidebarValues.keySet());
                        entries.addAll(otherValues.keySet());
                        return entries;
                    case "getScores":
                        return new HashSet<>(Arrays.asList(score(true, (String) a[0]), score(false, (String) a[0])));
                    case "resetScores":
                        sidebarValues.remove(a[0]); otherValues.remove(a[0]); resets++; return null;
                    default: throw new AssertionError(m.getName());
                }
            });
            sidebar = objective(true);
            other = objective(false);
        }

        Objective objective(boolean own) {
            return proxy(Objective.class, (p, m, a) -> {
                if (m.getName().equals("getScoreboard")) return board;
                if (m.getName().equals("getScore")) return score(own, (String) a[0]);
                throw new AssertionError(m.getName());
            });
        }

        Score score(boolean own, String entry) {
            Map<String, Integer> values = own ? sidebarValues : otherValues;
            return proxy(Score.class, (p, m, a) -> {
                switch (m.getName()) {
                    case "getObjective": return own ? sidebar : other;
                    case "isScoreSet": return values.containsKey(entry);
                    case "getScore": return values.getOrDefault(entry, 0);
                    case "setScore": values.put(entry, (Integer) a[0]); writes++; return null;
                    default: throw new AssertionError(m.getName());
                }
            });
        }
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (p, m, a) -> {
            if (m.getName().equals("equals")) return p == a[0];
            if (m.getName().equals("hashCode")) return System.identityHashCode(p);
            return handler.invoke(p, m, a);
        }));
    }
}
