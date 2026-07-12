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
    passiveSkill = "연결은 잠시 유지되며 팻말이 자동 제거됩니다.",
    grade = AbilityGrade.B
)
final class VoodooAbility extends BaseAbility {
    private Block postSign;
    private long lastPulseMillis;

    @Override
    public void onInteract(AbilityPlayerContext context, PlayerInteractEvent event) {
        if (postSign != null && event.getAction() == Action.LEFT_CLICK_BLOCK && event.getClickedBlock().equals(postSign)) {
            Player target = targetPlayer();
            if (target != null && readyPulse(context)) {
                damage(context, target, damagePerPulse(context), context.player());
            }
            return;
        }
        if (postSign == null && holdingSign(context.player()) && isLeft(event.getAction())) {
            Player player = context.player();
            if (readyNormal(context, player, 1) && hasNormalCost(context, player)) {
                player.sendMessage("스킬을 사용 할 수 있습니다.");
            }
        }
    }

    @Override
    public void onBlockPlace(AbilityPlayerContext context, BlockPlaceEvent event) {
        if (isSign(event.getBlock()) && (!readyNormal(context, context.player(), 1) || !hasNormalCost(context, context.player()))) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onSignChange(final AbilityPlayerContext context, SignChangeEvent event) {
        Player target = Bukkit.getPlayer(event.getLine(0));
        if (target != null && canAffectEnemy(context, context.player(), target) && useNormal(context, context.player())) {
            targetName = target.getName();
            postSign = event.getBlock();
            lastPulseMillis = 0L;
            final Block sign = postSign;
            context.player().sendMessage(ChatColor.RED + targetName + ChatColor.WHITE + " 를(을) 팻말과 연결시켰습니다.");
            target.sendMessage(ChatColor.RED + "부두술사가 당신을 위협합니다.");
            laterCleanup(context, 7, "부두 연결 해제", "부두 연결 해제", () -> {
                targetName = null;
                lastPulseMillis = 0L;
                if (postSign != null && postSign.equals(sign)) {
                    postSign = null;
                    sign.breakNaturally();
                }
            });
        }
    }

    private boolean holdingSign(Player player) {
        ItemStack item = player.getItemInHand();
        return item != null && isSignMaterial(item.getType());
    }

    private boolean isSign(Block block) {
        return block != null && isSignMaterial(block.getType());
    }

    private boolean isSignMaterial(Material material) {
        if (material == null) {
            return false;
        }
        String name = material.name();
        return "SIGN".equals(name) || "SIGN_POST".equals(name) || "LEGACY_SIGN".equals(name) || "LEGACY_SIGN_POST".equals(name)
            || name.endsWith("_SIGN") || name.endsWith("_WALL_SIGN") || name.endsWith("_HANGING_SIGN") || name.endsWith("_WALL_HANGING_SIGN");
    }

    private boolean readyPulse(AbilityPlayerContext context) {
        long interval = Math.max(0L, context.plugin().getConfig().getLong(context.configPath("hit-interval-millis"), 1000L));
        long now = System.currentTimeMillis();
        if (lastPulseMillis > 0L && now - lastPulseMillis < interval) {
            return false;
        }
        lastPulseMillis = now;
        return true;
    }

    private double damagePerPulse(AbilityPlayerContext context) {
        return Math.max(0.0D, context.plugin().getConfig().getDouble(context.configPath("damage"), 0.5D));
    }
}
