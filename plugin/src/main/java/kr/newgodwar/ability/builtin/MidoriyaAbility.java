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
    id = "midoriya",
    name = "미도리야",
    description = "원 포 올을 준비한 뒤 맨손 공격으로 큰 피해를 줍니다.",
    normalSkill = "원 포 올을 준비합니다.",
    normalStoneCost = 50,
    normalCooldownSeconds = 150,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "준비 후 맨손 공격 시 큰 피해와 자기 디버프를 발생시킵니다.",
    grade = AbilityGrade.A
)
final class MidoriyaAbility extends BaseAbility {
    private boolean ready;

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (hasNormalCost(context, player) && readyNormal(context, player, 1)) {
            ready = true;
            player.sendMessage(ChatColor.YELLOW + "원" + ChatColor.GREEN + " 포 " + ChatColor.AQUA + "올" + ChatColor.WHITE + "이 준비되었습니다!");
        }
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        Player player = context.player();
        if (attacker && ready && player.getItemInHand().getType() == Material.AIR && useNormal(context, player)) {
            ready = false;
            event.setDamage(200.0D);
            effect(player, "NAUSEA", "CONFUSION", 12, 0);
            effect(player, PotionEffectType.HUNGER, 12, 0);
            effect(player, PotionEffectType.WEAKNESS, 12, 0);
            effect(player, "SLOWNESS", "SLOW", 12, 0);
        }
    }
}
