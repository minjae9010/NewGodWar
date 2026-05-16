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
    id = "scrooge",
    name = "스크루지",
    description = "팀원의 능력 코블스톤 비용을 절반으로 낮춥니다.",
    normalSkill = "없음",
    normalStoneCost = 0,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "같은 팀의 코블스톤 능력 비용을 절반으로 줄입니다."
)
final class ScroogeAbility extends BaseAbility {
    @Override
    public void onAssign(AbilityPlayerContext context) {
        GodTeam team = context.plugin().game().teamOf(context.player());
        if (team != null) {
            SCROOGE_TEAMS.add(team.id());
        }
    }
}
