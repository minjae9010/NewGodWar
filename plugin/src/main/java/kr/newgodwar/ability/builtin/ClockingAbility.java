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
    id = "clocking",
    name = "클로킹",
    description = "투명화한 뒤 첫 공격으로 즉사를 노립니다.",
    normalSkill = "블레이즈 막대기 좌클릭: 9초 동안 투명화합니다. 처음 7초 안의 첫 공격이 암습이 됩니다.",
    normalStoneCost = 25,
    normalCooldownSeconds = 60,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "암습하면 투명화가 풀리고 20% 확률로 피해 100을 줍니다.",
    grade = AbilityGrade.A
)
final class ClockingAbility extends BaseAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.SHADOW)
        .normal(EffectCue.STEALTH)
        .hit(EffectCue.SLASH)
        .privateCast()
        .effect(EffectCue.STEALTH, AbilityDesigns.CLOAK)
        .effect(EffectCue.SLASH, AbilityDesigns.DAGGER_STEP)
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    private boolean invisible;

    @Override
    public void onRemove(AbilityPlayerContext context) {
        context.player().removePotionEffect(PotionEffectType.INVISIBILITY);
    }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            invisible = true;
            effect(context, player, PotionEffectType.INVISIBILITY, 9, 0);
            laterCleanup(context, 7, "클로킹 종료", "클로킹 종료", () -> invisible = false);
        }
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (attacker && invisible) {
            context.player().removePotionEffect(PotionEffectType.INVISIBILITY);
            invisible = false;
            confirmedAttack(context, event, opponent);
            if (oneIn(5)) {
                event.setDamage(100.0D);
            }
        }
    }
}
