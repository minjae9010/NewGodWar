package kr.newgodwar.ability;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.ability.api.AbilityDamageContext;
import kr.newgodwar.ability.api.AbilityDefinition;
import kr.newgodwar.ability.api.AbilityKillContext;
import kr.newgodwar.ability.api.AbilityPlayerContext;
import kr.newgodwar.ability.builtin.AresAbility;
import kr.newgodwar.ability.builtin.HermesAbility;
import kr.newgodwar.ability.builtin.PoseidonAbility;
import kr.newgodwar.ability.builtin.ZeusAbility;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AbilityManager {

    private final NewGodWarPlugin plugin;
    private final Random random = new Random();
    private final AbilityRegistry registry = new AbilityRegistry();
    private final Map<UUID, AbilitySession> assignments = new ConcurrentHashMap<UUID, AbilitySession>();

    public AbilityManager(NewGodWarPlugin plugin) {
        this.plugin = plugin;
        registerBuiltInAbilities();
    }

    public AbilityRegistry registry() {
        return registry;
    }

    public void clear() {
        for (Map.Entry<UUID, AbilitySession> entry : assignments.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player != null) {
                entry.getValue().ability().onRemove(playerContext(player, entry.getValue().definition()));
            }
        }
        assignments.clear();
    }

    public AbilityDefinition assignRandom(Player player) {
        List<AbilityDefinition> enabled = enabledAbilities(player);
        AbilityDefinition definition = enabled.isEmpty() ? registry.get("ares") : enabled.get(random.nextInt(enabled.size()));
        set(player, definition);
        return definition;
    }

    public void set(Player player, AbilityDefinition definition) {
        if (definition == null) {
            return;
        }
        AbilitySession previous = assignments.remove(player.getUniqueId());
        if (previous != null) {
            previous.ability().onRemove(playerContext(player, previous.definition()));
        }

        AbilitySession session = new AbilitySession(definition, definition.create());
        assignments.put(player.getUniqueId(), session);
        session.ability().onAssign(playerContext(player, definition));
    }

    public AbilitySession session(Player player) {
        return assignments.get(player.getUniqueId());
    }

    public AbilityDefinition get(Player player) {
        AbilitySession session = session(player);
        return session == null ? null : session.definition();
    }

    public List<AbilityDefinition> enabledAbilities(Player player) {
        List<AbilityDefinition> enabled = new ArrayList<AbilityDefinition>();
        for (AbilityDefinition definition : registry.all()) {
            boolean defaultEnabled = definition.enabledByDefault();
            if (!plugin.getConfig().getBoolean("abilities." + definition.id() + ".enabled", defaultEnabled)) {
                continue;
            }
            if (!definition.create().supports(player)) {
                continue;
            }
            enabled.add(definition);
        }
        return enabled;
    }

    public void handleDamage(Player damager, Player victim, EntityDamageByEntityEvent event) {
        AbilitySession session = session(damager);
        if (session == null) {
            return;
        }
        session.ability().onDamage(new AbilityDamageContext(plugin, damager, victim, session.definition(), event));
    }

    public void handleKill(Player killer, Player victim, PlayerDeathEvent event) {
        AbilitySession session = session(killer);
        if (session == null) {
            return;
        }
        session.ability().onKill(new AbilityKillContext(plugin, killer, victim, session.definition(), event));
    }

    public void reapply(Player player) {
        AbilitySession session = session(player);
        if (session != null) {
            session.ability().onAssign(playerContext(player, session.definition()));
        }
    }

    public void tick(Iterable<? extends Player> players) {
        for (Player player : players) {
            AbilitySession session = session(player);
            if (session != null) {
                session.ability().onTick(playerContext(player, session.definition()));
            }
        }
    }

    private AbilityPlayerContext playerContext(Player player, AbilityDefinition definition) {
        return new AbilityPlayerContext(plugin, player, definition);
    }

    private void registerBuiltInAbilities() {
        registry.register(ZeusAbility.class);
        registry.register(AresAbility.class);
        registry.register(HermesAbility.class);
        registry.register(PoseidonAbility.class);
    }
}
