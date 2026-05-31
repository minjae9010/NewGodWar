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
    passiveSkill = "피격 시 확률로 공격자에게 실명을 겁니다.",
    grade = AbilityGrade.A
)
final class BlinderAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = nearbyPlayers(context, player, 5, false);
        if (targets.isEmpty()) {
            player.sendMessage("능력을 사용할 수 있는 대상이 없습니다.");
            return;
        }
        if (useNormal(context, player, 0)) {
            for (Player target : targets) {
                effect(target, PotionEffectType.BLINDNESS, 10, 0);
            }
        }
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (!attacker && oneIn(10)) {
            effect(opponent, PotionEffectType.BLINDNESS, 7, 0);
        }
    }
}
