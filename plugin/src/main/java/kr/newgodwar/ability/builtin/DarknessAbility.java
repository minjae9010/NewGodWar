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
    id = "darkness",
    name = "다크니스",
    description = "받는 피해를 줄이고 자신 공격은 피해가 없습니다.",
    normalSkill = "없음",
    normalStoneCost = 0,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "받는 피해가 크게 줄고 자신이 주는 피해는 0이 됩니다."
)
final class DarknessAbility extends BaseAbility {
    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (attacker) {
            event.setDamage(0.0D);
            return;
        }
        double multiplier = Math.max(0.0D, context.plugin().getConfig().getDouble(context.configPath("incoming-damage-multiplier"), 0.25D));
        event.setDamage(event.getDamage() * multiplier);
    }
}
