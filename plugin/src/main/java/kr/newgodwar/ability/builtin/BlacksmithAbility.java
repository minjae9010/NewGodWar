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
    id = "blacksmith",
    name = "대장장이",
    description = "코블스톤을 철로, 철을 다이아몬드로 바꿉니다.",
    normalSkill = "코블스톤을 철괴로 바꿉니다.",
    normalStoneCost = 70,
    normalCooldownSeconds = 300,
    advancedSkill = "철괴를 다이아몬드로 바꿉니다.",
    advancedStoneCost = 0,
    advancedCooldownSeconds = 600,
    passiveSkill = "없음"
)
final class BlacksmithAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            give(player, Material.IRON_INGOT, 10);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (use(context, player, 2, Material.IRON_INGOT, 20, 600)) {
            give(player, Material.DIAMOND, 5);
        }
    }
}
