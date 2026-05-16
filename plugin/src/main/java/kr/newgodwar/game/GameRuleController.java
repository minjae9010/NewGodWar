package kr.newgodwar.game;

import kr.newgodwar.NewGodWarPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

public final class GameRuleController {

    private final NewGodWarPlugin plugin;
    private final Map<String, Map<String, String>> previousValues = new HashMap<String, Map<String, String>>();
    private static final Map<String, String> LEGACY_RULE_NAMES = legacyRuleNames();

    public GameRuleController(NewGodWarPlugin plugin) {
        this.plugin = plugin;
    }

    public void applyConfiguredRules() {
        if (!plugin.getConfig().getBoolean("gamerules.enabled", true)) {
            return;
        }
        ConfigurationSection rules = plugin.getConfig().getConfigurationSection("gamerules.rules");
        if (rules == null) {
            return;
        }

        previousValues.clear();
        for (World world : Bukkit.getWorlds()) {
            Map<String, String> worldValues = new HashMap<String, String>();
            for (String rule : rules.getKeys(false)) {
                String resolvedRule = resolveRuleName(world, rule);
                if (resolvedRule == null) {
                    plugin.getLogger().warning("Unknown or unsupported gamerule: " + rule);
                    continue;
                }
                String value = String.valueOf(rules.get(rule));
                String resolvedValue = resolveRuleValue(rule, resolvedRule, value);
                String previous = getGameRuleValue(world, resolvedRule);
                if (previous != null) {
                    worldValues.put(resolvedRule, previous);
                }
                if (!setGameRuleValue(world, resolvedRule, resolvedValue)) {
                    plugin.getLogger().warning("Failed to set gamerule " + rule + " (" + resolvedRule + ") to " + resolvedValue);
                }
            }
            previousValues.put(world.getName(), worldValues);
        }
    }

    public void restorePreviousRules() {
        if (!plugin.getConfig().getBoolean("gamerules.restore-on-stop", true)) {
            previousValues.clear();
            return;
        }

        for (World world : Bukkit.getWorlds()) {
            Map<String, String> worldValues = previousValues.get(world.getName());
            if (worldValues == null) {
                continue;
            }
            for (Map.Entry<String, String> entry : worldValues.entrySet()) {
                setGameRuleValue(world, entry.getKey(), entry.getValue());
            }
        }
        previousValues.clear();
    }

    private String resolveRuleName(World world, String rule) {
        if (rule == null) {
            return null;
        }
        if (isGameRule(world, rule)) {
            return rule;
        }

        String withoutNamespace = stripMinecraftNamespace(rule);
        if (!withoutNamespace.equals(rule) && isGameRule(world, withoutNamespace)) {
            return withoutNamespace;
        }

        String legacyName = LEGACY_RULE_NAMES.get(withoutNamespace);
        if (legacyName != null) {
            if (isGameRule(world, legacyName)) {
                return legacyName;
            }
            String namespacedLegacyName = "minecraft:" + legacyName;
            if (isGameRule(world, namespacedLegacyName)) {
                return namespacedLegacyName;
            }
        }

        String normalizedRule = normalizeRuleName(withoutNamespace);
        String normalizedLegacyName = legacyName == null ? null : normalizeRuleName(legacyName);
        for (String availableRule : world.getGameRules()) {
            String availableWithoutNamespace = stripMinecraftNamespace(availableRule);
            String normalizedAvailableRule = normalizeRuleName(availableWithoutNamespace);
            if (availableRule.equalsIgnoreCase(rule)
                || availableWithoutNamespace.equalsIgnoreCase(withoutNamespace)
                || normalizedAvailableRule.equals(normalizedRule)
                || normalizedAvailableRule.equals(normalizedLegacyName)) {
                return availableRule;
            }
        }
        return null;
    }

    private String resolveRuleValue(String configuredRule, String resolvedRule, String value) {
        if ("disableElytraMovementCheck".equals(stripMinecraftNamespace(configuredRule))
            && "elytra_movement_check".equals(stripMinecraftNamespace(resolvedRule))) {
            if ("true".equalsIgnoreCase(value)) {
                return "false";
            }
            if ("false".equalsIgnoreCase(value)) {
                return "true";
            }
        }
        return value;
    }

    private boolean isGameRule(World world, String rule) {
        try {
            return world.isGameRule(rule);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private String getGameRuleValue(World world, String rule) {
        try {
            return world.getGameRuleValue(rule);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Failed to read gamerule " + rule + ": " + ex.getMessage());
            return null;
        }
    }

    private boolean setGameRuleValue(World world, String rule, String value) {
        try {
            return world.setGameRuleValue(rule, value);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Failed to set gamerule " + rule + ": " + ex.getMessage());
            return false;
        }
    }

    private static String stripMinecraftNamespace(String rule) {
        return rule.startsWith("minecraft:") ? rule.substring("minecraft:".length()) : rule;
    }

    private static String normalizeRuleName(String rule) {
        if (rule == null) {
            return "";
        }
        return rule.replace("_", "").replace("-", "").toLowerCase();
    }

    private static Map<String, String> legacyRuleNames() {
        Map<String, String> names = new HashMap<String, String>();
        names.put("announceAdvancements", "show_advancement_messages");
        names.put("commandBlockOutput", "command_block_output");
        names.put("commandBlocksEnabled", "command_blocks_work");
        names.put("disableElytraMovementCheck", "elytra_movement_check");
        names.put("doDaylightCycle", "advance_time");
        names.put("doEntityDrops", "entity_drops");
        names.put("doImmediateRespawn", "immediate_respawn");
        names.put("doInsomnia", "spawn_phantoms");
        names.put("doLimitedCrafting", "limited_crafting");
        names.put("doMobLoot", "mob_drops");
        names.put("doMobSpawning", "spawn_mobs");
        names.put("doPatrolSpawning", "spawn_patrols");
        names.put("doTileDrops", "block_drops");
        names.put("doTraderSpawning", "spawn_wandering_traders");
        names.put("doVinesSpread", "spread_vines");
        names.put("doWardenSpawning", "spawn_wardens");
        names.put("keepInventory", "keep_inventory");
        names.put("logAdminCommands", "log_admin_commands");
        names.put("maxCommandChainLength", "max_command_sequence_length");
        names.put("maxEntityCramming", "max_entity_cramming");
        names.put("mobGriefing", "mob_griefing");
        names.put("naturalRegeneration", "natural_health_regeneration");
        names.put("playersSleepingPercentage", "players_sleeping_percentage");
        names.put("randomTickSpeed", "random_tick_speed");
        names.put("reducedDebugInfo", "reduced_debug_info");
        names.put("sendCommandFeedback", "send_command_feedback");
        names.put("showDeathMessages", "show_death_messages");
        names.put("spawnRadius", "respawn_radius");
        names.put("spectatorsGenerateChunks", "spectators_generate_chunks");
        names.put("universalAnger", "universal_anger");
        return names;
    }
}
