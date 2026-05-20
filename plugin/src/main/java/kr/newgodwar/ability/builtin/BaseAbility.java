package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.AbilityPlayerContext;
import kr.newgodwar.ability.api.GodAbility;
import kr.newgodwar.game.GodTeam;
import kr.newgodwar.util.BukkitCompat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

abstract class BaseAbility implements GodAbility {
    protected static final Material COBBLESTONE = Material.COBBLESTONE;
    protected static final Material STAFF = Material.BLAZE_ROD;
    protected static final Random RANDOM = new Random();
    protected static final Set<String> SCROOGE_TEAMS = new HashSet<String>();

    private final Map<Integer, Long> cooldowns = new LinkedHashMap<Integer, Long>();
    private final Map<String, Long> timers = new LinkedHashMap<String, Long>();
    private final Map<Integer, Long> cooldownAnnouncements = new LinkedHashMap<Integer, Long>();
    private final Map<String, Long> timerAnnouncements = new LinkedHashMap<String, Long>();
    protected String targetName;

    @Override
    public void onInteract(AbilityPlayerContext context, PlayerInteractEvent event) {
        Player player = context.player();
        if (!holding(player, STAFF)) {
            return;
        }
        Action action = event.getAction();
        if (isLeft(action)) {
            onStaffLeft(context, player, event);
        } else if (isRight(action)) {
            onStaffRight(context, player, event);
        }
    }

    protected void onStaffLeft(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
    }

    protected void onStaffRight(AbilityPlayerContext context, Player player, PlayerInteractEvent event) {
    }

    protected boolean isLeft(Action action) {
        return action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
    }

    protected boolean isRight(Action action) {
        return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
    }

    @Override
    public void setTarget(AbilityPlayerContext context, CommandSender sender, String targetName) {
        if (!requiresTarget()) {
            sender.sendMessage(ChatColor.RED + "이 능력은 타깃 지정이 필요하지 않습니다.");
            return;
        }
        if (context.player().getName().equalsIgnoreCase(targetName)) {
            sender.sendMessage("자기 자신을 타깃으로 등록 할 수 없습니다.");
            return;
        }
        this.targetName = targetName;
        sender.sendMessage("타깃을 등록했습니다.   " + ChatColor.GREEN + targetName);
    }

    protected boolean use(AbilityPlayerContext context, Player player, int slot, Material material, int amount, int cooldownSeconds) {
        int realCost = material == COBBLESTONE ? cost(context, amount) : amount;
        if (!readyCooldown(context, player, slot, cooldownSeconds)) {
            refreshDisplay(context);
            return false;
        }
        if (!has(context, player, material, realCost)) {
            refreshDisplay(context);
            return false;
        }
        if (realCost > 0) {
            player.getInventory().removeItem(new ItemStack(material, realCost));
        }
        setCooldown(context, slot, cooldownSeconds);
        sendAbilityMessage(context, player, "success", ChatColor.GREEN + "능력을 사용했습니다.");
        return true;
    }

    protected boolean useNormal(AbilityPlayerContext context, Player player) {
        return useNormal(context, player, 1);
    }

    protected boolean useNormal(AbilityPlayerContext context, Player player, int slot) {
        return use(context, player, slot, COBBLESTONE, context.ability().normalStoneCost(), context.ability().normalCooldownSeconds());
    }

    protected boolean useAdvanced(AbilityPlayerContext context, Player player) {
        return useAdvanced(context, player, 2);
    }

    protected boolean useAdvanced(AbilityPlayerContext context, Player player, int slot) {
        return use(context, player, slot, COBBLESTONE, context.ability().advancedStoneCost(), context.ability().advancedCooldownSeconds());
    }

    protected boolean readyNormal(AbilityPlayerContext context, Player player, int slot) {
        return readyCooldown(context, player, slot, context.ability().normalCooldownSeconds());
    }

    protected boolean hasNormalCost(AbilityPlayerContext context, Player player) {
        return has(context, player, COBBLESTONE, cost(context, context.ability().normalStoneCost()));
    }

