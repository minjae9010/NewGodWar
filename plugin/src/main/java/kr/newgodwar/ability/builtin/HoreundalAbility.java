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
    id = "horeundal",
    name = "호른달",
    description = "현재 위치를 기억하고 잠시 후 되돌아옵니다.",
    normalSkill = "현재 위치를 저장하고 잠시 후 되돌아옵니다.",
    normalStoneCost = 15,
    normalCooldownSeconds = 80,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "없음",
    grade = AbilityGrade.B
)
final class HoreundalAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player, 0)) {
            recall(context, player);
        }
    }
}
