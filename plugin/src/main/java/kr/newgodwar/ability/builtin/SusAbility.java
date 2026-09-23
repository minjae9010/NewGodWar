package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import kr.newgodwar.ability.feedback.AbilityStyle;
import kr.newgodwar.ability.feedback.AbilityTheme;
import kr.newgodwar.ability.feedback.EffectCue;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

@AbilityInfo(
    id = "sus",
    name = "수상한녀석",
    description = "의심스러운 움직임으로 숨어들고 적을 교란합니다.",
    normalSkill = "블레이즈 막대기 좌클릭: 잠시 투명화하고 신속을 얻습니다.",
    normalStoneCost = 16,
    normalCooldownSeconds = 70,
    advancedSkill = "블레이즈 막대기 우클릭: 주변 적 하나와 위치를 바꾸고 서로 실명합니다.",
    advancedStoneCost = 22,
    advancedCooldownSeconds = 105,
    passiveSkill = "피격 시 가끔 짧게 투명화합니다.",
    grade = AbilityGrade.B
)
final class SusAbility extends BaseAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.SHADOW)
        .hit(EffectCue.PORTAL)
        .privateCast()
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            effect(context, player, PotionEffectType.INVISIBILITY, 10, 0);
            effect(context, player, PotionEffectType.SPEED, 10, 0);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = nearbyPlayers(context, player, 10, false);
        if (targets.isEmpty()) {
            player.sendMessage("능력을 사용할 수 있는 대상이 없습니다.");
            return;
        }
        if (useAdvanced(context, player)) {
            Player target = targets.get(RANDOM.nextInt(targets.size()));
            Location playerLocation = player.getLocation();
            Location targetLocation = target.getLocation();
            player.teleport(targetLocation);
            target.teleport(playerLocation);
            effect(context, player, PotionEffectType.BLINDNESS, 5, 0);
            effect(context, target, PotionEffectType.BLINDNESS, 8, 0);
        }
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (!attacker && rollChance(1, 5)) {
            effect(context, context.player(), PotionEffectType.INVISIBILITY, 7, 0);
        }
    }
}
