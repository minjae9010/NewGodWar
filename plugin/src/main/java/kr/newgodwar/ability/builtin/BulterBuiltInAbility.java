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
final class BulterBuiltInAbility extends BuiltInAbility {
    @Override
    public void onBlockExplode(BlockExplodeEvent event) {
        event.setCancelled(true);
        Bukkit.broadcastMessage(ChatColor.GREEN + "집사에 의해 폭발이 진정되었습니다.");
    }
}
