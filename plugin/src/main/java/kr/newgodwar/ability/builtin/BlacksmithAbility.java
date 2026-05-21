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
    description = "조약돌을 철로, 철괴를 다이아몬드로 바꿔 장비 성장을 돕습니다.",
    normalSkill = "조약돌을 소모해 철괴 3개를 생성합니다.",
    normalStoneCost = 64,
    normalCooldownSeconds = 40,
    advancedSkill = "철괴 15개를 소모해 다이아몬드 1개를 생성합니다.",
    advancedStoneCost = 0,
    advancedCooldownSeconds = 180,
    passiveSkill = "없음",
    grade = AbilityGrade.B
)
final class BlacksmithAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            give(player, Material.IRON_INGOT, 3);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (use(context, player, 2, Material.IRON_INGOT, 15, 180)) {
            give(player, Material.DIAMOND, 1);
        }
    }
}
