package kr.newgodwar.game;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.api.event.GameStateChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/** One reusable enemy dummy per administrator; never part of teams, selection, kills or recovery. */
public final class TrainingDummyManager implements Listener {
    private final NewGodWarPlugin plugin;
    private final Map<UUID, TrainingDummyEntity> byOwner = new LinkedHashMap<UUID, TrainingDummyEntity>();
    private final Map<UUID, TrainingDummyEntity> byId = new LinkedHashMap<UUID, TrainingDummyEntity>();

    public TrainingDummyManager(NewGodWarPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public Player spawn(Player owner) throws ReflectiveOperationException {
        Location location = spawnLocation(owner);
        String name;
        do { name = "GW_D_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10); }
        while (Bukkit.getPlayerExact(name) != null);
        TrainingDummyEntity dummy = TrainingDummyEntity.spawn(location, name);
        remove(owner.getUniqueId());
        byOwner.put(owner.getUniqueId(), dummy);
        byId.put(dummy.player().getUniqueId(), dummy);
        return dummy.player();
    }

    private Location spawnLocation(Player owner) {
        Location origin = owner.getLocation();
        Vector direction = origin.getDirection().setY(0);
        if (direction.lengthSquared() < 0.001D) direction = new Vector(0, 0, 1);
        direction.normalize();
        for (int distance = 3; distance >= 1; distance--) {
            Location candidate = origin.clone().add(direction.clone().multiply(distance));
            candidate.setPitch(0);
            candidate.setYaw(origin.getYaw() + 180);
            if (!candidate.getBlock().getType().isSolid() && !candidate.clone().add(0, 1, 0).getBlock().getType().isSolid()
                && candidate.clone().subtract(0, 1, 0).getBlock().getType().isSolid()) return candidate;
        }
        throw new IllegalStateException("앞쪽 1~3블록에 더미를 세울 빈 공간과 바닥이 필요합니다.");
    }

    public boolean isDummy(Entity entity) { return entity != null && byId.containsKey(entity.getUniqueId()); }

    public List<Player> players() {
        List<Player> result = new ArrayList<Player>();
        for (TrainingDummyEntity dummy : byOwner.values()) result.add(dummy.player());
        return result;
    }

    public boolean remove(UUID owner) {
        TrainingDummyEntity dummy = byOwner.remove(owner);
        if (dummy == null) return false;
        try {
            // Clean up state assigned manually through commands using the dummy's exact name.
            if (plugin.abilities().session(dummy.player()) != null) plugin.abilities().remove(dummy.player());
            if (plugin.game().teamOf(dummy.player()) != null) plugin.game().leave(dummy.player());
        } catch (RuntimeException ex) {
            plugin.getLogger().log(Level.WARNING, "Could not clear training dummy combat state", ex);
        } finally {
            byId.remove(dummy.player().getUniqueId());
            try { dummy.remove(); }
            catch (ReflectiveOperationException | RuntimeException ex) { plugin.getLogger().log(Level.WARNING, "Could not remove a training dummy", ex); }
        }
        return true;
    }

    public void clear() { for (UUID owner : new ArrayList<UUID>(byOwner.keySet())) remove(owner); }

    private void tick() {
        for (Map.Entry<UUID, TrainingDummyEntity> entry : new ArrayList<Map.Entry<UUID, TrainingDummyEntity>>(byOwner.entrySet())) {
            Player dummy = entry.getValue().player();
            if (dummy.isDead() || !dummy.isValid()) { remove(entry.getKey()); continue; }
            try { entry.getValue().tick(); }
            catch (ReflectiveOperationException | RuntimeException ex) {
                plugin.getLogger().log(Level.WARNING, "Training dummy connection failed", ex);
                remove(entry.getKey());
                continue;
            }
            dummy.setFoodLevel(20);
            if (dummy.getHealth() < dummy.getMaxHealth()) dummy.setHealth(dummy.getMaxHealth());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!isDummy(event.getEntity())) return;
        Player dummy = (Player) event.getEntity();
        // Keep an ordinary damage event (and hit passives), but prevent a lethal hit from recording a kill.
        if (event.getFinalDamage() >= dummy.getHealth()) event.setDamage(Math.max(0, dummy.getHealth() - 1));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (!isDummy(event.getEntity())) return;
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setDeathMessage(null);
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) { remove(event.getPlayer().getUniqueId()); }
    @EventHandler public void onWorldChange(PlayerChangedWorldEvent event) { remove(event.getPlayer().getUniqueId()); }
    @EventHandler public void onStateChange(GameStateChangeEvent event) {
        if (event.getNewState() == GameState.ENDED || event.getNewState() == GameState.WAITING) clear();
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent event) {
        for (UUID owner : new ArrayList<UUID>(byOwner.keySet())) {
            if (byOwner.get(owner).player().getWorld().equals(event.getWorld())) remove(owner);
        }
    }
    @EventHandler public void onJoin(PlayerJoinEvent event) {
        for (TrainingDummyEntity dummy : byOwner.values()) {
            try { dummy.show(event.getPlayer()); }
            catch (ReflectiveOperationException ex) { plugin.getLogger().log(Level.WARNING, "Could not show a training dummy", ex); }
        }
    }
}
