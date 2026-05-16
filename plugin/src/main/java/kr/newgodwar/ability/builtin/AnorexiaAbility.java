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
    id = "anorexia",
    name = "거식증",
    description = "허기가 절반으로 유지됩니다.",
    normalSkill = "없음",
    normalStoneCost = 0,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "허기가 항상 절반으로 유지됩니다."
)
final class AnorexiaAbility extends BaseAbility {
    @Override
    public void onAssign(AbilityPlayerContext context) {
        context.player().setFoodLevel(10);
    }

    @Override
    public void onFoodLevelChange(AbilityPlayerContext context, FoodLevelChangeEvent event) {
        event.setFoodLevel(10);
    }
}
