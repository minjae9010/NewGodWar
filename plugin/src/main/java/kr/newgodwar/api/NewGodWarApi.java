package kr.newgodwar.api;

import kr.newgodwar.NewGodWarPlugin;
import kr.newgodwar.ability.api.AbilityDefinition;
import kr.newgodwar.ability.api.AbilityInfo;
import kr.newgodwar.ability.api.GodAbility;
import kr.newgodwar.game.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** API v1. Registration and game mutations must run on the server thread. */
public final class NewGodWarApi implements Listener {
    public static final int VERSION = 1;
    private final NewGodWarPlugin plugin;
    private final Map<Plugin, Set<String>> ownedAbilities = new HashMap<Plugin, Set<String>>();
    private final Map<String, GameMode> modes = new HashMap<String, GameMode>();
    private final Map<String, Plugin> modeOwners = new HashMap<String, Plugin>();

    public void registerGameMode(Plugin owner, String id, GameMode mode) {
        checkOwner(owner);
        if (id == null || !id.matches("[a-z0-9_-]+") || "default".equals(id) || mode == null) {
            throw new IllegalArgumentException("A non-default lowercase mode id and implementation are required");
        }
        if (modes.containsKey(id)) throw new IllegalArgumentException("Duplicate game mode: " + id);
        modes.put(id, mode);
        modeOwners.put(id, owner);
    }

    /** Resolves game.mode at each start; unknown modes fail instead of starting default rules. */
    public GameMode configuredGameMode() {
        String id = plugin.getConfig().getString("game.mode", "default").trim().toLowerCase(java.util.Locale.ROOT);
        if ("default".equals(id)) return null;
        GameMode mode = modes.get(id);
        if (mode == null) throw new IllegalStateException("게임 모드 애드온을 찾을 수 없습니다: " + id);
        return mode;
    }

    private void removeModes(Plugin owner) {
        for (String id : new HashSet<String>(modeOwners.keySet())) {
            if (modeOwners.get(id) == owner) {
                GameMode mode = modes.remove(id);
                modeOwners.remove(id);
                plugin.game().stopMode(mode);
            }
        }
    }

    public NewGodWarApi(NewGodWarPlugin plugin) {
        this.plugin = plugin;
    }

    public GameManager game() {
        return plugin.game();
    }

    public void registerAbility(Plugin owner, Class<? extends GodAbility> abilityClass) {
        checkOwner(owner);
        if (plugin.abilities().registry().isRegistered(abilityClass)) {
            throw new IllegalArgumentException("Ability class already registered: " + abilityClass);
        }
        plugin.abilities().registry().register(abilityClass);
        remember(owner, abilityClass.getAnnotation(AbilityInfo.class).id());
    }

    public void registerAbility(Plugin owner, AbilityDefinition definition) {
        checkOwner(owner);
        plugin.abilities().registry().register(definition);
        remember(owner, definition.id());
    }

    /** Also called automatically when the owning Bukkit plugin is disabled. */
    public void unregisterAbilities(Plugin owner) {
        checkThread();
        Set<String> ids = ownedAbilities.remove(owner);
        if (ids != null) {
            // Remove registry entries first so cleanup hooks cannot select them again.
            for (String id : ids) {
                plugin.abilities().registry().unregister(id);
            }
            plugin.abilities().removeDefinitions(ids);
        }
    }

    public void shutdown() {
        for (Plugin owner : new HashSet<Plugin>(modeOwners.values())) removeModes(owner);
        for (Plugin owner : new HashSet<Plugin>(ownedAbilities.keySet())) {
            unregisterAbilities(owner);
        }
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        removeModes(event.getPlugin());
        unregisterAbilities(event.getPlugin());
    }

    private void remember(Plugin owner, String id) {
        ownedAbilities.computeIfAbsent(owner, ignored -> new HashSet<String>())
            .add(id.trim().toLowerCase(java.util.Locale.ROOT));
    }

    private void checkOwner(Plugin owner) {
        checkThread();
        if (!plugin.isEnabled() || owner == null || owner == plugin || !owner.isEnabled()) {
            throw new IllegalArgumentException("An enabled addon plugin is required.");
        }
    }

    private void checkThread() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("NewGodWar API requires the server thread.");
        }
    }
}
