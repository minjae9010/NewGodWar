package kr.newgodwar.gui;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.ability.AbilityManager;
import kr.newgodwar.ability.api.AbilityDefinition;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class AbilityGui implements Listener {

    private static final String CURRENT_TITLE = ChatColor.DARK_AQUA + "현재 능력 보기";
    private static final String LIST_TITLE = ChatColor.DARK_AQUA + "신의 능력 도감";
    private static final int CURRENT_SIZE = 27;
    private static final int LIST_SIZE = 54;

    private final NewGodWarPlugin plugin;
    private final AbilityManager abilityManager;
    private final Set<UUID> openViewers = new HashSet<UUID>();

    public AbilityGui(NewGodWarPlugin plugin, AbilityManager abilityManager) {
        this.plugin = plugin;
        this.abilityManager = abilityManager;
    }

    public void open(Player viewer, Player target) {
        openCurrent(viewer, target);
    }

    public void openCurrent(Player viewer, Player target) {
        Inventory inventory = Bukkit.createInventory(viewer, CURRENT_SIZE, CURRENT_TITLE);
        fillCurrent(inventory, viewer, target);
        viewer.openInventory(inventory);
        openViewers.add(viewer.getUniqueId());
    }

    public void openList(Player viewer) {
        Inventory inventory = Bukkit.createInventory(viewer, LIST_SIZE, LIST_TITLE);
        fillList(inventory, viewer);
        viewer.openInventory(inventory);
        openViewers.add(viewer.getUniqueId());
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player) || !isAbilityInventory(event)) {
            return;
        }
        event.setCancelled(true);

        boolean list = LIST_TITLE.equals(event.getView().getTitle());
        if ((list && event.getRawSlot() == 49) || (!list && event.getRawSlot() == 22)) {
            event.getWhoClicked().closeInventory();
            return;
        }
        if (!list) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        AbilityDefinition ability = abilityAtSlot(event.getRawSlot());
        if (ability != null && event.isRightClick() && player.hasPermission("newgodwar.admin")) {
            abilityManager.toggleBlacklisted(ability.id());
            plugin.messages().send(player, "&a" + ability.name() + " 블랙리스트 상태를 전환했습니다.");
            openList(player);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (isAbilityInventory(event)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        openViewers.remove(event.getPlayer().getUniqueId());
    }

    private boolean isAbilityInventory(InventoryClickEvent event) {
        return openViewers.contains(event.getWhoClicked().getUniqueId())
            && event.getView() != null
            && (CURRENT_TITLE.equals(event.getView().getTitle()) || LIST_TITLE.equals(event.getView().getTitle()))
            && event.getRawSlot() >= 0
            && event.getRawSlot() < event.getView().getTopInventory().getSize();
    }

    private boolean isAbilityInventory(InventoryDragEvent event) {
        return openViewers.contains(event.getWhoClicked().getUniqueId())
            && event.getView() != null
            && (CURRENT_TITLE.equals(event.getView().getTitle()) || LIST_TITLE.equals(event.getView().getTitle()));
    }

    private void fillCurrent(Inventory inventory, Player viewer, Player target) {
        for (int i = 0; i < CURRENT_SIZE; i++) {
            inventory.setItem(i, item("GRAY_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", 1, (short) 7, " "));
        }

        Player shown = target == null ? viewer : target;
        AbilityDefinition current = abilityManager.get(shown);
        inventory.setItem(13, currentAbilityItem(shown, current));
        inventory.setItem(22, item("BARRIER", "BARRIER", 1, (short) 0, ChatColor.RED + "닫기"));
    }

    private void fillList(Inventory inventory, Player viewer) {
        for (int i = 0; i < LIST_SIZE; i++) {
            inventory.setItem(i, item("GRAY_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", 1, (short) 7, " "));
        }

        inventory.setItem(4, item("BOOK", "BOOK", 1, (short) 0,
            ChatColor.YELLOW + "" + ChatColor.BOLD + "능력 도감",
            ChatColor.GRAY + "등록된 능력을 한눈에 확인합니다.",
            ChatColor.GRAY + "능력 아이템에 마우스를 올리면 세부 설명이 보입니다."));
        inventory.setItem(6, guideItem(viewer));
        inventory.setItem(9, item("COMPASS", "COMPASS", 1, (short) 0,
            ChatColor.AQUA + "" + ChatColor.BOLD + "능력 목록",
            ChatColor.GRAY + "초록: 사용 가능",
            ChatColor.GRAY + "빨강: 비활성 또는 블랙리스트"));

        int[] slots = new int[] {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
        };

        int index = 0;
        for (AbilityDefinition ability : abilityManager.registry().all()) {
            if (index >= slots.length) {
                break;
            }
            inventory.setItem(slots[index], abilityItem(ability, viewer.hasPermission("newgodwar.admin")));
            index++;
        }

        inventory.setItem(49, item("BARRIER", "BARRIER", 1, (short) 0, ChatColor.RED + "닫기"));
    }

    private ItemStack guideItem(Player viewer) {
        if (viewer.hasPermission("newgodwar.admin")) {
            return item("NAME_TAG", "NAME_TAG", 1, (short) 0,
                ChatColor.GOLD + "" + ChatColor.BOLD + "관리자 조작",
                ChatColor.GRAY + "우클릭: 능력 블랙리스트 전환",
                ChatColor.GRAY + "/gw blacklist 로도 관리할 수 있습니다.");
        }
        return item("NAME_TAG", "NAME_TAG", 1, (short) 0,
            ChatColor.GOLD + "" + ChatColor.BOLD + "보기 안내",
            ChatColor.GRAY + "능력 이름, 설명, 돌 소모량을 확인하세요.",
            ChatColor.GRAY + "게임 시작 후 내 현재 능력이 위에 표시됩니다.");
    }

    private ItemStack currentAbilityItem(Player target, AbilityDefinition ability) {
        if (ability == null) {
            return item("BOOK", "BOOK", 1, (short) 0,
                ChatColor.YELLOW + "" + ChatColor.BOLD + target.getName() + " 님의 현재 능력",
                ChatColor.GRAY + "아직 능력이 배정되지 않았습니다.",
                ChatColor.DARK_GRAY + "게임 시작 후 자동으로 배정됩니다.");
        }
        return item("NETHER_STAR", "NETHER_STAR", 1, (short) 0,
            ChatColor.GOLD + "" + ChatColor.BOLD + target.getName() + " 님의 현재 능력",
            ChatColor.WHITE + ability.name() + ChatColor.GRAY + " (" + ability.id() + ")",
            ChatColor.GRAY + ability.description(),
            ChatColor.YELLOW + "일반: " + ChatColor.GRAY + ability.normalSkill(),
            ChatColor.YELLOW + "일반 돌 소모: " + ChatColor.GRAY + stoneCost(ability.normalStoneCost()),
            ChatColor.GOLD + "고급: " + ChatColor.GRAY + ability.advancedSkill(),
            ChatColor.GOLD + "고급 돌 소모: " + ChatColor.GRAY + stoneCost(ability.advancedStoneCost()),
            ChatColor.AQUA + "패시브: " + ChatColor.GRAY + ability.passiveSkill(),
            ChatColor.DARK_GRAY + "제작자: " + ability.author());
    }

    private ItemStack abilityItem(AbilityDefinition ability, boolean showAdminState) {
        boolean enabled = abilityManager.isEnabled(ability);
        boolean blacklisted = abilityManager.isBlacklisted(ability);
        ChatColor color = enabled ? ChatColor.GREEN : ChatColor.RED;
        String state = blacklisted ? "블랙리스트" : (enabled ? "사용 가능" : "비활성");
        ArrayList<String> lore = new ArrayList<String>();
        lore.add(ChatColor.GRAY + ability.description());
        lore.add(ChatColor.YELLOW + "일반: " + ChatColor.GRAY + ability.normalSkill());
        lore.add(ChatColor.YELLOW + "일반 돌 소모: " + ChatColor.GRAY + stoneCost(ability.normalStoneCost()));
        lore.add(ChatColor.GOLD + "고급: " + ChatColor.GRAY + ability.advancedSkill());
        lore.add(ChatColor.GOLD + "고급 돌 소모: " + ChatColor.GRAY + stoneCost(ability.advancedStoneCost()));
        lore.add(ChatColor.AQUA + "패시브: " + ChatColor.GRAY + ability.passiveSkill());
        lore.add(ChatColor.DARK_GRAY + "ID: " + ability.id());
        lore.add(ChatColor.DARK_GRAY + "제작자: " + ability.author());
        if (showAdminState) {
            lore.add(color + state);
            lore.add(ChatColor.DARK_GRAY + "우클릭: 블랙리스트 전환");
        }
        return item(enabled ? "ENCHANTED_BOOK" : "BOOK", enabled ? "ENCHANTED_BOOK" : "BOOK", 1, (short) 0,
            color + ability.name(), lore);
    }

    private AbilityDefinition abilityAtSlot(int slot) {
        int[] slots = new int[] {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
        };
        int index = -1;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return null;
        }

        int current = 0;
        for (AbilityDefinition ability : abilityManager.registry().all()) {
            if (current == index) {
                return ability;
            }
            current++;
        }
        return null;
    }

    private String stoneCost(int cost) {
        return cost <= 0 ? "없음" : cost + "개";
    }

    private ItemStack item(String modernMaterial, String legacyMaterial, int amount, short damage, String name, String... lore) {
        return item(modernMaterial, legacyMaterial, amount, damage, name, new ArrayList<String>(Arrays.asList(lore)));
    }

    private ItemStack item(String modernMaterial, String legacyMaterial, int amount, short damage, String name, ArrayList<String> lore) {
        Material material = material(modernMaterial, legacyMaterial);
        ItemStack stack = new ItemStack(material, Math.max(1, Math.min(64, amount)), damage);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private Material material(String modernName, String legacyName) {
        Material modern = Material.matchMaterial(modernName);
        if (modern != null) {
            return modern;
        }
        Material legacy = Material.matchMaterial(legacyName);
        if (legacy != null) {
            return legacy;
        }
        return Material.STONE;
    }
}
