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
    id = "reflection",
    name = "반사",
    description = "피격 시 일정 확률로 받은 피해를 공격자에게 되돌립니다.",
    normalSkill = "없음",
    normalStoneCost = 0,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "피격 시 33% 확률로 받은 피해를 공격자에게 반사합니다.",
    grade = AbilityGrade.A
)
final class ReflectionAbility extends BaseAbility {
    private boolean reflecting;

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (!attacker && !reflecting && event.getFinalDamage() > 0.0D && oneIn(3)) {
            reflecting = true;
            try {
                opponent.damage(event.getFinalDamage(), context.player());
            } finally {
                reflecting = false;
            }
        }
    }
}
