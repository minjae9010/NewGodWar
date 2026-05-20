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
    description = "피격 시 낮은 확률로 공격자의 위치를 비틀어 섬 전투를 흔듭니다.",
    normalSkill = "없음",
    normalStoneCost = 0,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "피격 시 12.5% 확률로 공격자를 대각선 5블록 위치로 이동시킵니다.",
    grade = AbilityGrade.B
)
final class ErisAbility extends BaseAbility {
    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (!attacker && oneIn(8)) {
            opponent.teleport(context.player().getLocation().add(5, 0, 5));
        }
    }
}
