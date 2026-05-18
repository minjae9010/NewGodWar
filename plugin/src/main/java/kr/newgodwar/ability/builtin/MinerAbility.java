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
    id = "miner",
    name = "광부",
    description = "코블스톤 채굴 보너스와 곡괭이 고정 피해를 가집니다.",
    normalSkill = "없음",
    normalStoneCost = 0,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "상시 성급함 효과를 받고 코블스톤 채굴 보너스와 곡괭이 고정 피해를 가집니다.",
    grade = AbilityGrade.A
)
final class MinerAbility extends BaseAbility {
    @Override
    public void onAssign(AbilityPlayerContext context) {
        effect(context.player(), "HASTE", "FAST_DIGGING", 24 * 60 * 60, 0);
    }

    @Override
    public void onRemove(AbilityPlayerContext context) {
        removeEffect(context.player(), "HASTE", "FAST_DIGGING");
    }

    @Override
    public void onTick(AbilityPlayerContext context) {
        effect(context.player(), "HASTE", "FAST_DIGGING", 3, 0);
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (attacker && isPickaxe(context.player().getItemInHand().getType())) {
            event.setDamage(4.0D);
        }
    }

    @Override
    public void onBlockBreak(AbilityPlayerContext context, BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.COBBLESTONE && oneIn(33)) {
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(Material.COBBLESTONE, 9));
        }
    }
}
