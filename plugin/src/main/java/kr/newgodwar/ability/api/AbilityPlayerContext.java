package kr.newgodwar.ability.api;

import kr.newgodwar.NewGodWarPlugin;
import org.bukkit.entity.Player;
import kr.newgodwar.util.BukkitCompat;
import java.util.ArrayList;
import java.util.List;

public final class AbilityPlayerContext {

    private final NewGodWarPlugin plugin;
    private final Player player;
    private final AbilityDefinition ability;

    public AbilityPlayerContext(NewGodWarPlugin plugin, Player player, AbilityDefinition ability) {
        this.plugin = plugin;
        this.player = player;
        this.ability = ability;
    }

    public NewGodWarPlugin plugin() {
        return plugin;
    }

    public Player player() {
        return player;
    }

    public AbilityDefinition ability() {
        return ability;
    }

    public String configPath(String key) {
        return "abilities." + ability.id() + "." + key;
    }

    public List<Player> targetPlayers() {
        List<Player> players = new ArrayList<Player>(BukkitCompat.onlinePlayers());
        if (plugin.trainingDummies() != null) players.addAll(plugin.trainingDummies().players());
        return players;
    }
}
