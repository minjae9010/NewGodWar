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
    id = "witch",
    name = "마녀",
    description = "주변 적과 공격자에게 저주를 겁니다.",
    normalSkill = "블레이즈 막대기 좌클릭: 주변 적에게 저주를 겁니다.",
    normalStoneCost = 15,
    normalCooldownSeconds = 60,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "피격 시 확률로 공격자에게 저주를 겁니다.",
    grade = AbilityGrade.A
)
final class WitchAbility extends BaseAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.SHADOW)
        .hit(EffectCue.POISON)
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
            curse(context, targets);
        }
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (!attacker && oneIn(14)) {
            curse(context, opponent);
        }
    }

    private void curse(AbilityPlayerContext context, List<Player> players) {
        for (Player player : players) {
            curse(context, player);
        }
    }

    private void curse(AbilityPlayerContext context, Player player) {
        effect(context, player, PotionEffectType.HUNGER, 12, 0);
        effect(context, player, PotionEffectType.POISON, 12, 0);
        effect(context, player, "SLOWNESS", "SLOW", 12, 0);
        effect(context, player, "MINING_FATIGUE", "SLOW_DIGGING", 12, 0);
    }
}
