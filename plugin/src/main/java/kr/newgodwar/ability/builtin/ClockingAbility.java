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
    id = "clocking",
    name = "클로킹",
    description = "투명화 후 공격 시 확률로 즉사시킵니다.",
    normalSkill = "잠시 투명화합니다.",
    normalStoneCost = 25,
    normalCooldownSeconds = 60,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "투명화 중 공격 시 확률로 큰 피해를 줍니다."
)
final class ClockingAbility extends BaseAbility {
    private boolean invisible;

    @Override
    public void onRemove(AbilityPlayerContext context) {
        context.player().removePotionEffect(PotionEffectType.INVISIBILITY);
    }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player, 0)) {
            invisible = true;
            effect(player, PotionEffectType.INVISIBILITY, 7, 0);
            later(context, 7, new Runnable() {
                @Override
                public void run() {
                    invisible = false;
                }
            });
        }
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (attacker && invisible) {
            context.player().removePotionEffect(PotionEffectType.INVISIBILITY);
            invisible = false;
            if (RANDOM.nextInt(5) == 0) {
                event.setDamage(100.0D);
            }
        }
    }
}
