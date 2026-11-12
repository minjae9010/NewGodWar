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
    id = "examinee",
    name = "수험생",
    description = "수학 문제를 맞히면 무작위 능력으로 바뀝니다.",
    normalSkill = "블레이즈 막대기 좌클릭: 채팅으로 풀 짧은 덧셈 또는 곱셈 문제를 받습니다. 풀이 중 다시 클릭하면 같은 문제를 비용 없이 보여줍니다.",
    normalStoneCost = 5,
    normalCooldownSeconds = 5,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "채팅에 정답 숫자를 입력하면 무작위 능력으로 바뀝니다. 틀려도 같은 문제에 바로 다시 답할 수 있으며 추가 비용은 없습니다.",
    grade = AbilityGrade.C
)
final class ExamineeAbility extends TransientAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.ARCANE)
        .normal(EffectCue.ARCANE)
        .effect(EffectCue.ARCANE, AbilityDesigns.EXAM)
        .effect(EffectCue.SEAL, AbilityDesigns.EXAM_WRONG)
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    private String pendingQuestion;
    private int pendingAnswer = -1;

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (pendingAnswer >= 0) {
            player.sendMessage(pendingQuestion);
            return;
        }
        if (useNormal(context, player)) {
            askQuestion(player);
        }
    }

    @Override
    public void onChatMessage(AbilityPlayerContext context, String message) {
        answerQuestion(context, message);
    }

    private void askQuestion(Player player) {
        int first = 2 + RANDOM.nextInt(11), second = 2 + RANDOM.nextInt(11);
        boolean multiply = RANDOM.nextBoolean();
        pendingQuestion = first + (multiply ? " × " : " + ") + second + " = ? 채팅에 정답을 입력하세요.";
        pendingAnswer = multiply ? first * second : first + second;
        player.sendMessage(pendingQuestion);
    }

    private void answerQuestion(AbilityPlayerContext context, String message) {
        if (pendingAnswer < 0) {
            return;
        }
        try {
            int answer = Integer.parseInt(message.trim());
            if (answer == pendingAnswer) {
                pendingAnswer = -1;
                pendingQuestion = null;
                context.plugin().abilities().assignRandom(context.player());
                context.player().sendMessage(ChatColor.AQUA + "문제를 맞혀 새 능력을 얻었습니다!");
            } else {
                context.player().sendMessage("한 번 더 풀어보세요. 추가 비용 없이 다시 답할 수 있습니다. " + pendingQuestion);
                feedback.castCue(context, context.player(), kr.newgodwar.ability.feedback.EffectCue.SEAL);
            }
        } catch (NumberFormatException ex) {
            // Ordinary team conversation is not an attempted answer.
        }
    }

    @Override
    protected void clearTransientState() { pendingQuestion = null; pendingAnswer = -1; }
}
