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
    id = "aeolus",
    name = "아이올로스",
    description = "아군에게 바람의 축복을 주거나 적을 밀쳐냅니다.",
    normalSkill = "주변 아군에게 신속과 재생을 부여합니다.",
    normalStoneCost = 10,
    normalCooldownSeconds = 60,
    advancedSkill = "주변 적을 밀쳐내고 약화/감속을 부여합니다.",
    advancedStoneCost = 20,
    advancedCooldownSeconds = 150,
    passiveSkill = "없음"
)
final class AeolusAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            teamBuff(context, player);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useAdvanced(context, player)) {
            List<Player> targets = nearbyPlayers(context, player, 10, false);
            push(player, targets, 2.4D);
            for (Player target : targets) {
                effect(target, PotionEffectType.WEAKNESS, 5, 0);
                effect(target, PotionEffectType.SLOW, 5, 0);
            }
        }
    }
}
