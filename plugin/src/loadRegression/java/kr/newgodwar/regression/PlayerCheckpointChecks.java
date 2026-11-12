package kr.newgodwar.regression;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.game.GameSessionStore;
import kr.newgodwar.game.GameState;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Real online inventories, ender chests, stats and a world-to-player transfer across process kills. */
final class PlayerCheckpointChecks {
    private final NewGodWarPlugin core;
    private final World world;
    private final List<Player> players;

    PlayerCheckpointChecks(NewGodWarPlugin core, World world, List<Player> players) {
        this.core = core; this.world = world; this.players = players;
    }

    @SuppressWarnings("unchecked") void seed(int generation) throws Exception {
        require(core.game().state() == GameState.RUNNING, "Checkpoint match is not running");
        Field field = core.game().getClass().getDeclaredField("kills"); field.setAccessible(true);
        Map<UUID, Integer> kills = (Map<UUID, Integer>) field.get(core.game());
        world.getBlockAt(5, 65, 55).setType(Material.CHEST);
        Chest chest = (Chest) world.getBlockAt(5, 65, 55).getState();
        chest.getInventory().clear();
        if (generation == 1) chest.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 16));
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            player.getInventory().clear();
            player.getEnderChest().clear();
            // Swap each pair's unique stack in generation two; neither copy may be lost/duplicated.
            int owner = generation == 1 ? i : i ^ 1;
            player.getInventory().setItem(0, named(Material.GOLD_INGOT, owner + 1, "checkpoint-" + generation + "-" + owner));
            player.getEnderChest().setItem(3, named(Material.EMERALD, i + 1, "ender-" + generation + "-" + i));
            player.setLevel(20 + generation + i);
            player.setExp(0.25f);
            player.setStatistic(Statistic.JUMP, generation * 100 + i);
            AdvancementProgress progress = player.getAdvancementProgress(advancement(generation));
            for (String criterion : progress.getRemainingCriteria()) progress.awardCriteria(criterion);
            kills.put(player.getUniqueId(), generation * 100 + i);
        }
        if (generation == 2) players.get(0).getInventory().setItem(8, new ItemStack(Material.DIAMOND, 16));
    }

    void verifySession(int generation) throws Exception {
        YamlConfiguration data = new GameSessionStore(core.getDataFolder()).load();
        require(data != null && data.getString("state").equals("RUNNING"), "Session was not committed");
        for (int i = 0; i < players.size(); i++) {
            require(data.getInt("kills." + players.get(i).getUniqueId()) == generation * 100 + i,
                "Periodic session checkpoint missed " + players.get(i).getName());
        }
    }

    void verify(int generation) throws Exception {
        require(!core.game().isRecovering() && core.game().state() == GameState.RUNNING, "Match recovery failed");
        verifySession(generation);
        Chest chest = (Chest) world.getBlockAt(5, 65, 55).getState();
        int diamonds = count(chest.getInventory().getContents(), Material.DIAMOND);
        require(diamonds == (generation == 1 ? 16 : 0), "World side of transfer not recovered");
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            int owner = generation == 1 ? i : i ^ 1;
            require(named(Material.GOLD_INGOT, owner + 1, "checkpoint-" + generation + "-" + owner).equals(player.getInventory().getItem(0)),
                "Player inventory lost/duplicated: " + player.getName());
            require(named(Material.EMERALD, i + 1, "ender-" + generation + "-" + i).equals(player.getEnderChest().getItem(3)),
                "Ender inventory lost: " + player.getName());
            require(player.getLevel() == 20 + generation + i && Math.abs(player.getExp() - 0.25f) < 0.001f, "XP not recovered");
            require(player.getStatistic(Statistic.JUMP) == generation * 100 + i, "Statistics not recovered");
            for (int saved = 1; saved <= generation; saved++) {
                require(player.getAdvancementProgress(advancement(saved)).isDone(), "Advancement not recovered");
            }
            require(core.game().teamOf(player) != null && core.abilities().get(player) != null, "Team/ability not recovered");
            int carried = count(player.getInventory().getContents(), Material.DIAMOND);
            require(carried == (generation == 2 && i == 0 ? 16 : 0), "Player side of transfer not recovered");
            diamonds += carried;
        }
        require(diamonds == 16, "Item transfer lost or duplicated diamonds");
    }

    private static Advancement advancement(int generation) {
        Advancement advancement = Bukkit.getAdvancement(NamespacedKey.minecraft(generation == 1 ? "story/mine_stone" : "story/upgrade_tools"));
        require(advancement != null, "Test advancement is missing");
        return advancement;
    }

    private static ItemStack named(Material type, int amount, String name) {
        ItemStack item = new ItemStack(type, amount);
        ItemMeta meta = item.getItemMeta(); meta.setDisplayName(name); item.setItemMeta(meta);
        return item;
    }

    private static int count(ItemStack[] items, Material material) {
        int count = 0;
        for (ItemStack item : items) if (item != null && item.getType() == material) count += item.getAmount();
        return count;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
