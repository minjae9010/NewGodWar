package kr.newgodwar.regression;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.ability.api.AbilityDefinition;
import kr.newgodwar.ability.api.AbilityPlayerContext;
import kr.newgodwar.ability.builtin.BaseAbility;
import kr.newgodwar.game.*;
import kr.newgodwar.gui.GamblingGui;
import kr.newgodwar.listener.GameListener;
import kr.newgodwar.util.InventoryItems;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/** Uses a real CraftPlayer inventory, including metadata, armor and offhand slots. */
final class InventoryRegressionChecks {
    private final NewGodWarPlugin core;
    private Player player;
    private TrainingDummyEntity dummy;
    private final Set<UUID> originalItems = new HashSet<UUID>();

    private InventoryRegressionChecks(NewGodWarPlugin core) { this.core = core; }

    static void run(NewGodWarPlugin core) throws Exception { new InventoryRegressionChecks(core).run(); }

    private void run() throws Exception {
        Map<String, Object> config = new LinkedHashMap<String, Object>();
        for (String key : Arrays.asList("abilities.effects.enabled", "gambling.rewards.normal", "gambling.cost.cobblestone",
                "game.clear-inventory", "game.clear-inventory-on-stop", "lobby.teleport-on-game-stop")) {
            config.put(key, core.getConfig().get(key));
        }
        try {
            core.getConfig().set("abilities.effects.enabled", false);
            core.getConfig().set("lobby.teleport-on-game-stop", false);
            World world = Bukkit.getWorlds().get(0);
            // The smoke world's spawn can sit at the void boundary. Keep natural drops
            // inside a loaded chunk and within the world's entity height range.
            world.getChunkAt(0, 0).load();
            dummy = TrainingDummyEntity.spawn(new Location(world, 8.5, 100, 8.5), "InventoryCheck");
            player = dummy.player();
            for (Entity entity : player.getWorld().getEntities()) if (entity instanceof Item) originalItems.add(entity.getUniqueId());
            resources();
            rewards();
            deathDrops();
            offlineCleanup();
            core.getLogger().info("PASS inventory lifecycle: metadata costs, atomic insufficient costs, exact starter grants, gambling overflow, death drops and offline cleanup");
        } finally {
            if (player != null) for (Entity entity : new ArrayList<Entity>(player.getWorld().getEntities())) {
                if (entity instanceof Item && !originalItems.contains(entity.getUniqueId())) entity.remove();
            }
            if (dummy != null) dummy.remove();
            for (Map.Entry<String, Object> entry : config.entrySet()) core.getConfig().set(entry.getKey(), entry.getValue());
        }
    }

    private void resources() {
        player.getInventory().clear();
        ItemStack named = namedStones(8);
        player.getInventory().setItem(0, new ItemStack(Material.COBBLESTONE, 4));
        player.getInventory().setItem(1, named);
        player.getInventory().setItemInOffHand(new ItemStack(Material.COBBLESTONE, 20));
        AbilityDefinition definition = core.abilities().registry().get("ares");
        ResourceAbility ability = new ResourceAbility();
        AbilityPlayerContext context = new AbilityPlayerContext(core, player, definition);
        require(ability.cast(context, 10), "Named resources rejected");
        require(count(Material.COBBLESTONE) == 2 && player.getInventory().getItem(0) == null,
            "Mixed plain/named cost was not charged exactly once");
        require(player.getInventory().getItem(1).isSimilar(named), "Cost stripped remaining item metadata");
        require(player.getInventory().getItemInOffHand().getAmount() == 20, "Cost touched non-storage slots");
        ItemStack[] before = player.getInventory().getContents();
        ability.clearCooldowns();
        require(!ability.cast(context, 3) && Arrays.equals(before, player.getInventory().getContents()),
            "Insufficient cost partially consumed resources or used offhand to bypass the cost");
        require(ability.cooldownRemainingMillis(1) == 0, "Failed cost started cooldown");
    }

