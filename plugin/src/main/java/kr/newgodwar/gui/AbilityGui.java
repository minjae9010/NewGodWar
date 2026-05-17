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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class AbilityGui implements Listener {

    private static final String CURRENT_TITLE = ChatColor.BLACK + "능력 정보";
    private static final String LIST_TITLE = ChatColor.BLACK + "능력 목록";
    private static final int CURRENT_SIZE = 27;
    private static final int LIST_SIZE = 54;
    private static final int LIST_PAGE_SIZE = 36;
    private static final int CURRENT_CLOSE_SLOT = 22;
    private static final int LIST_PREVIOUS_SLOT = 48;
    private static final int LIST_PAGE_SLOT = 49;
    private static final int LIST_NEXT_SLOT = 50;
    private static final int LIST_CLOSE_SLOT = 53;

    private final NewGodWarPlugin plugin;
    private final AbilityManager abilityManager;
    private final Set<UUID> openViewers = new HashSet<UUID>();
    private final Map<UUID, Integer> listPages = new HashMap<UUID, Integer>();
    private final Map<UUID, String> listQueries = new HashMap<UUID, String>();

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
        openList(viewer, null);
    }

    public void openList(Player viewer, String query) {
        openList(viewer, 1, query);
    }

    private void openList(Player viewer, int page) {
        openList(viewer, page, listQuery(viewer));
    }

    private void openList(Player viewer, int page, String query) {
        Inventory inventory = Bukkit.createInventory(viewer, LIST_SIZE, LIST_TITLE);
        int currentPage = fillList(inventory, viewer, page, query);
        viewer.openInventory(inventory);
        openViewers.add(viewer.getUniqueId());
        listPages.put(viewer.getUniqueId(), currentPage);
        setListQuery(viewer, query);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player) || !isAbilityInventory(event)) {
            return;
        }
        event.setCancelled(true);

        boolean list = LIST_TITLE.equals(event.getView().getTitle());
        if (!list && event.getRawSlot() == CURRENT_CLOSE_SLOT) {
            event.getWhoClicked().closeInventory();
            return;
        }
        if (!list) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        if (event.getRawSlot() == LIST_CLOSE_SLOT) {
            event.getWhoClicked().closeInventory();
            return;
        }
        if (event.getRawSlot() == LIST_PREVIOUS_SLOT) {
            openList(player, listPage(player) - 1);
            return;
        }
        if (event.getRawSlot() == LIST_NEXT_SLOT) {
            openList(player, listPage(player) + 1);
            return;
        }
        if (event.getRawSlot() >= LIST_PAGE_SIZE) {
            return;
        }

        AbilityDefinition ability = abilityAtSlot(event.getRawSlot(), player);
        if (ability != null && event.isRightClick() && player.hasPermission("newgodwar.admin")) {
            abilityManager.toggleBlacklisted(ability.id());
            plugin.messages().send(player, "&a" + ability.name() + " 블랙리스트 상태를 전환했습니다.");
            openList(player, listPage(player));
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
        listPages.remove(event.getPlayer().getUniqueId());
        listQueries.remove(event.getPlayer().getUniqueId());
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
        fill(inventory, deco());

        Player shown = target == null ? viewer : target;
        AbilityDefinition current = abilityManager.get(shown);
        inventory.setItem(4, item("KNOWLEDGE_BOOK", "BOOK", 1, (short) 0,
            ChatColor.YELLOW + "" + ChatColor.BOLD + shown.getName() + " 님의 능력 정보",
            ChatColor.GRAY + "/a 로 다시 열 수 있습니다."));

        if (current == null) {
            inventory.setItem(13, noAbilityItem(shown));
        } else {
            if (hasSkill(current.normalSkill())) {
                inventory.setItem(10, skillItem("LIGHT_BLUE_STAINED_GLASS", (short) 3, ChatColor.AQUA + "일반 능력",
                    current.normalSkill(), current.normalStoneCost(), current.normalCooldown(), cooldownLine(shown, current, 1)));
            }
            inventory.setItem(13, currentAbilityItem(shown, current));
            if (hasSkill(current.advancedSkill())) {
                inventory.setItem(16, skillItem("RED_STAINED_GLASS", (short) 14, ChatColor.RED + "고급 능력",
                    current.advancedSkill(), current.advancedStoneCost(), current.advancedCooldown(), cooldownLine(shown, current, 2)));
            }
            inventory.setItem(20, item("EMERALD", "EMERALD", 1, (short) 0,
                ChatColor.GREEN + "" + ChatColor.BOLD + "패시브",
                ChatColor.WHITE + current.passiveSkill()));
            inventory.setItem(24, item("PAPER", "PAPER", 1, (short) 0,
                ChatColor.GOLD + "" + ChatColor.BOLD + "세부 정보",
                ChatColor.GRAY + "ID: " + ChatColor.WHITE + current.id(),
                ChatColor.GRAY + "제작자: " + ChatColor.WHITE + current.author(),
                ChatColor.GRAY + "타이머: " + currentTimerText(shown)));
        }

        inventory.setItem(CURRENT_CLOSE_SLOT, closeItem());
    }

    private int fillList(Inventory inventory, Player viewer, int requestedPage, String query) {
        fill(inventory, deco());

        List<AbilityDefinition> abilities = filteredAbilities(query);
        int maxPage = Math.max(1, ((abilities.size() - 1) / LIST_PAGE_SIZE) + 1);
        int page = Math.max(1, Math.min(maxPage, requestedPage));
        int start = (page - 1) * LIST_PAGE_SIZE;
        int end = Math.min(abilities.size(), start + LIST_PAGE_SIZE);

        for (int i = start; i < end; i++) {
            inventory.setItem(i - start, abilityItem(abilities.get(i), viewer.hasPermission("newgodwar.admin")));
        }

        inventory.setItem(45, guideItem(viewer));
        if (hasQuery(query)) {
            inventory.setItem(46, item("COMPASS", "COMPASS", 1, (short) 0,
                ChatColor.AQUA + "" + ChatColor.BOLD + "검색 결과",
                ChatColor.GRAY + "검색어: " + ChatColor.WHITE + query,
                ChatColor.GRAY + "일치한 능력: " + ChatColor.WHITE + abilities.size() + "개",
                ChatColor.DARK_GRAY + "/gw abilities 로 전체 목록을 봅니다."));
        }
        if (page > 1) {
            inventory.setItem(LIST_PREVIOUS_SLOT, item("ARROW", "ARROW", 1, (short) 0, ChatColor.AQUA + "이전 페이지"));
        }
        inventory.setItem(LIST_PAGE_SLOT, item("PAPER", "PAPER", 1, (short) 0,
            ChatColor.GOLD + "페이지 " + ChatColor.YELLOW + page + ChatColor.GOLD + " / " + ChatColor.YELLOW + maxPage,
            ChatColor.GRAY + (hasQuery(query) ? "검색된 능력: " : "등록된 능력: ") + ChatColor.WHITE + abilities.size() + "개"));
        if (page < maxPage) {
            inventory.setItem(LIST_NEXT_SLOT, item("ARROW", "ARROW", 1, (short) 0, ChatColor.AQUA + "다음 페이지"));
        }
        inventory.setItem(LIST_CLOSE_SLOT, closeItem());
        return page;
    }

    private ItemStack guideItem(Player viewer) {
        if (viewer.hasPermission("newgodwar.admin")) {
            return item("NAME_TAG", "NAME_TAG", 1, (short) 0,
                ChatColor.GOLD + "" + ChatColor.BOLD + "관리자 조작",
                ChatColor.GRAY + "우클릭: 능력 블랙리스트 전환",
                ChatColor.GRAY + "/gw abilities <검색어>: 능력 검색",
                ChatColor.GRAY + "/gw blacklist 로도 관리할 수 있습니다.");
        }
        return item("NAME_TAG", "NAME_TAG", 1, (short) 0,
            ChatColor.GOLD + "" + ChatColor.BOLD + "보기 안내",
            ChatColor.GRAY + "/gw abilities <검색어>: 능력 검색",
            ChatColor.GRAY + "능력 이름, 설명, 돌 소모량을 확인하세요.");
    }

    private ItemStack currentAbilityItem(Player target, AbilityDefinition ability) {
        return item("NETHER_STAR", "NETHER_STAR", 1, (short) 0,
            ChatColor.AQUA + "" + ChatColor.BOLD + ability.name(),
            ChatColor.WHITE + target.getName() + ChatColor.GRAY + " 님의 현재 능력",
            ChatColor.GRAY + "종류: " + ChatColor.YELLOW + "신의 능력",
            "",
            ChatColor.GRAY + ability.description(),
            "",
            ChatColor.DARK_GRAY + "ID: " + ability.id());
    }

    private ItemStack noAbilityItem(Player target) {
        return item("BARRIER", "BARRIER", 1, (short) 0,
            ChatColor.RED + "" + ChatColor.BOLD + "능력이 없습니다",
            ChatColor.WHITE + target.getName() + ChatColor.GRAY + " 님에게 아직 능력이 배정되지 않았습니다.",
            ChatColor.DARK_GRAY + "게임 시작 후 자동으로 배정됩니다.");
    }

    private ItemStack skillItem(String material, short damage, String name, String skill, int cost, String cooldown, String state) {
        return item(material, "STAINED_GLASS", 1, damage,
            name,
            ChatColor.WHITE + skill,
            "",
            ChatColor.GRAY + "조약돌: " + ChatColor.WHITE + stoneCost(cost),
            ChatColor.GRAY + "기본 쿨타임: " + ChatColor.WHITE + cooldown(cooldown),
            state);
    }

    private ItemStack abilityItem(AbilityDefinition ability, boolean showAdminState) {
        boolean enabled = abilityManager.isEnabled(ability);
        boolean blacklisted = abilityManager.isBlacklisted(ability);
        ChatColor color = enabled ? ChatColor.AQUA : ChatColor.RED;
        String state = blacklisted ? "블랙리스트" : (enabled ? "사용 가능" : "비활성");
        ArrayList<String> lore = new ArrayList<String>();
        lore.add(ChatColor.WHITE + "상태: " + (enabled ? ChatColor.GREEN : ChatColor.RED) + state);
        lore.add("");
        lore.add(ChatColor.WHITE + ability.description());
        lore.add("");
        if (hasSkill(ability.normalSkill())) {
            lore.add(ChatColor.AQUA + "일반: " + ChatColor.GRAY + ability.normalSkill());
            lore.add(ChatColor.GRAY + "조약돌: " + ChatColor.WHITE + stoneCost(ability.normalStoneCost()));
            lore.add(ChatColor.GRAY + "쿨타임: " + ChatColor.WHITE + cooldown(ability.normalCooldown()));
        }
        if (hasSkill(ability.advancedSkill())) {
            lore.add(ChatColor.RED + "고급: " + ChatColor.GRAY + ability.advancedSkill());
            lore.add(ChatColor.GRAY + "조약돌: " + ChatColor.WHITE + stoneCost(ability.advancedStoneCost()));
            lore.add(ChatColor.GRAY + "쿨타임: " + ChatColor.WHITE + cooldown(ability.advancedCooldown()));
        }
        lore.add(ChatColor.AQUA + "패시브: " + ChatColor.GRAY + ability.passiveSkill());
        lore.add(ChatColor.DARK_GRAY + "ID: " + ability.id());
        if (showAdminState) {
            lore.add("");
            lore.add(ChatColor.DARK_GRAY + "우클릭: 블랙리스트 전환");
        }
        String material = blacklisted ? "RED_STAINED_GLASS" : (enabled ? "IRON_BLOCK" : "GRAY_STAINED_GLASS");
        short damage = blacklisted ? (short) 14 : (enabled ? (short) 0 : (short) 7);
        return item(material, blacklisted || !enabled ? "STAINED_GLASS" : "IRON_BLOCK", 1, damage,
            color + ability.name(), lore);
    }

    private AbilityDefinition abilityAtSlot(int slot, Player viewer) {
        if (slot < 0 || slot >= LIST_PAGE_SIZE) {
            return null;
        }

        int index = (listPage(viewer) - 1) * LIST_PAGE_SIZE + slot;
        List<AbilityDefinition> abilities = filteredAbilities(listQuery(viewer));
        if (index < 0 || index >= abilities.size()) {
            return null;
        }
        return abilities.get(index);
    }

    private int listPage(Player viewer) {
        Integer page = listPages.get(viewer.getUniqueId());
        return page == null ? 1 : page;
    }

    private String listQuery(Player viewer) {
        return listQueries.get(viewer.getUniqueId());
    }

    private void setListQuery(Player viewer, String query) {
        if (hasQuery(query)) {
            listQueries.put(viewer.getUniqueId(), query.trim());
            return;
        }
        listQueries.remove(viewer.getUniqueId());
    }

    private List<AbilityDefinition> filteredAbilities(String query) {
        List<AbilityDefinition> abilities = sortedAbilities();
        if (!hasQuery(query)) {
            return abilities;
        }
        String normalized = query.toLowerCase(Locale.ROOT).trim();
        ArrayList<AbilityDefinition> filtered = new ArrayList<AbilityDefinition>();
        for (AbilityDefinition ability : abilities) {
            if (matches(ability, normalized)) {
                filtered.add(ability);
            }
        }
        return filtered;
    }

    private boolean matches(AbilityDefinition ability, String query) {
        return contains(ability.id(), query)
            || contains(ability.name(), query)
            || contains(ability.description(), query)
            || contains(ability.normalSkill(), query)
            || contains(ability.advancedSkill(), query)
            || contains(ability.passiveSkill(), query)
            || contains(ability.author(), query);
    }

    private boolean contains(String text, String query) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(query);
    }

    private boolean hasQuery(String query) {
        return query != null && query.trim().length() > 0;
    }

    private List<AbilityDefinition> sortedAbilities() {
        ArrayList<AbilityDefinition> abilities = new ArrayList<AbilityDefinition>();
        for (AbilityDefinition ability : abilityManager.registry().all()) {
            abilities.add(ability);
        }
        for (int i = 1; i < abilities.size(); i++) {
            AbilityDefinition current = abilities.get(i);
            int cursor = i - 1;
            while (cursor >= 0 && abilities.get(cursor).name().compareTo(current.name()) > 0) {
                abilities.set(cursor + 1, abilities.get(cursor));
                cursor--;
            }
            abilities.set(cursor + 1, current);
        }
        return abilities;
    }

    private String cooldownLine(Player player, AbilityDefinition ability, int slot) {
        long millis = semanticCooldownMillis(player, ability, slot);
        if (millis <= 0L) {
            return ChatColor.WHITE + "상태: " + ChatColor.GREEN + "사용 가능";
        }
        return ChatColor.WHITE + "상태: " + ChatColor.YELLOW + "쿨타임 " + ((millis + 999L) / 1000L) + "초";
    }

    private String currentTimerText(Player player) {
        List<String> timers = abilityManager.activeTimerLines(player);
        if (timers.isEmpty()) {
            return ChatColor.GREEN + "없음";
        }
        return timers.get(0);
    }

    private long semanticCooldownMillis(Player player, AbilityDefinition ability, int slot) {
        long millis = abilityManager.cooldownRemainingMillis(player, slot);
        if (millis > 0L || ability == null) {
            return millis;
        }
        long shared = abilityManager.cooldownRemainingMillis(player, 0);
        if (shared <= 0L) {
            return 0L;
        }
        if (slot == 1 && hasCooldown(ability.normalCooldown()) && !hasCooldown(ability.advancedCooldown())) {
            return shared;
        }
        if (slot == 2 && hasCooldown(ability.advancedCooldown()) && !hasCooldown(ability.normalCooldown())) {
            return shared;
        }
        return 0L;
    }

    private String stoneCost(int cost) {
        return cost <= 0 ? "없음" : cost + "개";
    }

    private String cooldown(String cooldown) {
        return hasCooldown(cooldown) ? cooldown : "없음";
    }

    private boolean hasCooldown(String cooldown) {
        return cooldown != null && cooldown.trim().length() > 0 && !"없음".equals(cooldown.trim());
    }

    private boolean hasSkill(String skill) {
        return skill != null && skill.trim().length() > 0 && !"없음".equals(skill.trim());
    }

    private void fill(Inventory inventory, ItemStack item) {
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, item);
        }
    }

    private ItemStack deco() {
        return item("GRAY_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", 1, (short) 7, ChatColor.WHITE.toString());
    }

    private ItemStack closeItem() {
        return item("SPRUCE_DOOR", "WOOD_DOOR", 1, (short) 0, ChatColor.DARK_AQUA + "나가기");
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
