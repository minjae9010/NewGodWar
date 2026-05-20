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
    id = "demeter",
    name = "데메테르",
    description = "식량을 만들고 허기 감소를 막으며 상시 재생으로 버팁니다.",
    normalSkill = "빵 10개를 생성합니다.",
    normalStoneCost = 12,
    normalCooldownSeconds = 25,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "재생 효과를 유지하고 허기가 항상 20으로 유지됩니다.",
    grade = AbilityGrade.A
)
final class DemeterAbility extends BaseAbility {
    @Override
    public void onAssign(AbilityPlayerContext context) {
        effect(context.player(), PotionEffectType.REGENERATION, 24 * 60 * 60, 0);
        context.player().setFoodLevel(20);
    }

    @Override
    public void onRemove(AbilityPlayerContext context) {
        context.player().removePotionEffect(PotionEffectType.REGENERATION);
    }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player, 0)) {
            give(player, Material.BREAD, 10);
        }
    }

    @Override
    public void onFoodLevelChange(AbilityPlayerContext context, FoodLevelChangeEvent event) {
        event.setCancelled(true);
        context.player().setFoodLevel(20);
    }

    @Override
    public void onRespawn(AbilityPlayerContext context, PlayerRespawnEvent event) {
        respawnEffect(context, PotionEffectType.REGENERATION, 24 * 60 * 60, 0);
        respawnFoodLevel(context, 20);
    }
}
