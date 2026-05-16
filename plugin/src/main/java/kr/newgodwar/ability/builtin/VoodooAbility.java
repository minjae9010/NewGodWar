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
    id = "voodoo",
    name = "부두술사",
    description = "팻말로 대상을 연결해 원격 피해를 줍니다.",
    normalSkill = "팻말 첫 줄에 대상 이름을 적어 연결합니다.",
    normalStoneCost = 5,
    normalCooldownSeconds = 180,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "연결은 잠시 유지되며 팻말이 자동 제거됩니다."
)
final class VoodooAbility extends BaseAbility {
    private Block postSign;

    @Override
    public void onInteract(AbilityPlayerContext context, PlayerInteractEvent event) {
        if (postSign != null && event.getAction() == Action.LEFT_CLICK_BLOCK && event.getClickedBlock().equals(postSign)) {
            Player target = targetPlayer();
            if (target != null) {
                damage(target, 1.0D, context.player());
            }
            return;
        }
        if (postSign == null && holding(context.player(), Material.SIGN) && isLeft(event.getAction())) {
            Player player = context.player();
            if (readyNormal(context, player, 0) && hasNormalCost(context, player)) {
                player.sendMessage("스킬을 사용 할 수 있습니다.");
            }
        }
    }

    @Override
    public void onBlockPlace(AbilityPlayerContext context, BlockPlaceEvent event) {
        if (event.getBlock().getType() == Material.SIGN && (!readyNormal(context, context.player(), 0) || !hasNormalCost(context, context.player()))) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onSignChange(final AbilityPlayerContext context, SignChangeEvent event) {
        Player target = Bukkit.getPlayer(event.getLine(0));
        if (target != null && useNormal(context, context.player(), 0)) {
            targetName = target.getName();
            postSign = event.getBlock();
            final Block sign = postSign;
            context.player().sendMessage(ChatColor.RED + targetName + ChatColor.WHITE + " 를(을) 팻말과 연결시켰습니다.");
            target.sendMessage(ChatColor.RED + "부두술사가 당신을 위협합니다.");
            later(context, 7, new Runnable() {
                @Override
                public void run() {
                    targetName = null;
                    if (postSign != null && postSign.equals(sign)) {
                        postSign = null;
                        sign.breakNaturally();
                    }
                }
            });
        }
    }
}
