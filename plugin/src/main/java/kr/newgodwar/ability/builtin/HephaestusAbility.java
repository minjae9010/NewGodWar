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
    id = "hephaestus",
    name = "헤파이토스",
    description = "짧은 용암 장악과 화염 면역으로 근접 전장을 압박합니다.",
    normalSkill = "5블록 안의 바라보는 위치에 2초 동안 용암을 만듭니다.",
    normalStoneCost = 4,
    normalCooldownSeconds = 20,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "화염 피해를 무시하지만 익사 피해를 2배로 받습니다.",
    grade = AbilityGrade.A
)
final class HephaestusAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        lava(player, context);
    }

    @Override
    public void onGenericDamage(AbilityPlayerContext context, EntityDamageEvent event) {
        if (fire(event.getCause())) {
            event.setCancelled(true);
            context.player().setFireTicks(0);
        } else if (event.getCause() == EntityDamageEvent.DamageCause.DROWNING) {
            event.setDamage(event.getDamage() * 2.0D);
        }
    }
}
