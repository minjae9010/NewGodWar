package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

@AbilityInfo(
    id = "nature",
    name = "자연계",
    description = "세계수의 자녀로서 주변 식물을 빠르게 자라게 하고 자연의 힘을 나눕니다.",
    normalSkill = "무작위 식물을 얻습니다.",
    normalStoneCost = 10,
    normalCooldownSeconds = 35,
    advancedSkill = "팀원들에게 치유 버프를 부여합니다.",
    advancedStoneCost = 24,
    advancedCooldownSeconds = 110,
    passiveSkill = "주변 식물이 빠르게 자라며, 식물을 훼손하면 디버프를 받고 식물 근처에서는 버프를 얻습니다.",
    grade = AbilityGrade.A
)
final class NatureAbility extends BaseAbility {
    private static final int GROW_RADIUS = 5;
    private static final int BUFF_RADIUS = 4;
    private static final int MAX_GROWTHS_PER_TICK = 8;

    @Override
    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (useNormal(context, player)) {
            player.getInventory().addItem(randomPlantItem());
        }
    }

    @Override
    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
        if (!useAdvanced(context, player)) {
            return;
        }
        List<Player> targets = alliedPlayers(context, player, true);
        if (targets.isEmpty()) {
            targets.add(player);
        }
        for (Player target : targets) {
            effect(target, PotionEffectType.REGENERATION, 15, 1);
            effect(target, "ABSORPTION", "ABSORPTION", 12, 0);
        }
    }

    @Override
    public void onTick(AbilityPlayerContext context) {
        Player player = context.player();
        growNearbyPlants(player);
        if (hasPlantNearby(player.getLocation(), BUFF_RADIUS)) {
            effect(player, PotionEffectType.REGENERATION, 6, 0);
            effect(player, "HASTE", "FAST_DIGGING", 6, 0);
        }
    }

    @Override
    public void onBlockBreak(AbilityPlayerContext context, BlockBreakEvent event) {
        if (isPlant(event.getBlock())) {
            Player player = context.player();
            effect(player, PotionEffectType.WEAKNESS, 9, 0);
            effect(player, "SLOWNESS", "SLOW", 9, 0);
            player.sendMessage(ChatColor.DARK_GREEN + "식물을 훼손해 자연의 반발을 받았습니다.");
        }
    }

    private ItemStack randomPlantItem() {
        PlantReward[] rewards = new PlantReward[] {
            new PlantReward("WHEAT_SEEDS", "SEEDS", 3),
            new PlantReward("CARROT", "CARROT_ITEM", 2),
            new PlantReward("POTATO", "POTATO_ITEM", 2),
            new PlantReward("BEETROOT_SEEDS", "BEETROOT_SEEDS", 2),
            new PlantReward("OAK_SAPLING", "SAPLING", 1),
            new PlantReward("SUGAR_CANE", "SUGAR_CANE", 2),
            new PlantReward("CACTUS", "CACTUS", 1),
            new PlantReward("POPPY", "RED_ROSE", 1),
            new PlantReward("DANDELION", "YELLOW_FLOWER", 1),
            new PlantReward("BROWN_MUSHROOM", "BROWN_MUSHROOM", 1),
            new PlantReward("PUMPKIN_SEEDS", "PUMPKIN_SEEDS", 2),
            new PlantReward("MELON_SEEDS", "MELON_SEEDS", 2)
        };
        PlantReward reward = rewards[RANDOM.nextInt(rewards.length)];
        Material material = resolveMaterial(reward.modernName, reward.legacyName);
        if (material == null) {
            material = resolveMaterial("WHEAT_SEEDS", "SEEDS");
        }
        return new ItemStack(material == null ? Material.SEEDS : material, reward.amount);
    }

    private void growNearbyPlants(Player player) {
        Location center = player.getLocation();
        World world = player.getWorld();
        int grown = 0;
        for (int x = -GROW_RADIUS; x <= GROW_RADIUS && grown < MAX_GROWTHS_PER_TICK; x++) {
            for (int y = -2; y <= 2 && grown < MAX_GROWTHS_PER_TICK; y++) {
                for (int z = -GROW_RADIUS; z <= GROW_RADIUS && grown < MAX_GROWTHS_PER_TICK; z++) {
                    if (!rollPercent(18)) {
                        continue;
                    }
                    Block block = world.getBlockAt(center.getBlockX() + x, center.getBlockY() + y, center.getBlockZ() + z);
                    if (growPlant(block)) {
                        grown++;
                    }
                }
            }
        }
    }

    private boolean growPlant(Block block) {
        String name = block.getType().name();
        if ("CROPS".equals(name) || "CARROT".equals(name) || "POTATO".equals(name)) {
            return growAge(block, 7);
        }
        if ("BEETROOT_BLOCK".equals(name) || "NETHER_WARTS".equals(name)) {
            return growAge(block, 3);
        }
        if ("MELON_STEM".equals(name) || "PUMPKIN_STEM".equals(name)) {
            return growAge(block, 7);
        }
        if ("COCOA".equals(name)) {
            return growAge(block, 2);
        }
        if ("SUGAR_CANE_BLOCK".equals(name) || "SUGAR_CANE".equals(name) || "CACTUS".equals(name)) {
            return growColumn(block, 3);
        }
        if ("SAPLING".equals(name) && rollPercent(12) && safeTreeLocation(block)) {
            Material sapling = block.getType();
            block.setType(Material.AIR);
            if (block.getWorld().generateTree(block.getLocation(), TreeType.TREE)) {
                return true;
            }
            block.setType(sapling);
        }
        return false;
    }

    private boolean growAge(Block block, int maxAge) {
        int data = block.getData();
        int age = data & 0x7;
        if (age >= maxAge) {
            return false;
        }
        BlockState state = block.getState();
        state.setRawData((byte) ((data & ~0x7) | Math.min(maxAge, age + 1)));
        return state.update(true);
    }

    private boolean growColumn(Block block, int maxHeight) {
        int height = 1;
        Block current = block;
        while (current.getRelative(BlockFace.DOWN).getType() == block.getType()) {
            current = current.getRelative(BlockFace.DOWN);
            height++;
        }
        current = block;
        while (current.getRelative(BlockFace.UP).getType() == block.getType()) {
            current = current.getRelative(BlockFace.UP);
            height++;
        }
        if (height >= maxHeight || current.getRelative(BlockFace.UP).getType() != Material.AIR) {
            return false;
        }
        current.getRelative(BlockFace.UP).setType(block.getType());
        return true;
    }

    private boolean safeTreeLocation(Block sapling) {
        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y <= 7; y++) {
                for (int z = -2; z <= 2; z++) {
                    if (sapling.getRelative(x, y, z).getType() == Material.DIAMOND_BLOCK) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean hasPlantNearby(Location location, int radius) {
        World world = location.getWorld();
        int baseX = location.getBlockX();
        int baseY = location.getBlockY();
        int baseZ = location.getBlockZ();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (isPlant(world.getBlockAt(baseX + x, baseY + y, baseZ + z))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isPlant(Block block) {
        if (block == null) {
            return false;
        }
        String name = block.getType().name();
        return "CROPS".equals(name)
            || "CARROT".equals(name)
            || "POTATO".equals(name)
            || "BEETROOT_BLOCK".equals(name)
            || "NETHER_WARTS".equals(name)
            || "MELON_STEM".equals(name)
            || "PUMPKIN_STEM".equals(name)
            || "SAPLING".equals(name)
            || "SUGAR_CANE_BLOCK".equals(name)
            || "SUGAR_CANE".equals(name)
            || "CACTUS".equals(name)
            || "COCOA".equals(name)
            || "VINE".equals(name)
            || "WATER_LILY".equals(name)
            || "LONG_GRASS".equals(name)
            || "DOUBLE_PLANT".equals(name)
            || "YELLOW_FLOWER".equals(name)
            || "RED_ROSE".equals(name)
            || "BROWN_MUSHROOM".equals(name)
            || "RED_MUSHROOM".equals(name)
            || "LEAVES".equals(name)
            || "LEAVES_2".equals(name)
            || "LOG".equals(name)
            || "LOG_2".equals(name)
            || name.endsWith("_SAPLING")
            || name.endsWith("_LEAVES")
            || name.endsWith("_LOG")
            || name.endsWith("_STEM")
            || name.endsWith("_FLOWER");
    }

    private Material resolveMaterial(String modernName, String legacyName) {
        Material material = Material.matchMaterial(modernName);
        if (material == null) {
            material = Material.matchMaterial(legacyName);
        }
        if (material == null) {
            material = Material.matchMaterial("LEGACY_" + legacyName);
        }
        return material;
    }

    private static final class PlantReward {
        private final String modernName;
        private final String legacyName;
        private final int amount;

        private PlantReward(String modernName, String legacyName, int amount) {
            this.modernName = modernName;
            this.legacyName = legacyName;
            this.amount = amount;
        }
    }
}
