package kr.newgodwar.regression;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.ability.AbilitySession;
import kr.newgodwar.ability.api.*;
import kr.newgodwar.game.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitScheduler;
import java.lang.reflect.*;
import java.util.*;

/** Real Paper item metadata and registries with deterministic participant/event routing. */
final class SiksinFoodRegressionChecks {
    private final NewGodWarPlugin core;
    private final List<Actor> actors = new ArrayList<Actor>();
    private final Map<Integer, Runnable> pending = new LinkedHashMap<Integer, Runnable>();
    private Map<UUID, GodTeam> teams;
    private Map<UUID, AbilitySession> assignments;
    private Actor owner, ally, enemy, outsider;
    private GodAbility ability;
    private AbilityDefinition definition;
    private Method createFood;
    private Object[] buffs;
    private World world;
    private int nextTask;

    SiksinFoodRegressionChecks(NewGodWarPlugin core) { this.core = core; }

    @SuppressWarnings("unchecked")
    void run() throws Exception {
        Server original = Bukkit.getServer();
        Field serverField = field(Bukkit.class, "server");
        Field pluginServerField = field(org.bukkit.plugin.java.JavaPlugin.class, "server");
        Object originalPluginServer = pluginServerField.get(core);
        teams = (Map<UUID, GodTeam>) field(GameManager.class, "teams").get(core.game());
        assignments = (Map<UUID, AbilitySession>) field(core.abilities().getClass(), "assignments").get(core.abilities());
        Object effectsConfig = core.getConfig().get("abilities.effects.enabled");
        core.getConfig().set("abilities.effects.enabled", false);
        world = original.getWorlds().get(0);
        owner = new Actor("FoodChef"); ally = new Actor("FoodAlly");
        enemy = new Actor("FoodEnemy"); outsider = new Actor("FoodOutsider");
        definition = core.abilities().registry().get("siksin");
        ability = definition.create();
        new AbilitySession(definition, ability);
        new AbilityPlayerContext(core, owner.player, definition);
        Class<?> buffType = Class.forName(ability.getClass().getName() + "$BuffKind", true, core.getClass().getClassLoader());
        buffs = buffType.getEnumConstants();
        createFood = ability.getClass().getDeclaredMethod("createFood", Player.class, boolean.class, buffType);
        createFood.setAccessible(true);
        // Resolve legacy event/material transformations before substituting the server lookup.
        new PlayerItemConsumeEvent(owner.player, new ItemStack(Material.BREAD));
        food(false, buffs[0]);
        BukkitScheduler scheduler = proxy(BukkitScheduler.class, (p, m, a) -> {
            if (m.getName().equals("scheduleSyncDelayedTask")) {
                require(((Number) a[2]).longValue() == 1L, "Food must resolve on the following tick");
                pending.put(++nextTask, (Runnable) a[1]); return nextTask;
            }
            if (m.getName().equals("cancelTask")) { pending.remove(a[0]); return null; }
            return invoke(original.getScheduler(), m, a);
        });
        try {
            Server fixtureServer = proxy(Server.class, (p, m, a) -> {
                if (m.getName().equals("getScheduler")) return scheduler;
                if (m.getName().equals("getOnlinePlayers")) {
                    List<Player> online = new ArrayList<Player>();
                    for (Actor actor : actors) if (actor.online) online.add(actor.player);
                    return online;
                }
                if (m.getName().equals("getPlayer") || m.getName().equals("getPlayerExact")) {
                    for (Actor actor : actors) if (actor.online && (actor.id.equals(a[0]) || actor.name.equals(a[0]))) return actor.player;
                    return null;
                }
                return invoke(original, m, a);
            });
            serverField.set(null, fixtureServer);
            pluginServerField.set(core, fixtureServer);
            verifyCatalogue();
            for (Object buff : buffs) {
                reset(); consume(owner, food(false, buff)); advance();
                require(owner.effects.size() == 1 && ally.effects.isEmpty(), "Solo food failed to target only its chef: " + buff);
                reset(); consume(ally, food(true, buff));
                require(owner.effects.isEmpty() && ally.effects.isEmpty(), "Food applied before consumption was confirmed");
                advance();
                require(owner.effects.size() == 1 && ally.effects.size() == 1 && enemy.effects.isEmpty() && outsider.effects.isEmpty(),
                    "Team food recipients differ: " + buff);
                PotionEffect first = owner.effects.values().iterator().next();
                PotionEffect second = ally.effects.values().iterator().next();
                require(first.getType().equals(second.getType()) && first.getDuration() == second.getDuration()
                    && first.getAmplifier() == 0, "Shared meal did not apply the same buff");
            }
            reset(); consume(ally, food(false, buffs[0])); advance(); requireNone("Solo meal buffed a teammate");
            reset(); consume(enemy, food(true, buffs[0])); advance(); requireNone("Shared meal buffed an enemy");
            reset(); consume(outsider, food(true, buffs[0])); advance(); requireNone("Shared meal buffed a nonparticipant");
            reset(); ally.dead = true; consume(ally, food(true, buffs[0])); advance(); requireNone("Meal buffed a dead teammate");
            reset(); owner.dead = true; consume(ally, food(true, buffs[0])); advance(); requireNone("Meal survived its chef's death");
            reset(); ally.mode = GameMode.SPECTATOR; consume(ally, food(true, buffs[0])); advance(); requireNone("Meal buffed a spectator");
            reset(); ally.world = proxy(World.class, (p, m, a) -> defaultValue(m.getReturnType()));
            consume(ally, food(true, buffs[0])); advance(); requireNone("Meal crossed world boundaries");
            reset(); PlayerItemConsumeEvent cancelled = consume(ally, food(true, buffs[0])); cancelled.setCancelled(true);
            advance(); requireNone("Later event cancellation still granted meal buffs");
            reset(); PlayerItemConsumeEvent replaced = consume(ally, food(true, buffs[0])); replaced.setItem(new ItemStack(Material.BREAD));
            advance(); requireNone("Replaced consumption item retained the original meal's buff");
            reset(); consume(ally, food(true, buffs[0])); teams.put(ally.id, GodTeam.BLUE);
            advance(); requireNone("Meal ignored a team change before consumption confirmation");
            reset(); consume(ally, food(true, buffs[0])); owner.online = false;
            advance(); requireNone("Meal granted a buff after its chef disconnected");
            reset(); ItemStack stale = food(true, buffs[0]);
            ability = definition.create(); assignments.put(owner.id, new AbilitySession(definition, ability));
            consume(ally, stale); advance(); requireNone("Old session meal granted a buff after ability reassignment");
            reset(); ItemStack malformed = food(true, buffs[0]);
            ItemMeta metadata = malformed.getItemMeta(); List<String> lore = new ArrayList<String>(metadata.getLore());
            Method readMarker = ability.getClass().getDeclaredMethod("storedMarker", ItemMeta.class); readMarker.setAccessible(true);
            Method writeMarker = ability.getClass().getDeclaredMethod("storeMarker", ItemMeta.class, String.class); writeMarker.setAccessible(true);
            String storedMarker = (String) readMarker.invoke(ability, metadata);
            if (storedMarker != null) writeMarker.invoke(ability, metadata, storedMarker.replace(":TEAM:", ":INVALID:"));
            else for (int i = 0; i < lore.size(); i++) lore.set(i, lore.get(i).replace(":TEAM:", ":INVALID:"));
            metadata.setLore(lore); malformed.setItemMeta(metadata);
            consume(ally, malformed); advance(); requireNone("Malformed recipient mode became valid food");
            reset(); ItemStack wrongMaterial = food(true, buffs[0]); wrongMaterial.setType(Material.APPLE);
            consume(ally, wrongMaterial); advance(); requireNone("Unrelated item with food lore applied buffs");
            reset(); owner.effects.put(PotionEffectType.SPEED, new PotionEffect(PotionEffectType.SPEED, 500, 2));
            ally.effects.put(PotionEffectType.SPEED, new PotionEffect(PotionEffectType.SPEED, 600, 0));
            consume(ally, food(true, buffs[0])); advance();
            require(owner.effects.get(PotionEffectType.SPEED).getAmplifier() == 2
                && owner.effects.get(PotionEffectType.SPEED).getDuration() == 500
                && ally.effects.get(PotionEffectType.SPEED).getDuration() == 600,
                "Food weakened or shortened a preexisting buff");
            require(owner.messages.toString().contains("기존") && ally.messages.toString().contains("기존"),
                "Food claimed a new effect when preserving the previous buff");
            reset(); owner.rejectEffects = true; consume(owner, food(false, buffs[0])); advance();
            requireNone("Rejected potion effect unexpectedly applied");
            require(owner.messages.toString().contains("적용되지"), "Rejected potion effect claimed success");
            core.getLogger().info("PASS Siksin food: six named models, 12 recipient cases, deferred cancellation, teams/world/liveness, session binding, malformed metadata and stronger/longer effect preservation");
        } finally {
            if (ability != null) ability.cancelScheduledTasks();
            pluginServerField.set(core, originalPluginServer);
            serverField.set(null, original);
            for (Actor actor : actors) { teams.remove(actor.id); assignments.remove(actor.id); }
            core.getConfig().set("abilities.effects.enabled", effectsConfig);
        }
    }

