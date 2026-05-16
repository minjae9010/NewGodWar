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
final class FisherBuiltInAbility extends BuiltInAbility {
    @Override
    public void onAssign(AbilityPlayerContext context) {
        give(context.player(), Material.FISHING_ROD, 1);
    }

    @Override
    public void onFish(AbilityPlayerContext context, PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            if (event.getCaught() != null) {
                event.getCaught().remove();
            }
            int roll = RANDOM.nextInt(100);
            if (roll <= 4) {
                give(context.player(), Material.DIAMOND, 1);
            } else if (roll <= 19) {
                give(context.player(), Material.LOG, 3);
            } else if (roll <= 34) {
                give(context.player(), STAFF, 1);
            } else if (roll <= 98) {
                give(context.player(), Material.IRON_INGOT, roll <= 79 ? 1 : 2);
            } else {
                give(context.player(), Material.DIAMOND, 2);
            }
        }
    }
}
