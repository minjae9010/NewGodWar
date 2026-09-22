package kr.newgodwar.regression;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.ability.api.AbilityDefinition;
import kr.newgodwar.ability.api.AbilityPlayerContext;
import kr.newgodwar.ability.builtin.BaseAbility;
import kr.newgodwar.game.GameState;
import kr.newgodwar.game.GodTeam;
import kr.newgodwar.game.TrainingDummyEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/** Real server-player entities: name lookup, /x, sight/AOE, damage, reuse and lifecycle cleanup. */
final class TrainingDummyChecks {
    static void run(JavaPlugin probe, NewGodWarPlugin core) throws Exception {
        Location origin = Bukkit.getWorlds().get(0).getSpawnLocation().clone();
        origin.setYaw(0); origin.setPitch(0);
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
            origin.clone().add(x, -1, z).getBlock().setType(Material.STONE);
            origin.clone().add(x, 0, z).getBlock().setType(Material.AIR);
            origin.clone().add(x, 1, z).getBlock().setType(Material.AIR);
            origin.clone().add(x, 2, z).getBlock().setType(Material.AIR);
        }
        TrainingDummyEntity ownerEntity = TrainingDummyEntity.spawn(origin, "GW_ProbeOwner");
        Player owner = ownerEntity.player();
        Field stateField = field(core.game(), "state");
        Object previousState = stateField.get(core.game());
        try {
            owner.addAttachment(probe, "newgodwar.admin", true);
            stateField.set(core.game(), GameState.RUNNING);
            teams(core).put(owner.getUniqueId(), GodTeam.RED);
            AbilityDefinition definition = core.abilities().registry().get("morpious");
            core.abilities().set(owner, definition);
            PluginCommand command = core.getCommand("godwar");
            command.getExecutor().onCommand(owner, command, "gw", new String[] {"dummy"});
            check(core.trainingDummies().players().size() == 1, "dummy command failed to spawn");
            Player dummy = core.trainingDummies().players().get(0);
            command.getExecutor().onCommand(owner, command, "gw", new String[] {"dummy", "remove", "extra"});
            check(core.trainingDummies().players().contains(dummy), "invalid extra arguments removed dummy");
            check(dummy.isOnline() && dummy.isValid(), "dummy is not a live player entity");
            check(Bukkit.getPlayerExact(dummy.getName()) == dummy && Bukkit.getPlayer(dummy.getUniqueId()) == dummy, "dummy lookups");
            check(!Bukkit.getOnlinePlayers().contains(dummy) && !core.game().participants().contains(dummy), "dummy counted as a real participant");
            check(!core.game().teamAssignments().containsKey(dummy.getUniqueId()), "dummy acquired a team");
            check(command.getTabCompleter().onTabComplete(owner, command, "gw", new String[] {"target", ""}).contains(dummy.getName()), "dummy target completion");
            PluginCommand target = core.getCommand("x");
            target.getExecutor().onCommand(owner, target, "x", new String[] {dummy.getName()});
            owner.getInventory().setItemInHand(new ItemStack(Material.BLAZE_ROD));
            owner.getInventory().addItem(new ItemStack(Material.COBBLESTONE, 64));
            core.abilities().handleInteract(owner, new PlayerInteractEvent(owner, Action.LEFT_CLICK_AIR,
                owner.getItemInHand(), null, org.bukkit.block.BlockFace.SELF));
            check(dummy.hasPotionEffect(PotionEffectType.BLINDNESS), "real /x-targeted ability did not affect dummy");
            ProbeAbility ability = new ProbeAbility();
            AbilityPlayerContext context = new AbilityPlayerContext(core, owner, definition);
            check(ability.sight(context) == dummy && ability.nearby(context).contains(dummy), "sight/area targeting misses dummy");
            double health = dummy.getHealth();
            ability.hit(context, dummy, 4);
            check(dummy.getHealth() < health, "ability damage did not reach dummy");
            dummy.setNoDamageTicks(0);
            ability.hit(context, dummy, 2048);
            check(!dummy.isDead() && core.game().killsOf(owner) == 0, "dummy death awarded a kill");
            // Also build/send the client player-info packet to a real CraftPlayer receiver.
            @SuppressWarnings("unchecked") Map<UUID, TrainingDummyEntity> entries = (Map<UUID, TrainingDummyEntity>) field(core.trainingDummies(), "byOwner").get(core.trainingDummies());
            entries.get(owner.getUniqueId()).show(owner);
            probe.getLogger().info("PASS player dummy /x, real ability, sight/AOE, damage, no participants or kills");
            Bukkit.getScheduler().runTaskLater(probe, () -> {
                try {
                    ownerEntity.tick();
                    check(dummy.isValid() && dummy.getHealth() == dummy.getMaxHealth(), "dummy did not survive/heal across server ticks");
                    Player replacement = core.trainingDummies().spawn(owner);
                    check(core.trainingDummies().players().size() == 1 && !replacement.getUniqueId().equals(dummy.getUniqueId()), "repeated spawn duplicated dummy");
                    check(Bukkit.getPlayer(dummy.getUniqueId()) == null && Bukkit.getPlayerExact(dummy.getName()) == null, "replaced dummy lookup leaked");
                    core.trainingDummies().remove(owner.getUniqueId());
                    check(Bukkit.getPlayer(replacement.getUniqueId()) == null && !replacement.isValid(), "dummy removal leaked entity/lookup");
                    check(core.trainingDummies().players().isEmpty(), "dummy manager did not clean up");
                    Player onQuit = core.trainingDummies().spawn(owner);
                    core.trainingDummies().onQuit(new org.bukkit.event.player.PlayerQuitEvent(owner, ""));
                    check(!onQuit.isValid() && Bukkit.getPlayer(onQuit.getUniqueId()) == null, "owner logout leaked dummy");
                    Player onEnd = core.trainingDummies().spawn(owner);
                    Bukkit.getPluginManager().callEvent(new kr.newgodwar.api.event.GameStateChangeEvent(core.game(), GameState.RUNNING, GameState.ENDED));
                    check(!onEnd.isValid() && core.trainingDummies().players().isEmpty(), "game end leaked dummy");
                    probe.getLogger().info("PASS dummy tick, healing, replacement, removal, logout and game-end cleanup");
                    probe.getLogger().info("COMMAND REGRESSION PASS");
                } catch (Throwable ex) { probe.getLogger().log(Level.SEVERE, "COMMAND REGRESSION FAILED", ex); }
                finally { cleanup(core, ownerEntity, stateField, previousState); }
            }, 5L);
        } catch (Throwable ex) {
            cleanup(core, ownerEntity, stateField, previousState);
            throw ex;
        }
    }

    private static void cleanup(NewGodWarPlugin core, TrainingDummyEntity owner, Field state, Object previous) {
        core.trainingDummies().remove(owner.player().getUniqueId());
        try {
            core.abilities().remove(owner.player());
            teams(core).remove(owner.player().getUniqueId());
            state.set(core.game(), previous);
            owner.remove();
        } catch (Exception ex) { core.getLogger().log(Level.SEVERE, "Dummy regression cleanup failed", ex); }
    }

    @SuppressWarnings("unchecked") private static Map<UUID, GodTeam> teams(NewGodWarPlugin core) throws Exception {
        return (Map<UUID, GodTeam>) field(core.game(), "teams").get(core.game());
    }
    private static Field field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); return field;
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static final class ProbeAbility extends BaseAbility {
        Player sight(AbilityPlayerContext context) { return targetPlayerInSight(context, context.player(), 10, false); }
        java.util.List<Player> nearby(AbilityPlayerContext context) { return nearbyPlayers(context, context.player(), 10, false); }
        void hit(AbilityPlayerContext context, Player target, double amount) { damage(context, target, amount, context.player()); }
    }
}
