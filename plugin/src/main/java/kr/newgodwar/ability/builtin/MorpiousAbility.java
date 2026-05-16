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
    id = "morpious",
    name = "모르피우스",
    description = "지정한 적을 수면 상태로 만듭니다.",
    normalSkill = "지정한 적에게 실명과 강한 감속을 부여합니다.",
    normalStoneCost = 20,
    normalCooldownSeconds = 100,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "타깃 지정 명령을 사용할 수 있습니다."
)
final class MorpiousAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        Player target = targetPlayerInSight(context, player, 20, false);
        if (target == null) {
            return;
        }
        if (useNormal(context, player, 0)) {
            sleepTarget(target);
        }
    }
}
