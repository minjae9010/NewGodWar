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
    id = "hermione",
    name = "헤르미온느",
    description = "채팅 주문으로 시간 변경, 폭발, 보호, 무장 해제, 즉사 주문을 사용합니다.",
    normalSkill = "채팅으로 루모스, 녹스, 봄바르다를 시전합니다.",
    normalStoneCost = 6,
    normalCooldownSeconds = 8,
    advancedSkill = "채팅으로 고급 주문을 시전합니다.",
    advancedStoneCost = 18,
    advancedCooldownSeconds = 50,
    passiveSkill = "배정 시 주문서를 받고 보호 주문 중 모든 피해를 무시합니다.",
    grade = AbilityGrade.S
)
final class HermioneAbility extends BaseAbility {
    @Override
    public void onAssign(AbilityPlayerContext context) {
        giveSpellBook(context.player(), false);
    }

    @Override
    public void onChatMessage(AbilityPlayerContext context, String message) {
        castSpell(context, message, false);
    }

    @Override
    public void onGenericDamage(AbilityPlayerContext context, EntityDamageEvent event) {
        if (invincible) {
            event.setCancelled(true);
        }
    }
}
