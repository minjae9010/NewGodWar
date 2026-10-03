package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import kr.newgodwar.ability.feedback.AbilityStyle;
import kr.newgodwar.ability.feedback.AbilityTheme;
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
    description = "공중 섬 아래 나락으로 적을 떨어뜨리고 죽으면 낮은 확률로 장비를 보존해요.",
    normalSkill = "블레이즈 막대기 좌클릭: 반경 2블록의 적·몹과 자신을 나락으로 떨어뜨려요. 아군은 제외해요.",
    normalStoneCost = 30,
    normalCooldownSeconds = 150,
    advancedSkill = "블레이즈 막대기 우클릭: 반경 4블록의 적·몹을 나락으로 떨어뜨려요. 자신과 아군은 제외해요.",
    advancedStoneCost = 52,
    advancedCooldownSeconds = 240,
    passiveSkill = "죽으면 25% 확률로 인벤토리와 방어구를 보존해요.",
    grade = AbilityGrade.S
)
final class HadesAbility extends BaseAbility {
    private static final AbilityStyle STYLE = AbilityStyle.builder(AbilityTheme.SHADOW)
        .dedicated()
        .build();

    @Override
    public AbilityStyle style() { return STYLE; }

    private ItemStack[] savedInventory;
    private ItemStack[] savedArmor;

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            abyss(context, player, 2, true);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useAdvanced(context, player)) {
            abyss(context, player, 4, false);
        }
    }

    private void abyss(AbilityPlayerContext context, Player player, int radius, boolean includeSelf) {
        Location origin = player.getLocation(); origin.setPitch(0); origin.setYaw(0);
        if (!feedback.object(context, "abyss", AbilityDesigns.ABYSS, origin, 18, radius))
            feedback.modelOutline(context, origin, AbilityDesigns.ABYSS, 0, radius, feedback.effectViewers(context, origin));
        Location destination = player.getLocation().clone();
        destination.setY(abyssY(player.getWorld()));
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof LivingEntity) || entity.isDead()
                || origin.distanceSquared(entity.getLocation()) > (double) radius * radius) {
                continue;
            }
            if (entity instanceof Player) {
                Player target = (Player) entity;
                if (!canAffectEnemy(context, player, target)) {
                    continue;
                }
                context.plugin().abilities().rememberDamageSource(target, player);
            }
            entity.teleport(destination);
        }
        if (includeSelf) {
            player.teleport(destination);
        }
    }

    static double abyssY(World world) {
        try {
            return ((Number) world.getClass().getMethod("getMinHeight").invoke(world)).doubleValue() - 2.0D;
        } catch (NoSuchMethodException legacyWorld) {
            return -2.0D;
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot determine the world's abyss height", error);
        }
    }

    @Override
    public void onDeath(AbilityPlayerContext context, PlayerDeathEvent event) {
        if (event.getEntity().equals(context.player()) && rollChance(1, 4)) {
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
