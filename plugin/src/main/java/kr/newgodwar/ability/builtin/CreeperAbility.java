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
    id = "creeper",
    name = "크리퍼",
    description = "자폭으로 섬 지형을 흔들고 번개를 맞으면 다음 폭발력이 커집니다.",
    normalSkill = "자신의 위치에서 자폭 폭발을 일으키고 사망합니다.",
    normalStoneCost = 24,
    normalCooldownSeconds = 90,
    advancedSkill = "없음",
    advancedStoneCost = 0,
    passiveSkill = "번개를 맞으면 다음 자폭 폭발력이 증가합니다.",
    grade = AbilityGrade.B
)
final class CreeperAbility extends BaseAbility {
    private boolean plasma;

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            createExplosion(context, player, player.getLocation(), plasma ? 6.0F : 3.0F, false, true);
            player.setHealth(0.0D);
        }
    }

    @Override
    public void onGenericDamage(AbilityPlayerContext context, EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.LIGHTNING) {
            plasma = true;
        }
    }

    @Override
    public void onDeath(AbilityPlayerContext context, PlayerDeathEvent event) {
        if (event.getEntity().equals(context.player())) {
            plasma = false;
        }
    }
}
