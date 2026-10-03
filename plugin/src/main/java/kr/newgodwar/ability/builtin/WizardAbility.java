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
    id = "wizard",
    name = "마법사",
    description = "주변 적을 밀쳐내거나 체력을 바쳐 번개 심판을 내립니다.",
    normalSkill = "블레이즈 막대기 좌클릭: 반경 10블록 적을 강하게 밀쳐냅니다.",
    normalStoneCost = 12,
    normalCooldownSeconds = 110,
    advancedSkill = "블레이즈 막대기 우클릭: 자신의 체력을 절반 바쳐 반경 5블록 적을 공중으로 띄운 뒤 번개를 내리고 5초 동안 불태웁니다.",
    advancedStoneCost = 24,
    advancedCooldownSeconds = 210,
    passiveSkill = "없음",
    grade = AbilityGrade.A
)
final class WizardAbility extends BaseAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.ARCANE)
        .hit(EffectCue.WIND)
        .effect(EffectCue.WIND, AbilityDesigns.WAND_GUST)
        .effect(EffectCue.FIRE, AbilityDesigns.WAND_GUST)
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = nearbyPlayers(context, player, 10, false);
        if (targets.isEmpty()) {
            player.sendMessage("능력을 사용할 수 있는 대상이 없습니다.");
            return;
        }
        if (useNormal(context, player)) {
            push(context, player, targets, 2.4D, 4L);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (nearbyPlayers(context, player, 5, false).isEmpty()) {
            player.sendMessage("능력을 사용할 수 있는 대상이 없습니다.");
            return;
        }
        if (useAdvanced(context, player)) {
            judgment(context, player);
        }
    }

    private void judgment(AbilityPlayerContext context, final Player player) {
        final List<Player> targets = nearbyPlayers(context, player, 5, false);
        if (targets.isEmpty()) {
            player.sendMessage("능력을 사용할 수 있는 대상이 없습니다.");
            return;
        }
        player.setHealth(Math.max(1.0D, player.getHealth() / 2.0D));
        for (Player target : targets) {
            target.setVelocity(new Vector(0, 1.6D, 0));
        }
        scheduleLater(context, () -> {
            if (!player.isOnline() || player.isDead() || !context.plugin().game().canUseAbility(player)) return;
            for (Player target : targets) {
                if (!player.getWorld().equals(target.getWorld()) || !canAffectEnemy(context, player, target)) continue;
                strikeLightning(context, player, target.getLocation());
                target.setFireTicks(100);
            }
        }, 4L);
    }
}
