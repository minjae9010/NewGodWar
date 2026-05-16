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
    id = "asclepius",
    name = "아스클리피어스",
    description = "자신 또는 주변 아군을 완전히 회복합니다.",
    normalSkill = "자신을 완전히 회복합니다.",
    normalStoneCost = 10,
    normalCooldownSeconds = 60,
    advancedSkill = "주변 아군을 완전히 회복합니다.",
    advancedStoneCost = 15,
    advancedCooldownSeconds = 120,
    passiveSkill = "없음"
)
final class AsclepiusAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            heal(player);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useAdvanced(context, player)) {
            for (Player target : nearbyPlayers(context, player, 5, true)) {
                heal(target);
            }
        }
    }
}
