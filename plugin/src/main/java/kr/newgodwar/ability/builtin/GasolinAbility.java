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
    id = "gasolin",
    name = "가솔린기관",
    description = "화염 피해를 받으면 빨라집니다.",
    normalSkill = "없음",
    normalStoneCost = 0,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "화염 피해를 받으면 신속 효과를 얻습니다."
)
final class GasolinAbility extends BaseAbility {
    @Override
    public void onGenericDamage(AbilityPlayerContext context, EntityDamageEvent event) {
        if (fire(event.getCause()) && !context.player().hasPotionEffect(PotionEffectType.SPEED)) {
            effect(context.player(), PotionEffectType.SPEED, 10, 0);
        }
    }
}
