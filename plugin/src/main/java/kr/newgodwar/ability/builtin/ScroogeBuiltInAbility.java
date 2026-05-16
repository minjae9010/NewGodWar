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
final class ScroogeBuiltInAbility extends BuiltInAbility {
    @Override
    public void onAssign(AbilityPlayerContext context) {
        GodTeam team = context.plugin().game().teamOf(context.player());
        if (team != null) {
            SCROOGE_TEAMS.add(team.id());
        }
    }
}
