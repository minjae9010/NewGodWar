package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

@AbilityInfo(
    id = "shinsaimdang",
    name = "신사임당",
    description = "섬세한 그림과 돌봄으로 아군을 안정적으로 보조합니다.",
    normalSkill = "염료와 꽃을 만들고 자신에게 재생을 부여합니다.",
    normalStoneCost = 8,
    normalCooldownSeconds = 55,
    advancedSkill = "주변 아군의 체력을 조금 회복하고 재생을 부여합니다.",
    advancedStoneCost = 20,
    advancedCooldownSeconds = 115,
    passiveSkill = "피격 시 가끔 공격자를 약화시킵니다.",
    grade = AbilityGrade.B
)
final class ShinSaimdangAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (!useNormal(context, player)) {
            return;
        }
        player.getInventory().addItem(new ItemStack(Material.RED_ROSE, 3));
        player.getInventory().addItem(new ItemStack(Material.INK_SACK, 4));
        effect(player, PotionEffectType.REGENERATION, 8, 0);
        player.sendMessage(ChatColor.LIGHT_PURPLE + "초충도의 생기가 피어납니다.");
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = nearbyPlayers(context, player, 8, true);
        targets.add(player);
        if (!useAdvanced(context, player)) {
            return;
        }
        for (Player target : targets) {
            target.setHealth(Math.min(target.getMaxHealth(), target.getHealth() + 5.0D));
            effect(target, PotionEffectType.REGENERATION, 8, 0);
        }
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (!attacker && oneIn(5)) {
            effect(opponent, PotionEffectType.WEAKNESS, 8, 0);
        }
    }
}
