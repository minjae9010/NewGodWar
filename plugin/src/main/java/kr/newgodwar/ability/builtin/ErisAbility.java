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
    id = "eris",
    name = "에리스",
    description = "피격 시 확률로 공격자를 밀쳐냅니다.",
    normalSkill = "없음",
    normalStoneCost = 0,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "피격 시 확률로 공격자를 다른 위치로 이동시킵니다."
)
final class ErisAbility extends BaseAbility {
    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (!attacker && oneIn(5)) {
            opponent.teleport(context.player().getLocation().add(5, 0, 5));
        }
    }
}
