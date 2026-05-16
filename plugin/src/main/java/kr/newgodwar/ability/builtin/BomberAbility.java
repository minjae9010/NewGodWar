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
final class BomberAbility extends BaseAbility {
    private Location bombLocation;

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        bombLocation = targetBlock(player, 5).getLocation().add(0, 1, 0);
        player.sendMessage("해당 블럭에 폭탄이 설치되었습니다.");
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (use(context, player, 0, COBBLESTONE, 25, 30) && bombLocation != null) {
            player.getWorld().createExplosion(bombLocation, 2.0F, true);
            bombLocation = null;
        }
    }
}
