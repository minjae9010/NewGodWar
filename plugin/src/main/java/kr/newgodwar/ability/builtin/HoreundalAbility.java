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
    id = "horeundal",
    name = "호른달",
    description = "위험한 섬 진입 후 10초 뒤 저장한 위치로 귀환합니다.",
    normalSkill = "현재 위치를 저장하고 10초 후 되돌아옵니다.",
    normalStoneCost = 18,
    normalCooldownSeconds = 100,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "없음",
    grade = AbilityGrade.B
)
final class HoreundalAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player, 0)) {
            recall(context, player);
        }
    }

    private void recall(final AbilityPlayerContext context, final Player player) {
        final Location location = player.getLocation();
        later(context, 10, "귀환 발동", "귀환 발동", () -> {
            player.teleport(location);
            effect(player, PotionEffectType.INVISIBILITY, 6, 0);
        });
    }
}
