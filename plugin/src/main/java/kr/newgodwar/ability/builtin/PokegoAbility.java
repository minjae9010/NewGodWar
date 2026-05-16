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
final class PokegoAbility extends BaseAbility {
    private int steps;

    @Override
    public void onMove(AbilityPlayerContext context, PlayerMoveEvent event) {
        if (event.getFrom().distanceSquared(event.getTo()) > 0.01D) {
            steps++;
            if (steps >= 1000) {
                steps = 0;
                context.plugin().abilities().assignRandom(context.player());
                context.player().sendMessage(ChatColor.AQUA + "새 능력을 잡았습니다!");
            }
        }
    }
}
