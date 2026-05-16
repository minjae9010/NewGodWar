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
    id = "aprodite",
    name = "아프로디테",
    description = "주변 플레이어를 자신의 위치로 끌어옵니다.",
    normalSkill = "주변 플레이어를 자신의 위치로 끌어옵니다.",
    normalStoneCost = 20,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "없음"
)
final class AproditeAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (use(context, player, 0, COBBLESTONE, 20, 100)) {
            pullAll(player, 20);
        }
    }
}
