package kr.newgodwar.game;

import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

final class SidebarUpdater {
    private SidebarUpdater() { }

    static void update(Objective objective, Map<String, Integer> lines) {
        Scoreboard board = objective.getScoreboard();
        for (String entry : new HashSet<String>(board.getEntries())) {
            if (objective.getScore(entry).isScoreSet() && !lines.containsKey(entry)) {
                // Legacy Bukkit only exposes a board-wide reset. Preserve other objectives.
                Map<Score, Integer> preserved = new LinkedHashMap<Score, Integer>();
                for (Score score : board.getScores(entry)) {
                    if (!score.getObjective().equals(objective) && score.isScoreSet()) {
                        preserved.put(score, score.getScore());
                    }
                }
                board.resetScores(entry);
                for (Map.Entry<Score, Integer> score : preserved.entrySet()) {
                    score.getKey().setScore(score.getValue());
                }
            }
        }
        for (Map.Entry<String, Integer> line : lines.entrySet()) {
            Score current = objective.getScore(line.getKey());
            if (!current.isScoreSet() || current.getScore() != line.getValue()) {
                current.setScore(line.getValue());
            }
        }
    }
}
