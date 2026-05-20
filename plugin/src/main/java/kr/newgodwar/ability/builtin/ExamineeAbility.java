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
    private String pendingQuestion;
    private int pendingAnswer = -1;

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

    private void askQuestion(Player player) {
        String[] questions = new String[] {
            "15*12의 값을 구하시오.", "2+2*2+1의 값을 구하시오.", "가로 4, 세로 3인 직사각형의 넓이를 구하시오."
        };
        int[] answers = new int[] {180, 7, 12};
        int index = RANDOM.nextInt(questions.length);
        pendingQuestion = questions[index];
        pendingAnswer = answers[index];
        player.sendMessage(pendingQuestion);
    }

    private void answerQuestion(AbilityPlayerContext context, String message) {
        if (pendingAnswer < 0) {
            return;
        }
        try {
            int answer = Integer.parseInt(message.trim());
            if (answer == pendingAnswer) {
                context.plugin().abilities().assignRandom(context.player());
                context.player().sendMessage(ChatColor.AQUA + "문제를 맞혀 새 능력을 얻었습니다!");
            } else {
                context.player().sendMessage("아쉽습니다! 정답은 " + pendingAnswer + "입니다.");
            }
            pendingAnswer = -1;
            pendingQuestion = null;
        } catch (NumberFormatException ex) {
            context.player().sendMessage("0~999의 음이 아닌 정수만 입력하십시오.");
        }
    }
}
