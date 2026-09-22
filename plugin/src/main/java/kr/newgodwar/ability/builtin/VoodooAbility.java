package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.AbilityGrade;
import kr.newgodwar.ability.api.AbilityInfo;
import kr.newgodwar.ability.api.AbilityPlayerContext;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

@AbilityInfo(
    id = "voodoo",
    name = "부두술사",
    description = "팻말로 대상을 연결해 원격 피해를 줍니다.",
    normalSkill = "팻말 설치 후 첫 줄에 접속 중인 적의 정확한 이름을 적고, 연결된 팻말을 좌클릭해 피해를 줍니다. 유저 간 전투를 막는 킬타임 중에는 연결할 수 없습니다.",
    normalStoneCost = 5,
    normalCooldownSeconds = 180,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "연결은 7초 유지되며 팻말이 자동 제거됩니다. 피해량과 타격 간격은 서버 설정을 따릅니다.",
    grade = AbilityGrade.B
)
final class VoodooAbility extends BaseAbility {
    private Block postSign;
    private UUID linkedTarget;
    private long lastPulseMillis;

    @Override
    public void onInteract(AbilityPlayerContext context, PlayerInteractEvent event) {
        if (postSign != null && event.getAction() == Action.LEFT_CLICK_BLOCK && postSign.equals(event.getClickedBlock())) {
            if (!isSign(postSign)) {
                return;
            }
            event.setCancelled(true);
            Player target = linkedTarget == null ? null : Bukkit.getPlayer(linkedTarget);
            if (target != null && target.getWorld().equals(context.player().getWorld())
                && canAffectEnemy(context, context.player(), target) && readyPulse(context)) {
                feedback.impact(context, target);
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
        Player player = context.player();
        if (postSign != null) {
            sendAbilityMessage(context, player, "failure", ChatColor.RED + "이미 연결된 팻말이 있습니다. 연결이 끝난 뒤 다시 사용하세요.");
            return;
        }
        String line = event.getLine(0);
        String name = line == null ? "" : line.trim();
        if (name.isEmpty()) {
            sendAbilityMessage(context, player, "failure", ChatColor.RED + "팻말 첫 줄에 접속 중인 적의 정확한 이름을 입력하세요.");
            return;
        }
        Player target = Bukkit.getPlayerExact(name);
        if (target == null || !target.isOnline()) {
            sendAbilityMessage(context, player, "failure", ChatColor.RED + "접속 중인 플레이어를 찾을 수 없습니다. 정확한 이름을 입력하세요: " + name);
            return;
        }
        if (!target.getWorld().equals(player.getWorld())) {
            sendAbilityMessage(context, player, "failure", ChatColor.RED + "같은 월드의 적만 팻말과 연결할 수 있습니다.");
            return;
        }
        if (context.plugin().game().isPlayerCombatProtectedByKilltime()) {
            sendKilltimeTargetMessage(context, player);
            return;
        }
        if (!canAffectEnemy(context, player, target)) {
            sendAbilityMessage(context, player, "failure", ChatColor.RED + "게임에 참가 중인 적만 팻말과 연결할 수 있습니다. 자신, 아군, 관전자, 탈락자는 연결할 수 없습니다.");
            return;
        }
        if (!useNormal(context, player)) {
            return;
        }
        linkedTarget = target.getUniqueId();
        postSign = event.getBlock();
        lastPulseMillis = 0L;
        final Block sign = postSign;
        player.sendMessage(ChatColor.RED + target.getName() + ChatColor.WHITE + " 를(을) 팻말과 연결시켰습니다.");
        target.sendMessage(ChatColor.RED + "부두술사가 당신을 위협합니다.");
        laterCleanup(context, 7, "부두 연결 해제", "부두 연결 해제", () -> {
            if (sign.equals(postSign)) {
                clearLink();
                if (isSign(sign)) {
                    sign.breakNaturally();
                }
            }
        });
    }

    private void clearLink() {
        postSign = null;
        linkedTarget = null;
        lastPulseMillis = 0L;
    }

    private boolean holdingSign(Player player) {
        ItemStack item = player.getItemInHand();
        if (item == null) {
            return false;
        }
        // Legacy plugin remapping turns newer sign materials into AIR; read the raw API value.
        try {
            return isSignMaterial((Material) ItemStack.class.getMethod("getType").invoke(item));
        } catch (ReflectiveOperationException ex) {
            return isSignMaterial(item.getType());
        }
    }

    private boolean isSign(Block block) {
        return block != null && block.getState() instanceof Sign;
    }

    private boolean isSignMaterial(Material material) {
        if (material == null) {
            return false;
        }
        String name = ((Enum<?>) material).name();
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
