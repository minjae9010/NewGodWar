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
    id = "examinee",
    name = "수험생",
    description = "수학 문제를 맞히면 무작위 능력으로 바뀝니다.",
    normalSkill = "수학 문제를 출제합니다.",
    normalStoneCost = 5,
    normalCooldownSeconds = 60,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "채팅으로 정답을 맞히면 무작위 능력으로 바뀝니다.",
    grade = AbilityGrade.C
)
final class ExamineeAbility extends BaseAbility {
    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player, 0)) {
            askQuestion(player);
        }
    }

    @Override
    public void onChatMessage(AbilityPlayerContext context, String message) {
        answerQuestion(context, message);
    }
}
