package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

@AbilityInfo(
    id = "sejong",
    name = "세종대왕",
    description = "집현전의 지혜로 아군을 강화하고 훈민정음의 칙령으로 적의 능력을 봉인합니다.",
    normalSkill = "자신과 주변 아군에게 재생, 저항, 성급함을 부여하고 경험 레벨을 나눕니다.",
    normalStoneCost = 40,
    normalCooldownSeconds = 180,
    advancedSkill = "바라보는 적의 능력을 봉인하고 실명, 약화, 감속을 부여합니다.",
    advancedStoneCost = 64,
    advancedCooldownSeconds = 300,
    passiveSkill = "책을 들고 공격하면 피해가 증가하고, 배정 시 책을 받습니다.",
    grade = AbilityGrade.S
)
final class SejongAbility extends BaseAbility {
    @Override
    public void onAssign(AbilityPlayerContext context) {
        give(context.player(), Material.BOOK, 1);
    }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = nearbyPlayers(context, player, 10, true);
        targets.add(player);
        if (!useNormal(context, player)) {
            return;
        }
        for (Player target : targets) {
            effect(target, PotionEffectType.REGENERATION, 8, 1);
            effect(target, "RESISTANCE", "DAMAGE_RESISTANCE", 8, 0);
            effect(target, "HASTE", "FAST_DIGGING", 14, 1);
            target.setLevel(target.getLevel() + 2);
        }
        player.sendMessage(ChatColor.AQUA + "집현전의 지혜가 아군에게 퍼졌습니다.");
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        Player target = targetPlayerInSight(context, player, 28, false);
        if (target == null) {
            return;
        }
        if (!useAdvanced(context, player)) {
            return;
        }
        if (context.plugin().abilities().session(target) != null) {
            context.plugin().abilities().suppressAbility(target, 10);
        }
        effect(target, PotionEffectType.BLINDNESS, 6, 0);
        effect(target, PotionEffectType.WEAKNESS, 10, 0);
        effect(target, "SLOWNESS", "SLOW", 10, 2);
        target.sendMessage(ChatColor.DARK_PURPLE + "훈민정음의 칙령이 능력을 봉합니다.");
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (attacker && holding(context.player(), Material.BOOK)) {
            event.setDamage(event.getDamage() * 1.35D);
        }
    }
}
