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
    id = "tajja",
    name = "타짜",
    description = "검을 숨겨 맨손 공격에 검 피해를 싣습니다.",
    normalSkill = "인벤토리의 검 하나를 숨깁니다.",
    normalStoneCost = 10,
    normalCooldownSeconds = 60,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "맨손 공격에 숨긴 검 피해를 제한 횟수만큼 싣습니다.",
    grade = AbilityGrade.B
)
final class TajjaAbility extends BaseAbility {
    private int tajjaDamage;
    private int tajjaUses;

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            stealSword(player);
        }
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (attacker && context.player().getItemInHand().getType() == Material.AIR && tajjaDamage > 0) {
            event.setDamage(tajjaDamage);
            tajjaUses--;
            if (tajjaUses <= 0) {
                tajjaDamage = 0;
            }
        }
    }

    private void stealSword(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && isSword(item.getType())) {
                tajjaDamage = swordDamage(item.getType());
                tajjaUses = 10;
                player.getInventory().removeItem(new ItemStack(item.getType(), 1));
                player.sendMessage("손은 눈보다 빠르다.");
                return;
            }
        }
        player.sendMessage("소비할 검이 인벤토리에 없습니다.");
    }
}
