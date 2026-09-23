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

import java.util.ArrayList;
import java.util.List;
@AbilityInfo(
    id = "priest",
    name = "사제",
    description = "자신과 팀원에게 여러 전투 축복 중 하나 이상을 무작위로 부여합니다.",
    normalSkill = "블레이즈 막대기 좌클릭: 자신에게 30초짜리 무작위 축복을 하나 이상 부여합니다.",
    normalStoneCost = 22,
    normalCooldownSeconds = 45,
    advancedSkill = "블레이즈 막대기 우클릭: 팀원 전체에게 30초짜리 무작위 축복을 하나 이상 부여합니다.",
    advancedStoneCost = 40,
    advancedCooldownSeconds = 105,
    passiveSkill = "없음",
    grade = AbilityGrade.A
)
final class PriestAbility extends BaseAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.HEALING)
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            bless(context, player);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = alliedPlayers(context, player, true);
        if (targets.isEmpty()) {
            player.sendMessage("능력을 사용할 팀원이 없습니다!");
            return;
        }
        if (useAdvanced(context, player)) {
            for (Player target : targets) {
                bless(context, target);
            }
        }
    }

    private void bless(AbilityPlayerContext context, Player player) {
        List<String> blessings = new ArrayList<String>();
        if (RANDOM.nextBoolean()) {
            effect(context, player, "RESISTANCE", "DAMAGE_RESISTANCE", 30, 0);
            blessings.add("저항");
        }
        if (RANDOM.nextBoolean()) {
            effect(context, player, "STRENGTH", "INCREASE_DAMAGE", 30, 0);
            blessings.add("공격력 증가");
        }
        if (RANDOM.nextBoolean()) {
            effect(context, player, PotionEffectType.REGENERATION, 30, 0);
            blessings.add("재생");
        }
        if (RANDOM.nextBoolean()) {
            effect(context, player, PotionEffectType.SPEED, 30, 0);
            blessings.add("신속");
        }
        if (RANDOM.nextBoolean()) {
            effect(context, player, "HASTE", "FAST_DIGGING", 30, 0);
            blessings.add("성급함");
        }
        if (blessings.isEmpty()) {
            effect(context, player, PotionEffectType.REGENERATION, 30, 0);
            blessings.add("재생");
        }
        feedback.affected(context, player, "축복 · " + String.join(" / ", blessings) + " 30초", false);
    }
}
