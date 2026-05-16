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
    id = "fisher",
    name = "노인과바다",
    description = "낚시로 잡동사니와 광물을 얻습니다.",
    normalSkill = "없음",
    normalStoneCost = 0,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "배정 시 낚싯대를 받고 낚시 성공 시 광물/아이템을 얻습니다."
)
final class FisherAbility extends BaseAbility {
    @Override
    public void onAssign(AbilityPlayerContext context) {
        give(context.player(), Material.FISHING_ROD, 1);
    }

    @Override
    public void onFish(AbilityPlayerContext context, PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            if (event.getCaught() != null) {
                event.getCaught().remove();
            }
            int roll = RANDOM.nextInt(100);
            if (roll < 5) {
                give(context.player(), Material.DIAMOND, 1);
            } else if (roll < 20) {
                give(context.player(), Material.LOG, 3);
            } else if (roll < 35) {
                give(context.player(), STAFF, 1);
            } else if (roll < 99) {
                give(context.player(), Material.IRON_INGOT, roll < 80 ? 1 : 2);
            } else {
                give(context.player(), Material.DIAMOND, 2);
            }
        }
    }
}
