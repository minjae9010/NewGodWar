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
    id = "goldspoon",
    name = "금수저",
    description = "리스폰할 때 레깅스를 받습니다.",
    normalSkill = "없음",
    normalStoneCost = 0,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "리스폰할 때 금 또는 다이아몬드 레깅스를 받습니다.",
    grade = AbilityGrade.B
)
final class GoldspoonAbility extends BaseAbility {
    @Override
    public void onRespawn(AbilityPlayerContext context, PlayerRespawnEvent event) {
        give(context.player(), rollChance(9, 10) ? material("GOLDEN_LEGGINGS", "GOLD_LEGGINGS") : Material.DIAMOND_LEGGINGS, 1);
    }
}
