package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.AbilityPlayerContext;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/** Short-lived combat state must not survive death, suppression, logout or reassignment. */
abstract class TransientAbility extends BaseAbility {
    @Override
    public void cancelScheduledTasks() {
        super.cancelScheduledTasks();
        clearTransientState();
    }

    protected void clearTransientState() { }

    @Override
    public void onDeath(AbilityPlayerContext context, PlayerDeathEvent event) {
        if (event.getEntity().equals(context.player())) cancelScheduledTasks();
    }

    protected boolean active(AbilityPlayerContext context) {
        Player player = context.player();
        return player.isOnline() && !player.isDead() && context.plugin().game().canUseAbility(player);
    }

    @Override
    protected void damage(AbilityPlayerContext context, Player target, double amount, Player source) {
        if (active(context) && target != null && !target.isDead()) super.damage(context, target, amount, source);
    }

    protected boolean validEnemy(AbilityPlayerContext context, Player target, double range) {
        Player player = context.player();
        return active(context) && target != null && !target.isDead()
            && player.getWorld().equals(target.getWorld()) && canAffectEnemy(context, player, target)
            && player.getLocation().distanceSquared(target.getLocation()) <= range * range;
    }

    protected List<Player> enemies(AbilityPlayerContext context, Location center, double radius) {
        List<Player> targets = new ArrayList<Player>();
        if (!active(context) || !context.player().getWorld().equals(center.getWorld())) return targets;
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof Player)) continue;
            Player target = (Player) entity;
            if (!target.isDead() && canAffectEnemy(context, context.player(), target)
                && target.getLocation().distanceSquared(center) <= radius * radius) targets.add(target);
        }
        return targets;
    }

    protected void repel(Player target, Location center, double power) {
        Vector direction = target.getLocation().toVector().subtract(center.toVector()).setY(0);
        if (direction.lengthSquared() > 0.001D) direction.normalize().multiply(power);
        target.setVelocity(direction.setY(0.3D));
    }

    protected List<Player> alliesInRange(AbilityPlayerContext context, Location center, double radius) {
        List<Player> result = new ArrayList<Player>();
        if (!active(context) || !context.player().getWorld().equals(center.getWorld())) return result;
        for (Player target : alliedPlayers(context, context.player(), true)) {
            if (!target.isDead() && center.getWorld().equals(target.getWorld())
                && target.getLocation().distanceSquared(center) <= radius * radius) result.add(target);
        }
        return result;
    }

    protected boolean directAttack(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player target, boolean attacker) {
        return attacker && !event.isCancelled() && event.getDamage() > 0
            && event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK
            && event.getDamager().equals(context.player()) && validEnemy(context, target, 5);
    }

    protected void restoreHealth(Player player, double amount) {
        if (player.isOnline() && !player.isDead() && amount > 0)
            player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + amount));
    }
}
