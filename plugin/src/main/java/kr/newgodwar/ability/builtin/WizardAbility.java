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
    id = "wizard",
    name = "마법사",
    description = "주변 플레이어를 날리거나 신의 심판을 내립니다.",
    normalSkill = "주변 적을 밀쳐냅니다.",
    normalStoneCost = 5,
    advancedSkill = "주변 적을 띄운 뒤 번개를 내립니다.",
    advancedStoneCost = 10,
    passiveSkill = "없음"
)
final class WizardAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (use(context, player, 1, COBBLESTONE, 5, 180)) {
            push(player, nearbyPlayers(context, player, 10, false), 2.4D);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (use(context, player, 2, COBBLESTONE, 10, 300)) {
            judgment(context, player);
        }
    }
}
