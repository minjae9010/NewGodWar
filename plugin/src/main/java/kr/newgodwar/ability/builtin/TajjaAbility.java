package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import kr.newgodwar.ability.feedback.AbilityStyle;
import kr.newgodwar.ability.feedback.AbilityTheme;
import kr.newgodwar.ability.feedback.EffectCue;
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
    id = "tajja",
    name = "타짜",
    description = "검을 숨겨 두고 맨손 공격을 검처럼 씁니다.",
    normalSkill = "블레이즈 막대기 좌클릭: 인벤토리의 검 하나를 소모해 숨깁니다.",
    normalStoneCost = 10,
    normalCooldownSeconds = 60,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "숨긴 뒤 맨손 공격 10회는 숨긴 검의 피해로 들어갑니다. 다시 숨기면 새 검으로 바뀝니다.",
    grade = AbilityGrade.B
)
final class TajjaAbility extends BaseAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.SHADOW)
        .normal(EffectCue.SLASH)
        .hit(EffectCue.SLASH)
        .privateCast()
        .effect(EffectCue.SLASH, AbilityDesigns.HIDDEN_CARDS)
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    private int tajjaDamage;
    private int tajjaUses;

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        boolean hasSword = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && isSword(item.getType())) { hasSword = true; break; }
        }
        if (!hasSword) {
            sendAbilityMessage(context, player, "failure", "소비할 검이 인벤토리에 없습니다.");
            return;
        }
        if (useNormal(context, player)) {
            stealSword(player);
        }
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (attacker && context.player().getItemInHand().getType() == Material.AIR && tajjaDamage > 0) {
            event.setDamage(tajjaDamage);
            confirmedAttack(context, event, opponent);
            tajjaUses--;
            if (tajjaUses <= 0) {
                tajjaDamage = 0;
            }
        }
    }

    private void stealSword(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && isSword(item.getType())) {
                tajjaDamage = swordDamage(item.getType());
                tajjaUses = 10;
                ItemStack consumed = item.clone();
                consumed.setAmount(1);
                player.getInventory().removeItem(consumed);
                player.sendMessage("손은 눈보다 빠르다.");
                return;
            }
        }
        player.sendMessage("소비할 검이 인벤토리에 없습니다.");
    }
}
