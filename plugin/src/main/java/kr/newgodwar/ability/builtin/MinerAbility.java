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
    id = "miner",
    name = "광부",
    description = "항상 성급함을 받고 곡괭이로 고정 피해를 줍니다.",
    normalSkill = "없음",
    normalStoneCost = 0,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "항상 성급함 I을 받습니다. 곡괭이 공격 피해는 항상 4로 고정됩니다. 조약돌을 캐면 3% 확률로 조약돌 9개가 추가로 떨어집니다.",
    grade = AbilityGrade.A
)
final class MinerAbility extends BaseAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.CRAFT)
        .hit(EffectCue.HIT)
        .effect(EffectCue.HIT, AbilityDesigns.ORE_SPLIT)
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    @Override
    public void onAssign(AbilityPlayerContext context) {
        effect(context, context.player(), "HASTE", "FAST_DIGGING", 24 * 60 * 60, 0);
    }

    @Override
    public void onRemove(AbilityPlayerContext context) {
        removeEffect(context.player(), "HASTE", "FAST_DIGGING");
    }

    @Override
    public void onRespawn(AbilityPlayerContext context, PlayerRespawnEvent event) {
        respawnEffect(context, "HASTE", "FAST_DIGGING", 24 * 60 * 60, 0);
    }

    @Override
    public void onTick(AbilityPlayerContext context) {
        effect(context, context.player(), "HASTE", "FAST_DIGGING", 6, 0);
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (attacker && isPickaxe(context.player().getItemInHand().getType())) {
            event.setDamage(4.0D);
            confirmedAttack(context, event, opponent);
        }
    }

    @Override
    public void onBlockBreak(AbilityPlayerContext context, BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.COBBLESTONE && oneIn(33)) {
            feedback.impact(context, event.getBlock().getLocation().add(0.5D, 0, 0.5D));
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(Material.COBBLESTONE, 9));
        }
    }
}
