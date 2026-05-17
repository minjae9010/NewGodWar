package kr.newgodwar.ability.builtin;

import kr.newgodwar.ability.api.AbilityPlayerContext;
import kr.newgodwar.ability.api.GodAbility;
import kr.newgodwar.game.GodTeam;
import kr.newgodwar.util.BukkitCompat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
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
    protected boolean ready;
    protected boolean invincible;
    protected String pendingQuestion;
    protected int pendingAnswer = -1;

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

    protected void abyss(Player player, int radius, boolean includeSelf) {
        Location destination = player.getLocation().clone();
        destination.setY(-2.0D);
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity) {
                entity.teleport(destination);
            }
        }
        if (includeSelf) {
            player.teleport(destination);
        }
    }

    protected void lava(Player player, AbilityPlayerContext context) {
        Block base = targetBlock(player, 5);
        final Block block = base.getLocation().add(0, 1, 0).getBlock();
        if (block.getType() == Material.AIR && useNormal(context, player, 0)) {
            block.setType(Material.LAVA);
            later(context, 2, "용암 제거", "용암 제거", () -> {
                if (block.getType() == Material.LAVA) {
                    block.setType(Material.AIR);
                }
            });
        }
    }

    protected void fly(final AbilityPlayerContext context, final Player player, int seconds) {
        player.setAllowFlight(true);
        player.setFlying(true);
        later(context, seconds, "비행 종료", "비행 종료", () -> {
            player.setFlying(false);
            player.setAllowFlight(false);
        });
    }

    protected void pullAll(Player player, int range) {
        if (player.isSneaking() || player.getLocation().clone().add(0, -1, 0).getBlock().getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "웅크리고 있거나 발 밑의 블록이 없어 능력이 발동되지 않았습니다.");
            return;
        }
        for (Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof Player) {
                entity.teleport(player);
            }
        }
    }

    protected void sleepTarget(Player target) {
        effect(target, PotionEffectType.BLINDNESS, 30, 0);
        effect(target, "SLOWNESS", "SLOW", 30, 3);
    }

    protected void teamBuff(AbilityPlayerContext context, Player player) {
        for (Player target : nearbyPlayers(context, player, 20, true)) {
            effect(target, PotionEffectType.SPEED, 15, 0);
            effect(target, PotionEffectType.REGENERATION, 15, 0);
        }
    }

    protected void recall(final AbilityPlayerContext context, final Player player) {
        final Location location = player.getLocation();
        later(context, 10, "귀환 발동", "귀환 발동", () -> {
            player.teleport(location);
            effect(player, PotionEffectType.INVISIBILITY, 3, 0);
        });
    }

    protected void iceSphere(final AbilityPlayerContext context, Location center, int radius, int seconds) {
        final Map<Location, Material> oldBlocks = new LinkedHashMap<Location, Material>();
        for (Location location : sphere(center, radius)) {
            Block block = location.getBlock();
            if (block.getType() != Material.DIAMOND_BLOCK) {
                oldBlocks.put(block.getLocation(), block.getType());
                block.setType(Material.ICE);
            }
        }
        later(context, seconds, "얼음 구체 복구", "얼음 구체 복구", () -> {
            for (Map.Entry<Location, Material> entry : oldBlocks.entrySet()) {
                entry.getKey().getBlock().setType(entry.getValue());
            }
        });
    }

    protected List<Location> sphere(Location center, int radius) {
        List<Location> locations = new ArrayList<Location>();
        int bx = center.getBlockX();
        int by = center.getBlockY();
        int bz = center.getBlockZ();
        for (int x = bx - radius; x <= bx + radius; x++) {
            for (int y = by - radius; y <= by + radius; y++) {
                for (int z = bz - radius; z <= bz + radius; z++) {
                    double distance = (bx - x) * (bx - x) + (by - y) * (by - y) + (bz - z) * (bz - z);
                    if (distance < radius * radius && distance >= (radius - 1) * (radius - 1)) {
                        locations.add(new Location(center.getWorld(), x, y, z));
                    }
                }
            }
        }
        return locations;
    }

    protected void teleportToTargetBlock(Player player) {
        Block block = targetBlock(player, 25);
        Location location = block.getLocation().add(0.5D, 1.0D, 0.5D);
        if (location.getBlock().getType() == Material.AIR && location.clone().add(0, 1, 0).getBlock().getType() == Material.AIR) {
            location.setPitch(player.getLocation().getPitch());
            location.setYaw(player.getLocation().getYaw());
            player.teleport(location);
        }
    }

    protected boolean swapTarget(AbilityPlayerContext context, Player player) {
        Player target = targetPlayerInSight(context, player, 30, true);
        if (target == null) {
            return false;
        }
        Location first = player.getLocation();
        Location second = target.getLocation();
        player.teleport(second);
        target.teleport(first);
        return true;
    }

    protected void push(Player player, List<Player> targets, double power) {
        Vector vector = player.getEyeLocation().getDirection().setY(0.5D).normalize().multiply(power);
        for (Player target : targets) {
            target.setVelocity(vector);
        }
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

    protected void dash(Player player) {
        Vector vector = player.getEyeLocation().getDirection();
        vector.setY(0.5D);
        player.setVelocity(vector);
        player.getWorld().playEffect(player.getLocation(), Effect.ENDER_SIGNAL, 1);
    }

    protected void backstab(AbilityPlayerContext context, Player player) {
        for (Player target : nearbyPlayers(context, player, 10, false)) {
            Location location = target.getLocation().subtract(target.getLocation().getDirection().normalize());
            player.teleport(location);
            return;
        }
        player.sendMessage("스킬을 사용 할 수 있는 상대가 없습니다.");
    }

    protected void judgment(AbilityPlayerContext context, final Player player) {
        final List<Player> targets = nearbyPlayers(player, 5);
        if (targets.isEmpty()) {
            player.sendMessage("능력을 사용할 수 있는 대상이 없습니다.");
            return;
        }
        player.setHealth(Math.max(1.0D, player.getHealth() / 2.0D));
        for (Player target : targets) {
            target.setVelocity(new Vector(0, 1.6D, 0));
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(context.plugin(), () -> {
            for (Player target : targets) {
                target.getWorld().strikeLightning(target.getLocation());
                target.setFireTicks(100);
            }
        }, 4L);
    }

    protected void bless(Player player) {
        if (RANDOM.nextBoolean()) effect(player, "RESISTANCE", "DAMAGE_RESISTANCE", 30, 0);
        if (RANDOM.nextBoolean()) effect(player, "STRENGTH", "INCREASE_DAMAGE", 30, 0);
        if (RANDOM.nextBoolean()) effect(player, PotionEffectType.REGENERATION, 30, 0);
        if (RANDOM.nextBoolean()) effect(player, PotionEffectType.SPEED, 30, 0);
        if (RANDOM.nextBoolean()) effect(player, "HASTE", "FAST_DIGGING", 30, 0);
    }

    protected void curse(List<Player> players) {
        for (Player player : players) {
            curse(player);
        }
    }

    protected void curse(Player player) {
        effect(player, PotionEffectType.HUNGER, 10, 0);
        effect(player, PotionEffectType.POISON, 10, 0);
        effect(player, "SLOWNESS", "SLOW", 10, 0);
        effect(player, "MINING_FATIGUE", "SLOW_DIGGING", 10, 0);
    }

    protected boolean pullTarget(AbilityPlayerContext context, Player player, int range) {
        Player target = targetPlayerInSight(context, player, range, false);
        if (target == null) {
            return false;
        }
        target.teleport(player);
        return true;
    }

    protected void giveSpellBook(Player player, boolean harry) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setTitle("마법 스펠 암기장");
        meta.setAuthor(harry ? "해리 포터" : "헤르미온느 진 그레인저");
        meta.addPage("루모스/Lumos\n녹스/Nox\n봄바르다/Bombarda");
        meta.addPage("스투페파이/Stupefy\n익스펙토 패트로눔/Expecto Patronum");
        meta.addPage("엑스펠리아무스/Expelliarmus\n아바다 케다브라/Avada Kedavra");
        book.setItemMeta(meta);
        player.getInventory().addItem(book);
    }

    protected void castSpell(AbilityPlayerContext context, String spell, boolean harry) {
        final Player player = context.player();
        spell = normalizeSpell(spell);
        if (spell.equals("루모스") || spell.equalsIgnoreCase("Lumos")) {
            if (useNormal(context, player)) {
                setWorldTime(context, player, 1000);
            }
        } else if (spell.equals("녹스") || spell.equalsIgnoreCase("Nox")) {
            if (useNormal(context, player)) {
                setWorldTime(context, player, 18000);
            }
        } else if (spell.equals("봄바르다") || spell.equalsIgnoreCase("Bombarda")) {
            if (useNormal(context, player)) {
                player.getWorld().createExplosion(targetLocation(player, 5), 1.0F);
            }
        } else if (spell.equals("스투페파이") || spell.equalsIgnoreCase("Stupefy")) {
            if (useAdvanced(context, player)) {
                for (Player target : nearbyPlayers(context, player, 10, false)) {
                    if (RANDOM.nextBoolean()) {
                        effect(target, "SLOWNESS", "SLOW", 8, harry ? 1 : 2);
                    }
                }
            }
        } else if (spell.equals("익스펙토 패트로눔") || spell.equalsIgnoreCase("Expecto Patronum")) {
            if (useAdvanced(context, player) && rollChance(harry ? 3 : 2, 4)) {
                invincible = true;
                later(context, 5, "보호 주문 종료", "보호 주문 종료", () -> invincible = false);
            }
        } else if (spell.equals("엑스펠리아무스") || spell.equalsIgnoreCase("Expelliarmus")) {
            Player target = targetPlayerInSight(context, player, 20, false);
            if (target != null && useAdvanced(context, player) && rollPercent(harry ? 25 : 20)) {
                dropHeldAndArmor(target);
            }
        } else if (spell.equals("아바다 케다브라") || spell.equalsIgnoreCase("Avada Kedavra")) {
            Player target = targetPlayerInSight(context, player, 20, false);
            if (target != null && useAdvanced(context, player) && rollPercent(harry ? 20 : 15)) {
                target.setHealth(0.0D);
            }
        }
    }

    private String normalizeSpell(String spell) {
        if (spell == null) {
            return "";
        }
        String normalized = ChatColor.stripColor(spell).trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1).trim();
        }
        return normalized;
    }

    protected void askQuestion(Player player) {
        String[] questions = new String[] {
            "15*12의 값을 구하시오.", "2+2*2+1의 값을 구하시오.", "가로 4, 세로 3인 직사각형의 넓이를 구하시오."
        };
        int[] answers = new int[] {180, 7, 12};
        int index = RANDOM.nextInt(questions.length);
        pendingQuestion = questions[index];
        pendingAnswer = answers[index];
        player.sendMessage(pendingQuestion);
    }

    protected void answerQuestion(AbilityPlayerContext context, String message) {
        if (pendingAnswer < 0) {
            return;
        }
        try {
            int answer = Integer.parseInt(message.trim());
            if (answer == pendingAnswer) {
                context.plugin().abilities().assignRandom(context.player());
                context.player().sendMessage(ChatColor.AQUA + "문제를 맞혀 새 능력을 얻었습니다!");
            } else {
                context.player().sendMessage("아쉽습니다! 정답은 " + pendingAnswer + "입니다.");
            }
            pendingAnswer = -1;
            pendingQuestion = null;
        } catch (NumberFormatException ex) {
            context.player().sendMessage("0~999의 음이 아닌 정수만 입력하십시오.");
        }
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
