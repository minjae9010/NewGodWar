package kr.newgodwar.regression;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.gui.GamblingGui;
import kr.newgodwar.util.InventoryItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Real inventory views and events, using the inventory fixture's connected CraftPlayer. */
final class GuiRegressionChecks {
    private static final int[] REWARD_SLOTS = {
        10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34
    };
    private final NewGodWarPlugin core;
    private final Player player;
    private final GamblingGui gui;

    private GuiRegressionChecks(NewGodWarPlugin core, Player player) {
        this.core = core;
        this.player = player;
        this.gui = new GamblingGui(core);
    }

    static void run(NewGodWarPlugin core, Player player) throws Exception {
        new GuiRegressionChecks(core, player).run();
    }

    private void run() throws Exception {
        Map<String, Object> saved = new LinkedHashMap<String, Object>();
        for (String key : Arrays.asList("gambling.enabled", "gambling.cost.cobblestone",
                "gambling.rewards.normal", "ui.resource-pack.enabled")) saved.put(key, core.getConfig().get(key));
        ItemStack[] contents = player.getInventory().getContents();
        try {
            core.getConfig().set("gambling.enabled", true);
            core.getConfig().set("gambling.cost.cobblestone", 32);
            drawAndProtectPreviews();
            paginatesEveryReward();
            checksOptionalModels();
            core.getLogger().info("PASS menu interactions: left-click cost/result refresh, alternate-click and drag protection, 21-reward pagination and resource-pack fallback");
        } finally {
            close();
            player.getInventory().setContents(contents);
            for (Map.Entry<String, Object> entry : saved.entrySet()) core.getConfig().set(entry.getKey(), entry.getValue());
        }
    }

