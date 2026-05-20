package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

@AbilityInfo(
    id = "jangyeongsil",
    name = "장영실",
    description = "과학 장치로 전장을 보조하고 실용적인 자원을 만들어냅니다.",
    normalSkill = "레드스톤 장치를 만들고 잠시 성급함을 얻습니다.",
    normalStoneCost = 16,
    normalCooldownSeconds = 70,
    advancedSkill = "주변 아군에게 성급함, 신속, 저항을 부여합니다.",
    advancedStoneCost = 30,
    advancedCooldownSeconds = 145,
    passiveSkill = "철괴를 들고 공격하면 가끔 내구도 좋은 타격을 가합니다.",
    grade = AbilityGrade.A
)
final class JangYeongSilAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (!useNormal(context, player)) {
            return;
        }
        player.getInventory().addItem(new ItemStack(Material.REDSTONE, 8));
        player.getInventory().addItem(new ItemStack(Material.PISTON_BASE, 1));
        effect(player, "HASTE", "FAST_DIGGING", 12, 1);
        player.sendMessage(ChatColor.AQUA + "과학 장치를 제작했습니다.");
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = nearbyPlayers(context, player, 10, true);
        targets.add(player);
        if (!useAdvanced(context, player)) {
            return;
        }
        for (Player target : targets) {
            effect(target, "HASTE", "FAST_DIGGING", 12, 1);
            effect(target, PotionEffectType.SPEED, 8, 0);
            effect(target, "RESISTANCE", "DAMAGE_RESISTANCE", 6, 0);
        }
        player.sendMessage(ChatColor.AQUA + "장영실의 장치가 아군의 전투 준비를 돕습니다.");
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, org.bukkit.event.entity.EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (attacker && holding(context.player(), Material.IRON_INGOT) && oneIn(4)) {
            event.setDamage(event.getDamage() + 2.0D);
            effect(opponent, "SLOWNESS", "SLOW", 3, 0);
        }
    }
}
