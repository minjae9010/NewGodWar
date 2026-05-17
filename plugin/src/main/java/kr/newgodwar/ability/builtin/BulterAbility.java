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
    id = "bulter",
    name = "집사",
    description = "폭발을 안정시켜 막습니다.",
    normalSkill = "없음",
    normalStoneCost = 0,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "블록 폭발 이벤트를 취소합니다.",
    grade = AbilityGrade.C
)
final class BulterAbility extends BaseAbility {
    @Override
    public void onBlockExplode(BlockExplodeEvent event) {
        event.setCancelled(true);
        Bukkit.broadcastMessage(ChatColor.GREEN + "집사에 의해 폭발이 진정되었습니다.");
    }
}
