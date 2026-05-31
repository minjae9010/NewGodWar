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
    id = "witch",
    name = "마녀",
    description = "주변 적과 공격자에게 저주를 겁니다.",
    normalSkill = "주변 적에게 저주를 겁니다.",
    normalStoneCost = 15,
    normalCooldownSeconds = 60,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "피격 시 확률로 공격자에게 저주를 겁니다.",
    grade = AbilityGrade.A
)
final class WitchAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = nearbyPlayers(context, player, 10, false);
        if (targets.isEmpty()) {
            player.sendMessage("능력을 사용할 수 있는 대상이 없습니다.");
            return;
        }
        if (useNormal(context, player, 0)) {
            curse(targets);
        }
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (!attacker && oneIn(14)) {
            curse(opponent);
        }
    }

    private void curse(List<Player> players) {
        for (Player player : players) {
            curse(player);
        }
    }

    private void curse(Player player) {
        effect(player, PotionEffectType.HUNGER, 12, 0);
        effect(player, PotionEffectType.POISON, 12, 0);
        effect(player, "SLOWNESS", "SLOW", 12, 0);
        effect(player, "MINING_FATIGUE", "SLOW_DIGGING", 12, 0);
    }
}