    private void drawAndProtectPreviews() {
        ItemStack prize = namedReward("GUI regression reward", 2);
        core.getConfig().set("gambling.rewards.normal", Collections.singletonList(reward(prize)));
        player.getInventory().clear();
        player.getInventory().setItem(0, new ItemStack(Material.COBBLESTONE, 64));
        gui.open(player);
        Inventory inventory = top();
        require(inventory.getSize() == 36, "Draw menu is not four rows");
        require(name(inventory.getItem(10)).contains("64"), "Opening balance does not show 64 cobblestone");
        require(name(inventory.getItem(13)).contains("32"), "Draw button does not explain the price");
        click(13, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        inventory = top();
        require(count(Material.COBBLESTONE) == 32 && count(Material.DIAMOND) == 2,
            "Left-click draw did not charge 32 and grant exactly one reward");
        require(name(inventory.getItem(10)).contains("32") && name(inventory.getItem(13)).contains("32"),
            "Draw did not refresh the balance and affordability");
        require(name(inventory.getItem(16)).contains("받은 보상")
                && lore(inventory.getItem(16)).contains("GUI regression result"),
            "Recent result did not update after the draw");
        ItemStack result = inventory.getItem(16).clone();

        for (ClickType type : Arrays.asList(ClickType.RIGHT, ClickType.SHIFT_LEFT, ClickType.SHIFT_RIGHT,
                ClickType.NUMBER_KEY, ClickType.DOUBLE_CLICK, ClickType.DROP)) {
            InventoryAction action = type == ClickType.NUMBER_KEY ? InventoryAction.HOTBAR_SWAP
                : type == ClickType.DOUBLE_CLICK ? InventoryAction.COLLECT_TO_CURSOR
                : type == ClickType.DROP ? InventoryAction.DROP_ONE_SLOT
                : type.isShiftClick() ? InventoryAction.MOVE_TO_OTHER_INVENTORY : InventoryAction.PICKUP_HALF;
            click(13, type, action);
            require(count(Material.COBBLESTONE) == 32 && count(Material.DIAMOND) == 2,
                "Non-left draw click spent resources or granted a reward: " + type);
            require(result.equals(top().getItem(16)), "Result changed after " + type);
        }
        click(29, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        inventory = top();
        require(inventory.getSize() == 54, "Reward catalogue is not six rows");
        require(name(inventory.getItem(10)).equals("GUI regression reward"), "First reward is not visible");
        require(lore(inventory.getItem(10)).contains("100.00%"), "Reward preview omits its actual chance");
        ItemStack preview = inventory.getItem(10).clone();
        for (ClickType type : Arrays.asList(ClickType.LEFT, ClickType.RIGHT, ClickType.SHIFT_LEFT,
                ClickType.SHIFT_RIGHT, ClickType.NUMBER_KEY, ClickType.DOUBLE_CLICK, ClickType.DROP)) {
            click(10, type, InventoryAction.PICKUP_ALL);
            require(preview.equals(inventory.getItem(10)), "Preview changed after " + type);
        }
        click(54, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY);
        ItemStack[] before = player.getInventory().getContents();
        InventoryDragEvent drag = new InventoryDragEvent(player.getOpenInventory(),
            new ItemStack(Material.DIAMOND, 1), new ItemStack(Material.DIAMOND, 2), false,
            Collections.singletonMap(10, new ItemStack(Material.DIAMOND, 1)));
        gui.onDrag(drag);
        require(drag.isCancelled() && preview.equals(inventory.getItem(10))
                && Arrays.equals(before, player.getInventory().getContents()),
            "Dragging into a preview was not cancelled without changing inventory");
        require(count(Material.COBBLESTONE) == 32 && count(Material.DIAMOND) == 2,
            "Preview interactions changed the player's items");

        click(49, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        require(result.equals(top().getItem(16)), "Returning from rewards lost the last draw result");
        click(23, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        inventory = top();
        require(count(Material.COBBLESTONE) == 0 && count(Material.DIAMOND) == 4,
            "Second left-click did not charge the remaining cobblestone exactly once");
        require(inventory.getItem(13).getType() == Material.BARRIER
                && lore(inventory.getItem(13)).contains("더 필요"), "Exhausted balance did not disable the draw affordance");
        click(13, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        require(count(Material.COBBLESTONE) == 0 && count(Material.DIAMOND) == 4,
            "Unaffordable draw granted another reward");
        inventory = top();
        click(35, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        require(player.getOpenInventory().getTopInventory() != inventory, "Close button did not close the menu");
    }

    private void paginatesEveryReward() {
        List<Map<String, Object>> rewards = new ArrayList<Map<String, Object>>();
        for (int index = 1; index <= 23; index++) rewards.add(reward(namedReward("Page reward " + index, 1)));
        core.getConfig().set("gambling.rewards.normal", rewards);
        gui.open(player);
        click(29, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        Inventory inventory = top();
        for (int index = 0; index < REWARD_SLOTS.length; index++) {
            require(name(inventory.getItem(REWARD_SLOTS[index])).equals("Page reward " + (index + 1)),
                "First reward page skipped/reordered reward " + (index + 1));
        }
        require(name(inventory.getItem(40)).contains("1 / 2"), "First-page indicator is incorrect");
        require(lore(inventory.getItem(10)).contains("4.35%"), "Chance was calculated per page instead of all rewards");
        click(53, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        inventory = top();
        require(name(inventory.getItem(10)).equals("Page reward 22")
                && name(inventory.getItem(11)).equals("Page reward 23")
                && inventory.getItem(12) == null && inventory.getItem(34) == null,
            "Second reward page is missing rewards or retained stale previews");
        require(name(inventory.getItem(40)).contains("2 / 2"), "Second-page indicator is incorrect");
        click(53, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        require(name(top().getItem(10)).equals("Page reward 22"), "Next moved past the last page");
        click(45, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        inventory = top();
        require(name(inventory.getItem(10)).equals("Page reward 1")
                && name(inventory.getItem(34)).equals("Page reward 21"), "Previous page did not restore every preview");
        click(45, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        require(name(top().getItem(10)).equals("Page reward 1"), "Previous moved before the first page");
        require(count(Material.DIAMOND) == 4 && count(Material.COBBLESTONE) == 0,
            "Pagination awarded preview items or charged resources");
        close();
    }

    @SuppressWarnings("unchecked")
    private void checksOptionalModels() throws Exception {
        require(kr.newgodwar.ability.feedback.PackModels.currentPack() != null,
            "Supported server did not resolve a versioned resource pack");
        if (!kr.newgodwar.ability.feedback.PackModels.modern()) {
            checksLegacyModels();
            return;
        }
        Method getModel;
        Method setModel;
        Class<?> keyType = Class.forName("org.bukkit.NamespacedKey");
        try {
            getModel = ItemMeta.class.getMethod("getItemModel");
            setModel = ItemMeta.class.getMethod("setItemModel", keyType);
        } catch (NoSuchMethodException legacy) {
            core.getLogger().info("PASS legacy menu fallback: vanilla menu items and interactions require no item-model API");
            return;
        }
        Class<?> theme = Class.forName("kr.newgodwar.gui.GuiTheme");
        Class<?> iconType = Class.forName("kr.newgodwar.gui.GuiIcon");
        Field coin = iconType.getDeclaredField("COIN");
        coin.setAccessible(true);
        Method icon = theme.getDeclaredMethod("icon", ItemStack.class, iconType);
        Method present = theme.getDeclaredMethod("present", Player.class, Inventory.class, NewGodWarPlugin.class);
        icon.setAccessible(true);
        present.setAccessible(true);
        require(core.effectArtPack() != null, "Resource-pack readiness service was not initialized");
        Field loadedField = core.effectArtPack().getClass().getDeclaredField("loaded");
        loadedField.setAccessible(true);
        Set<UUID> loaded = (Set<UUID>) loadedField.get(core.effectArtPack());
        boolean wasLoaded = loaded.contains(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(player, 9, "Model fallback regression");
        ItemStack externalReward = namedReward("External custom reward", 1);
        ItemMeta externalMeta = externalReward.getItemMeta();
        setModel.invoke(externalMeta, keyType.getConstructor(String.class, String.class).newInstance("otherplugin", "reward/prize"));
        externalReward.setItemMeta(externalMeta);
        inventory.setItem(1, externalReward);
        ItemStack abilityReward = namedReward("Ability model reward", 1);
        ItemMeta abilityMeta = abilityReward.getItemMeta();
        setModel.invoke(abilityMeta, keyType.getConstructor(String.class, String.class).newInstance("newgodwar", "ability/test"));
        abilityReward.setItemMeta(abilityMeta);
        inventory.setItem(2, abilityReward);
        try {
            core.getConfig().set("ui.resource-pack.enabled", true);
            inventory.setItem(0, (ItemStack) icon.invoke(null, new ItemStack(Material.GOLD_INGOT), coin.get(null)));
            require("newgodwar:gui/coin".equals(String.valueOf(getModel.invoke(inventory.getItem(0).getItemMeta()))),
                "GUI icon did not assign its namespaced model");
            loaded.remove(player.getUniqueId());
            present.invoke(null, player, inventory, core);
            require(getModel.invoke(inventory.getItem(0).getItemMeta()) == null,
                "Menu model remained visible before this player's pack loaded");
            require(externalReward.equals(inventory.getItem(1)) && abilityReward.equals(inventory.getItem(2)),
                "Menu fallback removed an unrelated custom reward model");
            loaded.add(player.getUniqueId());
            inventory.setItem(0, (ItemStack) icon.invoke(null, new ItemStack(Material.GOLD_INGOT), coin.get(null)));
            present.invoke(null, player, inventory, core);
            require("newgodwar:gui/coin".equals(String.valueOf(getModel.invoke(inventory.getItem(0).getItemMeta()))),
                "Loaded pack did not retain menu models");
            core.getConfig().set("ui.resource-pack.enabled", false);
            present.invoke(null, player, inventory, core);
            require(getModel.invoke(inventory.getItem(0).getItemMeta()) == null
                    && externalReward.equals(inventory.getItem(1)) && abilityReward.equals(inventory.getItem(2)),
                "Disabling menu art did not selectively restore vanilla menu items");
        } finally {
            if (wasLoaded) loaded.add(player.getUniqueId()); else loaded.remove(player.getUniqueId());
        }
    }

    private void click(int rawSlot, ClickType type, InventoryAction action) {
        Set<Integer> existingTasks = new java.util.HashSet<Integer>();
        for (org.bukkit.scheduler.BukkitTask task : Bukkit.getScheduler().getPendingTasks()) existingTasks.add(task.getTaskId());
        InventoryClickEvent event = new InventoryClickEvent(player.getOpenInventory(), InventoryType.SlotType.CONTAINER,
            rawSlot, type, action, type == ClickType.NUMBER_KEY ? 0 : -1);
        gui.onClick(event);
        require(event.isCancelled(), "Menu did not cancel inventory action at " + rawSlot + ": " + type);
        // Complete only this click's deferred transaction; do not advance unrelated game tasks.
        for (org.bukkit.scheduler.BukkitTask task : Bukkit.getScheduler().getPendingTasks()) {
            if (task.getOwner() != core || existingTasks.contains(task.getTaskId())) continue;
            require(task.isSync() && task instanceof Runnable, "Unexpected click task implementation");
            try { ((Runnable) task).run(); }
            finally { task.cancel(); }
        }
    }

    private void close() {
        InventoryView view = player.getOpenInventory();
        gui.onClose(new InventoryCloseEvent(view));
        player.closeInventory();
    }

    private Inventory top() { return player.getOpenInventory().getTopInventory(); }
    private int count(Material material) { return InventoryItems.count(player.getInventory(), material); }
    private static String name(ItemStack item) {
        return item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()
            ? "" : ChatColor.stripColor(item.getItemMeta().getDisplayName());
    }
    private static String lore(ItemStack item) {
        return item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()
            ? "" : ChatColor.stripColor(String.join("\n", item.getItemMeta().getLore()));
    }
    private static ItemStack namedReward(String name, int amount) {
        ItemStack prize = new ItemStack(Material.DIAMOND, amount);
        ItemMeta meta = prize.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Collections.singletonList("Original reward lore"));
        prize.setItemMeta(meta);
        return prize;
    }
    private static Map<String, Object> reward(ItemStack prize) {
        Map<String, Object> reward = new LinkedHashMap<String, Object>();
        reward.put("chance", 1);
        reward.put("item", prize);
        reward.put("message", "GUI regression result");
        return reward;
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    @SuppressWarnings("unchecked")
    private void checksLegacyModels() throws Exception {
        Method hasModel;
        try { hasModel = ItemMeta.class.getMethod("hasCustomModelData"); }
        catch (NoSuchMethodException legacy) {
            core.getLogger().info("PASS 1.12/1.13 vanilla menu fallback");
            return;
        }
        Class<?> theme = Class.forName("kr.newgodwar.gui.GuiTheme");
        Class<?> icons = Class.forName("kr.newgodwar.gui.GuiIcon");
        Field coin = icons.getDeclaredField("COIN"); coin.setAccessible(true);
        Method icon = theme.getDeclaredMethod("icon", ItemStack.class, icons);
        Method present = theme.getDeclaredMethod("present", Player.class, Inventory.class, NewGodWarPlugin.class);
        icon.setAccessible(true); present.setAccessible(true);
        Field field = core.effectArtPack().getClass().getDeclaredField("loaded"); field.setAccessible(true);
        Set<UUID> loaded = (Set<UUID>) field.get(core.effectArtPack());
        boolean wasLoaded = loaded.contains(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(player, 9, "Legacy model regression");
        ItemStack reward = namedReward("External model", 1);
        ItemMeta rewardMeta = reward.getItemMeta();
        ItemMeta.class.getMethod("setCustomModelData", Integer.class).invoke(rewardMeta, Integer.valueOf(900001));
        reward.setItemMeta(rewardMeta); inventory.setItem(1, reward);
        try {
            core.getConfig().set("ui.resource-pack.enabled", true);
            loaded.remove(player.getUniqueId());
            inventory.setItem(0, (ItemStack) icon.invoke(null, new ItemStack(Material.GOLD_INGOT), coin.get(null)));
            present.invoke(null, player, inventory, core);
            require(inventory.getItem(0).getType() == Material.GOLD_INGOT
                && !(Boolean) hasModel.invoke(inventory.getItem(0).getItemMeta()), "Legacy pre-load fallback failed");
            loaded.add(player.getUniqueId());
            inventory.setItem(0, (ItemStack) icon.invoke(null, new ItemStack(Material.GOLD_INGOT), coin.get(null)));
            present.invoke(null, player, inventory, core);
            require(inventory.getItem(0).getType() == Material.PAPER
                && (Boolean) hasModel.invoke(inventory.getItem(0).getItemMeta()), "Legacy menu model did not use paper dispatch");
            present.invoke(null, player, inventory, core);
            core.getConfig().set("ui.resource-pack.enabled", false);
            present.invoke(null, player, inventory, core);
            require(inventory.getItem(0).getType() == Material.GOLD_INGOT
                && !(Boolean) hasModel.invoke(inventory.getItem(0).getItemMeta()), "Legacy disable did not restore original item");
            require(reward.equals(inventory.getItem(1)), "Legacy fallback changed an unrelated reward");
            core.getLogger().info("PASS legacy menu models: readiness, paper dispatch, repeated render, material restoration and external rewards");
        } finally {
            if (wasLoaded) loaded.add(player.getUniqueId()); else loaded.remove(player.getUniqueId());
        }
    }

}