    private void verifyCatalogue() throws Exception {
        Set<String> names = new HashSet<String>(); Set<Material> materials = new HashSet<Material>();
        for (int i = 0; i < buffs.length; i++) {
            ItemStack meal = food(false, buffs[i]);
            require(names.add(meal.getItemMeta().getDisplayName()) && materials.add(meal.getType()), "Food identities are duplicated");
            require(meal.getItemMeta().getLore().toString().contains("자신만"), "Food does not explain who may use it");
            try {
                int model = (Integer) ItemMeta.class.getMethod("getCustomModelData").invoke(meal.getItemMeta());
                require(model == 73101 + i, "Food custom model does not match its named texture");
                require(!meal.getItemMeta().getLore().toString().contains("NGW_SIKSIN_FOOD"), "Modern food leaked its internal token into the tooltip");
            } catch (NoSuchMethodException legacy) { /* Ordinary edible materials on 1.12. */ }
        }
        require(names.size() == 6, "Expected six different named foods");
    }

    private void reset() {
        ability.cancelScheduledTasks(); pending.clear();
        ability = definition.create(); assignments.put(owner.id, new AbilitySession(definition, ability));
        for (Actor actor : actors) {
            actor.effects.clear(); actor.messages.clear(); actor.online = true; actor.dead = false;
            actor.world = world; actor.mode = GameMode.SURVIVAL; actor.rejectEffects = false;
        }
        teams.put(owner.id, GodTeam.RED); teams.put(ally.id, GodTeam.RED); teams.put(enemy.id, GodTeam.BLUE); teams.remove(outsider.id);
    }
    private ItemStack food(boolean shared, Object buff) throws Exception { return (ItemStack) createFood.invoke(ability, owner.player, shared, buff); }
    private PlayerItemConsumeEvent consume(Actor eater, ItemStack item) {
        PlayerItemConsumeEvent event = new PlayerItemConsumeEvent(eater.player, item);
        core.abilities().handleItemConsume(eater.player, event); return event;
    }
    private void advance() { for (Runnable action : new ArrayList<Runnable>(pending.values())) action.run(); pending.clear(); }
    private void requireNone(String message) { for (Actor actor : actors) require(actor.effects.isEmpty(), message); }

