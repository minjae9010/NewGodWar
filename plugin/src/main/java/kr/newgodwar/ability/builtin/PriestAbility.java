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
    id = "priest",
    name = "사제",
    description = "자신과 팀원에게 여러 전투 축복 중 하나 이상을 무작위로 부여합니다.",
    normalSkill = "자신에게 30초짜리 무작위 축복을 하나 이상 부여합니다.",
    normalStoneCost = 22,
    normalCooldownSeconds = 45,
    advancedSkill = "팀원 전체에게 30초짜리 무작위 축복을 하나 이상 부여합니다.",
    advancedStoneCost = 40,
    advancedCooldownSeconds = 105,
    passiveSkill = "없음",
    grade = AbilityGrade.A
)
final class PriestAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            bless(player);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = alliedPlayers(context, player, true);
        if (targets.isEmpty()) {
            player.sendMessage("능력을 사용할 팀원이 없습니다!");
            return;
        }
        if (useAdvanced(context, player)) {
            for (Player target : targets) {
                bless(target);
            }
        }
    }

    private void bless(Player player) {
        boolean applied = false;
        if (RANDOM.nextBoolean()) {
            effect(player, "RESISTANCE", "DAMAGE_RESISTANCE", 30, 0);
            applied = true;
        }
        if (RANDOM.nextBoolean()) {
            effect(player, "STRENGTH", "INCREASE_DAMAGE", 30, 0);
            applied = true;
        }
        if (RANDOM.nextBoolean()) {
            effect(player, PotionEffectType.REGENERATION, 30, 0);
            applied = true;
        }
        if (RANDOM.nextBoolean()) {
            effect(player, PotionEffectType.SPEED, 30, 0);
            applied = true;
        }
        if (RANDOM.nextBoolean()) {
            effect(player, "HASTE", "FAST_DIGGING", 30, 0);
            applied = true;
        }
        if (!applied) {
            effect(player, PotionEffectType.REGENERATION, 30, 0);
        }
    }
}
