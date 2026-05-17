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
    description = "익힌 돼지고기를 만들고 허기와 재생에 강합니다.",
    normalSkill = "익힌 돼지고기를 생성합니다.",
    normalStoneCost = 10,
    normalCooldownSeconds = 20,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "재생 효과를 유지하고 허기가 감소하지 않습니다."
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
