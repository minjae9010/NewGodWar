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
    id = "jujak",
    name = "주작",
    description = "화염을 무시하고 긴 비행으로 공중 섬 전장을 가로지릅니다.",
    normalSkill = "12초 동안 비행합니다.",
    normalStoneCost = 18,
    normalCooldownSeconds = 100,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "화염 피해를 무시하지만 익사 피해를 2배로 받습니다.",
    grade = AbilityGrade.A
)
final class JujakAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player, 0)) {
            fly(context, player, 12);
        }
    }

    private void fly(final AbilityPlayerContext context, final Player player, int seconds) {
        player.setAllowFlight(true);
        player.setFlying(true);
        later(context, seconds, "비행 종료", "비행 종료", () -> {
            player.setFlying(false);
            player.setAllowFlight(false);
        });
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
