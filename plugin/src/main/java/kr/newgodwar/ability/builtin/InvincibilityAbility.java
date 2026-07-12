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
    id = "invincibility",
    name = "무적",
    description = "짧은 완전 무적과 긴 재생으로 위기 상황을 버팁니다.",
    normalSkill = "7초 동안 모든 피해를 무시합니다.",
    normalStoneCost = 35,
    normalCooldownSeconds = 75,
    advancedSkill = "25초 동안 재생 효과를 얻습니다.",
    advancedStoneCost = 40,
    advancedCooldownSeconds = 100,
    passiveSkill = "무적 상태일 때 피해와 화염을 취소합니다.",
    grade = AbilityGrade.S
)
final class InvincibilityAbility extends BaseAbility {
    private boolean invincible;

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            invincible = true;
            laterCleanup(context, 7, "무적 종료", "무적 종료", () -> invincible = false);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useAdvanced(context, player)) {
            effect(player, PotionEffectType.REGENERATION, 25, 0);
        }
    }

    @Override
    public void onGenericDamage(AbilityPlayerContext context, EntityDamageEvent event) {
        if (invincible) {
            event.setCancelled(true);
            context.player().setFireTicks(0);
        }
    }
}
