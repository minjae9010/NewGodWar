package kr.newgodwar.ability.api;

import kr.newgodwar.NewGodWarPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;

public final class AbilityKillContext {

    private final NewGodWarPlugin plugin;
    private final Player killer;
    private final Player victim;
    private final AbilityDefinition ability;
    private final PlayerDeathEvent event;

    public AbilityKillContext(NewGodWarPlugin plugin, Player killer, Player victim, AbilityDefinition ability, PlayerDeathEvent event) {
        this.plugin = plugin;
        this.killer = killer;
        this.victim = victim;
        this.ability = ability;
        this.event = event;
    }

    public NewGodWarPlugin plugin() {
        return plugin;
    }

    public Player killer() {
        return killer;
    }

    public Player victim() {
        return victim;
    }

    public AbilityDefinition ability() {
        return ability;
    }

    public PlayerDeathEvent event() {
        return event;
    }

    public String configPath(String key) {
        return "abilities." + ability.id() + "." + key;
    }
}
