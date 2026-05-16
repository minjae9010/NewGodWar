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
final class GoldspoonBuiltInAbility extends BuiltInAbility {
    @Override
    public void onRespawn(AbilityPlayerContext context, PlayerRespawnEvent event) {
        give(context.player(), RANDOM.nextInt(10) < 9 ? Material.GOLD_LEGGINGS : Material.DIAMOND_LEGGINGS, 1);
    }
}
