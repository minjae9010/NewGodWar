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
    id = "damocles",
    name = "다모클",
    description = "머리 위의 검처럼 피격 시 희박한 확률로 즉사합니다.",
    normalSkill = "없음",
    normalStoneCost = 0,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "피격 시 희박한 확률로 즉사합니다.",
    grade = AbilityGrade.D
)
final class DamoclesAbility extends BaseAbility {
    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (!attacker && oneIn(100)) {
            event.setDamage(Math.max(event.getDamage(), context.player().getHealth() + 1000.0D));
            context.player().sendMessage(ChatColor.DARK_RED + "다모클의 검이 떨어졌습니다.");
        }
    }
}
