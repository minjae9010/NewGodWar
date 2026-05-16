package kr.newgodwar.ability.api;

import kr.newgodwar.NewGodWarPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class AbilityDamageContext {

    private final NewGodWarPlugin plugin;
    private final Player damager;
    private final Player victim;
    private final AbilityDefinition ability;
    private final EntityDamageByEntityEvent event;

    public AbilityDamageContext(NewGodWarPlugin plugin, Player damager, Player victim, AbilityDefinition ability, EntityDamageByEntityEvent event) {
        this.plugin = plugin;
        this.damager = damager;
        this.victim = victim;
        this.ability = ability;
        this.event = event;
    }

    public NewGodWarPlugin plugin() {
        return plugin;
    }

    public Player damager() {
        return damager;
    }

    public Player victim() {
        return victim;
    }

    public AbilityDefinition ability() {
        return ability;
    }

    public EntityDamageByEntityEvent event() {
        return event;
    }

    public double damage() {
        return event.getDamage();
    }

    public void damage(double damage) {
        event.setDamage(damage);
    }

    public String configPath(String key) {
        return "abilities." + ability.id() + "." + key;
    }
}
