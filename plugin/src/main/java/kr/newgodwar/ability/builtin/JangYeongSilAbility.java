package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import kr.newgodwar.ability.feedback.AbilityStyle;
import kr.newgodwar.ability.feedback.AbilityTheme;
import kr.newgodwar.ability.feedback.EffectCue;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

@AbilityInfo(
    id = "jangyeongsil",
    name = "장영실",
    description = "실용적인 부품과 전투 보조 장치로 아군의 채굴과 진입을 돕습니다.",
    normalSkill = "블레이즈 막대기 좌클릭: 부품을 1개 만들고 12초 동안 성급함 II를 얻습니다. 부품 3개가 모이면 철 곡괭이를 만듭니다.",
    normalStoneCost = 12,
    normalCooldownSeconds = 60,
    advancedSkill = "블레이즈 막대기 우클릭: 자신과 반경 10블록 아군에게 성급함 II 12초, 신속 10초, 저항 8초를 줍니다.",
    advancedStoneCost = 30,
    advancedCooldownSeconds = 145,
    passiveSkill = "곡괭이로 공격하면 25% 확률로 피해 2를 더하고 6초 감속을 줍니다.",
    grade = AbilityGrade.A
)
final class JangYeongSilAbility extends BaseAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.CRAFT)
        // Assembly and the advanced device use source scenes; allies receive separate buff marks.
        .advanced(EffectCue.CLEANSE)
        .hit(EffectCue.HIT)
        .benefit(EffectCue.GUARD)
        .effect(EffectCue.FORGE, AbilityDesigns.PART_ONE)
        .effect(EffectCue.ARCANE, AbilityDesigns.PART_TWO)
        .effect(EffectCue.CHARGE, AbilityDesigns.PICKAXE_CRAFT)
        .effect(EffectCue.CLEANSE, AbilityDesigns.DEVICE_FIELD)
        .received(EffectCue.GUARD, AbilityDesigns.GEAR_MARK)
        .received(EffectCue.WIND, AbilityDesigns.GEAR_MARK)
        .effect(EffectCue.HIT, AbilityDesigns.PICKAXE_STRIKE)
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    private static final int PICKAXE_PARTS_REQUIRED = 3;

    private int pickaxeParts;

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (!useNormal(context, player)) {
            return;
        }
        pickaxeParts++;
        effect(context, player, "HASTE", "FAST_DIGGING", 12, 1);
        feedback.castCue(context, player, pickaxeParts >= PICKAXE_PARTS_REQUIRED ? EffectCue.CHARGE
            : pickaxeParts == 1 ? EffectCue.FORGE : EffectCue.ARCANE);
        if (pickaxeParts >= PICKAXE_PARTS_REQUIRED) {
            pickaxeParts = 0;
            give(player, Material.IRON_PICKAXE, 1);
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
            effect(context, target, "HASTE", "FAST_DIGGING", 12, 1);
            effect(context, target, PotionEffectType.SPEED, 10, 0);
            effect(context, target, "RESISTANCE", "DAMAGE_RESISTANCE", 8, 0);
        }
        player.sendMessage(ChatColor.AQUA + "장영실의 장치가 아군의 전투 준비를 돕습니다.");
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, org.bukkit.event.entity.EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (attacker && isPickaxe(context.player().getItemInHand().getType()) && oneIn(4)) {
            event.setDamage(event.getDamage() + 2.0D);
            confirmedAttack(context, event, opponent);
            effect(context, opponent, "SLOWNESS", "SLOW", 6, 0);
        }
    }
}
