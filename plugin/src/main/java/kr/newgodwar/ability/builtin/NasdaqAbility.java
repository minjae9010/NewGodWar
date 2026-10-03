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
    id = "nasdaq",
    name = "나스닥",
    description = "철괴나 다이아몬드를 걸고 확률적으로 자원 복사를 시도합니다.",
    normalSkill = "철괴나 다이아몬드를 손에 들고 좌클릭: 들고 있는 수량만큼 복사를 시도합니다. 기본 성공률은 철괴 25%, 다이아몬드 5%입니다.",
    normalStoneCost = 16,
    normalCooldownSeconds = 45,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "성공하면 같은 수량을 더 얻고, 실패하면 들고 있던 아이템을 모두 잃습니다. 블레이즈 막대기는 필요 없습니다.",
    grade = AbilityGrade.B
)
final class NasdaqAbility extends BaseAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.CRAFT)
        .hit(EffectCue.ITEM)
        .effect(EffectCue.ITEM, AbilityDesigns.STOCK_SPLIT)
        .effect(EffectCue.HUNGER, AbilityDesigns.STOCK_CRASH)
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    @Override
    public void onInteract(AbilityPlayerContext context, PlayerInteractEvent event) {
        Player player = context.player();
        if (isLeft(event.getAction()) && (holding(player, Material.IRON_INGOT) || holding(player, Material.DIAMOND))) {
            nasdaq(context, player);
        }
    }

    private void nasdaq(AbilityPlayerContext context, Player player) {
        if (!readyNormal(context, player, 1) || !hasNormalCost(context, player)) {
            return;
        }
        ItemStack item = player.getItemInHand();
        int successPercent = successPercent(context, item.getType());
        takeNormalCost(context, player);
        setCooldown(context, 1, context.ability().normalCooldownSeconds());
        feedback.activated(context, player, false);
        if (rollPercent(successPercent)) {
            give(player, item);
            feedback.castCue(context, player, kr.newgodwar.ability.feedback.EffectCue.ITEM);
            sendAbilityMessage(context, player, "success", ChatColor.GREEN + "복사에 성공했습니다. 확률 " + successPercent + "%");
        } else {
            player.getInventory().removeItem(item.clone());
            feedback.castCue(context, player, kr.newgodwar.ability.feedback.EffectCue.HUNGER);
            sendAbilityMessage(context, player, "failure", ChatColor.RED + "복사에 실패해 들고 있던 자원을 잃었습니다. 확률 " + successPercent + "%");
        }
    }

    private int successPercent(AbilityPlayerContext context, Material material) {
        String path = material == Material.DIAMOND ? "abilities.nasdaq.diamond-success-percent" : "abilities.nasdaq.iron-success-percent";
        int fallback = material == Material.DIAMOND ? 5 : 25;
        return Math.max(0, Math.min(100, context.plugin().getConfig().getInt(path, fallback)));
    }
}
