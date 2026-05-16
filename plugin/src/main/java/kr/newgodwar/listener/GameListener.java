package kr.newgodwar.listener;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.ability.AbilityManager;
import kr.newgodwar.game.GameManager;
import kr.newgodwar.game.GodTeam;
import kr.newgodwar.nms.NmsAdapter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Iterator;

public final class GameListener implements Listener {

    private final NewGodWarPlugin plugin;
    private final GameManager gameManager;
    private final AbilityManager abilityManager;
    private final NmsAdapter nmsAdapter;

    public GameListener(NewGodWarPlugin plugin, GameManager gameManager, AbilityManager abilityManager, NmsAdapter nmsAdapter) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.abilityManager = abilityManager;
        this.nmsAdapter = nmsAdapter;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (gameManager.isRunning() && gameManager.teamOf(player) != null) {
            abilityManager.reapply(player);
        }
        gameManager.refreshPlayerDisplay(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!gameManager.isRunning()) {
            gameManager.leave(event.getPlayer());
            return;
        }
        gameManager.forgetPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Entity damagerEntity = event.getDamager();
        Entity victimEntity = event.getEntity();
        if (gameManager.isRunning() && victimEntity instanceof Player && damagerEntity instanceof org.bukkit.entity.Projectile) {
            ProjectileSource shooter = ((org.bukkit.entity.Projectile) damagerEntity).getShooter();
            if (shooter instanceof Player) {
                abilityManager.handleProjectileHit((Player) shooter, (Player) victimEntity, event);
            }
        }

        if (!(damagerEntity instanceof Player) || !(victimEntity instanceof Player)) {
            return;
        }

        Player damager = (Player) damagerEntity;
        Player victim = (Player) victimEntity;
        if (!gameManager.canDamage(damager, victim)) {
            event.setCancelled(true);
            nmsAdapter.sendActionBar(damager, ChatColor.RED + "팀원은 공격할 수 없습니다.");
            return;
        }
        if (!gameManager.isRunning()) {
            return;
        }

        abilityManager.handleDamage(damager, victim, event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGenericDamage(EntityDamageEvent event) {
        if (gameManager.isRunning() && event.getEntity() instanceof Player) {
            abilityManager.handleGenericDamage((Player) event.getEntity(), event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (gameManager.isRunning()) {
            abilityManager.handleProjectileLaunch(event);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (gameManager.isRunning()) {
            abilityManager.handleInteract(event.getPlayer(), event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!gameManager.isRunning()) {
            return;
        }
        Block block = event.getBlock();
        if (block.getType() != Material.DIAMOND_BLOCK) {
            return;
        }
        GodTeam team = gameManager.templeTeam(block);
        if (team == null) {
            return;
        }
        GodTeam breakerTeam = gameManager.teamOf(event.getPlayer());
        if (breakerTeam == team) {
            event.setCancelled(true);
            plugin.messages().send(event.getPlayer(), "&c자기 팀의 다이아 심장은 파괴할 수 없습니다.");
            return;
        }
        if (plugin.getConfig().getBoolean("core.forbid-diamond-pickaxe", true)
            && event.getPlayer().getItemInHand() != null
            && event.getPlayer().getItemInHand().getType() == Material.DIAMOND_PICKAXE) {
            event.setCancelled(true);
            Bukkit.broadcastMessage(plugin.messages().prefix() + ChatColor.RED + event.getPlayer().getName()
                + ChatColor.WHITE + " 님이 다이아 곡괭이로 코어를 파괴하려다 적발되었습니다.");
            return;
        }
        gameManager.eliminate(team, event.getPlayer());
        abilityManager.handleBlockBreak(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAbilityBlockBreak(BlockBreakEvent event) {
        if (gameManager.isRunning() && event.getBlock().getType() != Material.DIAMOND_BLOCK) {
            abilityManager.handleBlockBreak(event.getPlayer(), event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (gameManager.isRunning()) {
            abilityManager.handleBlockPlace(event.getPlayer(), event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        protectTempleDiamonds(event.blockList());
        if (gameManager.isRunning()) {
            abilityManager.handleBlockExplode(event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        protectTempleDiamonds(event.blockList());
    }

    @EventHandler(ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        if (gameManager.isRunning()) {
            abilityManager.handleSignChange(event.getPlayer(), event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (gameManager.isRunning() && event.getEntity() instanceof Player) {
            abilityManager.handleFoodLevelChange((Player) event.getEntity(), event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onRegainHealth(EntityRegainHealthEvent event) {
        if (gameManager.isRunning() && event.getEntity() instanceof Player) {
            abilityManager.handleRegainHealth((Player) event.getEntity(), event);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (gameManager.isRunning()) {
            try {
                org.bukkit.Location location = gameManager.respawnLocation(event.getPlayer(), event.isBedSpawn());
                if (location != null) {
                    event.setRespawnLocation(location);
                }
            } catch (NoSuchMethodError ignored) {
                org.bukkit.Location location = gameManager.respawnLocation(event.getPlayer(), false);
                if (location != null) {
                    event.setRespawnLocation(location);
                }
            }
            abilityManager.handleRespawn(event.getPlayer(), event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (gameManager.isRunning()) {
            abilityManager.handleMove(event.getPlayer(), event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (gameManager.isRunning()) {
            abilityManager.handleChat(event.getPlayer(), event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (gameManager.isRunning()) {
            abilityManager.handleFish(event.getPlayer(), event);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (gameManager.isRunning()) {
            abilityManager.handleDeath(event);
        }
        Player killer = event.getEntity().getKiller();
        if (killer != null && gameManager.isRunning()) {
            gameManager.recordKill(killer);
            abilityManager.handleKill(killer, event.getEntity(), event);
            event.setDeathMessage(plugin.messages().prefix() + ChatColor.RED + event.getEntity().getName()
                + ChatColor.GRAY + " 님이 " + ChatColor.YELLOW + killer.getName() + ChatColor.GRAY + " 님에게 쓰러졌습니다.");
        }
    }

    private void protectTempleDiamonds(java.util.List<Block> blocks) {
        if (!plugin.getConfig().getBoolean("core.protect-diamond-from-explosion", true)) {
            return;
        }
        Iterator<Block> iterator = blocks.iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (block.getType() == Material.DIAMOND_BLOCK && gameManager.templeTeam(block) != null) {
                iterator.remove();
            }
        }
    }
}
