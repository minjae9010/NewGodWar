package kr.newgodwar.ability;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.ability.api.AbilityDamageContext;
import kr.newgodwar.ability.api.AbilityDefinition;
import kr.newgodwar.ability.api.AbilityKillContext;
import kr.newgodwar.ability.api.AbilityPlayerContext;
import kr.newgodwar.ability.api.AbilityRegistrar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AbilityManager {

    private final NewGodWarPlugin plugin;
    private final Random random = new Random();
    private final AbilityRegistry registry = new AbilityRegistry();
    private final Map<UUID, AbilitySession> assignments = new ConcurrentHashMap<UUID, AbilitySession>();

    public AbilityManager(NewGodWarPlugin plugin) {
        this.plugin = plugin;
        loadRegistrars();
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
        AbilityDefinition definition = chooseLeastDuplicated(enabled);
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
            if (!isEnabled(definition)) {
                continue;
            }
            if (!definition.create().supports(player)) {
                continue;
            }
            enabled.add(definition);
        }
        return enabled;
    }

    public boolean isEnabled(AbilityDefinition definition) {
        return plugin.getConfig().getBoolean("abilities." + definition.id() + ".enabled", definition.enabledByDefault());
    }

    public void handleDamage(Player damager, Player victim, EntityDamageByEntityEvent event) {
        AbilitySession session = session(damager);
        if (session != null) {
            session.ability().onDamage(new AbilityDamageContext(plugin, damager, victim, session.definition(), event));
            session.ability().onDamageByEntity(playerContext(damager, session.definition()), event, victim, true);
        }

        AbilitySession victimSession = session(victim);
        if (victimSession != null) {
            victimSession.ability().onDamageByEntity(playerContext(victim, victimSession.definition()), event, damager, false);
        }
    }

    public void handleKill(Player killer, Player victim, PlayerDeathEvent event) {
        AbilitySession session = session(killer);
        if (session != null) {
            session.ability().onKill(new AbilityKillContext(plugin, killer, victim, session.definition(), event));
        }

        AbilitySession victimSession = session(victim);
        if (victimSession != null) {
            victimSession.ability().onKill(new AbilityKillContext(plugin, killer, victim, victimSession.definition(), event));
        }
    }

    public void handleDeath(PlayerDeathEvent event) {
        for (Map.Entry<UUID, AbilitySession> entry : assignments.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player != null) {
                AbilitySession session = entry.getValue();
                session.ability().onDeath(playerContext(player, session.definition()), event);
            }
        }
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

    public void handleInteract(Player player, PlayerInteractEvent event) {
        AbilitySession session = session(player);
        if (session != null) {
            session.ability().onInteract(playerContext(player, session.definition()), event);
        }
    }

    public void handleGenericDamage(Player player, EntityDamageEvent event) {
        AbilitySession session = session(player);
        if (session != null) {
            session.ability().onGenericDamage(playerContext(player, session.definition()), event);
        }
    }

    public void handleProjectileHit(Player shooter, Player victim, EntityDamageByEntityEvent event) {
        AbilitySession session = session(shooter);
        if (session != null) {
            session.ability().onProjectileHit(playerContext(shooter, session.definition()), event, victim);
        }
    }

    public void handleProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Arrow) && !(event.getEntity() instanceof Snowball)) {
            return;
        }
        Projectile projectile = (Projectile) event.getEntity();
        if (!(projectile.getShooter() instanceof Player)) {
            return;
        }
        Player player = (Player) projectile.getShooter();
        AbilitySession session = session(player);
        if (session != null) {
            session.ability().onProjectileLaunch(playerContext(player, session.definition()), event);
        }
    }

    public void handleBlockBreak(Player player, BlockBreakEvent event) {
        AbilitySession session = session(player);
        if (session != null) {
            session.ability().onBlockBreak(playerContext(player, session.definition()), event);
        }
    }

    public void handleBlockPlace(Player player, BlockPlaceEvent event) {
        AbilitySession session = session(player);
        if (session != null) {
            session.ability().onBlockPlace(playerContext(player, session.definition()), event);
        }
    }

    public void handleBlockExplode(BlockExplodeEvent event) {
        for (AbilitySession session : assignments.values()) {
            session.ability().onBlockExplode(event);
        }
    }

    public void handleSignChange(Player player, SignChangeEvent event) {
        AbilitySession session = session(player);
        if (session != null) {
            session.ability().onSignChange(playerContext(player, session.definition()), event);
        }
    }

    public void handleFoodLevelChange(Player player, FoodLevelChangeEvent event) {
        AbilitySession session = session(player);
        if (session != null) {
            session.ability().onFoodLevelChange(playerContext(player, session.definition()), event);
        }
    }

    public void handleRegainHealth(Player player, EntityRegainHealthEvent event) {
        AbilitySession session = session(player);
        if (session != null) {
            session.ability().onRegainHealth(playerContext(player, session.definition()), event);
        }
    }

    public void handleRespawn(Player player, PlayerRespawnEvent event) {
        AbilitySession session = session(player);
        if (session != null) {
            session.ability().onRespawn(playerContext(player, session.definition()), event);
        }
    }

    public void handleMove(Player player, PlayerMoveEvent event) {
        AbilitySession session = session(player);
        if (session != null) {
            session.ability().onMove(playerContext(player, session.definition()), event);
        }
    }

    public void handleChat(Player player, AsyncPlayerChatEvent event) {
        AbilitySession session = session(player);
        if (session != null) {
            session.ability().onChat(playerContext(player, session.definition()), event);
        }
    }

    public void handleFish(Player player, PlayerFishEvent event) {
        AbilitySession session = session(player);
        if (session != null) {
            session.ability().onFish(playerContext(player, session.definition()), event);
        }
    }

    public void setTarget(Player player, CommandSender sender, String targetName) {
        AbilitySession session = session(player);
        if (session == null) {
            plugin.messages().send(sender, "&c대상 플레이어에게 능력이 없습니다.");
            return;
        }
        session.ability().setTarget(playerContext(player, session.definition()), sender, targetName);
    }

    private AbilityPlayerContext playerContext(Player player, AbilityDefinition definition) {
        return new AbilityPlayerContext(plugin, player, definition);
    }

    private AbilityDefinition chooseLeastDuplicated(List<AbilityDefinition> enabled) {
        if (enabled.isEmpty()) {
            return registry.get("ares");
        }

        Set<String> used = new HashSet<String>();
        for (AbilitySession session : assignments.values()) {
            used.add(session.definition().id());
        }

        List<AbilityDefinition> unused = new ArrayList<AbilityDefinition>();
        for (AbilityDefinition definition : enabled) {
            if (!used.contains(definition.id())) {
                unused.add(definition);
            }
        }

        List<AbilityDefinition> pool = unused.isEmpty() ? enabled : unused;
        Collections.shuffle(pool, random);
        return pool.get(0);
    }

    private void loadRegistrars() {
        int count = 0;
        for (AbilityRegistrar registrar : ServiceLoader.load(AbilityRegistrar.class)) {
            registrar.registerAbilities(registry);
            count++;
        }
        plugin.getLogger().info("Loaded " + registry.all().size() + " abilities from " + count + " registrar(s).");
    }
}
