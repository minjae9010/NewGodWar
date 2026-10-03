package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import kr.newgodwar.ability.feedback.AbilityStyle;
import kr.newgodwar.ability.feedback.AbilityTheme;
import kr.newgodwar.ability.feedback.EffectCue;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffectType;

@AbilityInfo(
    id = "anjunggeun",
    name = "안중근",
    description = "의거의 결의로 적 하나를 정확히 제압합니다.",
    normalSkill = "블레이즈 막대기 좌클릭: 24블록 안에서 바라보는 적에게 피해 5를 주고 9초 동안 감속 II와 약화를 줍니다.",
    normalStoneCost = 22,
    normalCooldownSeconds = 95,
    advancedSkill = "블레이즈 막대기 우클릭: 28블록 안에서 바라보는 적에게 피해 9를 주고 실명 7초, 감속 III 9초를 줍니다. 능력도 5초 동안 봉인합니다.",
    advancedStoneCost = 38,
    advancedCooldownSeconds = 180,
    passiveSkill = "능력을 받으면 철 검을 받습니다. 검으로 공격하면 20% 확률로 7초 동안 공격력 증가를 얻습니다.",
    grade = AbilityGrade.A
)
final class AhnJungGeunAbility extends BaseAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.COMBAT)
        .hit(EffectCue.HIT)
        .passive(EffectCue.CHARGE)
        .effect(EffectCue.HIT, AbilityDesigns.BULLET)
        .effect(EffectCue.CHARGE, AbilityDesigns.CHAMBER)
        .effect(EffectCue.SEAL, AbilityDesigns.COUNTER_LOCK)
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    @Override
    public void onPrepare(AbilityPlayerContext context) {
        give(context.player(), Material.IRON_SWORD, 1);
    }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        Player target = targetPlayerInSight(context, player, 24, false);
        if (target == null) {
            return;
        }
        if (!useNormal(context, player)) {
            return;
        }
        damage(context, target, 5.0D, player);
        effect(context, target, "SLOWNESS", "SLOW", 9, 1);
        effect(context, target, PotionEffectType.WEAKNESS, 9, 0);
        target.sendMessage(ChatColor.RED + "의거의 결의가 움직임을 꺾었습니다.");
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
        damage(context, target, 9.0D, player);
        effect(context, target, PotionEffectType.BLINDNESS, 7, 0);
        effect(context, target, "SLOWNESS", "SLOW", 9, 2);
        if (context.plugin().abilities().session(target) != null) {
            context.plugin().abilities().suppressAbility(target, 5);
            feedback.cue(context, target, kr.newgodwar.ability.feedback.EffectCue.SEAL);
        }
        target.sendMessage(ChatColor.DARK_PURPLE + "결의의 일격이 능력을 흔들었습니다.");
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (attacker && isSword(context.player().getItemInHand().getType()) && oneIn(5)) {
            effect(context, context.player(), "STRENGTH", "INCREASE_DAMAGE", 7, 0);
        }
    }
}
