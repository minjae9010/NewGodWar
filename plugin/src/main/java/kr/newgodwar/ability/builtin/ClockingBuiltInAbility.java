package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import kr.newgodwar.game.GodTeam;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;
final class ClockingBuiltInAbility extends BuiltInAbility {
    private boolean invisible;

    @Override
    public void onRemove(AbilityPlayerContext context) {
        context.player().removePotionEffect(PotionEffectType.INVISIBILITY);
    }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (use(context, player, 0, COBBLESTONE, 25, 60)) {
            invisible = true;
            effect(player, PotionEffectType.INVISIBILITY, 7, 0);
            later(context, 7, new Runnable() {
                @Override
                public void run() {
                    invisible = false;
                }
            });
        }
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (attacker && invisible) {
            context.player().removePotionEffect(PotionEffectType.INVISIBILITY);
            invisible = false;
            if (RANDOM.nextInt(5) == 0) {
                event.setDamage(100.0D);
            }
        }
    }
}
