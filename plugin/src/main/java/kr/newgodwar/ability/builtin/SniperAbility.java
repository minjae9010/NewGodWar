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
    id = "sniper",
    name = "저격수",
    description = "웅크려 저격 자세를 잡은 뒤 다음 화살을 초고속으로 발사합니다.",
    normalSkill = "활을 들고 웅크린 채 좌클릭하면 4초 후 저격 모드가 활성화됩니다.",
    normalStoneCost = 0,
    advancedSkill = "저격 모드에서 발사한 다음 화살의 속도를 크게 높입니다.",
    advancedStoneCost = 6,
    advancedCooldownSeconds = 18,
    passiveSkill = "배정 시 활과 화살 10개를 받고 리스폰 시 화살 2개를 받습니다.",
    grade = AbilityGrade.A
)
final class SniperAbility extends BaseAbility {
    private boolean ready;

    @Override
    public void onAssign(AbilityPlayerContext context) {
        give(context.player(), Material.BOW, 1);
        give(context.player(), Material.ARROW, 10);
    }

    @Override
    public void onRespawn(AbilityPlayerContext context, PlayerRespawnEvent event) {
        give(context.player(), Material.ARROW, 2);
    }

    @Override
    public void onInteract(final AbilityPlayerContext context, PlayerInteractEvent event) {
        final Player player = context.player();
        if (isLeft(event.getAction()) && holding(player, Material.BOW) && player.isSneaking() && !ready) {
            ready = true;
            player.sendMessage("스나이핑 모드를 준비합니다.");
            later(context, 4, "저격 준비", "저격 모드 활성화", () -> {
                if (ready) {
                    player.sendMessage("스나이핑 모드가 활성화되었습니다.");
                }
            });
        }
    }

    @Override
    public void onProjectileLaunch(AbilityPlayerContext context, ProjectileLaunchEvent event) {
        if (ready && event.getEntity() instanceof Arrow && useAdvanced(context, context.player(), 0)) {
            ready = false;
            event.getEntity().setVelocity(context.player().getEyeLocation().getDirection().multiply(20));
        }
    }
}
