package example.godwar;

import kr.newgodwar.api.NewGodWarApi;
import kr.newgodwar.api.event.GameStateChangeEvent;
import kr.newgodwar.game.GameState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class ExampleAddon extends JavaPlugin implements Listener {
    private boolean loaded;

    @Override
    public void onLoad() {
        if (loaded) throw new IllegalStateException("Addon onLoad called twice");
        loaded = true;
    }

    @Override
    public void onEnable() {
        if (!loaded) throw new IllegalStateException("Addon onLoad was not called");
        NewGodWarApi api = getServer().getServicesManager().load(NewGodWarApi.class);
        if (api == null) {
            throw new IllegalStateException("NewGodWar API is unavailable");
        }
        api.registerAbility(this, WindRunnerAbility.class);
        api.registerGameMode(this, "example_timed", new TimedGameMode(this));
        getServer().getPluginManager().registerEvents(this, this);
    }

    // A game feature independent of individual abilities.
    @EventHandler
    public void onGameStateChange(GameStateChangeEvent event) {
        if (event.getNewState() == GameState.RUNNING) {
            event.getGame().participants().forEach(player ->
                player.sendMessage("§b[예제 애드온] 게임이 시작되었습니다. 행운을 빕니다!"));
        }
    }
}
