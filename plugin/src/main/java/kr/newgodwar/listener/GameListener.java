package kr.newgodwar.listener;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.ability.AbilityManager;
import kr.newgodwar.game.GameManager;
import kr.newgodwar.game.GodTeam;
import kr.newgodwar.nms.NmsAdapter;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

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
            abilityManager.applyPersistentEffects(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!gameManager.isRunning()) {
            gameManager.leave(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Entity damagerEntity = event.getDamager();
        Entity victimEntity = event.getEntity();
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

        event.setDamage(abilityManager.modifyDamage(damager, event.getDamage()));
        if (abilityManager.shouldStrikeLightning(damager)) {
            victim.getWorld().strikeLightningEffect(victim.getLocation());
            event.setDamage(event.getDamage() + 3.0D);
            nmsAdapter.sendActionBar(damager, ChatColor.YELLOW + "제우스의 번개가 내려쳤습니다.");
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
        gameManager.eliminate(team, event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer != null && gameManager.isRunning()) {
            gameManager.recordKill(killer);
            event.setDeathMessage(plugin.messages().prefix() + ChatColor.RED + event.getEntity().getName()
                + ChatColor.GRAY + " 님이 " + ChatColor.YELLOW + killer.getName() + ChatColor.GRAY + " 님에게 쓰러졌습니다.");
        }
    }
}
