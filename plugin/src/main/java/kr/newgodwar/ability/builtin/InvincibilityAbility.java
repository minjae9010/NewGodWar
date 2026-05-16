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
    description = "잠시 무적이 되거나 재생 효과를 얻습니다.",
    normalSkill = "잠시 모든 피해를 무시합니다.",
    normalStoneCost = 30,
    normalCooldownSeconds = 50,
    advancedSkill = "재생 효과를 얻습니다.",
    advancedStoneCost = 50,
    advancedCooldownSeconds = 90,
    passiveSkill = "무적 상태일 때 피해를 취소합니다."
)
final class InvincibilityAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            invincible = true;
            later(context, 7, new Runnable() {
                @Override
                public void run() {
                    invincible = false;
                }
            });
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
