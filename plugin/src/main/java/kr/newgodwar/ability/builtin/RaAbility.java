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
    id = "ra",
    name = "라",
    description = "태양을 불러 자신을 강화하고 주변 적을 불태웁니다.",
    normalSkill = "시간을 낮으로 바꾸고 신속과 공격력 증가를 얻습니다.",
    normalStoneCost = 10,
    normalCooldownSeconds = 50,
    advancedSkill = "주변 적에게 실명과 화염을 부여합니다.",
    advancedStoneCost = 24,
    advancedCooldownSeconds = 115,
    passiveSkill = "화염 피해를 무시합니다.",
    grade = AbilityGrade.A
)
final class RaAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player) && setWorldTime(context, player, 1000)) {
            effect(player, PotionEffectType.SPEED, 10, 0);
            effect(player, "STRENGTH", "INCREASE_DAMAGE", 8, 0);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = nearbyPlayers(context, player, 8, false);
        if (targets.isEmpty()) {
            player.sendMessage("능력을 사용할 수 있는 대상이 없습니다.");
            return;
        }
        if (useAdvanced(context, player)) {
            for (Player target : targets) {
                target.setFireTicks(100);
                effect(target, PotionEffectType.BLINDNESS, 7, 0);
            }
        }
    }

    @Override
    public void onGenericDamage(AbilityPlayerContext context, EntityDamageEvent event) {
        if (fire(event.getCause())) {
            event.setCancelled(true);
            context.player().setFireTicks(0);
        }
    }
}
