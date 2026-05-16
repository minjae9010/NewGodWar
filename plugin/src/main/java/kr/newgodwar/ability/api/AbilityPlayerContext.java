package kr.newgodwar.ability.api;

import kr.newgodwar.NewGodWarPlugin;
import org.bukkit.entity.Player;

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
}
