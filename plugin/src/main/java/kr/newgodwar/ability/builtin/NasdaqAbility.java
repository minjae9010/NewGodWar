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
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "실패 시 들고 있던 아이템을 잃을 수 있습니다."
)
final class NasdaqAbility extends BaseAbility {
    @Override
    public void onInteract(AbilityPlayerContext context, PlayerInteractEvent event) {
        Player player = context.player();
        if (isLeft(event.getAction()) && (holding(player, Material.IRON_INGOT) || holding(player, Material.DIAMOND))) {
            nasdaq(player);
        }
    }

    private void nasdaq(Player player) {
        if (!readyCooldown(player, 0, 30) || !has(player, COBBLESTONE, 20)) {
            return;
        }
        ItemStack item = player.getItemInHand();
        player.getInventory().removeItem(new ItemStack(COBBLESTONE, 20));
        setRawCooldown(0, 30000L);
        if (RANDOM.nextInt(4) < 3) {
            player.getInventory().addItem(item.clone());
        } else {
            player.getInventory().removeItem(item.clone());
        }
    }
}
