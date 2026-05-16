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
@AbilityInfo(
    id = "priest",
    name = "사제",
    description = "자신 또는 팀원에게 무작위 축복을 부여합니다.",
    normalSkill = "자신에게 무작위 축복을 부여합니다.",
    normalStoneCost = 30,
    normalCooldownSeconds = 35,
    advancedSkill = "주변 아군에게 무작위 축복을 부여합니다.",
    advancedStoneCost = 45,
    advancedCooldownSeconds = 90,
    passiveSkill = "없음"
)
final class PriestAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            bless(player);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useAdvanced(context, player)) {
            for (Player target : nearbyPlayers(context, player, 30, true)) {
                bless(target);
            }
        }
    }
}
