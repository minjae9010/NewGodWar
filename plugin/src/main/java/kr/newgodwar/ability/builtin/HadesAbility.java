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
    id = "hades",
    name = "하데스",
    description = "공중 섬 아래 나락으로 적을 떨어뜨리고 사망 시 일부 장비 보존을 노립니다.",
    normalSkill = "반경 2블록 생물과 자신을 나락으로 떨어뜨립니다.",
    normalStoneCost = 24,
    normalCooldownSeconds = 120,
    advancedSkill = "반경 4블록 생물을 나락으로 떨어뜨립니다.",
    advancedStoneCost = 42,
    advancedCooldownSeconds = 190,
    passiveSkill = "사망 시 40% 확률로 인벤토리와 방어구를 보존합니다.",
    grade = AbilityGrade.A
)
final class HadesAbility extends BaseAbility {
    private ItemStack[] savedInventory;
    private ItemStack[] savedArmor;

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            abyss(player, 2, true);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useAdvanced(context, player)) {
            abyss(player, 4, false);
        }
    }

    private void abyss(Player player, int radius, boolean includeSelf) {
        Location destination = player.getLocation().clone();
        destination.setY(-2.0D);
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity) {
                entity.teleport(destination);
            }
        }
        if (includeSelf) {
            player.teleport(destination);
        }
    }

    @Override
    public void onDeath(AbilityPlayerContext context, PlayerDeathEvent event) {
        if (event.getEntity().equals(context.player()) && rollChance(4, 10)) {
            savedInventory = context.player().getInventory().getContents();
            savedArmor = context.player().getInventory().getArmorContents();
            event.setKeepInventory(false);
            event.getDrops().clear();
            context.player().sendMessage(ChatColor.DARK_PURPLE + "하데스의 권능으로 인벤토리가 보존되었습니다.");
        }
    }

    @Override
    public void onRespawn(AbilityPlayerContext context, PlayerRespawnEvent event) {
        Player player = context.player();
        boolean restored = false;
        if (savedInventory != null) {
            player.getInventory().setContents(savedInventory);
            savedInventory = null;
            restored = true;
        }
        if (savedArmor != null) {
            player.getInventory().setArmorContents(savedArmor);
            savedArmor = null;
            restored = true;
        }
        if (restored) {
            player.sendMessage(ChatColor.DARK_PURPLE + "보존된 인벤토리가 복원되었습니다.");
        }
    }
}
