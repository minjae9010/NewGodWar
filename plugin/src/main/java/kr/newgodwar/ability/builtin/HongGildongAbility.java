package kr.newgodwar.ability.builtin;

import kr.newgodwar.util.InventoryItems;

import kr.newgodwar.ability.api.*;
import kr.newgodwar.ability.feedback.AbilityStyle;
import kr.newgodwar.ability.feedback.AbilityTheme;
import kr.newgodwar.ability.feedback.EffectCue;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

@AbilityInfo(
    id = "honggildong",
    name = "홍길동",
    description = "재빠른 의적으로 숨어들고 적의 조약돌을 크게 빼앗습니다.",
    normalSkill = "블레이즈 막대기 좌클릭: 짧게 투명화하고 신속을 얻습니다.",
    normalStoneCost = 10,
    normalCooldownSeconds = 55,
    advancedSkill = "블레이즈 막대기 우클릭: 바라보는 적에게서 조약돌을 훔치고 자신의 위치를 흐립니다.",
    advancedStoneCost = 20,
    advancedCooldownSeconds = 100,
    passiveSkill = "피격 시 가끔 짧은 신속을 얻습니다.",
    grade = AbilityGrade.B
)
final class HongGildongAbility extends BaseAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.SHADOW)
        .hit(EffectCue.STEALTH)
        .privateCast()
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (!useNormal(context, player)) {
            return;
        }
        effect(context, player, PotionEffectType.INVISIBILITY, 9, 0);
        effect(context, player, PotionEffectType.SPEED, 10, 1);
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        Player target = targetPlayerInSight(context, player, 20, false);
        if (target == null) {
            return;
        }
        if (!useAdvanced(context, player)) {
            return;
        }
        int stolen = Math.min(24, InventoryItems.count(target.getInventory(), COBBLESTONE));
        if (stolen > 0 && InventoryItems.take(target.getInventory(), COBBLESTONE, stolen)) {
            give(player, COBBLESTONE, stolen);
        }
        effect(context, player, PotionEffectType.INVISIBILITY, 7, 0);
        effect(context, target, PotionEffectType.CONFUSION, 8, 0);
        player.sendMessage(ChatColor.GREEN + "조약돌 " + stolen + "개를 훔쳤습니다.");
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (!attacker && oneIn(4)) {
            effect(context, context.player(), PotionEffectType.SPEED, 8, 1);
        }
    }

}
