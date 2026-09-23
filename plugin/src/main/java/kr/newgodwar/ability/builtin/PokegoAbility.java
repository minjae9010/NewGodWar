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
    id = "pokego",
    name = "포켓몬고",
    description = "많이 걸으면 다른 능력으로 바뀝니다.",
    normalSkill = "없음",
    normalStoneCost = 0,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "일정 거리 이상 이동하면 무작위 능력으로 바뀝니다.",
    grade = AbilityGrade.C
)
final class PokegoAbility extends BaseAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.NATURE)
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    private int steps;

    @Override
    public void onMove(AbilityPlayerContext context, PlayerMoveEvent event) {
        if (event.getTo() != null && event.getFrom().getWorld().equals(event.getTo().getWorld())
            && event.getFrom().distanceSquared(event.getTo()) > 0.01D) {
            steps++;
            if (steps % 100 == 0) feedback.passive(context, "탐험 " + steps + "/1000");
            if (steps >= 1000) {
                steps = 0;
                context.plugin().abilities().assignRandom(context.player());
                context.player().sendMessage(ChatColor.AQUA + "새 능력을 잡았습니다!");
            }
        }
    }
}
