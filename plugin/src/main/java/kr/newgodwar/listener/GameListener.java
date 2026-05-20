package kr.newgodwar.listener;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.ability.AbilityManager;
import kr.newgodwar.game.GameManager;
import kr.newgodwar.game.GodTeam;
import kr.newgodwar.nms.NmsAdapter;
import kr.newgodwar.util.BukkitCompat;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

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
        plugin.updater().notifyAdminIfOutdated(player);
        if (gameManager.isRunning() && gameManager.teamOf(player) != null) {
            if (gameManager.handleEliminatedJoin(player)) {
                gameManager.refreshPlayerDisplay(player);
                return;
            }
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
        if (team.equals(breakerTeam)) {
            event.setCancelled(true);
            plugin.messages().send(event.getPlayer(), "&c자기 팀의 다이아 심장은 파괴할 수 없습니다.");
            return;
        }
        ItemStack itemInHand = BukkitCompat.mainHandItem(event.getPlayer());
        if (!canBreakCoreWithItem(itemInHand)) {
            event.setCancelled(true);
            sendCoreBreakRestriction(event.getPlayer(), itemInHand);
            return;
        }
        gameManager.eliminate(team, event.getPlayer());
        abilityManager.handleBlockBreak(event.getPlayer(), event);
    }

    private boolean canBreakCoreWithItem(ItemStack itemInHand) {
        if (BukkitCompat.isEmptyItem(itemInHand)) {
            return true;
        }
        Material material = itemInHand.getType();
        if (isPickaxe(material) && isPickaxeUnlocked(material)) {
            return true;
        }
        if (plugin.getConfig().getBoolean("core.require-empty-hand", true)) {
            return false;
        }
        return !isDiamondPickaxe(material) || !plugin.getConfig().getBoolean("core.forbid-diamond-pickaxe", true);
    }

    private void sendCoreBreakRestriction(Player player, ItemStack itemInHand) {
        Material material = itemInHand == null ? null : itemInHand.getType();
        if (isPickaxe(material)) {
            String message = pickaxeUnlockMessage(material);
            if (message != null) {
                plugin.messages().send(player, message);
                return;
            }
        }
        if (isDiamondPickaxe(material) && plugin.getConfig().getBoolean("core.forbid-diamond-pickaxe", true)) {
            Bukkit.broadcastMessage(plugin.messages().prefix() + ChatColor.RED + player.getName()
                + ChatColor.WHITE + " 님이 다이아 곡괭이로 코어를 파괴하려다 적발되었습니다.");
            return;
        }
        plugin.messages().send(player, "&c코어는 맨손으로만 파괴할 수 있습니다.");
    }

    private String pickaxeUnlockMessage(Material material) {
        String path = pickaxeUnlockPath(material);
        if (path == null) {
            return null;
        }
        int seconds = plugin.getConfig().getInt(path, -1);
        if (seconds < 0) {
            return "&c이 곡괭이는 코어 파괴에 아직 허용되지 않았습니다.";
        }
        long remaining = seconds - gameManager.runningElapsedSeconds();
        if (remaining <= 0L) {
            return null;
        }
        return "&c이 곡괭이는 " + formatSeconds(remaining) + " 후 코어 파괴에 허용됩니다.";
    }

    private boolean isPickaxeUnlocked(Material material) {
        String path = pickaxeUnlockPath(material);
        if (path == null) {
            return false;
        }
        int seconds = plugin.getConfig().getInt(path, -1);
        return seconds >= 0 && gameManager.runningElapsedSeconds() >= seconds;
    }

    private String pickaxeUnlockPath(Material material) {
        if (material == null) {
            return null;
        }
        String name = material.name();
        if ("WOOD_PICKAXE".equals(name) || "WOODEN_PICKAXE".equals(name) || "LEGACY_WOOD_PICKAXE".equals(name)) {
            return "core.pickaxe-unlock.wooden-seconds";
        }
        if (material == Material.STONE_PICKAXE) {
            return "core.pickaxe-unlock.stone-seconds";
        }
        if (material == Material.IRON_PICKAXE) {
            return "core.pickaxe-unlock.iron-seconds";
        }
        if ("GOLD_PICKAXE".equals(name) || "GOLDEN_PICKAXE".equals(name) || "LEGACY_GOLD_PICKAXE".equals(name)) {
            return "core.pickaxe-unlock.gold-seconds";
        }
        if (isDiamondPickaxe(material)) {
            return "core.pickaxe-unlock.diamond-seconds";
        }
        return null;
    }

    private boolean isPickaxe(Material material) {
        return pickaxeUnlockPath(material) != null;
    }

    private boolean isDiamondPickaxe(Material material) {
        return material == Material.DIAMOND_PICKAXE;
    }

    private String formatSeconds(long seconds) {
        long safeSeconds = Math.max(0L, seconds);
        long minutes = safeSeconds / 60L;
        long remain = safeSeconds % 60L;
        if (minutes <= 0L) {
            return remain + "초";
        }
        if (remain == 0L) {
            return minutes + "분";
        }
        return minutes + "분 " + remain + "초";
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplodeMonitor(BlockExplodeEvent event) {
        eliminateExplodedTempleDiamonds(event.blockList());
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        protectTempleDiamonds(event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplodeMonitor(EntityExplodeEvent event) {
        eliminateExplodedTempleDiamonds(event.blockList());
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
        final Player player = event.getPlayer();
        final String message = event.getMessage();
        final boolean running = gameManager.isRunning();
        if (!running && !gameManager.isTeamChatMode(player)) {
            return;
        }
        if (gameManager.isTeamChatMode(player)) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (running) {
                    abilityManager.handleChatMessage(player, message);
                }
                gameManager.sendTeamChat(player, message);
            });
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> abilityManager.handleChatMessage(player, message));
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
            forceInventoryDrop(event);
            abilityManager.handleDeath(event);
        }
        Player killer = event.getEntity().getKiller();
        if (killer != null && gameManager.isRunning()) {
            gameManager.recordKill(killer);
            abilityManager.handleKill(killer, event.getEntity(), event);
            event.setDeathMessage(plugin.messages().prefix() + gameManager.playerColoredName(event.getEntity())
                + ChatColor.GRAY + " 님이 " + gameManager.playerColoredName(killer) + ChatColor.GRAY + " 님에게 쓰러졌습니다.");
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

    private void eliminateExplodedTempleDiamonds(List<Block> blocks) {
        if (!gameManager.isRunning() || plugin.getConfig().getBoolean("core.protect-diamond-from-explosion", true)) {
            return;
        }
        for (Block block : blocks) {
            if (block.getType() != Material.DIAMOND_BLOCK) {
                continue;
            }
            GodTeam team = gameManager.templeTeam(block);
            if (team != null) {
                gameManager.eliminate(team, null);
            }
        }
    }

    private void forceInventoryDrop(PlayerDeathEvent event) {
        boolean wasKeepingInventory = event.getKeepInventory();
        event.setKeepInventory(false);
        if (!wasKeepingInventory || !event.getDrops().isEmpty()) {
            return;
        }

        Player player = event.getEntity();
        addDrops(event, player.getInventory().getContents());
        addDrops(event, player.getInventory().getArmorContents());
        addDrop(event, offHandItem(player));
    }

    private void addDrops(PlayerDeathEvent event, ItemStack[] items) {
        if (items == null) {
            return;
        }
        for (ItemStack item : items) {
            addDrop(event, item);
        }
    }

    private void addDrop(PlayerDeathEvent event, ItemStack item) {
        if (item != null && item.getType() != Material.AIR && item.getAmount() > 0) {
            event.getDrops().add(item.clone());
        }
    }

    private ItemStack offHandItem(Player player) {
        try {
            Method method = player.getInventory().getClass().getMethod("getItemInOffHand");
            Object result = method.invoke(player.getInventory());
            return result instanceof ItemStack ? (ItemStack) result : null;
        } catch (ReflectiveOperationException ex) {
            return null;
        } catch (NoClassDefFoundError ex) {
            return null;
        }
    }
}
