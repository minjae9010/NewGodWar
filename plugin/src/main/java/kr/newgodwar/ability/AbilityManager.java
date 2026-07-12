package kr.newgodwar.ability;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.ability.api.AbilityDamageContext;
import kr.newgodwar.ability.api.AbilityDefinition;
import kr.newgodwar.ability.api.AbilityKillContext;
import kr.newgodwar.ability.api.AbilityPlayerContext;
import kr.newgodwar.ability.api.AbilityRegistrar;
import kr.newgodwar.game.GodTeam;
import kr.newgodwar.util.BukkitCompat;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AbilityManager {

    private static final long DAMAGE_ATTRIBUTION_MILLIS = 15000L;

    private final NewGodWarPlugin plugin;
    private final Random random = new Random();
    private final AbilityRegistry registry = new AbilityRegistry();
    private final Map<UUID, AbilitySession> assignments = new ConcurrentHashMap<UUID, AbilitySession>();
    private final Map<UUID, Long> suppressedUntil = new ConcurrentHashMap<UUID, Long>();
    private final DamageAttributionTracker recentDamageSources = new DamageAttributionTracker(DAMAGE_ATTRIBUTION_MILLIS);
    private final DamageAttributionTracker attributedEntities = new DamageAttributionTracker(DAMAGE_ATTRIBUTION_MILLIS);
    private final ThreadLocal<UUID> synchronousDamageSource = new ThreadLocal<UUID>();
    private final List<String> recentRandomAbilityIds = Collections.synchronizedList(new ArrayList<String>());

    public AbilityManager(NewGodWarPlugin plugin) {
        this.plugin = plugin;
        loadRegistrars();
    }

    public AbilityRegistry registry() {
        return registry;
    }

    public void clear() {
        for (Map.Entry<UUID, AbilitySession> entry : assignments.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            entry.getValue().ability().cancelScheduledTasks();
            if (player != null) {
                entry.getValue().ability().onRemove(playerContext(player, entry.getValue().definition()));
                BukkitCompat.clearPotionEffects(player);
            }
        }
        assignments.clear();
        suppressedUntil.clear();
        recentDamageSources.clear();
        attributedEntities.clear();
        synchronousDamageSource.remove();
        recentRandomAbilityIds.clear();
    }

    public AbilityDefinition assignRandom(Player player) {
        List<AbilityDefinition> enabled = enabledAbilities(player);
        AbilityDefinition definition = chooseDiverseRandom(enabled, player);
        set(player, definition);
        rememberRandomAbility(definition);
        return definition;
    }

    public void set(Player player, AbilityDefinition definition) {
        if (definition == null) {
            return;
        }
        AbilitySession previous = assignments.remove(player.getUniqueId());
        if (previous != null) {
            deactivateSession(player, previous);
        }
        suppressedUntil.remove(player.getUniqueId());
        BukkitCompat.clearPotionEffects(player);

        AbilitySession session = new AbilitySession(definition, definition.create());
        assignments.put(player.getUniqueId(), session);
        session.ability().onAssign(playerContext(player, definition));
        if (plugin.game() != null && plugin.game().isRunning()) {
            session.ability().onPrepare(playerContext(player, definition));
        }
        sendAbilityInfo(player, definition);
        if (session.ability().requiresTarget()) {
            sendTargetGuide(player);
        }
        if (plugin.game() != null) {
            plugin.game().refreshPlayerDisplay(player);
        }
    }

    public boolean remove(Player player) {
        AbilitySession previous = assignments.remove(player.getUniqueId());
        if (previous == null) {
            return false;
        }
        deactivateSession(player, previous);
        suppressedUntil.remove(player.getUniqueId());
        if (plugin.game() != null) {
            plugin.game().refreshPlayerDisplay(player);
        }
        return true;
    }

    public AbilitySession session(Player player) {
        return assignments.get(player.getUniqueId());
    }

    public boolean suppressAbility(final Player player, final int seconds) {
        final AbilitySession session = session(player);
        if (session == null || seconds <= 0 || isAbilitySuppressed(player)) {
            return false;
        }

        final long until = System.currentTimeMillis() + seconds * 1000L;
        suppressedUntil.put(player.getUniqueId(), until);
        deactivateSession(player, session);
        player.sendMessage(ChatColor.DARK_PURPLE + "능력이 " + seconds + "초 동안 봉인되었습니다.");
        if (plugin.game() != null) {
            plugin.game().refreshPlayerDisplay(player);
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                restoreSuppressedAbility(player.getUniqueId(), until);
            }
        }, seconds * 20L);
        return true;
    }

    public boolean isAbilitySuppressed(Player player) {
        Long until = suppressedUntil.get(player.getUniqueId());
        if (until == null) {
            return false;
        }
        if (until <= System.currentTimeMillis()) {
            restoreSuppressedAbility(player.getUniqueId(), until);
            return false;
        }
        return true;
    }

    public AbilityDefinition get(Player player) {
        AbilitySession session = session(player);
        return session == null ? null : session.definition();
    }

    public Map<UUID, AbilityDefinition> assignedAbilities() {
        Map<UUID, AbilityDefinition> result = new ConcurrentHashMap<UUID, AbilityDefinition>();
        for (Map.Entry<UUID, AbilitySession> entry : assignments.entrySet()) {
            result.put(entry.getKey(), entry.getValue().definition());
        }
        return Collections.unmodifiableMap(result);
    }

    public long cooldownRemainingMillis(Player player, int slot) {
        AbilitySession session = activeSession(player);
        return session == null ? 0L : session.ability().cooldownRemainingMillis(slot);
    }

    public List<String> activeTimerLines(Player player) {
        AbilitySession session = activeSession(player);
        return session == null ? Collections.<String>emptyList() : session.ability().activeTimerLines();
    }

    public void clearCooldowns(Player player) {
        AbilitySession session = session(player);
        if (session != null) {
            session.ability().clearCooldowns();
        }
    }

    public void clearAllCooldowns() {
        for (AbilitySession session : assignments.values()) {
            session.ability().clearCooldowns();
        }
    }

    public List<AbilityDefinition> enabledAbilities(Player player) {
        List<AbilityDefinition> enabled = new ArrayList<AbilityDefinition>();
        for (AbilityDefinition definition : registry.all()) {
            if (!isEnabled(definition)) {
                continue;
            }
            if (!definition.create().supports(player)) {
                continue;
            }
            enabled.add(definition);
        }
        return enabled;
    }

    public boolean isEnabled(AbilityDefinition definition) {
        return plugin.getConfig().getBoolean("abilities." + definition.id() + ".enabled", definition.enabledByDefault())
            && !isBlacklisted(definition);
    }

    public boolean isBlacklisted(AbilityDefinition definition) {
        return definition != null && blacklistedAbilityIds().contains(definition.id().toLowerCase(Locale.ROOT));
    }

    public List<String> blacklistedAbilityIds() {
        List<String> ids = new ArrayList<String>();
        for (String id : plugin.getConfig().getStringList("blacklist.abilities")) {
            if (id != null && id.trim().length() > 0) {
                ids.add(id.toLowerCase(Locale.ROOT).trim());
            }
        }
        return ids;
    }

    public boolean setBlacklisted(String abilityId, boolean blacklisted) {
        AbilityDefinition definition = registry.get(abilityId);
        if (definition == null) {
            return false;
        }
        List<String> ids = blacklistedAbilityIds();
        String normalized = definition.id().toLowerCase(Locale.ROOT);
        if (blacklisted && !ids.contains(normalized)) {
            ids.add(normalized);
        } else if (!blacklisted) {
            ids.remove(normalized);
        }
        plugin.getConfig().set("blacklist.abilities", ids);
        plugin.saveConfig();
        return true;
    }

    public boolean toggleBlacklisted(String abilityId) {
        AbilityDefinition definition = registry.get(abilityId);
        if (definition == null) {
            return false;
        }
        return setBlacklisted(definition.id(), !isBlacklisted(definition));
    }

    public boolean urfEnabled() {
        return plugin.getConfig().getBoolean("game.urf.enabled", false);
    }

    public int urfCooldownPercent() {
        return (int) Math.round((1.0D - urfCooldownMultiplier()) * 100.0D);
    }

    public void setUrfCooldownPercent(int percent) {
        int clamped = Math.max(0, Math.min(100, percent));
        plugin.getConfig().set("game.urf.cooldown-multiplier", (100 - clamped) / 100.0D);
        plugin.saveConfig();
    }

    public double urfCooldownMultiplier() {
        return Math.max(0.0D, plugin.getConfig().getDouble("game.urf.cooldown-multiplier", 0.2D));
    }

    public long scaleCooldownMillis(long baseMillis) {
        if (!urfEnabled()) {
            return baseMillis;
        }
        return Math.max(0L, Math.round(baseMillis * urfCooldownMultiplier()));
    }

    private void sendAbilityInfo(Player player, AbilityDefinition definition) {
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "능력이 배정되었습니다: "
            + ChatColor.WHITE + definition.name() + ChatColor.DARK_GRAY + " (" + definition.id() + ")");
        player.sendMessage(ChatColor.GRAY + "설명: " + ChatColor.YELLOW + definition.description());
        player.sendMessage(ChatColor.GRAY + "등급: " + ChatColor.YELLOW + definition.gradeText());
        if (hasSkill(definition.normalSkill())) {
            player.sendMessage(ChatColor.GRAY + "일반: " + ChatColor.WHITE + skillLine(definition.normalSkill(), definition.normalStoneCost(), definition.normalCooldown()));
        }
        if (hasSkill(definition.advancedSkill())) {
            player.sendMessage(ChatColor.GRAY + "고급: " + ChatColor.WHITE + skillLine(definition.advancedSkill(), definition.advancedStoneCost(), definition.advancedCooldown()));
        }
        player.sendMessage(ChatColor.GRAY + "패시브: " + ChatColor.WHITE + emptySkill(definition.passiveSkill()));
        player.sendMessage(ChatColor.DARK_GRAY + "/a 로 다시 확인할 수 있습니다.");
    }

    private void sendTargetGuide(Player player) {
        player.sendMessage(ChatColor.AQUA + "이 능력은 타깃 지정이 필요합니다.");
        player.sendMessage(ChatColor.GRAY + "사용 전 " + ChatColor.YELLOW + "/x <플레이어>"
            + ChatColor.GRAY + " 또는 " + ChatColor.YELLOW + "/gw target <플레이어>"
            + ChatColor.GRAY + " 로 대상을 지정하세요.");
    }

    private String skillLine(String skill, int stoneCost, String cooldown) {
        return emptySkill(skill) + ChatColor.DARK_GRAY + " / 조약돌 " + stoneCost(stoneCost)
            + ChatColor.DARK_GRAY + " / 쿨타임 " + cooldown(cooldown);
    }

    private String emptySkill(String skill) {
        return skill == null || skill.trim().length() == 0 ? "없음" : skill;
    }

    private boolean hasSkill(String skill) {
        return skill != null && skill.trim().length() > 0 && !"없음".equals(skill.trim());
    }

    private String stoneCost(int cost) {
        return cost <= 0 ? "없음" : cost + "개";
    }

    private String cooldown(String cooldown) {
        return cooldown == null || cooldown.trim().length() == 0 ? "없음" : cooldown;
    }

    public void handleDamage(Player damager, Player victim, EntityDamageByEntityEvent event) {
        AbilitySession session = activeSession(damager);
        if (session != null) {
            session.ability().onDamage(new AbilityDamageContext(plugin, damager, victim, session.definition(), event));
            session.ability().onDamageByEntity(playerContext(damager, session.definition()), event, victim, true);
        }

        AbilitySession victimSession = activeSession(victim);
        if (victimSession != null) {
            victimSession.ability().onDamageByEntity(playerContext(victim, victimSession.definition()), event, damager, false);
        }
    }

    public void handleKill(Player killer, Player victim, PlayerDeathEvent event) {
        AbilitySession session = activeSession(killer);
        if (session != null) {
            session.ability().onKill(new AbilityKillContext(plugin, killer, victim, session.definition(), event));
        }
    }

    public void handleDeath(PlayerDeathEvent event) {
        for (Map.Entry<UUID, AbilitySession> entry : assignments.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player != null) {
                AbilitySession session = activeSession(player);
                if (session != null) {
                    session.ability().onDeath(playerContext(player, session.definition()), event);
                }
            }
        }
    }

    public void reapply(Player player) {
        AbilitySession session = activeSession(player);
        if (session != null) {
            session.ability().onAssign(playerContext(player, session.definition()));
        }
    }

    public void prepare(Player player) {
        AbilitySession session = activeSession(player);
        if (session != null) {
            AbilityPlayerContext context = playerContext(player, session.definition());
            session.ability().onAssign(context);
            session.ability().onPrepare(context);
        }
    }

    public void deactivate(Player player) {
        if (player == null) {
            return;
        }
        AbilitySession session = session(player);
        if (session != null) {
            deactivateSession(player, session);
        }
    }

    public void tick(Iterable<? extends Player> players) {
        for (Player player : players) {
            AbilitySession session = activeSession(player);
            if (session != null) {
                session.ability().onTick(playerContext(player, session.definition()));
            }
        }
    }

    public void tickCountdownAlerts(Iterable<? extends Player> players) {
        for (Player player : players) {
            AbilitySession session = activeSession(player);
            if (session != null) {
                session.ability().onCountdownTick(playerContext(player, session.definition()));
            }
        }
    }

    public void handleInteract(Player player, PlayerInteractEvent event) {
        if (BukkitCompat.hasOpenContainer(player)) {
            return;
        }
        if (!BukkitCompat.isMainHandInteract(event)) {
            return;
        }
        if (event.isCancelled() && !isAirInteract(event.getAction())) {
            return;
        }
        AbilitySession session = activeSession(player);
        if (session != null) {
            session.ability().onInteract(playerContext(player, session.definition()), event);
        }
    }

    private boolean isAirInteract(Action action) {
        return action == Action.LEFT_CLICK_AIR || action == Action.RIGHT_CLICK_AIR;
    }

    public void handleGenericDamage(Player player, EntityDamageEvent event) {
        AbilitySession session = activeSession(player);
        if (session != null) {
            session.ability().onGenericDamage(playerContext(player, session.definition()), event);
        }
    }

    public void handleProjectileHit(Player shooter, Player victim, EntityDamageByEntityEvent event) {
        AbilitySession session = activeSession(shooter);
        if (session != null) {
            session.ability().onProjectileHit(playerContext(shooter, session.definition()), event, victim);
        }
        if (event.isCancelled()) {
            return;
        }
        AbilitySession victimSession = activeSession(victim);
        if (victimSession != null) {
            victimSession.ability().onDamageByEntity(playerContext(victim, victimSession.definition()), event, shooter, false);
        }
    }

    public boolean hasActiveAbilityOnTeam(GodTeam team, String abilityId) {
        if (team == null || abilityId == null) {
            return false;
        }
        for (Map.Entry<UUID, AbilitySession> entry : assignments.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player == null || plugin.game().teamOf(player) != team) {
                continue;
            }
            AbilitySession session = activeSession(player);
            if (session != null && abilityId.equalsIgnoreCase(session.definition().id())) {
                return true;
            }
        }
        return false;
    }

    public void runAttributedDamage(Player source, Runnable action) {
        if (source == null || action == null) {
            return;
        }
        UUID previous = synchronousDamageSource.get();
        synchronousDamageSource.set(source.getUniqueId());
        try {
            action.run();
        } finally {
            if (previous == null) {
                synchronousDamageSource.remove();
            } else {
                synchronousDamageSource.set(previous);
            }
        }
    }

    public Player currentAttributedDamageSource() {
        UUID uuid = synchronousDamageSource.get();
        return uuid == null ? null : plugin.getServer().getPlayer(uuid);
    }

    public void registerAttributedEntity(Entity entity, Player source) {
        if (entity != null && source != null) {
            attributedEntities.remember(entity.getUniqueId(), source.getUniqueId(), System.currentTimeMillis());
        }
    }

    public Player attributedEntitySource(Entity entity) {
        if (entity == null) {
            return null;
        }
        UUID sourceId = attributedEntities.resolve(entity.getUniqueId(), System.currentTimeMillis());
        return sourceId == null ? null : plugin.getServer().getPlayer(sourceId);
    }

    public void rememberDamageSource(Player victim, Player source) {
        if (victim == null || source == null || victim.equals(source)) {
            return;
        }
        recentDamageSources.remember(victim.getUniqueId(), source.getUniqueId(), System.currentTimeMillis());
    }

    public Player consumeRecentDamageSource(Player victim) {
        if (victim == null) {
            return null;
        }
        UUID sourceId = recentDamageSources.consume(victim.getUniqueId(), System.currentTimeMillis());
        return sourceId == null ? null : plugin.getServer().getPlayer(sourceId);
    }

    public void handleProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Arrow) && !(event.getEntity() instanceof Snowball)) {
            return;
        }
        Projectile projectile = (Projectile) event.getEntity();
        if (!(projectile.getShooter() instanceof Player)) {
            return;
        }
        Player player = (Player) projectile.getShooter();
        AbilitySession session = activeSession(player);
        if (session != null) {
            session.ability().onProjectileLaunch(playerContext(player, session.definition()), event);
        }
    }

    public void handleBlockBreak(Player player, BlockBreakEvent event) {
        AbilitySession session = activeSession(player);
        if (session != null) {
            session.ability().onBlockBreak(playerContext(player, session.definition()), event);
        }
    }

    public void handleBlockPlace(Player player, BlockPlaceEvent event) {
        AbilitySession session = activeSession(player);
        if (session != null) {
            session.ability().onBlockPlace(playerContext(player, session.definition()), event);
        }
    }

    public void handleBlockExplode(BlockExplodeEvent event) {
        for (Map.Entry<UUID, AbilitySession> entry : assignments.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player == null) {
                continue;
            }
            AbilitySession session = activeSession(player);
            if (session != null) {
                session.ability().onBlockExplode(event);
            }
        }
    }

    public void handleSignChange(Player player, SignChangeEvent event) {
        AbilitySession session = activeSession(player);
        if (session != null) {
            session.ability().onSignChange(playerContext(player, session.definition()), event);
        }
    }

    public void handleFoodLevelChange(Player player, FoodLevelChangeEvent event) {
        AbilitySession session = activeSession(player);
        if (session != null) {
            session.ability().onFoodLevelChange(playerContext(player, session.definition()), event);
        }
    }

    public void handleItemConsume(Player consumer, PlayerItemConsumeEvent event) {
        for (Map.Entry<UUID, AbilitySession> entry : assignments.entrySet()) {
            Player owner = plugin.getServer().getPlayer(entry.getKey());
            if (owner == null) {
                continue;
            }
            AbilitySession session = activeSession(owner);
            if (session != null) {
                session.ability().onItemConsume(playerContext(owner, session.definition()), event);
            }
        }
    }

    public void handleRegainHealth(Player player, EntityRegainHealthEvent event) {
        AbilitySession session = activeSession(player);
        if (session != null) {
            session.ability().onRegainHealth(playerContext(player, session.definition()), event);
        }
    }

    public void handleRespawn(Player player, PlayerRespawnEvent event) {
        AbilitySession session = activeSession(player);
        if (session != null) {
            session.ability().onRespawn(playerContext(player, session.definition()), event);
        }
    }

    public void handleMove(Player player, PlayerMoveEvent event) {
        AbilitySession session = activeSession(player);
        if (session != null) {
            session.ability().onMove(playerContext(player, session.definition()), event);
        }
    }

    public void handleChat(Player player, AsyncPlayerChatEvent event) {
        handleChatMessage(player, event.getMessage());
    }

    public void handleChatMessage(Player player, String message) {
        AbilitySession session = activeSession(player);
        if (session != null) {
            session.ability().onChatMessage(playerContext(player, session.definition()), message);
        }
    }

    public void handleFish(Player player, PlayerFishEvent event) {
        AbilitySession session = activeSession(player);
        if (session != null) {
            session.ability().onFish(playerContext(player, session.definition()), event);
        }
    }

    public void setTarget(Player player, CommandSender sender, String targetName) {
        if (isAbilitySuppressed(player)) {
            plugin.messages().send(sender, "&c능력이 봉인되어 타깃을 지정할 수 없습니다.");
            return;
        }
        AbilitySession session = activeSession(player);
        if (session == null) {
            plugin.messages().send(sender, "&c아직 능력이 없습니다.");
            return;
        }
        session.ability().setTarget(playerContext(player, session.definition()), sender, targetName);
    }

    private AbilityPlayerContext playerContext(Player player, AbilityDefinition definition) {
        return new AbilityPlayerContext(plugin, player, definition);
    }

    private AbilitySession activeSession(Player player) {
        if (isAbilitySuppressed(player)) {
            return null;
        }
        if (plugin.game() != null && !plugin.game().canUseAbility(player)) {
            return null;
        }
        return session(player);
    }

    private void deactivateSession(Player player, AbilitySession session) {
        session.ability().cancelScheduledTasks();
        session.ability().onRemove(playerContext(player, session.definition()));
        BukkitCompat.clearPotionEffects(player);
    }

    private void restoreSuppressedAbility(UUID uuid, Long expectedUntil) {
        Long current = suppressedUntil.get(uuid);
        if (current == null || !current.equals(expectedUntil)) {
            return;
        }
        suppressedUntil.remove(uuid);

        Player player = plugin.getServer().getPlayer(uuid);
        if (player == null) {
            return;
        }
        AbilitySession session = activeSession(player);
        if (session != null) {
            session.ability().onAssign(playerContext(player, session.definition()));
            player.sendMessage(ChatColor.LIGHT_PURPLE + "봉인되었던 능력이 돌아왔습니다.");
            if (plugin.game() != null) {
                plugin.game().refreshPlayerDisplay(player);
            }
        }
    }

    private AbilityDefinition chooseDiverseRandom(List<AbilityDefinition> enabled, Player player) {
        if (enabled.isEmpty()) {
            return registry.get("ares");
        }

        UUID playerId = player.getUniqueId();
        String currentAbilityId = null;
        Map<String, Integer> counts = new ConcurrentHashMap<String, Integer>();
        for (Map.Entry<UUID, AbilitySession> entry : assignments.entrySet()) {
            String abilityId = entry.getValue().definition().id();
            if (entry.getKey().equals(playerId)) {
                currentAbilityId = abilityId;
                continue;
            }
            Integer count = counts.get(abilityId);
            counts.put(abilityId, count == null ? 1 : count.intValue() + 1);
        }

        int lowestCount = Integer.MAX_VALUE;
        for (AbilityDefinition definition : enabled) {
            int count = countFor(counts, definition.id());
            if (count < lowestCount) {
                lowestCount = count;
            }
        }

        List<AbilityDefinition> pool = new ArrayList<AbilityDefinition>();
        for (AbilityDefinition definition : enabled) {
            if (countFor(counts, definition.id()) == lowestCount) {
                pool.add(definition);
            }
        }

        pool = withoutAbility(pool, currentAbilityId);
        pool = withoutRecentAbilities(pool, enabled.size());
        return pool.get(0);
    }

    private int countFor(Map<String, Integer> counts, String abilityId) {
        Integer count = counts.get(abilityId);
        return count == null ? 0 : count.intValue();
    }

    private List<AbilityDefinition> withoutAbility(List<AbilityDefinition> source, String abilityId) {
        if (abilityId == null || source.size() <= 1) {
            return shuffledCopy(source);
        }
        List<AbilityDefinition> filtered = new ArrayList<AbilityDefinition>();
        for (AbilityDefinition definition : source) {
            if (!definition.id().equals(abilityId)) {
                filtered.add(definition);
            }
        }
        return filtered.isEmpty() ? shuffledCopy(source) : shuffledCopy(filtered);
    }

    private List<AbilityDefinition> withoutRecentAbilities(List<AbilityDefinition> source, int enabledCount) {
        if (source.size() <= 1 || enabledCount <= 2) {
            return shuffledCopy(source);
        }

        List<String> recent = recentRandomAbilitySnapshot(Math.min(source.size() - 1, Math.max(1, enabledCount / 4)));
        if (recent.isEmpty()) {
            return shuffledCopy(source);
        }

        List<AbilityDefinition> filtered = new ArrayList<AbilityDefinition>();
        for (AbilityDefinition definition : source) {
            if (!recent.contains(definition.id())) {
                filtered.add(definition);
            }
        }
        return filtered.isEmpty() ? shuffledCopy(source) : shuffledCopy(filtered);
    }

    private List<AbilityDefinition> shuffledCopy(List<AbilityDefinition> source) {
        List<AbilityDefinition> copy = new ArrayList<AbilityDefinition>(source);
        Collections.shuffle(copy, random);
        return copy;
    }

    private List<String> recentRandomAbilitySnapshot(int limit) {
        List<String> recent = new ArrayList<String>();
        synchronized (recentRandomAbilityIds) {
            int start = Math.max(0, recentRandomAbilityIds.size() - limit);
            for (int i = start; i < recentRandomAbilityIds.size(); i++) {
                recent.add(recentRandomAbilityIds.get(i));
            }
        }
        return recent;
    }

    private void rememberRandomAbility(AbilityDefinition definition) {
        if (definition == null) {
            return;
        }
        synchronized (recentRandomAbilityIds) {
            recentRandomAbilityIds.add(definition.id());
            int limit = Math.max(8, registry.all().size() / 3);
            while (recentRandomAbilityIds.size() > limit) {
                recentRandomAbilityIds.remove(0);
            }
        }
    }

    private void loadRegistrars() {
        int count = 0;
        for (AbilityRegistrar registrar : ServiceLoader.load(AbilityRegistrar.class, plugin.getClass().getClassLoader())) {
            registrar.registerAbilities(registry);
            count++;
        }
        plugin.getLogger().info("Loaded " + registry.all().size() + " abilities from " + count + " registrar(s).");
    }
}
