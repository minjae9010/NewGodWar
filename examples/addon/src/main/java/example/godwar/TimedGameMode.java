package example.godwar;

import kr.newgodwar.api.GameMode;
import kr.newgodwar.game.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

/** Minimal complete mode: a 60-second match without core/team startup requirements. */
public final class TimedGameMode implements GameMode {
    private final ExampleAddon addon;
    private BukkitTask finishTask;

    public TimedGameMode(ExampleAddon addon) { this.addon = addon; }

    @Override
    public void onStart(GameManager game) {
        Bukkit.broadcastMessage("§b[예제 모드] 60초 자유 게임을 시작합니다. 코어전 규칙은 적용되지 않습니다.");
        finishTask = Bukkit.getScheduler().runTaskLater(addon, () -> game.stop(true), 60L * 20L);
    }

    @Override
    public void onStop(GameManager game) {
        if (finishTask != null) {
            finishTask.cancel();
            finishTask = null;
        }
        Bukkit.broadcastMessage("§b[예제 모드] 게임을 종료했습니다.");
    }
}
