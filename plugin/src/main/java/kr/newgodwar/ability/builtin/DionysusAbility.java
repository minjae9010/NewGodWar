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
    id = "dionysus",
    name = "디오니소스",
    description = "피격 시 확률로 공격자를 취하게 합니다.",
    normalSkill = "없음",
    normalStoneCost = 0,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "피격 시 확률로 공격자에게 감속, 약화, 혼란을 부여합니다."
)
final class DionysusAbility extends BaseAbility {
    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (!attacker && RANDOM.nextInt(20) <= 2) {
            effect(opponent, PotionEffectType.SLOW, 10, 0);
            effect(opponent, PotionEffectType.WEAKNESS, 10, 0);
            effect(opponent, PotionEffectType.CONFUSION, 12, 0);
        }
    }
}
