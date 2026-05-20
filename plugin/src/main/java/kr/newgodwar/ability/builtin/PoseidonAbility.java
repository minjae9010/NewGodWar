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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@AbilityInfo(
    id = "poseidon",
    name = "포세이돈",
    description = "물을 만들어 전장을 바꾸고 물가에서 전투 능력이 크게 올라갑니다.",
    normalSkill = "10블록 안의 바라보는 위치에 8초 동안 3x3 해역을 만듭니다.",
    normalStoneCost = 12,
    normalCooldownSeconds = 45,
    advancedSkill = "반경 12블록 적에게 해일 피해를 주고 밀쳐내며 약화/감속시킵니다.",
    advancedStoneCost = 28,
    advancedCooldownSeconds = 115,
    passiveSkill = "익사를 무시하고 물속에서 신속, 재생, 저항과 물 전투 피해 증가를 얻습니다.",
    grade = AbilityGrade.S
)
final class PoseidonAbility extends BaseAbility {
    private static final int SEA_RADIUS = 1;
    private static final int SEA_DURATION_SECONDS = 8;
    private static final int TIDAL_RANGE = 12;
    private static final double TIDAL_DAMAGE = 4.0D;

    @Override
    public void onAssign(AbilityPlayerContext context) {
        effect(context.player(), PotionEffectType.WATER_BREATHING, 24 * 60 * 60, 0);
    }

    @Override
    public void onRemove(AbilityPlayerContext context) {
        context.player().removePotionEffect(PotionEffectType.WATER_BREATHING);
    }

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        Block base = targetBlock(player, 10);
        Location center = base.getLocation().add(0, 1, 0);
        if (!canPlaceWater(center.getBlock())) {
            player.sendMessage(ChatColor.RED + "물을 만들 공간이 없습니다.");
            return;
        }
        if (useNormal(context, player)) {
            createTemporarySea(context, center, SEA_RADIUS, SEA_DURATION_SECONDS, "해역 소멸", "해역 소멸");
            effect(player, PotionEffectType.SPEED, 6, 1);
            effect(player, PotionEffectType.REGENERATION, 4, 0);
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        List<Player> targets = nearbyPlayers(context, player, TIDAL_RANGE, false);
        if (targets.isEmpty()) {
            player.sendMessage("능력을 사용할 수 있는 대상이 없습니다.");
            return;
        }
        if (useAdvanced(context, player)) {
            createTemporarySea(context, player.getLocation(), 2, 6, "해일 소멸", "해일 소멸");
            push(context, player, targets, 2.6D, 6L);
            for (Player target : targets) {
                damage(target, TIDAL_DAMAGE, player);
                createTemporarySea(context, target.getLocation(), 1, 6, "해일 소멸", "해일 소멸");
                effect(target, "SLOWNESS", "SLOW", 8, 2);
                effect(target, PotionEffectType.WEAKNESS, 8, 0);
                effect(target, PotionEffectType.CONFUSION, 5, 0);
            }
        }
    }

    @Override
    public void onTick(AbilityPlayerContext context) {
        Player player = context.player();
        effect(player, PotionEffectType.WATER_BREATHING, 3, 0);
        if (touchingWater(player)) {
            effect(player, PotionEffectType.SPEED, 3, 1);
            effect(player, PotionEffectType.REGENERATION, 3, 0);
            effect(player, "RESISTANCE", "DAMAGE_RESISTANCE", 3, 0);
        }
    }

    @Override
    public void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {
        if (attacker && (touchingWater(context.player()) || touchingWater(opponent))) {
            event.setDamage(event.getDamage() * 1.25D);
            if (oneIn(4)) {
                effect(opponent, "SLOWNESS", "SLOW", 3, 0);
            }
        }
    }

    @Override
    public void onGenericDamage(AbilityPlayerContext context, EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.DROWNING) {
            event.setCancelled(true);
        } else if (fire(event.getCause()) && touchingWater(context.player())) {
            event.setDamage(event.getDamage() * 0.5D);
        }
    }

    @Override
    public void onRespawn(AbilityPlayerContext context, PlayerRespawnEvent event) {
        respawnEffect(context, PotionEffectType.WATER_BREATHING, 24 * 60 * 60, 0);
    }

    private void createTemporarySea(final AbilityPlayerContext context, Location center, int radius, int seconds, String timerName, String triggerText) {
        final Map<Location, Material> oldBlocks = new LinkedHashMap<Location, Material>();
        int bx = center.getBlockX();
        int by = center.getBlockY();
        int bz = center.getBlockZ();
        for (int x = bx - radius; x <= bx + radius; x++) {
            for (int z = bz - radius; z <= bz + radius; z++) {
                Block block = new Location(center.getWorld(), x, by, z).getBlock();
                if (canPlaceWater(block)) {
                    oldBlocks.put(block.getLocation(), block.getType());
                    block.setType(Material.WATER);
                }
            }
        }
        later(context, seconds, timerName, triggerText, () -> {
            for (Map.Entry<Location, Material> entry : oldBlocks.entrySet()) {
                Block block = entry.getKey().getBlock();
                if (isWater(block.getType())) {
                    block.setType(entry.getValue());
                }
            }
        });
    }

    private boolean touchingWater(Player player) {
        return isWater(player.getLocation().getBlock().getType())
            || isWater(player.getEyeLocation().getBlock().getType())
            || isWater(player.getLocation().clone().add(0.0D, -0.1D, 0.0D).getBlock().getType());
    }

    private boolean canPlaceWater(Block block) {
        return block.getType() == Material.AIR || isWater(block.getType());
    }

    private boolean isWater(Material material) {
        return material == Material.WATER || material == Material.STATIONARY_WATER;
    }
}
