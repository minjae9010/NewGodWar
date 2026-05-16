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
    description = "스나이핑 모드에서 매우 빠른 화살을 발사합니다.",
    normalSkill = "활을 들고 웅크린 채 좌클릭하면 저격을 준비합니다.",
    normalStoneCost = 0,
    advancedSkill = "준비 후 화살 발사 시 매우 빠른 화살을 발사합니다.",
    advancedStoneCost = 5,
    passiveSkill = "배정 시 활과 화살을 받습니다."
)
final class SniperAbility extends BaseAbility {
    @Override
    public void onAssign(AbilityPlayerContext context) {
        give(context.player(), Material.BOW, 1);
        give(context.player(), Material.ARROW, 10);
    }

    @Override
    public void onInteract(final AbilityPlayerContext context, PlayerInteractEvent event) {
        final Player player = context.player();
        if (isLeft(event.getAction()) && holding(player, Material.BOW) && player.isSneaking() && !ready) {
            ready = true;
            player.sendMessage("스나이핑 모드를 준비합니다.");
            later(context, 4, new Runnable() {
                @Override
                public void run() {
                    if (ready) {
                        player.sendMessage("스나이핑 모드가 활성화되었습니다.");
                    }
                }
            });
        }
    }

    @Override
    public void onProjectileLaunch(AbilityPlayerContext context, ProjectileLaunchEvent event) {
        if (ready && event.getEntity() instanceof Arrow && use(context, context.player(), 0, COBBLESTONE, 5, 50)) {
            ready = false;
            event.getEntity().setVelocity(context.player().getEyeLocation().getDirection().multiply(20));
        }
    }
}