    protected void takeNormalCost(AbilityPlayerContext context, Player player) {
        int realCost = cost(context, context.ability().normalStoneCost());
        if (realCost > 0) {
            player.getInventory().removeItem(new ItemStack(COBBLESTONE, realCost));
        }
    }

    protected void setCooldown(AbilityPlayerContext context, int slot, int cooldownSeconds) {
        cooldowns.put(slot, System.currentTimeMillis() + context.plugin().abilities().scaleCooldownMillis(cooldownSeconds * 1000L));
        cooldownAnnouncements.remove(slot);
        refreshDisplay(context);
    }

    protected void setRawCooldown(int slot, long millis) {
        cooldowns.put(slot, System.currentTimeMillis() + millis);
        cooldownAnnouncements.remove(slot);
    }

    protected void setRawCooldown(AbilityPlayerContext context, int slot, long millis) {
        cooldowns.put(slot, System.currentTimeMillis() + millis);
        cooldownAnnouncements.remove(slot);
        refreshDisplay(context);
    }

    private void refreshDisplay(final AbilityPlayerContext context) {
        context.plugin().game().refreshPlayerDisplay(context.player());
    }

    @Override
    public long cooldownRemainingMillis(int slot) {
        Long until = cooldowns.get(slot);
        if (until == null) {
            return 0L;
        }
        return Math.max(0L, until - System.currentTimeMillis());
    }

    @Override
    public void clearCooldowns() {
        cooldowns.clear();
        cooldownAnnouncements.clear();
    }

    protected boolean readyCooldown(AbilityPlayerContext context, Player player, int slot, int cooldownSeconds) {
        Long until = cooldowns.get(slot);
        long now = System.currentTimeMillis();
        if (until != null && until > now) {
            sendAbilityMessage(context, player, "failure", ChatColor.YELLOW + "아직 능력을 사용할 수 없습니다. 쿨타임 "
                + ((until - now + 999L) / 1000L) + "초 남았습니다.");
            return false;
        }
        return true;
    }

    protected int cost(AbilityPlayerContext context, int amount) {
        GodTeam team = context.plugin().game().teamOf(context.player());
        if (team != null && SCROOGE_TEAMS.contains(team.id())) {
            return Math.max(0, amount / 2);
        }
        return amount;
    }

    protected boolean has(AbilityPlayerContext context, Player player, Material material, int amount) {
        if (amount <= 0 || player.getInventory().contains(material, amount)) {
            return true;
        }
        sendAbilityMessage(context, player, "failure", ChatColor.RED + materialDisplayName(material) + " " + amount + "개가 부족합니다.");
        return false;
    }

    protected void sendAbilityMessage(AbilityPlayerContext context, Player player, String type, String message) {
        if (!context.plugin().getConfig().getBoolean("abilities.messages.enabled", true)) {
            return;
        }
        if (!context.plugin().getConfig().getBoolean("abilities.messages." + type, true)) {
            return;
        }
        player.sendMessage(message);
    }

    protected boolean rollChance(int successfulOutcomes, int totalOutcomes) {
        if (successfulOutcomes <= 0 || totalOutcomes <= 0) {
            return false;
        }
        if (successfulOutcomes >= totalOutcomes) {
            return true;
        }
        return RANDOM.nextInt(totalOutcomes) < successfulOutcomes;
    }

    protected boolean rollPercent(int percent) {
        return rollChance(Math.max(0, Math.min(100, percent)), 100);
    }

    protected boolean oneIn(int totalOutcomes) {
        return rollChance(1, totalOutcomes);
    }

    private String materialDisplayName(Material material) {
        String name = material.name();
        if (material == COBBLESTONE || "COBBLESTONE".equals(name)) {
            return "조약돌";
        }
        if ("IRON_INGOT".equals(name) || "IRON".equals(name)) {
            return "철괴";
        }
        if ("DIAMOND".equals(name)) {
            return "다이아몬드";
        }
        return name.replace('_', ' ');
    }

    protected boolean holding(Player player, Material material) {
        return player.getItemInHand() != null && player.getItemInHand().getType() == material;
    }