    private void rewards() throws Exception {
        player.getInventory().clear();
        player.getInventory().setItem(0, new ItemStack(Material.COBBLESTONE, 60));
        ItemStack grant = new ItemStack(Material.COBBLESTONE, 64);
        invoke(core.game(), "giveStarterItem", new Class<?>[] {Player.class, ItemStack.class}, player, grant);
        require(count(Material.COBBLESTONE) == 124 && grant.getAmount() == 64, "Starter grant duplicated or mutated items");
        player.getInventory().clear();
        invoke(core.game(), "giveStarterItem", new Class<?>[] {Player.class, ItemStack.class}, player, new ItemStack(Material.LAVA_BUCKET, 2));
        require(count(Material.LAVA_BUCKET) == 2, "Starter lost unstackable items");
        for (ItemStack item : player.getInventory().getStorageContents()) {
            require(item == null || item.getAmount() <= item.getMaxStackSize(), "Starter created an oversized stack");
        }

        Map<String, Object> reward = new LinkedHashMap<String, Object>();
        ItemStack prize = new ItemStack(Material.DIAMOND, 5);
        ItemMeta meta = prize.getItemMeta();
        meta.setDisplayName("Named reward"); prize.setItemMeta(meta);
        reward.put("chance", 1); reward.put("item", prize); reward.put("message", "Inventory regression reward");
        core.getConfig().set("gambling.rewards.normal", Collections.singletonList(reward));
        core.getConfig().set("gambling.cost.cobblestone", 32);
        GamblingGui gui = new GamblingGui(core);
        player.getInventory().clear();
        player.getInventory().setItem(0, namedStones(32));
        gamble(gui);
        require(count(Material.COBBLESTONE) == 0 && count(Material.DIAMOND) == 5, "Gambling bypassed named cost");
        gamble(gui);
        require(count(Material.DIAMOND) == 5, "Gambling granted a prize without sufficient resources");

        player.getInventory().clear();
        for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, new ItemStack(Material.STONE, 64));
        player.getInventory().setItem(0, new ItemStack(Material.COBBLESTONE, 64));
        ItemStack partial = prize.clone(); partial.setAmount(62);
        player.getInventory().setItem(1, partial);
        gamble(gui);
        require(count(Material.COBBLESTONE) == 32 && count(Material.DIAMOND) == 64,
            "Gambling partially fitting prize changed the wrong quantities");
        require(dropped(prize) == 3, "Gambling overflow lost/duplicated items or stripped metadata: dropped="
            + dropped(prize) + ", location=" + player.getLocation());
        gamble(gui);
        require(count(Material.DIAMOND) == 69 && dropped(prize) == 3,
            "Spending the last cost stack did not make space for a reward");
        require(prize.getAmount() == 5, "Gambling mutated configured reward");

