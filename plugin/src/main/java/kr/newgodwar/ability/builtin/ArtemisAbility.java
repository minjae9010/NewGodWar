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
    id = "artemis",
    name = "아르테미스",
    description = "화살과 활을 만들며 화살로 즉사 확률을 가집니다.",
    normalSkill = "화살을 생성합니다.",
    normalStoneCost = 7,
    normalCooldownSeconds = 20,
    advancedSkill = "활을 생성합니다.",
    advancedStoneCost = 15,
    advancedCooldownSeconds = 180,
    passiveSkill = "화살 적중 시 확률로 큰 피해를 줍니다.",
    grade = AbilityGrade.S
)
final class ArtemisAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            give(player, Material.ARROW, 1);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useAdvanced(context, player)) {
            give(player, Material.BOW, 1);
        }
    }

    @Override
    public void onProjectileHit(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player victim) {
        if (rollChance(2, 20)) {
            event.setDamage(100.0D);
        }
    }
}
