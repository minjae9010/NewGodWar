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
    id = "galbi",
    name = "명륜진사",
    description = "구운 고기를 만들고 허기 감소를 막으며 상시 재생으로 버팁니다.",
    normalSkill = "익힌 돼지고기 3개를 생성합니다.",
    normalStoneCost = 12,
    normalCooldownSeconds = 30,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "재생 효과를 유지하고 허기가 항상 20으로 유지됩니다.",
    grade = AbilityGrade.B
)
final class GalbiAbility extends BaseAbility {
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
            give(player, material("COOKED_PORKCHOP", "GRILLED_PORK"), 3);
        }
    }

    @Override
    public void onFoodLevelChange(AbilityPlayerContext context, FoodLevelChangeEvent event) {
        event.setCancelled(true);
        context.player().setFoodLevel(20);
    }

    @Override
    public void onRespawn(AbilityPlayerContext context, PlayerRespawnEvent event) {
        effect(context.player(), PotionEffectType.REGENERATION, 24 * 60 * 60, 0);
    }
}
