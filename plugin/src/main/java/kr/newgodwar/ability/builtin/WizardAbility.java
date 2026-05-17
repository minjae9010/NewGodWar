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
    normalSkill = "주변 플레이어를 밀쳐냅니다.",
    normalStoneCost = 5,
    normalCooldownSeconds = 180,
    advancedSkill = "주변 플레이어를 띄운 뒤 번개를 내립니다.",
    advancedStoneCost = 10,
    advancedCooldownSeconds = 300,
    passiveSkill = "없음"
)
final class WizardAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = nearbyPlayers(player, 10);
        if (targets.isEmpty()) {
            player.sendMessage("능력을 사용할 수 있는 대상이 없습니다.");
            return;
        }
        if (useNormal(context, player)) {
            push(context, player, targets, 2.4D, 4L);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (nearbyPlayers(player, 5).isEmpty()) {
            player.sendMessage("능력을 사용할 수 있는 대상이 없습니다.");
            return;
        }
        if (useAdvanced(context, player)) {
            judgment(context, player);
        }
    }
}
