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
    id = "archer",
    name = "아처",
    description = "화살/활을 만들고 활 피해가 증가합니다.",
    normalSkill = "화살 4개를 생성합니다.",
    normalStoneCost = 5,
    normalCooldownSeconds = 20,
    advancedSkill = "활을 생성합니다.",
    advancedStoneCost = 15,
    advancedCooldownSeconds = 60,
    passiveSkill = "활 피해가 증가합니다.",
    grade = AbilityGrade.B
)
final class ArcherAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            give(player, Material.ARROW, 4);
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
        event.setDamage(event.getDamage() * 1.3D);
    }
}
