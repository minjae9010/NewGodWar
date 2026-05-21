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
    description = "실용적인 부품과 전투 보조 장치로 아군의 채굴과 진입을 돕습니다.",
    normalSkill = "부품을 쌓고 3스택이 되면 철 곡괭이를 제작합니다.",
    normalStoneCost = 12,
    normalCooldownSeconds = 60,
    advancedSkill = "주변 아군에게 성급함, 신속, 저항을 부여합니다.",
    advancedStoneCost = 30,
    advancedCooldownSeconds = 145,
    passiveSkill = "곡괭이를 들고 공격하면 가끔 추가 피해와 감속을 부여합니다.",
    grade = AbilityGrade.A
)
final class JangYeongSilAbility extends BaseAbility {
    private static final int PICKAXE_PARTS_REQUIRED = 3;

    private int pickaxeParts;

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (!useNormal(context, player)) {
            return;
        }
        pickaxeParts++;
        effect(player, "HASTE", "FAST_DIGGING", 12, 1);
        if (pickaxeParts >= PICKAXE_PARTS_REQUIRED) {
            pickaxeParts = 0;
            player.getInventory().addItem(new ItemStack(Material.IRON_PICKAXE, 1));
            player.sendMessage(ChatColor.AQUA + "장영실의 부품이 완성되어 철 곡괭이를 제작했습니다.");
            return;
        }
        player.sendMessage(ChatColor.AQUA + "철 곡괭이 부품을 제작했습니다. "
            + ChatColor.GRAY + "(" + pickaxeParts + "/" + PICKAXE_PARTS_REQUIRED + ")");
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
        if (attacker && isPickaxe(context.player().getItemInHand().getType()) && oneIn(4)) {
            event.setDamage(event.getDamage() + 2.0D);
            effect(opponent, "SLOWNESS", "SLOW", 3, 0);
        }
    }
}
