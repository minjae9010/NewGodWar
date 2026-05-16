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
final class HermioneBuiltInAbility extends BuiltInAbility {
    @Override
    public void onAssign(AbilityPlayerContext context) {
        giveSpellBook(context.player(), false);
    }

    @Override
    public void onChat(AbilityPlayerContext context, AsyncPlayerChatEvent event) {
        castSpell(context, event.getMessage(), false);
    }

    @Override
    public void onGenericDamage(AbilityPlayerContext context, EntityDamageEvent event) {
        if (invincible) {
            event.setCancelled(true);
        }
    }
}
