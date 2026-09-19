package kr.newgodwar.regression;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.ability.api.AbilityDefinition;
import kr.newgodwar.ability.api.AbilityPlayerContext;
import kr.newgodwar.ability.builtin.BaseAbility;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Uses Paper's real inventory stacking and item metadata with recorded player notifications/drops. */
final class ItemGrantRegressionChecks {
    private final List<String> messages = new ArrayList<String>();
    private final List<ItemStack> drops = new ArrayList<ItemStack>();
    private final List<String> events = new ArrayList<String>();
    private final Inventory inventory = Bukkit.createInventory(null, 36);
    private final GrantAbility ability = new GrantAbility();
    private Player player;

    void run(NewGodWarPlugin core) {
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[] {World.class}, (p, m, a) -> {
            if (m.getName().equals("dropItemNaturally")) {
                Location location = (Location) a[0];
                require(location.getX() == 12 && location.getY() == 80 && location.getZ() == 34,
                    "Overflow was not dropped at the recipient's location");
                drops.add(((ItemStack) a[1]).clone());
                events.add("drop");
                return null;
            }
            throw new AssertionError("Unexpected world call: " + m.getName());
        });
        PlayerInventory storage = (PlayerInventory) Proxy.newProxyInstance(PlayerInventory.class.getClassLoader(),
            new Class<?>[] {PlayerInventory.class}, (p, m, a) -> {
                if (m.getName().equals("addItem")) return inventory.addItem((ItemStack[]) a[0]);
                throw new AssertionError("Unexpected inventory call: " + m.getName());
            });
        player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[] {Player.class}, (p, m, a) -> {
            if (m.getName().equals("getInventory")) return storage;
            if (m.getName().equals("getWorld")) return world;
            if (m.getName().equals("getLocation")) return new Location(world, 12, 80, 34);
            if (m.getName().equals("sendMessage")) {
                messages.add(ChatColor.stripColor((String) a[0]));
                events.add("message");
                return null;
            }
            throw new AssertionError("Unexpected player call: " + m.getName());
        });

        reset(false);
        ability.grant(player, Material.ARROW, 10);
        require(amount(Material.ARROW) == 10 && drops.isEmpty() && messages.isEmpty(),
            "Normal grants must fit without an overflow warning");

        reset(true);
        ability.grant(player, Material.ARROW, 10);
        require(amount(Material.ARROW) == 0 && drops.size() == 1 && drops.get(0).getAmount() == 10,
            "Full inventory lost the reward");
        require(events.equals(Arrays.asList("message", "drop")), "Warn before dropping overflow");
        require(messages.get(0).contains("인벤토리가 꽉 찼습니다"), "Missing inventory-full explanation");

        reset(true);
        inventory.setItem(0, new ItemStack(Material.ARROW, 60));
        ItemStack reward = new ItemStack(Material.ARROW, 10);
        ability.grant(player, reward);
        require(amount(Material.ARROW) == 64 && drops.size() == 1 && drops.get(0).getAmount() == 6,
            "Partial fit must drop only the remainder without loss or duplication");
        require(reward.getAmount() == 10, "Granting must not mutate the caller's item stack");

        reset(true);
        inventory.setItem(0, new ItemStack(Material.ARROW, 54));
        ability.grant(player, Material.ARROW, 10);
        require(amount(Material.ARROW) == 64 && drops.isEmpty() && messages.isEmpty(),
            "Occupied slots with stacking space must accept the entire reward");

        reset(true);
        inventory.setItem(0, null);
        ability.grant(player, new ItemStack(Material.ARROW, 70), new ItemStack(Material.BOW));
        require(amount(Material.ARROW) == 64 && drops.size() == 2 && messages.size() == 1,
            "Multiple rewards must share capacity and produce one warning per grant");
        require(drops.get(0).getType() == Material.ARROW && drops.get(0).getAmount() == 6
            && drops.get(1).getType() == Material.BOW && drops.get(1).getAmount() == 1,
            "Overflow lost a stackable or unstackable reward");

        reset(true);
        ItemStack food = new ItemStack(Material.BREAD);
        ItemMeta meta = food.getItemMeta();
        meta.setDisplayName("식신의 성찬");
        meta.setLore(Arrays.asList("NGW_SIKSIN_FOOD:owner:TEAM:SPEED"));
        food.setItemMeta(meta);
        ability.grant(player, food);
        require(drops.size() == 1 && drops.get(0).equals(food), "Overflow stripped special food metadata");

        for (String id : Arrays.asList("harry", "hermione")) {
            reset(true);
            AbilityDefinition definition = core.abilities().registry().get(id);
            definition.create().onPrepare(new AbilityPlayerContext(core, player, definition));
            require(messages.size() == 1 && drops.size() == 1, id + " bypassed overflow handling");
            require(drops.get(0).getItemMeta() instanceof BookMeta
                && ((BookMeta) drops.get(0).getItemMeta()).hasPages(), id + " lost its spellbook pages");
        }
        for (String id : Arrays.asList("thor", "artemis")) {
            reset(true);
            AbilityDefinition definition = core.abilities().registry().get(id);
            definition.create().onPrepare(new AbilityPlayerContext(core, player, definition));
            String expected = id.equals("thor") ? "묠니르" : "은빛 사냥활";
            require(!drops.isEmpty() && drops.get(0).hasItemMeta()
                && expected.equals(ChatColor.stripColor(drops.get(0).getItemMeta().getDisplayName())),
                id + " did not grant its named weapon with intact metadata");
        }
        core.getLogger().info("PASS item grants: normal/full/partial/stacking/multiple rewards, original stacks and special item metadata");
    }

    private void reset(boolean full) {
        inventory.clear();
        messages.clear();
        drops.clear();
        events.clear();
        if (full) {
            for (int slot = 0; slot < inventory.getSize(); slot++) {
                inventory.setItem(slot, new ItemStack(Material.STONE, 64));
            }
        }
    }

    private int amount(Material material) {
        int total = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == material) total += item.getAmount();
        }
        return total;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class GrantAbility extends BaseAbility {
        void grant(Player player, Material material, int amount) { give(player, material, amount); }
        void grant(Player player, ItemStack... items) { give(player, items); }
    }
}
