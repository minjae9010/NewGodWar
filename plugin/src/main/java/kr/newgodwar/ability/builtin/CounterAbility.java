package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.AbilityGrade;
import kr.newgodwar.ability.api.AbilityInfo;
import kr.newgodwar.ability.api.AbilityPlayerContext;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.ArrayList;
import java.util.List;

@AbilityInfo(
    id = "counter",
    name = "카운터",
    description = "성급함으로 전투를 준비하고 적의 능력을 잠시 봉인합니다.",
    normalSkill = "지정한 적의 능력을 12초 동안 봉인합니다.",
    normalStoneCost = 20,
    normalCooldownSeconds = 90,
    advancedSkill = "주변 적의 능력을 6초 동안 봉인하고 자신은 짧게 더 강한 성급함을 얻습니다.",
    advancedStoneCost = 32,
    advancedCooldownSeconds = 150,
    passiveSkill = "상시 성급함 효과를 유지하며 타깃 지정 명령을 사용할 수 있습니다.",
    grade = AbilityGrade.S
)
final class CounterAbility extends BaseAbility {
    private static final int ADVANCED_RANGE = 10;
    private static final int NORMAL_SUPPRESS_SECONDS = 12;
    private static final int ADVANCED_SUPPRESS_SECONDS = 6;

    @Override
    public boolean requiresTarget() {
        return true;
    }

    @Override
    public void onAssign(AbilityPlayerContext context) {
        effect(context.player(), "HASTE", "FAST_DIGGING", 24 * 60 * 60, 0);
    }

    @Override
    public void onRemove(AbilityPlayerContext context) {
        removeEffect(context.player(), "HASTE", "FAST_DIGGING");
    }

    @Override
    public void onRespawn(AbilityPlayerContext context, PlayerRespawnEvent event) {
        respawnEffect(context, "HASTE", "FAST_DIGGING", 24 * 60 * 60, 0);
    }

    @Override
    public void onTick(AbilityPlayerContext context) {
        effect(context.player(), "HASTE", "FAST_DIGGING", 3, 0);
    }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        Player target = commandTargetPlayer(context, player, false);
        if (target == null || !canSuppress(context, player, target)) {
            return;
        }
        if (!useNormal(context, player)) {
            return;
        }
        context.plugin().abilities().suppressAbility(target, NORMAL_SUPPRESS_SECONDS);
        player.sendMessage(ChatColor.DARK_PURPLE + target.getName() + "의 능력을 봉인했습니다.");
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = suppressibleTargets(context, player);
        if (targets.isEmpty()) {
            sendAbilityMessage(context, player, "failure", ChatColor.RED + "봉인할 수 있는 주변 적 능력이 없습니다.");
            return;
        }
        if (!useAdvanced(context, player)) {
            return;
        }
        for (Player target : targets) {
            context.plugin().abilities().suppressAbility(target, ADVANCED_SUPPRESS_SECONDS);
        }
        effect(player, "HASTE", "FAST_DIGGING", 8, 1);
        player.sendMessage(ChatColor.DARK_PURPLE + "주변 적 " + targets.size() + "명의 능력을 봉인했습니다.");
    }

    private boolean canSuppress(AbilityPlayerContext context, Player player, Player target) {
        if (context.plugin().abilities().session(target) == null) {
            sendAbilityMessage(context, player, "failure", ChatColor.RED + "대상에게 봉인할 능력이 없습니다.");
            return false;
        }
        if (context.plugin().abilities().isAbilitySuppressed(target)) {
            sendAbilityMessage(context, player, "failure", ChatColor.YELLOW + "대상의 능력은 이미 봉인되어 있습니다.");
            return false;
        }
        return true;
    }

    private List<Player> suppressibleTargets(AbilityPlayerContext context, Player player) {
        List<Player> result = new ArrayList<Player>();
        for (Player target : nearbyPlayers(context, player, ADVANCED_RANGE, false)) {
            if (context.plugin().abilities().session(target) != null
                && !context.plugin().abilities().isAbilitySuppressed(target)) {
                result.add(target);
            }
        }
        return result;
    }
}
