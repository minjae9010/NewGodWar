package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import kr.newgodwar.ability.feedback.AbilityStyle;
import kr.newgodwar.ability.feedback.AbilityTheme;
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
    id = "megumin",
    name = "메구밍",
    description = "게임 중 한 번 모든 것을 걸고 강력한 지연 폭발을 일으켜요.",
    normalSkill = "블레이즈 막대기 좌클릭 (게임당 1회): 25블록 안의 바라보는 위치에 3초 후 위력 5 폭발을 일으키고 사망해요. 바깥 경고선은 최대 10블록 피해 거리이며, 실제 피해는 거리와 장애물에 따라 달라져요.",
    normalStoneCost = 32,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "게임 중 한 번만 사용할 수 있어요.",
    grade = AbilityGrade.C
)
final class MeguminAbility extends BaseAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.FIRE)
        .dedicated()
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, final Player player, PlayerInteractEvent event) {
        if (!useNormal(context, player)) {
            return;
        }
        consumeSkill(context, 1);
        final Location location = targetLocation(player, 25);
        charge(context, location, 2.0D);
        scheduleLater(context, () -> charge(context, location, 3.0D), 20L);
        scheduleLater(context, () -> charge(context, location, 4.0D), 40L);
        player.sendMessage(ChatColor.RED + "익스플로전!");
        later(context, 3, "폭렬 발동", "폭렬 마법 발동", () -> {
            createExplosion(context, player, location, 5.0F, false, true);
            player.setHealth(0.0D);
        });
    }
    private void charge(AbilityPlayerContext context, Location center, double radius) {
        // Vanilla entity damage reaches up to power * 2; the growing core is charge progress, not the hit boundary.
        feedback.pulse(context, center, 10.0D);
        Location anchor = center.clone(); anchor.setPitch(0); anchor.setYaw(0);
        if (!feedback.object(context, "explosion-charge", AbilityDesigns.EXPLOSION_CHARGE, anchor, 22, radius))
            feedback.modelOutline(context, anchor, AbilityDesigns.EXPLOSION_CHARGE, radius * 5, radius, feedback.effectViewers(context, anchor));
    }

}
