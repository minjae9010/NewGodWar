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
    id = "persephone",
    name = "페르세포네",
    description = "봄의 회복과 저승의 뿌리로 전장을 보조합니다.",
    normalSkill = "바라보는 적을 짧게 속박합니다.",
    normalStoneCost = 12,
    normalCooldownSeconds = 55,
    advancedSkill = "주변 아군을 조금 회복시키고 재생을 부여합니다.",
    advancedStoneCost = 22,
    advancedCooldownSeconds = 115,
    passiveSkill = "치명상을 입으면 가끔 짧은 재생을 얻습니다.",
    grade = AbilityGrade.A
)
final class PersephoneAbility extends BaseAbility {
    private long bloomReadyAt;

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        Player target = targetPlayerInSight(context, player, 18, false);
        if (target == null) {
            return;
        }
        if (useNormal(context, player)) {
            effect(target, "SLOWNESS", "SLOW", 6, 3);
            effect(target, PotionEffectType.WEAKNESS, 6, 0);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = nearbyPlayers(context, player, 7, true);
        targets.add(player);
        if (useAdvanced(context, player)) {
            for (Player target : targets) {
                target.setHealth(Math.min(target.getMaxHealth(), target.getHealth() + 4.0D));
                effect(target, PotionEffectType.REGENERATION, 5, 0);
            }
        }
    }

    @Override
    public void onGenericDamage(AbilityPlayerContext context, EntityDamageEvent event) {
        long now = System.currentTimeMillis();
        Player player = context.player();
        if (now < bloomReadyAt || event.getFinalDamage() < player.getHealth() || player.getHealth() > 8.0D) {
            return;
        }
        if (rollChance(3, 10)) {
            bloomReadyAt = now + context.plugin().abilities().scaleCooldownMillis(60 * 1000L);
            effect(player, PotionEffectType.REGENERATION, 5, 0);
            player.sendMessage(ChatColor.GREEN + "페르세포네의 봄기운이 잠시 피어납니다.");
        }
    }
}
