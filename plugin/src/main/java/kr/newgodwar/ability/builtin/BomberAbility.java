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
    id = "bomber",
    name = "봄버",
    description = "보이지 않는 폭탄을 설치하고 원격 폭발시킵니다.",
    normalSkill = "바라보는 위치에 폭탄을 설치합니다.",
    normalStoneCost = 0,
    advancedSkill = "설치한 폭탄을 폭발시킵니다.",
    advancedStoneCost = 25,
    advancedCooldownSeconds = 30,
    passiveSkill = "없음",
    grade = AbilityGrade.A
)
final class BomberAbility extends BaseAbility {
    private Location bombLocation;

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        bombLocation = targetLocation(player, 5).add(0, 1, 0);
        player.sendMessage("해당 블럭에 폭탄이 설치되었습니다.");
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (bombLocation == null) {
            player.sendMessage("TNT가 설치되지 않았습니다.");
            return;
        }
        if (useAdvanced(context, player, 0)) {
            player.getWorld().createExplosion(bombLocation, 2.0F, true);
            bombLocation = null;
            player.sendMessage("TNT가 폭발했습니다!");
        }
    }
}
