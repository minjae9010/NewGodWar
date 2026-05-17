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
    id = "harry",
    name = "해리포터",
    description = "헤르미온느보다 숙련된 채팅 주문 마법을 사용합니다.",
    normalSkill = "채팅으로 기본 주문을 사용합니다.",
    normalStoneCost = 5,
    normalCooldownSeconds = 5,
    advancedSkill = "채팅으로 더 높은 확률의 고급 주문을 사용합니다.",
    advancedStoneCost = 10,
    advancedCooldownSeconds = 20,
    passiveSkill = "배정 시 주문서를 받고 보호 주문 중 피해를 무시합니다.",
    grade = AbilityGrade.S
)
final class HarryAbility extends BaseAbility {
    @Override
    public void onAssign(AbilityPlayerContext context) {
        giveSpellBook(context.player(), true);
    }

    @Override
    public void onChatMessage(AbilityPlayerContext context, String message) {
        castSpell(context, message, true);
    }

    @Override
    public void onGenericDamage(AbilityPlayerContext context, EntityDamageEvent event) {
        if (invincible) {
            event.setCancelled(true);
        }
    }
}
