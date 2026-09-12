package kr.newgodwar.gui;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.ability.AbilityManager;
import kr.newgodwar.ability.api.AbilityDefinition;
import kr.newgodwar.ability.api.AbilityGrade;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AbilityGui implements Listener {

    private static final String CURRENT_TITLE = ChatColor.BLACK + "능력 정보";
    private static final String DETAIL_TITLE = ChatColor.BLACK + "능력 상세";
    private static final String LIST_TITLE = ChatColor.BLACK + "능력 목록";
    private static final int CURRENT_SIZE = 45;
    private static final int LIST_SIZE = 54;
    private static final int LIST_PAGE_SIZE = ChestLayout.CATALOG.length;
    private static final int CURRENT_CLOSE_SLOT = 40;
    private static final int LIST_PREVIOUS_SLOT = 48;
    private static final int LIST_PAGE_SLOT = 49;
    private static final int LIST_NEXT_SLOT = 50;
    private static final int LIST_CLOSE_SLOT = 53;

    private final NewGodWarPlugin plugin;
    private final AbilityManager abilityManager;
    private final Set<UUID> openViewers = new HashSet<UUID>();
    private final Map<UUID, Integer> listPages = new HashMap<UUID, Integer>();
    private final Map<UUID, String> listQueries = new HashMap<UUID, String>();

    private final Map<UUID, SearchRequest> searches = new ConcurrentHashMap<UUID, SearchRequest>();
    private final Map<UUID, UUID> currentTargets = new HashMap<UUID, UUID>();
    private final Set<UUID> navigating = new HashSet<UUID>();

    private static final class SearchRequest {
        final int page;
        final String query;
        SearchRequest(int page, String query) { this.page = page; this.query = query; }
    }

    public AbilityGui(NewGodWarPlugin plugin, AbilityManager abilityManager) {
        this.plugin = plugin;
        this.abilityManager = abilityManager;
    }

    public void open(Player viewer, Player target) {
        openCurrent(viewer, target);
    }

    public void openCurrent(Player viewer, Player target) {
        Player shown = target == null ? viewer : target;
        if (!viewer.equals(shown) && !viewer.hasPermission("newgodwar.admin")
            && (plugin.game().teamOf(viewer) == null || !plugin.game().teamOf(viewer).equals(plugin.game().teamOf(shown)))) {
            viewer.closeInventory();
            plugin.messages().send(viewer, "&c본인 또는 같은 팀의 능력만 볼 수 있습니다.");
            return;
        }
        searches.remove(viewer.getUniqueId());
        Inventory inventory = Bukkit.createInventory(viewer, CURRENT_SIZE, CURRENT_TITLE);
        fillCurrent(inventory, viewer, target);
        viewer.openInventory(inventory);
        openViewers.add(viewer.getUniqueId());
        currentTargets.put(viewer.getUniqueId(), (target == null ? viewer : target).getUniqueId());
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
        searches.remove(viewer.getUniqueId());
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

        final Player player = (Player) event.getWhoClicked();
        final int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) return;
        final String title = event.getView().getTitle();
        final Inventory clickedInventory = event.getView().getTopInventory();
        final boolean rightClick = event.isRightClick(), leftClick = event.isLeftClick();
        // Inventory transitions must happen after the click transaction completes.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline() || !openViewers.contains(player.getUniqueId())
                || player.getOpenInventory().getTopInventory() != clickedInventory) return;
            if ((LIST_TITLE.equals(title) && slot == LIST_CLOSE_SLOT)
                || (!LIST_TITLE.equals(title) && slot == CURRENT_CLOSE_SLOT)) {
                player.closeInventory();
                return;
            }

            if (CURRENT_TITLE.equals(title)) {
                if (slot == 36) openList(player);
                if (slot == 44) {
                    UUID targetId = currentTargets.get(player.getUniqueId());
                    Player target = targetId == null ? null : Bukkit.getPlayer(targetId);
                    if (target != null) openCurrent(player, target);
                }
                return;
            }
            if (DETAIL_TITLE.equals(title)) {
                if (slot == 36) openList(player, listPage(player));
                return;
            }
            if (slot == 47) { beginSearch(player); return; }
            if (slot == 46) { openList(player); return; }
            if (slot == LIST_PREVIOUS_SLOT) { openList(player, listPage(player) - 1); return; }
            if (slot == LIST_NEXT_SLOT) { openList(player, listPage(player) + 1); return; }
            AbilityDefinition ability = abilityAtSlot(slot, player);
            if (ability == null) return;
            if (rightClick && player.hasPermission("newgodwar.admin")) {
                abilityManager.toggleBlacklisted(ability.id());
                plugin.messages().send(player, "&a" + ability.name() + ": "
                    + (abilityManager.isBlacklisted(ability) ? "랜덤 배정 제외" : "블랙리스트 해제"));
                openList(player, listPage(player));
            } else if (leftClick) {
                openDetail(player, ability);
            }
        });
    }

    private void beginSearch(Player player) {
        SearchRequest request = new SearchRequest(listPage(player), listQuery(player));
        player.closeInventory();
        searches.put(player.getUniqueId(), request);
        plugin.messages().send(player, "&e검색할 능력 이름이나 특징을 채팅에 입력하세요. &f취소&e를 입력하면 돌아갑니다. (60초)");
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (searches.remove(player.getUniqueId(), request) && player.isOnline()) {
                plugin.messages().send(player, "&e능력 검색 입력 시간이 끝났습니다. 도감의 검색 버튼으로 다시 시작하세요.");
            }
        }, 1200L);
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        SearchRequest request = searches.remove(event.getPlayer().getUniqueId());
        if (request == null) return;
        event.setCancelled(true);
        final String query = event.getMessage().trim();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!event.getPlayer().isOnline()) return;
            if ("취소".equals(query)) openList(event.getPlayer(), request.page, request.query);
            else openList(event.getPlayer(), query.length() > 64 ? query.substring(0, 64) : query);
        });
    }

    @EventHandler
    public void onOpen(org.bukkit.event.inventory.InventoryOpenEvent event) {
        searches.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        searches.remove(id);
        openViewers.remove(id);
        listPages.remove(id);
        listQueries.remove(id);
        currentTargets.remove(id);
        navigating.remove(id);
    }

    private void openDetail(Player player, AbilityDefinition ability) {
        Inventory inventory = Bukkit.createInventory(player, CURRENT_SIZE, DETAIL_TITLE);
        GuiTheme.frame(inventory);
        inventory.setItem(4, GuiTheme.heading(ability.name(), "각 항목에 마우스를 올려 사용법을 확인하세요."));
        inventory.setItem(22, item("NETHER_STAR", "NETHER_STAR", 1, (short) 0,
            ChatColor.AQUA + ability.name(), ChatColor.WHITE + ability.description(),
            ChatColor.GRAY + "등급: " + ability.gradeText(), ChatColor.GRAY + "제작자: " + ability.author()));
        if (hasSkill(ability.normalSkill())) inventory.setItem(20, skillItem("LIGHT_BLUE_STAINED_GLASS", (short) 3,
            ChatColor.AQUA + "일반 능력 · 사용법", ability.normalSkill(), ability.normalStoneCost(), ability.normalCooldown(),
            ChatColor.DARK_GRAY + "도감은 기본 수치를 표시합니다."));
        if (hasSkill(ability.advancedSkill())) inventory.setItem(24, skillItem("RED_STAINED_GLASS", (short) 14,
            ChatColor.RED + "고급 능력 · 사용법", ability.advancedSkill(), ability.advancedStoneCost(), ability.advancedCooldown(),
            ChatColor.DARK_GRAY + "추가 재료·조건은 위 사용법을 확인하세요."));
        inventory.setItem(30, item("EMERALD", "EMERALD", 1, (short) 0,
            ChatColor.GREEN + "패시브 · 조건에 따라 자동 적용", ChatColor.WHITE + ability.passiveSkill()));
        inventory.setItem(36, GuiTheme.item("ARROW", "ARROW", (short) 0, ChatColor.YELLOW + "도감으로 돌아가기",
            ChatColor.GRAY + "검색어와 페이지를 유지합니다."));
        inventory.setItem(CURRENT_CLOSE_SLOT, closeItem());
        navigating.add(player.getUniqueId());
        try { player.openInventory(inventory); }
        finally { navigating.remove(player.getUniqueId()); }
        openViewers.add(player.getUniqueId());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (isAbilityInventory(event)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (navigating.contains(event.getPlayer().getUniqueId())) return;
        currentTargets.remove(event.getPlayer().getUniqueId());
        openViewers.remove(event.getPlayer().getUniqueId());
        listPages.remove(event.getPlayer().getUniqueId());
        listQueries.remove(event.getPlayer().getUniqueId());
    }

    private boolean isAbilityInventory(InventoryClickEvent event) {
        return openViewers.contains(event.getWhoClicked().getUniqueId())
            && event.getView() != null
            && (CURRENT_TITLE.equals(event.getView().getTitle()) || DETAIL_TITLE.equals(event.getView().getTitle()) || LIST_TITLE.equals(event.getView().getTitle()));
    }

    private boolean isAbilityInventory(InventoryDragEvent event) {
        return openViewers.contains(event.getWhoClicked().getUniqueId())
            && event.getView() != null
            && (CURRENT_TITLE.equals(event.getView().getTitle()) || DETAIL_TITLE.equals(event.getView().getTitle()) || LIST_TITLE.equals(event.getView().getTitle()));
    }

    private void fillCurrent(Inventory inventory, Player viewer, Player target) {
        GuiTheme.frame(inventory);

        Player shown = target == null ? viewer : target;
        AbilityDefinition current = abilityManager.get(shown);
        inventory.setItem(4, item("KNOWLEDGE_BOOK", "BOOK", 1, (short) 0,
            ChatColor.YELLOW + "" + ChatColor.BOLD + shown.getName() + " 님의 능력 정보",
            ChatColor.GRAY + "/a 로 다시 열 수 있습니다."));

        if (current == null) {
            inventory.setItem(22, noAbilityItem(shown));
        } else {
            if (hasSkill(current.normalSkill())) {
                inventory.setItem(20, skillItem("LIGHT_BLUE_STAINED_GLASS", (short) 3, ChatColor.AQUA + "일반 능력",
                    current.normalSkill(), current.normalStoneCost(), current.normalCooldown(), cooldownLine(shown, current, 1)));
            }
            inventory.setItem(22, currentAbilityItem(shown, current));
            if (hasSkill(current.advancedSkill())) {
                inventory.setItem(24, skillItem("RED_STAINED_GLASS", (short) 14, ChatColor.RED + "고급 능력",
                    current.advancedSkill(), current.advancedStoneCost(), current.advancedCooldown(), cooldownLine(shown, current, 2)));
            }
            inventory.setItem(30, item("EMERALD", "EMERALD", 1, (short) 0,
                ChatColor.GREEN + "" + ChatColor.BOLD + "패시브",
                ChatColor.WHITE + current.passiveSkill()));
            inventory.setItem(32, item("PAPER", "PAPER", 1, (short) 0,
                ChatColor.GOLD + "" + ChatColor.BOLD + "세부 정보",
                ChatColor.GRAY + "ID: " + ChatColor.WHITE + current.id(),
                ChatColor.GRAY + "등급: " + gradeColor(current.grade()) + current.gradeText(),
                ChatColor.GRAY + "제작자: " + ChatColor.WHITE + current.author(),
                ChatColor.GRAY + "타이머: " + currentTimerText(shown)));
        }

        inventory.setItem(36, GuiTheme.item("BOOK", "BOOK", (short) 0, ChatColor.AQUA + "능력 도감"));
        inventory.setItem(44, GuiTheme.item("CLOCK", "WATCH", (short) 0, ChatColor.YELLOW + "상태 새로고침",
            ChatColor.GRAY + "남은 시간과 재료 상태는 열거나 새로고침한 시점의 값입니다."));
        inventory.setItem(CURRENT_CLOSE_SLOT, closeItem());
    }

    private int fillList(Inventory inventory, Player viewer, int requestedPage, String query) {
        GuiTheme.frame(inventory);

        List<AbilityDefinition> abilities = filteredAbilities(query);
        int maxPage = Math.max(1, ((abilities.size() - 1) / LIST_PAGE_SIZE) + 1);
        int page = Math.max(1, Math.min(maxPage, requestedPage));
        int start = (page - 1) * LIST_PAGE_SIZE;
        int end = Math.min(abilities.size(), start + LIST_PAGE_SIZE);

        for (int i = start; i < end; i++) {
            inventory.setItem(ChestLayout.CATALOG[i - start], abilityItem(abilities.get(i), viewer.hasPermission("newgodwar.admin")));
        }

        inventory.setItem(4, GuiTheme.heading("능력 도감", "능력을 좌클릭하면 사용법과 상세 수치를 볼 수 있습니다."));
        inventory.setItem(45, guideItem(viewer));
        inventory.setItem(47, GuiTheme.item("OAK_SIGN", "SIGN", (short) 0, ChatColor.AQUA + "능력 검색",
            ChatColor.GRAY + "클릭 후 채팅에 이름이나 특징을 입력하세요."));
        if (abilities.isEmpty()) inventory.setItem(22, GuiTheme.item("BARRIER", "BARRIER", (short) 0,
            ChatColor.YELLOW + "검색 결과가 없습니다", ChatColor.GRAY + "검색 버튼으로 다른 단어를 입력하거나 전체 보기를 누르세요."));
        if (hasQuery(query)) {
            inventory.setItem(46, item("COMPASS", "COMPASS", 1, (short) 0,
                ChatColor.AQUA + "" + ChatColor.BOLD + "검색 해제 · 전체 보기",
                ChatColor.GRAY + "검색어: " + ChatColor.WHITE + query,
                ChatColor.GRAY + "일치한 능력: " + ChatColor.WHITE + abilities.size() + "개",
                ChatColor.DARK_GRAY + "클릭하면 전체 목록을 봅니다."));
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
                ChatColor.GRAY + "검색 버튼: 이름·설명·등급으로 찾기",
                ChatColor.GRAY + "/gw blacklist 로도 관리할 수 있습니다.");
        }
        return item("NAME_TAG", "NAME_TAG", 1, (short) 0,
            ChatColor.GOLD + "" + ChatColor.BOLD + "보기 안내",
            ChatColor.GRAY + "검색 버튼: 이름·설명·등급으로 찾기",
            ChatColor.GRAY + "능력 이름, 설명, 돌 소모량을 확인하세요.");
    }

    private ItemStack currentAbilityItem(Player target, AbilityDefinition ability) {
        return item("NETHER_STAR", "NETHER_STAR", 1, (short) 0,
            ChatColor.AQUA + "" + ChatColor.BOLD + ability.name(),
            ChatColor.WHITE + target.getName() + ChatColor.GRAY + " 님의 현재 능력",
            ChatColor.GRAY + "종류: " + ChatColor.YELLOW + "신의 능력",
            ChatColor.GRAY + "등급: " + gradeColor(ability.grade()) + ability.gradeText(),
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
            ChatColor.GRAY + "기본 조약돌 소모: " + ChatColor.WHITE + stoneCost(cost),
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
        lore.add(ChatColor.WHITE + "등급: " + gradeColor(ability.grade()) + ability.gradeText());
        lore.add("");
        lore.add(ChatColor.WHITE + ability.description());
        lore.add("");
        lore.add(ChatColor.YELLOW + "좌클릭: 사용법 · 재료 · 쿨타임 상세 보기");
        lore.add(ChatColor.DARK_GRAY + "ID: " + ability.id());
        if (showAdminState) {
            lore.add("");
            lore.add(ChatColor.DARK_GRAY + "우클릭: 블랙리스트 전환");
        }
        String icon = ability.grade() == AbilityGrade.S ? "NETHER_STAR"
            : ability.grade() == AbilityGrade.A ? "DIAMOND"
            : ability.grade() == AbilityGrade.B ? "EMERALD"
            : ability.grade() == AbilityGrade.C ? "GOLD_INGOT"
            : ability.grade() == AbilityGrade.D ? "IRON_INGOT" : "BOOK";
        String material = blacklisted ? "RED_STAINED_GLASS" : (enabled ? icon : "GRAY_STAINED_GLASS");
        short damage = blacklisted ? (short) 14 : (enabled ? (short) 0 : (short) 7);
        return item(material, blacklisted || !enabled ? "STAINED_GLASS" : icon, 1, damage,
            color + ability.name(), lore);
    }

    private AbilityDefinition abilityAtSlot(int slot, Player viewer) {
        int offset = ChestLayout.catalogIndex(slot);
        if (offset < 0) {
            return null;
        }

        int index = (listPage(viewer) - 1) * LIST_PAGE_SIZE + offset;
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
            || contains(ability.author(), query)
            || contains(ability.grade().symbol(), query)
            || contains(ability.grade().label(), query);
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
        if (abilityManager.isAbilitySuppressed(player)) return ChatColor.RED + "현재 능력이 봉인되어 있습니다.";
        if (!plugin.game().canUseAbility(player)) return ChatColor.RED + "현재 참가 상태에서는 능력을 사용할 수 없습니다.";
        int baseCost = slot == 1 ? ability.normalStoneCost() : ability.advancedStoneCost();
        int cost = abilityManager.effectiveResourceCost(player, baseCost);
        int held = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack != null && stack.getType() == Material.COBBLESTONE) held += stack.getAmount();
        }
        String resources = "\n" + (held < cost ? ChatColor.RED + "조약돌 부족: " : ChatColor.GRAY + "조약돌 보유/필요: ")
            + held + "/" + cost + "개";
        if ("blacksmith".equals(ability.id()) && slot == 2) {
            int iron = 0;
            for (ItemStack stack : player.getInventory().getStorageContents())
                if (stack != null && stack.getType() == Material.IRON_INGOT) iron += stack.getAmount();
            int requiredIron = abilityManager.effectiveResourceCost(player, 15);
            resources += "\n" + (iron < requiredIron ? ChatColor.RED + "철괴 부족: " : ChatColor.GRAY + "철괴 보유/필요: ")
                + iron + "/" + requiredIron + "개";
        }
        int seconds = slot == 1 ? ability.normalCooldownSeconds() : ability.advancedCooldownSeconds();
        if (seconds > 0) resources += "\n" + ChatColor.GRAY + "현재 적용 쿨타임: "
            + (abilityManager.scaleCooldownMillis(seconds * 1000L) / 1000.0D) + "초";
        long millis = semanticCooldownMillis(player, ability, slot);
        if (millis <= 0L) {
            return ChatColor.WHITE + "쿨타임: " + ChatColor.GREEN + "준비 완료"
                + ChatColor.GRAY + " · 발동 조건은 사용법 참고" + resources;
        }
        return ChatColor.WHITE + "상태: " + ChatColor.YELLOW + "쿨타임 " + ((millis + 999L) / 1000L) + "초" + resources;
    }

    private String currentTimerText(Player player) {
        List<String> timers = abilityManager.activeTimerLines(player);
        if (timers.isEmpty()) {
            return ChatColor.GREEN + "없음";
        }
        return String.join("\n", timers);
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

    private ChatColor gradeColor(AbilityGrade grade) {
        if (grade == AbilityGrade.S) return ChatColor.LIGHT_PURPLE;
        if (grade == AbilityGrade.A) return ChatColor.GOLD;
        if (grade == AbilityGrade.B) return ChatColor.GREEN;
        if (grade == AbilityGrade.C) return ChatColor.YELLOW;
        if (grade == AbilityGrade.D) return ChatColor.RED;
        return ChatColor.GRAY;
    }

    private boolean hasCooldown(String cooldown) {
        return cooldown != null && cooldown.trim().length() > 0 && !"없음".equals(cooldown.trim());
    }

    private boolean hasSkill(String skill) {
        return skill != null && skill.trim().length() > 0 && !"없음".equals(skill.trim());
    }

    private ItemStack closeItem() {
        return GuiTheme.close();
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
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES, org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(GuiText.wrap(lore));
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
