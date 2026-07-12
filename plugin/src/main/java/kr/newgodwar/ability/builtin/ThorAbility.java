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
    id = "thor",
    name = "토르",
    description = "번개 망치로 적을 내려치고 도끼 공격이 강해집니다.",
    normalSkill = "바라보는 적에게 번개를 내립니다.",
    normalStoneCost = 16,
    normalCooldownSeconds = 75,
    advancedSkill = "주변 적에게 낙뢰를 내리고 밀쳐냅니다.",
    advancedStoneCost = 30,
    advancedCooldownSeconds = 145,
    passiveSkill = "도끼 공격 피해가 증가하고 번개 피해를 무시합니다.",
    grade = AbilityGrade.S
)
final class ThorAbility extends BaseAbility {
    @Override
    public void onPrepare(AbilityPlayerContext context) {
        give(context.player(), Material.IRON_AXE, 1);
    }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        Player target = targetPlayerInSight(context, player, 25, false);
        if (target == null) {
            return;
        }
        if (useNormal(context, player)) {
            strikeLightning(context, player, target.getLocation());
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = nearbyPlayers(context, player, 9, false);
        if (targets.isEmpty()) {
            player.sendMessage("능력을 사용할 수 있는 대상이 없습니다.");
            return;
        }
        if (useAdvanced(context, player)) {
            for (Player target : targets) {
                strikeLightning(context, player, target.getLocation());
            }
            push(context, player, targets, 2.0D, 8L);
        }
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (attacker && isAxe(context.player().getItemInHand().getType())) {
            event.setDamage(event.getDamage() * 1.25D);
        }
    }

    @Override
    public void onGenericDamage(AbilityPlayerContext context, EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.LIGHTNING) {
            event.setCancelled(true);
            context.player().setFireTicks(0);
        }
    }

    private boolean isAxe(Material material) {
        if (material == null) {
            return false;
        }
        String name = material.name();
        return name.endsWith("_AXE") && !name.endsWith("_PICKAXE");
    }
}
