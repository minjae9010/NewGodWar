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
    id = "blinder",
    name = "블라인더",
    description = "주변 적이나 공격자에게 실명을 겁니다.",
    normalSkill = "주변 적에게 실명을 겁니다.",
    normalStoneCost = 10,
    normalCooldownSeconds = 30,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "피격 시 확률로 공격자에게 실명을 겁니다."
)
final class BlinderAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player, 0)) {
            for (Player target : nearbyPlayers(context, player, 5, false)) {
                effect(target, PotionEffectType.BLINDNESS, 8, 0);
            }
        }
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (!attacker && RANDOM.nextInt(10) == 0) {
            effect(opponent, PotionEffectType.BLINDNESS, 4, 0);
        }
    }
}
