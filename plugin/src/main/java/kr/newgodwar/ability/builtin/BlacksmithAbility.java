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
    description = "조약돌과 철괴를 상위 자원으로 천천히 가공해 장비 성장을 돕습니다.",
    normalSkill = "조약돌을 소모해 철괴 6개를 생성합니다.",
    normalStoneCost = 96,
    normalCooldownSeconds = 360,
    advancedSkill = "철괴 24개를 소모해 다이아몬드 3개를 생성합니다.",
    advancedStoneCost = 0,
    advancedCooldownSeconds = 720,
    passiveSkill = "없음",
    grade = AbilityGrade.B
)
final class BlacksmithAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            give(player, Material.IRON_INGOT, 6);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (use(context, player, 2, Material.IRON_INGOT, 24, 720)) {
            give(player, Material.DIAMOND, 3);
        }
    }
}
