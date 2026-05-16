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
final class AthenaBuiltInAbility extends BuiltInAbility {
    private int enchantTables = 2;

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (use(context, player, 1, COBBLESTONE, 5, 10)) {
            give(player, Material.BOOK, 3);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (enchantTables <= 0) {
            player.sendMessage("이 능력은 더이상 사용할 수 없습니다.");
            return;
        }
        if (use(context, player, 2, COBBLESTONE, 64, enchantTables > 1 ? 3 : 0)) {
            enchantTables--;
            give(player, Material.ENCHANTMENT_TABLE, 1);
            player.sendMessage("남은 교환 횟수 : " + enchantTables);
        }
    }

    @Override
    public void onDeath(AbilityPlayerContext context, PlayerDeathEvent event) {
        if (!event.getEntity().equals(context.player())) {
            context.player().setLevel(context.player().getLevel() + 1);
        }
    }
}
