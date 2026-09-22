package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@AbilityInfo(
    id = "artemis", name = "아르테미스",
    description = "사냥의 여신의 은빛 활로 달빛 표식을 새기고 세 번째 화살로 사냥을 완성합니다.",
    normalSkill = "블레이즈 막대기 좌클릭: 화살 6개를 만들고 5초 동안 신속을 얻습니다.",
    normalStoneCost = 8, normalCooldownSeconds = 25,
    advancedSkill = "블레이즈 막대기 우클릭: 바라보는 30블록 안의 적에게 달빛 사냥 표식 2개를 남깁니다. 표식은 대상을 따라가며 8초 안에 화살을 맞히면 사냥이 완성됩니다.",
    advancedStoneCost = 20, advancedCooldownSeconds = 65,
    passiveSkill = "은빛 사냥활과 화살 12개를 지급받습니다. 같은 적에게 8초 안에 화살을 3번 적중하면 추가 피해 4와 감속 3초를 줍니다. 다른 적을 맞히면 표식이 옮겨갑니다.",
    grade = AbilityGrade.A
)
final class ArtemisAbility extends TransientAbility {
    private UUID prey;
    private int marks;
    private long expiresAt;
    private int markTask = -1;

    @Override
    public void onPrepare(AbilityPlayerContext context) {
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta meta = bow.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "은빛 사냥활");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "아르테미스의 달빛 사냥", ChatColor.GRAY + "같은 적에게 세 번째 화살을 맞혀 사냥을 완성합니다."));
        bow.setItemMeta(meta);
        give(context.player(), bow);
        give(context.player(), Material.ARROW, 12);
    }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (!useNormal(context, player)) return;
        give(player, Material.ARROW, 6);
        effect(context, player, "SPEED", "SPEED", 5, 0);
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        Player target = targetPlayerInSight(context, player, 30, false);
        if (target == null || !useAdvanced(context, player)) return;
        mark(context, target, 2);
        feedback.affected(context, target, "사냥 표식 2/3 · 화살을 피하세요!", true);
    }

    @Override
    public void onProjectileHit(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player victim) {
        if (!(event.getDamager() instanceof Arrow) || event.isCancelled() || event.getDamage() <= 0
            || !validEnemy(context, victim, 64)) return;
        int next = victim.getUniqueId().equals(prey) && System.currentTimeMillis() < expiresAt ? marks + 1 : 1;
        if (next >= 3) {
            clearTransientState();
            event.setDamage(event.getDamage() + 4);
            effect(context, victim, "SLOWNESS", "SLOW", 3, 1);
            feedback.huntMark(context, victim, 3);
            feedback.affected(context, victim, "사냥 완성 · 추가 피해 / 감속 3초", true);
        } else {
            mark(context, victim, next);
            feedback.passive(context, "사냥 표식 " + next + "/3");
        }
    }

    private void mark(AbilityPlayerContext context, Player target, int count) {
        cancelScheduledTask(markTask);
        prey = target.getUniqueId();
        marks = count;
        expiresAt = System.currentTimeMillis() + 8000L;
        feedback.huntMark(context, target, marks);
        final int[] remaining = {8};
        markTask = scheduleRepeating(context, () -> {
            if (--remaining[0] <= 0 || System.currentTimeMillis() >= expiresAt || !validEnemy(context, target, 64)) {
                clearTransientState();
                return;
            }
            feedback.huntMark(context, target, marks);
        }, 20L, 20L);
    }

    @Override
    public List<String> activeTimerLines() {
        List<String> lines = super.activeTimerLines();
        long remaining = expiresAt - System.currentTimeMillis();
        if (remaining > 0) lines.add(ChatColor.GREEN + "사냥 표식 " + marks + "/3 · " + ((remaining + 999) / 1000) + "초");
        return lines;
    }

    @Override
    public void onDeath(AbilityPlayerContext context, PlayerDeathEvent event) {
        super.onDeath(context, event);
        if (event.getEntity().getUniqueId().equals(prey)) clearTransientState();
    }

    @Override
    protected void clearTransientState() {
        cancelScheduledTask(markTask);
        markTask = -1;
        prey = null; marks = 0; expiresAt = 0;
    }
}