    protected void give(Player player, Material material, int amount) {
        player.getInventory().addItem(new ItemStack(material, amount));
    }

    protected Material material(String modernName, String legacyName) {
        Material material = Material.matchMaterial(modernName);
        if (material == null) {
            material = Material.matchMaterial(legacyName);
        }
        if (material == null) {
            material = Material.matchMaterial("LEGACY_" + legacyName);
        }
        return material == null ? Material.STONE : material;
    }

    protected void dropNaturally(Player player, ItemStack item) {
        if (item != null && item.getType() != Material.AIR && item.getAmount() > 0) {
            player.getWorld().dropItemNaturally(player.getLocation(), item.clone());
        }
    }

    protected void dropHeldAndArmor(Player player) {
        dropNaturally(player, player.getItemInHand());
        player.setItemInHand(new ItemStack(Material.AIR));

        for (ItemStack armor : player.getInventory().getArmorContents()) {
            dropNaturally(player, armor);
        }
        player.getInventory().setArmorContents(new ItemStack[] {null, null, null, null});
    }

    protected void effect(Player player, PotionEffectType type, int seconds, int amplifier) {
        BukkitCompat.addPotionEffect(player, type, seconds * 20, amplifier, true, false);
    }

    protected void effect(Player player, String modernName, String legacyName, int seconds, int amplifier) {
        PotionEffectType type = effectType(modernName, legacyName);
        if (type != null) {
            effect(player, type, seconds, amplifier);
        }
    }

    protected void respawnEffect(final AbilityPlayerContext context, final PotionEffectType type, final int seconds, final int amplifier) {
        effect(context.player(), type, seconds, amplifier);
        Bukkit.getScheduler().runTaskLater(context.plugin(), () -> {
            Player player = context.player();
            if (player != null && player.isOnline()) {
                effect(player, type, seconds, amplifier);
            }
        }, 1L);
    }

    protected void respawnEffect(AbilityPlayerContext context, String modernName, String legacyName, int seconds, int amplifier) {
        PotionEffectType type = effectType(modernName, legacyName);
        if (type != null) {
            respawnEffect(context, type, seconds, amplifier);
        }
    }

    protected void respawnFoodLevel(final AbilityPlayerContext context, final int foodLevel) {
        context.player().setFoodLevel(foodLevel);
        Bukkit.getScheduler().runTaskLater(context.plugin(), () -> {
            Player player = context.player();
            if (player != null && player.isOnline()) {
                player.setFoodLevel(foodLevel);
            }
        }, 1L);
    }

    protected void effectTicks(Player player, String modernName, String legacyName, int ticks, int amplifier) {
        PotionEffectType type = effectType(modernName, legacyName);
        if (type != null) {
            BukkitCompat.addPotionEffect(player, type, ticks, amplifier, true, false);
        }
    }

    protected void removeEffect(Player player, String modernName, String legacyName) {
        PotionEffectType type = effectType(modernName, legacyName);
        if (type != null) {
            player.removePotionEffect(type);
        }
    }

    protected PotionEffectType effectType(String modernName, String legacyName) {
        PotionEffectType type = PotionEffectType.getByName(modernName);
        if (type == null) {
            type = PotionEffectType.getByName(legacyName);
        }
        return type;
    }

    protected void heal(Player player) {
        player.setHealth(player.getMaxHealth());
    }

    protected void damage(Player target, double amount, Player source) {
        target.damage(Math.min(target.getHealth(), amount), source);
    }

    protected boolean setWorldTime(AbilityPlayerContext context, Player player, long time) {
        try {
            player.getWorld().setTime(time);
            return true;
        } catch (IllegalArgumentException ex) {
            sendAbilityMessage(context, player, "failure", ChatColor.RED + "이 월드는 시간을 변경할 수 없습니다.");
            return false;
        }
    }

    protected boolean fire(EntityDamageEvent.DamageCause cause) {
        return cause == EntityDamageEvent.DamageCause.FIRE || cause == EntityDamageEvent.DamageCause.FIRE_TICK || cause == EntityDamageEvent.DamageCause.LAVA;
    }

