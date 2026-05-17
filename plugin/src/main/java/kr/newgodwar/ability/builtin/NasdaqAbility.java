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
    id = "nasdaq",
    name = "나스닥",
    description = "철괴나 다이아몬드를 확률적으로 복사합니다.",
    normalSkill = "철괴나 다이아몬드를 든 채 좌클릭하면 확률적으로 복사합니다.",
    normalStoneCost = 20,
    normalCooldownSeconds = 30,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "실패 시 들고 있던 아이템을 잃을 수 있습니다.",
    grade = AbilityGrade.B
)
final class NasdaqAbility extends BaseAbility {
    @Override
    public void onInteract(AbilityPlayerContext context, PlayerInteractEvent event) {
        Player player = context.player();
        if (isLeft(event.getAction()) && (holding(player, Material.IRON_INGOT) || holding(player, Material.DIAMOND))) {
            nasdaq(context, player);
        }
    }

    private void nasdaq(AbilityPlayerContext context, Player player) {
        if (!readyNormal(context, player, 0) || !hasNormalCost(context, player)) {
            return;
        }
        ItemStack item = player.getItemInHand();
        int successPercent = successPercent(context, item.getType());
        takeNormalCost(context, player);
        setCooldown(context, 0, context.ability().normalCooldownSeconds());
        if (rollPercent(successPercent)) {
            player.getInventory().addItem(item.clone());
            sendAbilityMessage(context, player, "success", ChatColor.GREEN + "복사에 성공했습니다. 확률 " + successPercent + "%");
        } else {
            player.getInventory().removeItem(item.clone());
            sendAbilityMessage(context, player, "failure", ChatColor.RED + "복사에 실패해 들고 있던 아이템을 잃었습니다. 확률 " + successPercent + "%");
        }
    }

    private int successPercent(AbilityPlayerContext context, Material material) {
        String path = material == Material.DIAMOND ? "abilities.nasdaq.diamond-success-percent" : "abilities.nasdaq.iron-success-percent";
        int fallback = material == Material.DIAMOND ? 25 : 75;
        return Math.max(0, Math.min(100, context.plugin().getConfig().getInt(path, fallback)));
    }
}