    private final class Actor {
        final UUID id = UUID.randomUUID(); final String name; final Player player;
        final Map<PotionEffectType, PotionEffect> effects = new HashMap<PotionEffectType, PotionEffect>();
        final List<String> messages = new ArrayList<String>();
        World world = SiksinFoodRegressionChecks.this.world;
        boolean online = true, dead, rejectEffects; GameMode mode = GameMode.SURVIVAL;
        Actor(String name) {
            this.name = name;
            player = proxy(Player.class, (p, m, a) -> {
                switch (m.getName()) {
                    case "getUniqueId": return id;
                    case "getName": return name;
                    case "isOnline": return online;
                    case "isDead": return dead;
                    case "getGameMode": return mode;
                    case "getWorld": return world;
                    case "getLocation": return new Location(world, this == owner ? 0 : 500, 100, 0);
                    case "getHealth": return dead ? 0D : 20D;
                    case "getMaxHealth": return 20D;
                    case "getPotionEffect": return effects.get(a[0]);
                    case "getActivePotionEffects": return new ArrayList<PotionEffect>(effects.values());
                    case "addPotionEffect":
                        if (rejectEffects) return false;
                        PotionEffect effect = (PotionEffect) a[0]; effects.put(effect.getType(), effect); return true;
                    case "sendMessage":
                        if (a[0] instanceof String[]) messages.addAll(Arrays.asList((String[]) a[0])); else messages.add((String) a[0]);
                        return null;
                    default: return defaultValue(m.getReturnType());
                }
            }); actors.add(this);
        }
    }
    private static Field field(Class<?> type, String name) throws Exception { Field f = type.getDeclaredField(name); f.setAccessible(true); return f; }
    private static Object invoke(Object target, Method method, Object[] args) throws Throwable {
        try { return method.invoke(target, args); } catch (InvocationTargetException failure) { throw failure.getCause(); }
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (p, m, a) -> {
            if (m.getName().equals("equals")) return p == a[0];
            if (m.getName().equals("hashCode")) return System.identityHashCode(p);
            if (m.getName().equals("toString")) return type.getSimpleName() + "FoodFixture";
            return handler.invoke(p, m, a);
        }));
    }
    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive() || type == void.class) return null;
        if (type == boolean.class) return false; if (type == double.class) return 0D;
        if (type == float.class) return 0F; if (type == long.class) return 0L;
        if (type == byte.class) return (byte) 0; if (type == short.class) return (short) 0;
        if (type == char.class) return (char) 0; return 0;
    }
}
