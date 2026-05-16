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
    id = "ares",
    name = "아레스",
    description = "공격 피해가 증가하고 일정 확률로 공격을 회피합니다.",
    normalSkill = "없음",
    normalStoneCost = 0,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "공격 피해가 증가하고 피격 시 확률로 회피합니다."
)
final class AresAbility extends BaseAbility {
    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (attacker) {
            event.setDamage(event.getDamage() * 1.5D);
        } else if (oneIn(10)) {
            event.setCancelled(true);
            context.player().sendMessage("회피했습니다!");
        }
    }
}