        player.getInventory().setItem(0, new ItemStack(Material.COBBLESTONE, 64));
        gamble(gui);
        require(count(Material.COBBLESTONE) == 32 && dropped(prize) == 8,
            "Full inventory lost the complete gambling reward: cost=" + count(Material.COBBLESTONE)
                + ", diamonds=" + count(Material.DIAMOND) + ", dropped=" + dropped(prize)
                + ", contents=" + Arrays.toString(player.getInventory().getStorageContents()));
    }

    private void deathDrops() throws Exception {
        player.getInventory().clear();
        player.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        player.getInventory().setItemInOffHand(new ItemStack(Material.DIAMOND, 3));
        PlayerDeathEvent event = deathEvent(player);
        event.setKeepInventory(true);
        GameListener listener = new GameListener(core, core.game(), core.abilities(), core.nms());
        invoke(listener, "forceInventoryDrop", new Class<?>[] {PlayerDeathEvent.class}, event);
        require(amount(event.getDrops(), Material.DIAMOND_HELMET) == 1 && amount(event.getDrops(), Material.DIAMOND) == 3,
            "Armor/offhand were dropped more than once");
        invoke(listener, "forceInventoryDrop", new Class<?>[] {PlayerDeathEvent.class}, event);
        require(event.getDrops().size() == 2, "Existing drops were duplicated");
    }

    @SuppressWarnings("unchecked")
    private void offlineCleanup() throws Exception {
        core.getConfig().set("game.clear-inventory", true);
        core.getConfig().set("game.clear-inventory-on-stop", true);
        player.getInventory().clear();
        player.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 12));
        player.setGameMode(org.bukkit.GameMode.SPECTATOR);
        UUID uuid = player.getUniqueId();
        ((Map<UUID, GodTeam>) get(core.game(), "teams")).put(uuid, GodTeam.RED);
        ((Set<UUID>) get(core.game(), "preparedPlayerInventories")).add(uuid);
        field(core.game(), "state").set(core.game(), GameState.RUNNING);
        dummy.remove();
        require(Bukkit.getPlayer(uuid) == null, "Offline test player is still registered");
        core.game().stop(false);
        GameSessionStore store = new GameSessionStore(core.getDataFolder());
        YamlConfiguration saved = store.load();
        String path = "pending-player-cleanup." + uuid;
        require(saved.getBoolean(path + ".clear-inventory"), "Offline cleanup was not durably recorded");
        core.game().stop(false);
        require(store.load().getBoolean(path + ".clear-inventory"), "Repeated stop discarded pending cleanup");
        ((Map<?, ?>) get(core.game(), "pendingPlayerCleanup")).clear();
        invoke(core.game(), "restoreSession", new Class<?>[] {YamlConfiguration.class}, saved);
        core.getConfig().set("game.clear-inventory-on-stop", false);
        GameListener listener = new GameListener(core, core.game(), core.abilities(), core.nms());
        listener.onJoin(new PlayerJoinEvent(player, null));
        require(count(Material.DIAMOND) == 0 && player.getGameMode() == org.bukkit.GameMode.SURVIVAL,
            "Reconnect did not apply recorded cleanup policy and restore survival");
        require(!store.load().contains(path), "Completed cleanup remained in the checkpoint");
        player.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 7));
        listener.onJoin(new PlayerJoinEvent(player, null));
        require(count(Material.DIAMOND) == 7, "Repeated reconnect erased post-game items");

        ((Map<UUID, GodTeam>) get(core.game(), "teams")).put(uuid, GodTeam.RED);
        ((Set<UUID>) get(core.game(), "preparedPlayerInventories")).add(uuid);
        field(core.game(), "state").set(core.game(), GameState.RUNNING);
        core.game().stop(false);
        core.getConfig().set("game.clear-inventory-on-stop", true);
        listener.onJoin(new PlayerJoinEvent(player, null));
        require(count(Material.DIAMOND) == 7, "Cleanup ignored the inventory-preservation policy recorded at stop");
    }

    private void gamble(GamblingGui gui) throws Exception { invoke(gui, "gamble", new Class<?>[] {Player.class}, player); }
    private int count(Material material) { return InventoryItems.count(player.getInventory(), material); }
    private int dropped(ItemStack expected) {
        int total = 0;
        for (Entity entity : player.getWorld().getEntities()) if (entity instanceof Item && !originalItems.contains(entity.getUniqueId())) {
            ItemStack item = ((Item) entity).getItemStack();
            require(item.isSimilar(expected), "Overflow changed reward metadata");
            total += item.getAmount();
        }
        return total;
    }
    private static ItemStack namedStones(int count) {
        ItemStack item = new ItemStack(Material.COBBLESTONE, count);
        ItemMeta meta = item.getItemMeta(); meta.setDisplayName("Renamed stone");
        meta.setLore(Collections.singletonList("Preserve this lore")); item.setItemMeta(meta); return item;
    }
    private static int amount(List<ItemStack> items, Material material) {
        int total = 0; for (ItemStack item : items) if (item.getType() == material) total += item.getAmount(); return total;
    }
    private static PlayerDeathEvent deathEvent(Player player) throws Exception {
        try {
            Class<?> sourceType = Class.forName("org.bukkit.damage.DamageSource");
            Class<?> damageType = Class.forName("org.bukkit.damage.DamageType");
            Object builder = sourceType.getMethod("builder", damageType).invoke(null, damageType.getField("GENERIC").get(null));
            Object source = Class.forName("org.bukkit.damage.DamageSource$Builder").getMethod("build").invoke(builder);
            return (PlayerDeathEvent) PlayerDeathEvent.class.getConstructor(Player.class, sourceType, List.class, int.class, String.class)
                .newInstance(player, source, new ArrayList<ItemStack>(), 0, "regression");
        } catch (ClassNotFoundException legacy) { return new PlayerDeathEvent(player, new ArrayList<ItemStack>(), 0, "regression"); }
    }
    private static Field field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); return field;
    }
    private static Object get(Object target, String name) throws Exception { return field(target, name).get(target); }
    private static Object invoke(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, types); method.setAccessible(true); return method.invoke(target, args);
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static final class ResourceAbility extends BaseAbility {
        boolean cast(AbilityPlayerContext context, int cost) { return use(context, context.player(), 1, Material.COBBLESTONE, cost, 10); }
    }
}