    protected Block targetBlock(Player player, int range) {
        Block block = firstSightBlock(player, range);
        if (block != null) {
            return block;
        }
        return fallbackTargetLocation(player, range).getBlock();
    }

    protected Location targetLocation(Player player, int range) {
        Block block = firstSightBlock(player, range);
        if (block != null) {
            return block.getLocation();
        }
        return fallbackTargetLocation(player, range);
    }

    private Location fallbackTargetLocation(Player player, int range) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection();
        if (direction.lengthSquared() == 0.0D) {
            return eye;
        }
        return eye.add(direction.normalize().multiply(range));
    }

    private Block firstSightBlock(Player player, int range) {
        BlockIterator iterator = new BlockIterator(player, range);
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (!isAir(block)) {
                return block;
            }
        }
        return null;
    }

    private boolean isAir(Block block) {
        return block == null || block.getType() == Material.AIR || block.getType().name().endsWith("_AIR");
    }

    protected List<Player> nearbyPlayers(AbilityPlayerContext context, Player player, int range, boolean sameTeam) {
        List<Player> players = new ArrayList<Player>();
        for (Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof Player) {
                Player target = (Player) entity;
                if (sameTeam == sameTeam(context, player, target)) {
                    players.add(target);
                }
            }
        }
        return players;
    }

    protected List<Player> nearbyPlayers(Player player, int range) {
        List<Player> players = new ArrayList<Player>();
        for (Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof Player) {
                players.add((Player) entity);
            }
        }
        return players;
    }

    protected List<Player> nearbyPlayers(AbilityPlayerContext context, Player player, double x, double y, double z, boolean sameTeam) {
        List<Player> players = new ArrayList<Player>();
        for (Entity entity : player.getNearbyEntities(x, y, z)) {
            if (entity instanceof Player) {
                Player target = (Player) entity;
                if (sameTeam == sameTeam(context, player, target)) {
                    players.add(target);
                }
            }
        }
        return players;
    }

    protected List<Player> alliedPlayers(AbilityPlayerContext context, Player player, boolean includeSelf) {
        List<Player> players = new ArrayList<Player>();
        GodTeam team = context.plugin().game().teamOf(player);
        if (team == null) {
            return players;
        }
        for (Player target : BukkitCompat.onlinePlayers()) {
            if (!includeSelf && target.equals(player)) {
                continue;
            }
            if (team == context.plugin().game().teamOf(target)) {
                players.add(target);
            }
        }
        return players;
    }

    protected boolean sameTeam(AbilityPlayerContext context, Player a, Player b) {
        GodTeam first = context.plugin().game().teamOf(a);
        GodTeam second = context.plugin().game().teamOf(b);
        return first != null && first == second;
    }

    protected Player targetPlayer() {
        return targetName == null ? null : Bukkit.getPlayer(targetName);
    }

    protected Player targetPlayerInSight(AbilityPlayerContext context, Player player, int range, boolean sameTeam) {
        Player target = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Player candidate : BukkitCompat.onlinePlayers()) {
            if (candidate.equals(player) || candidate.getWorld() != player.getWorld()) {
                continue;
            }
            if (sameTeam(context, player, candidate) != sameTeam || !lookingAt(player, candidate, range)) {
                continue;
            }
            double distance = player.getLocation().distanceSquared(candidate.getLocation());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                target = candidate;
            }
        }
        if (target == null) {
            player.sendMessage(ChatColor.RED + "타깃이 해당 구역에 없습니다.");
        }
        return target;
    }

    protected Player commandTargetPlayerInSight(AbilityPlayerContext context, Player player, int range, boolean sameTeam) {
        Player target = commandTargetPlayer(context, player, sameTeam);
        if (target == null) {
            return null;
        }
        if (!lookingAt(player, target, range)) {
            player.sendMessage(ChatColor.RED + "타깃이 해당 구역에 없습니다.");
            return null;
        }
        return target;
    }

    protected Player commandTargetPlayerInRange(AbilityPlayerContext context, Player player, int range, boolean sameTeam) {
        Player target = commandTargetPlayer(context, player, sameTeam);
        if (target == null) {
            return null;
        }
        if (player.getLocation().distanceSquared(target.getLocation()) > range * range) {
            player.sendMessage(ChatColor.RED + "타깃이 해당 구역에 없습니다.");
            return null;
        }
        return target;
    }

    private Player commandTargetPlayer(AbilityPlayerContext context, Player player, boolean sameTeam) {
        if (targetName == null || targetName.trim().length() == 0) {
            sendAbilityMessage(context, player, "failure", ChatColor.RED + "먼저 /x <플레이어>로 타깃을 지정하세요.");
            return null;
        }
        Player target = targetPlayer();
        if (target == null || !target.isOnline() || target.getWorld() != player.getWorld()) {
            player.sendMessage(ChatColor.RED + "타깃이 해당 구역에 없습니다.");
            return null;
        }
        if (sameTeam(context, player, target) != sameTeam) {
            player.sendMessage(ChatColor.RED + "타깃이 해당 구역에 없습니다.");
            return null;
        }
        return target;
    }

    private boolean lookingAt(Player player, Player target, int range) {
        if (player.getLocation().distanceSquared(target.getLocation()) > range * range) {
            return false;
        }
        if (!player.hasLineOfSight(target)) {
            return false;
        }
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        return lookingNear(eye, direction, target.getEyeLocation(), range)
            || lookingNear(eye, direction, target.getLocation().add(0.0D, 0.9D, 0.0D), range);
    }

    private boolean lookingNear(Location eye, Vector direction, Location point, int range) {
        Vector offset = point.toVector().subtract(eye.toVector());
        double projected = offset.dot(direction);
        if (projected < 0.0D || projected > range) {
            return false;
        }
        double missDistanceSquared = offset.lengthSquared() - projected * projected;
        return missDistanceSquared <= 2.25D;
    }

    protected void later(AbilityPlayerContext context, int seconds, Runnable runnable) {
        later(context, seconds, "능력 타이머", "능력 효과", runnable);
    }

    protected void later(final AbilityPlayerContext context, final int seconds, final String timerName, final String triggerText, final Runnable runnable) {
        final String name = timerName == null || timerName.trim().length() == 0 ? "능력 타이머" : timerName;
        final String text = triggerText == null || triggerText.trim().length() == 0 ? "능력 효과" : triggerText;
        timers.put(name, System.currentTimeMillis() + seconds * 1000L);
        timerAnnouncements.remove(name);
        sendAbilityMessage(context, context.player(), "timer", ChatColor.YELLOW + text + ChatColor.WHITE + " : "
            + ChatColor.AQUA + seconds + "초 후");
        refreshDisplay(context);
        Bukkit.getScheduler().scheduleSyncDelayedTask(context.plugin(), () -> {
            timers.remove(name);
            timerAnnouncements.remove(name);
            refreshDisplay(context);
            runnable.run();
            refreshDisplay(context);
        }, seconds * 20L);
    }

    @Override
    public void onCountdownTick(AbilityPlayerContext context) {
        announceCooldowns(context);
        announceTimers(context);
    }

    private void announceCooldowns(AbilityPlayerContext context) {
        long now = System.currentTimeMillis();
        List<Integer> expired = new ArrayList<Integer>();
        for (Map.Entry<Integer, Long> entry : cooldowns.entrySet()) {
            long remaining = entry.getValue() - now;
            if (remaining <= 0L) {
                expired.add(entry.getKey());
                continue;
            }
            announceCountdown(context, cooldownAnnouncements, entry.getKey(), remaining,
                cooldownLabel(entry.getKey()) + " 쿨타임");
        }
        for (Integer slot : expired) {
            cooldowns.remove(slot);
            cooldownAnnouncements.remove(slot);
        }
    }

    private void announceTimers(AbilityPlayerContext context) {
        long now = System.currentTimeMillis();
        List<String> expired = new ArrayList<String>();
        for (Map.Entry<String, Long> entry : timers.entrySet()) {
            long remaining = entry.getValue() - now;
            if (remaining <= 0L) {
                expired.add(entry.getKey());
                continue;
            }
            announceCountdown(context, timerAnnouncements, entry.getKey(), remaining, entry.getKey());
        }
        for (String key : expired) {
            timers.remove(key);
            timerAnnouncements.remove(key);
        }
    }

    private <T> void announceCountdown(AbilityPlayerContext context, Map<T, Long> announcements, T key, long remaining, String label) {
        if (remaining > 3000L) {
            return;
        }
        long seconds = Math.max(1L, (remaining + 999L) / 1000L);
        Long last = announcements.get(key);
        if (last != null && last.longValue() == seconds) {
            return;
        }
        announcements.put(key, seconds);
        sendAbilityMessage(context, context.player(), "timer", ChatColor.YELLOW + label + " "
            + ChatColor.AQUA + seconds + "초" + ChatColor.YELLOW + " 남았습니다.");
    }

    private String cooldownLabel(int slot) {
        if (slot == 1) {
            return "일반 능력";
        }
        if (slot == 2) {
            return "고급 능력";
        }
        return "능력";
    }

    @Override
    public List<String> activeTimerLines() {
        long now = System.currentTimeMillis();
        List<String> lines = new ArrayList<String>();
        List<String> expired = new ArrayList<String>();
        for (Map.Entry<String, Long> entry : timers.entrySet()) {
            long remaining = entry.getValue() - now;
            if (remaining <= 0L) {
                expired.add(entry.getKey());
                continue;
            }
            lines.add(ChatColor.WHITE + entry.getKey() + ChatColor.GRAY + " "
                + ChatColor.YELLOW + ((remaining + 999L) / 1000L) + "초");
        }
        for (String key : expired) {
            timers.remove(key);
            timerAnnouncements.remove(key);
        }
        return lines;
    }

    protected void push(AbilityPlayerContext context, Player player, List<Player> targets, double power, long delayTicks) {
        Vector up = new Vector(0, 0.5D, 0);
        for (Player target : targets) {
            target.setVelocity(up);
        }
        Vector horizontal = player.getEyeLocation().getDirection().setY(0.0D);
        if (horizontal.lengthSquared() == 0.0D) {
            return;
        }
        final Vector vector = horizontal.normalize().multiply(power * 1.4D);
        Bukkit.getScheduler().scheduleSyncDelayedTask(context.plugin(), () -> {
            for (Player target : targets) {
                target.setVelocity(vector);
            }
        }, delayTicks);
    }

    protected boolean isPickaxe(Material material) {
        String name = material == null ? "" : material.name();
        return "WOOD_PICKAXE".equals(name) || "WOODEN_PICKAXE".equals(name) || "LEGACY_WOOD_PICKAXE".equals(name)
            || material == Material.STONE_PICKAXE || material == Material.IRON_PICKAXE || material == Material.DIAMOND_PICKAXE;
    }

    protected boolean isSword(Material material) {
        String name = material == null ? "" : material.name();
        return "WOOD_SWORD".equals(name) || "WOODEN_SWORD".equals(name) || "LEGACY_WOOD_SWORD".equals(name)
            || "GOLD_SWORD".equals(name) || "GOLDEN_SWORD".equals(name) || "LEGACY_GOLD_SWORD".equals(name)
            || material == Material.STONE_SWORD || material == Material.IRON_SWORD || material == Material.DIAMOND_SWORD;
    }

    protected int swordDamage(Material material) {
        String name = material == null ? "" : material.name();
        if ("WOOD_SWORD".equals(name) || "WOODEN_SWORD".equals(name) || "LEGACY_WOOD_SWORD".equals(name)
            || "GOLD_SWORD".equals(name) || "GOLDEN_SWORD".equals(name) || "LEGACY_GOLD_SWORD".equals(name)) return 4;
        if (material == Material.STONE_SWORD) return 5;
        if (material == Material.IRON_SWORD) return 6;
        if (material == Material.DIAMOND_SWORD) return 7;
        return 1;
    }
}
